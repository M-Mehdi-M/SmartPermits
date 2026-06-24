package project.smartpermits.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CopilotRecommendation {
    @SerializedName("permit_type")
    private String permitType;
    @SerializedName("suggested_description")
    private String suggestedDescription;
    private double fee;
    @SerializedName("validity_days")
    private int validityDays;
    @SerializedName("required_documents")
    private List<String> requiredDocuments;
    @SerializedName("estimated_days_min")
    private int estimatedDaysMin;
    @SerializedName("estimated_days_max")
    private int estimatedDaysMax;

    public String getPermitType() { return permitType; }
    public String getSuggestedDescription() { return suggestedDescription; }
    public double getFee() { return fee; }
    public int getValidityDays() { return validityDays; }
    public List<String> getRequiredDocuments() { return requiredDocuments; }
    public int getEstimatedDaysMin() { return estimatedDaysMin; }
    public int getEstimatedDaysMax() { return estimatedDaysMax; }
}
