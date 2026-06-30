package project.smartpermits.api;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class SocketIOManager {

    private static final String TAG = "SocketIOManager";
    private static SocketIOManager instance;
    private Socket socket;
    private Context context;
    private int userId;
    private final Set<Integer> joinedPermitRooms = new HashSet<>();

    private SocketIOManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized SocketIOManager getInstance(Context context) {
        if (instance == null) {
            instance = new SocketIOManager(context);
        }
        return instance;
    }

    public void connect(int userId) {
        if (userId <= 0) return;
        if (socket != null && socket.connected() && this.userId == userId) return;

        try {
            if (socket != null) {
                socket.off();
                socket.disconnect();
                socket = null;
            }

            this.userId = userId;

            IO.Options options = new IO.Options();
            options.reconnection = true;
            options.reconnectionDelay = 1000;
            options.reconnectionDelayMax = 5000;
            options.reconnectionAttempts = Integer.MAX_VALUE;
            options.path = "/socket.io/";

            java.net.URI uri = new java.net.URI(ApiConfig.getSocketUrl(context) + "?user_id=" + userId);
            socket = IO.socket(uri, options);

            socket.on(Socket.EVENT_CONNECT, onConnect);
            socket.on(Socket.EVENT_DISCONNECT, onDisconnect);
            socket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
            socket.on("new_permit_submitted", onNewPermitSubmitted);
            socket.on("permit_status_updated", onPermitStatusUpdated);
            socket.on("permit_reviewed", onPermitReviewed);
            socket.on("new_comment", onNewComment);
            socket.on("comment_notification", onCommentNotification);
            socket.on("appointment_scheduled", onAppointmentScheduled);
            socket.on("appointment_status_updated", onAppointmentStatusUpdated);

            socket.connect();
            Log.d(TAG, "Connecting socket for user " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Socket connection error: " + e.getMessage());
        }
    }

    public void joinPermitRoom(int permitId) {
        if (permitId <= 0) return;
        joinedPermitRooms.add(permitId);
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("permit_id", permitId);
                socket.emit("join_permit_room", data);
            } catch (JSONException e) {
                Log.e(TAG, "joinPermitRoom error: " + e.getMessage());
            }
        }
    }

    public void leavePermitRoom(int permitId) {
        joinedPermitRooms.remove(permitId);
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("permit_id", permitId);
                socket.emit("leave_permit_room", data);
            } catch (JSONException e) {
                Log.e(TAG, "leavePermitRoom error: " + e.getMessage());
            }
        }
    }

    public void disconnect() {
        if (socket != null) {
            socket.off();
            socket.disconnect();
            socket = null;
        }
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    private void sendBroadcast(String action) {
        Intent intent = new Intent(action);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    private final Emitter.Listener onConnect = args -> {
        Log.d(TAG, "Socket connected");
        for (Integer pid : joinedPermitRooms) {
            try {
                JSONObject d = new JSONObject();
                d.put("permit_id", pid);
                socket.emit("join_permit_room", d);
            } catch (JSONException e) {
                Log.e(TAG, "rejoin room error: " + e.getMessage());
            }
        }
        sendBroadcast("project.smartpermits.SOCKET_CONNECTED");
    };

    private final Emitter.Listener onDisconnect = args -> {
        Log.d(TAG, "Socket disconnected");
    };

    private final Emitter.Listener onConnectError = args -> {
        String msg = (args != null && args.length > 0 && args[0] != null) ? args[0].toString() : "unknown";
        Log.e(TAG, "Socket connect error: " + msg);
    };

    private final Emitter.Listener onNewPermitSubmitted = args -> {
        try {
            JSONObject data = (JSONObject) args[0];
            Intent intent = new Intent("project.smartpermits.NEW_PERMIT");
            intent.setPackage(context.getPackageName());
            intent.putExtra("permit_data", data.toString());
            context.sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "onNewPermitSubmitted error: " + e.getMessage());
        }
    };

    private final Emitter.Listener onPermitStatusUpdated = args -> {
        try {
            JSONObject data = (JSONObject) args[0];
            Intent intent = new Intent("project.smartpermits.PERMIT_STATUS_UPDATED");
            intent.setPackage(context.getPackageName());
            intent.putExtra("permit_id", data.optInt("permit_id", -1));
            intent.putExtra("status", data.optString("status", ""));
            intent.putExtra("reviewer_name", data.optString("reviewer_name", ""));
            intent.putExtra("notes", data.optString("notes", ""));
            context.sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "onPermitStatusUpdated error: " + e.getMessage());
        }
    };

    private final Emitter.Listener onPermitReviewed = args -> {
        try {
            JSONObject data = (JSONObject) args[0];
            Intent intent = new Intent("project.smartpermits.PERMIT_REVIEWED");
            intent.setPackage(context.getPackageName());
            intent.putExtra("permit_data", data.toString());
            context.sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "onPermitReviewed error: " + e.getMessage());
        }
    };

    private final Emitter.Listener onNewComment = args -> {
        try {
            JSONObject data = (JSONObject) args[0];
            int permitId = data.optInt("permit_id", -1);
            JSONObject comment = data.optJSONObject("comment");
            Intent intent = new Intent("project.smartpermits.NEW_COMMENT");
            intent.setPackage(context.getPackageName());
            intent.putExtra("permit_id", permitId);
            if (comment != null) intent.putExtra("comment_data", comment.toString());
            context.sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "onNewComment error: " + e.getMessage());
        }
    };

    private final Emitter.Listener onCommentNotification = args -> {
        try {
            JSONObject data = (JSONObject) args[0];
            Intent intent = new Intent("project.smartpermits.COMMENT_NOTIFICATION");
            intent.setPackage(context.getPackageName());
            intent.putExtra("permit_id", data.optInt("permit_id", -1));
            intent.putExtra("commenter_name", data.optString("commenter_name", ""));
            intent.putExtra("message_preview", data.optString("message_preview", ""));
            context.sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "onCommentNotification error: " + e.getMessage());
        }
    };

    private final Emitter.Listener onAppointmentScheduled = args -> {
        try {
            JSONObject data = (JSONObject) args[0];
            JSONObject appt = data.optJSONObject("appointment");
            Intent intent = new Intent("project.smartpermits.APPOINTMENT_SCHEDULED");
            intent.setPackage(context.getPackageName());
            if (appt != null) intent.putExtra("permit_id", appt.optInt("permit_id", -1));
            intent.putExtra("appointment_data", data.toString());
            context.sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "onAppointmentScheduled error: " + e.getMessage());
        }
    };

    private final Emitter.Listener onAppointmentStatusUpdated = args -> {
        try {
            JSONObject data = (JSONObject) args[0];
            Intent intent = new Intent("project.smartpermits.APPOINTMENT_STATUS_UPDATED");
            intent.setPackage(context.getPackageName());
            intent.putExtra("permit_id", data.optInt("permit_id", -1));
            intent.putExtra("status", data.optString("status", ""));
            intent.putExtra("permit_type", data.optString("permit_type", ""));
            context.sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "onAppointmentStatusUpdated error: " + e.getMessage());
        }
    };
}
