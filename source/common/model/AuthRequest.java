package common.model;

public class AuthRequest {
    private String username;
    private String password;
    private String clientType;

    public AuthRequest() {}
    public AuthRequest(String username, String password, String clientType) {
        this.username = username;
        this.password = password;
        this.clientType = clientType;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getClientType() { return clientType; }
    public void setClientType(String clientType) { this.clientType = clientType; }
}
