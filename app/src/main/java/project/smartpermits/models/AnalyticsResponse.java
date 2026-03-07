package project.smartpermits.models;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class AnalyticsResponse {
    @SerializedName("total_reviewed")
    private int totalReviewed;

    @SerializedName("total_approved")
    private int totalApproved;

    @SerializedName("total_rejected")
    private int totalRejected;

    @SerializedName("total_pending")
    private int totalPending;

    @SerializedName("permit_type_counts")
    private Map<String, Integer> permitTypeCounts;

    @SerializedName("avg_processing_hours")
    private double avgProcessingHours;

    public int getTotalReviewed() { return totalReviewed; }
    public int getTotalApproved() { return totalApproved; }
    public int getTotalRejected() { return totalRejected; }
    public int getTotalPending() { return totalPending; }
    public Map<String, Integer> getPermitTypeCounts() { return permitTypeCounts; }
    public double getAvgProcessingHours() { return avgProcessingHours; }
}

