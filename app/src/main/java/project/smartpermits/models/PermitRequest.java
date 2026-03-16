package project.smartpermits.models;

public class PermitRequest {
    private String permit_type;
    private String description;
    private Double latitude;
    private Double longitude;

    public PermitRequest(String permitType, String description) {
        this.permit_type = permitType;
        this.description = description;
    }

    public PermitRequest(String permitType, String description, Double latitude, Double longitude) {
        this.permit_type = permitType;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getPermitType() { return permit_type; }
    public String getDescription() { return description; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}

