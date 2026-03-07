package project.smartpermits.models;

import com.google.gson.annotations.SerializedName;

public class AppointmentRequest {
    @SerializedName("date")
    private String date;

    @SerializedName("time_slot")
    private String timeSlot;

    @SerializedName("notes")
    private String notes;

    public AppointmentRequest(String date, String timeSlot, String notes) {
        this.date = date;
        this.timeSlot = timeSlot;
        this.notes = notes;
    }
}

