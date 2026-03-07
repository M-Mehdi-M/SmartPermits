package project.smartpermits.models;

import com.google.gson.annotations.SerializedName;

public class Comment {
    @SerializedName("id")
    private int id;

    @SerializedName("permit_id")
    private int permitId;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("author_name")
    private String authorName;

    @SerializedName("author_role")
    private String authorRole;

    @SerializedName("message")
    private String message;

    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public int getPermitId() { return permitId; }
    public int getUserId() { return userId; }
    public String getAuthorName() { return authorName; }
    public String getAuthorRole() { return authorRole; }
    public String getMessage() { return message; }
    public String getCreatedAt() { return createdAt; }
}

