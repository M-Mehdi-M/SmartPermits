package project.smartpermits.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Permit {
    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("applicant_name")
    private String applicantName;

    @SerializedName("permit_type")
    private String permitType;

    @SerializedName("description")
    private String description;

    @SerializedName("status")
    private String status;

    @SerializedName("fee_amount")
    private double feeAmount;

    @SerializedName("is_paid")
    private boolean isPaid;

    @SerializedName("reviewer_notes")
    private String reviewerNotes;

    @SerializedName("reviewed_by")
    private Integer reviewedBy;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("documents")
    private List<Document> documents;

    @SerializedName("renewed_from")
    private Integer renewedFrom;

    @SerializedName("estimated_processing_time")
    private String estimatedProcessingTime;

    @SerializedName("deleted_at")
    private String deletedAt;

    @SerializedName("days_until_permanent_delete")
    private Integer daysUntilPermanentDelete;

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getApplicantName() { return applicantName; }
    public String getPermitType() { return permitType; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public double getFeeAmount() { return feeAmount; }
    public boolean isPaid() { return isPaid; }
    public String getReviewerNotes() { return reviewerNotes; }
    public Integer getReviewedBy() { return reviewedBy; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public List<Document> getDocuments() { return documents; }
    public Integer getRenewedFrom() { return renewedFrom; }
    public String getEstimatedProcessingTime() { return estimatedProcessingTime; }
    public String getDeletedAt() { return deletedAt; }
    public Integer getDaysUntilPermanentDelete() { return daysUntilPermanentDelete; }
}

