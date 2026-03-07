package project.smartpermits.models;

import com.google.gson.annotations.SerializedName;

public class ProfileUpdateRequest {
    @SerializedName("full_name")
    private String fullName;

    @SerializedName("email")
    private String email;

    public ProfileUpdateRequest(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
    }

    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
}

