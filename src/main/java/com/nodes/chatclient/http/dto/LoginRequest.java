package com.nodes.chatclient.http.dto;

public final class LoginRequest {
    public String userId;
    public String password;

    public LoginRequest(String userId, String password) {
        this.userId = userId;
        this.password = password;
    }
}
