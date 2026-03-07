package project.smartpermits.models;

import com.google.gson.annotations.SerializedName;

public class FcmTokenRequest {
    @SerializedName("fcm_token")
    private String fcmToken;

    public FcmTokenRequest(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}

