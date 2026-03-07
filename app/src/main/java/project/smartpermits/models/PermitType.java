package project.smartpermits.models;

import com.google.gson.annotations.SerializedName;

public class PermitType {
    @SerializedName("name")
    private String name;

    @SerializedName("fee")
    private double fee;

    public String getName() { return name; }
    public double getFee() { return fee; }
}

