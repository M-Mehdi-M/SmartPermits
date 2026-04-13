package project.smartpermits.models;

import com.google.gson.annotations.SerializedName;

public class AiAnalysisResponse {
    @SerializedName("ai_analysis")
    private String aiAnalysis;

    @SerializedName("ai_analysis_lang")
    private String aiAnalysisLang;

    public String getAiAnalysis() { return aiAnalysis; }
    public String getAiAnalysisLang() { return aiAnalysisLang; }
}

