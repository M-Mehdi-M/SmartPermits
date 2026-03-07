package project.smartpermits.models;

import com.google.gson.annotations.SerializedName;

public class ErrorResponse {
    @SerializedName("error")
    private String error;

    public String getError() { return error; }
}

