package project.smartpermits.models;

import com.google.gson.annotations.SerializedName;

public class AiAnalysisResponse {
    @SerializedName("ai_analysis")
    private String aiAnalysis;

    public String getAiAnalysis() { return aiAnalysis; }
}

