package project.smartpermits.models;

import com.google.gson.annotations.SerializedName;

public class PermitEvent {
    @SerializedName("id")
    private int id;

    @SerializedName("permit_id")
    private int permitId;

    @SerializedName("event_type")
    private String eventType;

    @SerializedName("actor_name")
    private String actorName;

    @SerializedName("actor_role")
    private String actorRole;

    @SerializedName("notes")
    private String notes;

    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public int getPermitId() { return permitId; }
    public String getEventType() { return eventType; }
    public String getActorName() { return actorName; }
    public String getActorRole() { return actorRole; }
    public String getNotes() { return notes; }
    public String getCreatedAt() { return createdAt; }
}

