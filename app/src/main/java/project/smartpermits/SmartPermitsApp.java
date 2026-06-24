package project.smartpermits;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.core.content.ContextCompat;

public class SmartPermitsApp extends Application {

    private BroadcastReceiver globalNotifReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannel(this);
        registerGlobalReceiver();
        // If a session already exists (app reopened / restarted by the system), bring the
        // background listener back up so notifications arrive even when the UI is closed.
        if (project.smartpermits.api.RetrofitClient.getInstance(this).isLoggedIn()) {
            NotificationService.start(this);
        }
    }

    private void registerGlobalReceiver() {
        globalNotifReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) return;
                String action = intent.getAction();
                if ("project.smartpermits.PERMIT_STATUS_UPDATED".equals(action)) {
                    int permitId = intent.getIntExtra("permit_id", -1);
                    String status = intent.getStringExtra("status");
                    String reviewerName = intent.getStringExtra("reviewer_name");
                    String notes = intent.getStringExtra("notes");
                    if (permitId > 0 && status != null && !status.isEmpty()) {
                        String title = "approved".equals(status) ? "✅ Permit Approved" : "❌ Permit Rejected";
                        String body;
                        if (notes != null && !notes.isEmpty()) {
                            body = notes;
                        } else if (reviewerName != null && !reviewerName.isEmpty()) {
                            body = "Reviewed by " + reviewerName;
                        } else {
                            body = "Your permit has been " + status;
                        }
                        NotificationHelper.showNotification(SmartPermitsApp.this, title, body, permitId);
                    }
                } else if ("project.smartpermits.PERMIT_REVIEWED".equals(action)) {
                    String raw = intent.getStringExtra("permit_data");
                    if (raw != null) {
                        try {
                            org.json.JSONObject obj = new org.json.JSONObject(raw);
                            int permitId = obj.optInt("id", -1);
                            String status = obj.optString("status", "");
                            String permitType = obj.optString("permit_type", "Permit");
                            if (permitId > 0 && !status.isEmpty()) {
                                String title = "approved".equals(status) ? "✅ Permit Approved" : "❌ Permit Rejected";
                                NotificationHelper.showNotification(SmartPermitsApp.this, title, permitType + " has been " + status, permitId);
                            }
                        } catch (Exception ignored) {}
                    }
                } else if ("project.smartpermits.NEW_PERMIT".equals(action)) {
                    String raw = intent.getStringExtra("permit_data");
                    int permitId = -1;
                    String permitType = "permit";
                    if (raw != null) {
                        try {
                            org.json.JSONObject obj = new org.json.JSONObject(raw).optJSONObject("permit");
                            if (obj != null) {
                                permitId = obj.optInt("id", -1);
                                permitType = obj.optString("permit_type", "permit");
                            }
                        } catch (Exception ignored) {}
                    }
                    if (permitId > 0) {
                        NotificationHelper.showNotification(SmartPermitsApp.this, "📋 New Permit Application", "New " + permitType + " submitted for review", permitId);
                    } else {
                        NotificationHelper.showSimpleNotification(SmartPermitsApp.this, "📋 New Permit Application", "A new permit has been submitted for review");
                    }
                } else if ("project.smartpermits.COMMENT_NOTIFICATION".equals(action)) {
                    int permitId = intent.getIntExtra("permit_id", -1);
                    String commenterName = intent.getStringExtra("commenter_name");
                    String preview = intent.getStringExtra("message_preview");
                    if (permitId > 0) {
                        String title = (commenterName != null && !commenterName.isEmpty() ? commenterName : "Someone") + " sent a message";
                        String body = (preview != null && !preview.isEmpty()) ? preview : "New message on permit";
                        NotificationHelper.showNotification(SmartPermitsApp.this, title, body, permitId);
                    }
                } else if ("project.smartpermits.APPOINTMENT_STATUS_UPDATED".equals(action)) {
                    int permitId = intent.getIntExtra("permit_id", -1);
                    String newStatus = intent.getStringExtra("status");
                    String permitType = intent.getStringExtra("permit_type");
                    if (permitId > 0 && newStatus != null) {
                        String title;
                        switch (newStatus) {
                            case "confirmed": title = "✅ Inspection Confirmed"; break;
                            case "completed": title = "🎉 Inspection Completed"; break;
                            case "cancelled": title = "❌ Inspection Cancelled"; break;
                            default: title = "📅 Inspection Update"; break;
                        }
                        String body = permitType != null
                                ? "Your " + permitType + " inspection has been " + newStatus
                                : "Your inspection has been " + newStatus;
                        NotificationHelper.showNotification(SmartPermitsApp.this, title, body, permitId);
                    }
                } else if ("project.smartpermits.APPOINTMENT_SCHEDULED".equals(action)) {
                    int permitId = intent.getIntExtra("permit_id", -1);
                    if (permitId > 0) {
                        NotificationHelper.showNotification(SmartPermitsApp.this, "📅 Inspection Scheduled", "An inspection appointment has been scheduled", permitId);
                    } else {
                        NotificationHelper.showSimpleNotification(SmartPermitsApp.this, "📅 Inspection Scheduled", "An inspection appointment has been scheduled");
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("project.smartpermits.PERMIT_STATUS_UPDATED");
        filter.addAction("project.smartpermits.PERMIT_REVIEWED");
        filter.addAction("project.smartpermits.NEW_PERMIT");
        filter.addAction("project.smartpermits.COMMENT_NOTIFICATION");
        filter.addAction("project.smartpermits.APPOINTMENT_SCHEDULED");
        filter.addAction("project.smartpermits.APPOINTMENT_STATUS_UPDATED");
        ContextCompat.registerReceiver(this, globalNotifReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (globalNotifReceiver != null) {
            try {
                unregisterReceiver(globalNotifReceiver);
            } catch (Exception ignored) {}
        }
    }
}

