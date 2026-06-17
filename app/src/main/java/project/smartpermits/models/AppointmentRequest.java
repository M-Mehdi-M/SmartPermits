package project.smartpermits.models;

import com.google.gson.annotations.SerializedName;

public class AppointmentRequest {
    @SerializedName("date")
    private String date;

    @SerializedName("time_slot")
    private String timeSlot;

    @SerializedName("notes")
    private String notes;

    @SerializedName("status")
    private String status;

    public AppointmentRequest(String date, String timeSlot, String notes) {
        this.date = date;
        this.timeSlot = timeSlot;
        this.notes = notes;
    }

    public AppointmentRequest(String date, String timeSlot, String status, String notes) {
        this.date = date;
        this.timeSlot = timeSlot;
        this.status = status;
        this.notes = notes;
    }
}
