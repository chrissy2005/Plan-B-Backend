package org.planb.dto;

public class LoginResponse {
    private String token;
    private String message;
    private Long userId;

    public LoginResponse(String token, String message, Long userId) {
        this.token = token;
        this.message = message;
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }
}