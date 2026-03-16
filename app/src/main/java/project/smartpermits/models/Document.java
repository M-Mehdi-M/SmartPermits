package project.smartpermits.models;

import com.google.gson.annotations.SerializedName;

public class Document {
    @SerializedName("id")
    private int id;

    @SerializedName("permit_id")
    private int permitId;

    @SerializedName("file_name")
    private String fileName;

    @SerializedName("document_label")
    private String documentLabel;

    @SerializedName("uploaded_at")
    private String uploadedAt;

    public int getId() { return id; }
    public int getPermitId() { return permitId; }
    public String getFileName() { return fileName; }
    public String getDocumentLabel() { return documentLabel; }
    public String getUploadedAt() { return uploadedAt; }
}

