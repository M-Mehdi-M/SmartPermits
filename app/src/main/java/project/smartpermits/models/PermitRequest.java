package project.smartpermits.models;

public class PermitRequest {
    private String permit_type;
    private String description;

    public PermitRequest(String permitType, String description) {
        this.permit_type = permitType;
        this.description = description;
    }

    public String getPermitType() { return permit_type; }
    public String getDescription() { return description; }
}

