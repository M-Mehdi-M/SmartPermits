package project.smartpermits.models;

import com.google.gson.annotations.SerializedName;

public class CommentRequest {
    @SerializedName("message")
    private String message;

    public CommentRequest(String message) {
        this.message = message;
    }
}

