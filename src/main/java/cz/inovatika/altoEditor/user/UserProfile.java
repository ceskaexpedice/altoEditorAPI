package cz.inovatika.altoEditor.user;

import java.util.List;

public class UserProfile {

    private String username;
    private String token;
    private List<String> roles;

    public UserProfile(String username, String token) {
        this.username = username;
        this.token = token;
    }

    public UserProfile(String username, String token, List<String> roles) {
        this.username = username;
        this.token = token;
        this.roles = roles;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }
}
