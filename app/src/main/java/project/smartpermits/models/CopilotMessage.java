package project.smartpermits.models;

public class CopilotMessage {
    private String role;
    private String content;

    public CopilotMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() { return role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isUser() { return "user".equals(role); }
}
