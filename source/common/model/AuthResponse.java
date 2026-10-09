package common.model;

public class AuthResponse {
    private boolean success;
    private String userId;
    private String fullName;
    private UserRole role;
    private String message;

    public AuthResponse() {}
    public AuthResponse(boolean success, String userId, String fullName, UserRole role, String message) {
        this.success = success;
        this.userId = userId;
        this.fullName = fullName;
        this.role = role;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
