package project.smartpermits.models;

import java.util.List;

public class CopilotRequest {
    private List<CopilotMessage> messages;

    public CopilotRequest(List<CopilotMessage> messages) {
        this.messages = messages;
    }

    public List<CopilotMessage> getMessages() { return messages; }
}
