package project.smartpermits.models;

public class ReviewRequest {
    private String action;
    private String notes;

    public ReviewRequest(String action, String notes) {
        this.action = action;
        this.notes = notes;
    }

    public String getAction() { return action; }
    public String getNotes() { return notes; }
}

