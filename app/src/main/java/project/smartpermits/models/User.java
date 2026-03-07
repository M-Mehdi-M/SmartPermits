package project.smartpermits.models;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")
    private int id;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private String role;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getFullName() { return fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getCreatedAt() { return createdAt; }
}

