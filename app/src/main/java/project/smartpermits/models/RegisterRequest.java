package project.smartpermits.models;

public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String role;
    private String full_name;

    public RegisterRequest(String username, String password, String email, String role, String fullName) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
        this.full_name = fullName;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getFullName() { return full_name; }
}

