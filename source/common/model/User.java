package common.model;

public class User {
    private String userId;
    private String username;
    private String fullName;
    private UserRole role;
    private String phone;

    public User() {}
    public User(String userId, String username, String fullName, UserRole role, String phone) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.phone = phone;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
