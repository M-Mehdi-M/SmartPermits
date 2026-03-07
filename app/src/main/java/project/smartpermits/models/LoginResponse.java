package project.smartpermits.models;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("token")
    private String token;

    @SerializedName("user")
    private User user;

    @SerializedName("error")
    private String error;

    public String getToken() { return token; }
    public User getUser() { return user; }
    public String getError() { return error; }
}

