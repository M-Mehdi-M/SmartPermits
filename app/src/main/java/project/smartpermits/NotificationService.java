package project.smartpermits;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import project.smartpermits.api.RetrofitClient;
import project.smartpermits.api.SocketIOManager;

/**
 * Foreground service that keeps the app process and the Socket.IO connection alive
 * even when the user has closed (swiped away) the app, so real-time notifications
 * from the opposite side (inspector <-> citizen) are still delivered.
 *
 * The whole system is LAN Socket.IO to a local Flask server, so instead of FCM we
 * keep a long-lived connection. The existing global receiver in {@link SmartPermitsApp}
 * turns the socket events into system notifications; this service just guarantees the
 * process stays alive to receive them.
 */
public class NotificationService extends Service {

    private static final String TAG = "NotificationService";
    public static final int SERVICE_NOTIFICATION_ID = 1001;

    /** Start the background listener if the user is logged in. Safe to call repeatedly. */
    public static void start(Context context) {
        try {
            if (!RetrofitClient.getInstance(context).isLoggedIn()) return;
            Intent intent = new Intent(context.getApplicationContext(), NotificationService.class);
            ContextCompat.startForegroundService(context.getApplicationContext(), intent);
        } catch (Exception e) {
            Log.e(TAG, "start error: " + e.getMessage());
        }
    }

    /** Stop the background listener (e.g. on logout / session expiry). */
    public static void stop(Context context) {
        try {
            context.getApplicationContext()
                    .stopService(new Intent(context.getApplicationContext(), NotificationService.class));
        } catch (Exception e) {
            Log.e(TAG, "stop error: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Must call startForeground promptly after startForegroundService(), even if we
        // immediately stop, otherwise the system throws.
        startForegroundCompat();

        RetrofitClient client = RetrofitClient.getInstance(this);
        int userId = client.getUserId();
        if (!client.isLoggedIn() || userId <= 0) {
            stopForegroundCompat();
            stopSelf();
            return START_NOT_STICKY;
        }

        // Ensure the socket is connected (no-op if already connected for this user).
        SocketIOManager.getInstance(this).connect(userId);
        // START_STICKY: if the system kills us under memory pressure, restart when possible.
        return START_STICKY;
    }

    private void startForegroundCompat() {
        android.app.Notification notification = NotificationHelper.buildServiceNotification(this);
        int type = 0;
        if (Build.VERSION.SDK_INT >= 34) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
        }
        try {
            ServiceCompat.startForeground(this, SERVICE_NOTIFICATION_ID, notification, type);
        } catch (Exception e) {
            Log.e(TAG, "startForeground error: " + e.getMessage());
        }
    }

    private void stopForegroundCompat() {
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // App swiped from recents: keep the listener alive so notifications keep coming.
        // Re-request a start so START_STICKY semantics survive task removal on stricter OEMs.
        try {
            if (RetrofitClient.getInstance(this).isLoggedIn()) {
                ContextCompat.startForegroundService(getApplicationContext(),
                        new Intent(getApplicationContext(), NotificationService.class));
            }
        } catch (Exception ignored) {
        }
        super.onTaskRemoved(rootIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
