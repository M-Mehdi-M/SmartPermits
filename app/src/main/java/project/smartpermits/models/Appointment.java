package project.smartpermits.models;

import com.google.gson.annotations.SerializedName;

public class Appointment {
    @SerializedName("id")
    private int id;

    @SerializedName("permit_id")
    private int permitId;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("date")
    private String date;

    @SerializedName("time_slot")
    private String timeSlot;

    @SerializedName("status")
    private String status;

    @SerializedName("notes")
    private String notes;

    @SerializedName("permit_type")
    private String permitType;

    @SerializedName("applicant_name")
    private String applicantName;

    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public int getPermitId() { return permitId; }
    public int getUserId() { return userId; }
    public String getDate() { return date; }
    public String getTimeSlot() { return timeSlot; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    public String getPermitType() { return permitType; }
    public String getApplicantName() { return applicantName; }
    public String getCreatedAt() { return createdAt; }
}

