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

    @SerializedName("reviewer_name")
    private String reviewerName;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("documents")
    private List<Document> documents;

    @SerializedName("renewed_from")
    private Integer renewedFrom;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("estimated_processing_time")
    private String estimatedProcessingTime;

    @SerializedName("prediction_confidence")
    private Integer predictionConfidence;

    @SerializedName("point_estimate")
    private String pointEstimate;

    @SerializedName("ai_analysis")
    private String aiAnalysis;

    @SerializedName("ai_analysis_lang")
    private String aiAnalysisLang;

    @SerializedName("blockchain_hash")
    private String blockchainHash;

    @SerializedName("blockchain_tx_hash")
    private String blockchainTxHash;

    @SerializedName("blockchain_error")
    private String blockchainError;

    @SerializedName("expires_at")
    private String expiresAt;

    @SerializedName("days_to_expiry")
    private Integer daysToExpiry;

    @SerializedName("is_expired")
    private boolean isExpired;

    @SerializedName("deleted_at")
    private String deletedAt;

    @SerializedName("days_until_permanent_delete")
    private Integer daysUntilPermanentDelete;

    @SerializedName("timeline")
    private List<PermitEvent> timeline;

    @SerializedName("last_comment_id")
    private int lastCommentId;

    @SerializedName("last_comment_user_id")
    private Integer lastCommentUserId;

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
    public String getReviewerName() { return reviewerName; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public List<Document> getDocuments() { return documents; }
    public Integer getRenewedFrom() { return renewedFrom; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getEstimatedProcessingTime() { return estimatedProcessingTime; }
    public Integer getPredictionConfidence() { return predictionConfidence; }
    public String getPointEstimate() { return pointEstimate; }
    public String getAiAnalysis() { return aiAnalysis; }
    public String getAiAnalysisLang() { return aiAnalysisLang; }
    public String getBlockchainHash() { return blockchainHash; }
    public String getBlockchainTxHash() { return blockchainTxHash; }
    public String getBlockchainError() { return blockchainError; }
    public String getExpiresAt() { return expiresAt; }
    public Integer getDaysToExpiry() { return daysToExpiry; }
    public boolean isExpired() { return isExpired; }
    public String getDeletedAt() { return deletedAt; }
    public Integer getDaysUntilPermanentDelete() { return daysUntilPermanentDelete; }
    public List<PermitEvent> getTimeline() { return timeline; }
    public int getLastCommentId() { return lastCommentId; }
    public Integer getLastCommentUserId() { return lastCommentUserId; }
}

