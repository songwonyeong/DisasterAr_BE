package com.example.disaster_ar.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AuthResponse {
    private String userId;
    private String email;
    private String name;
    private String role;
    private String schoolId;

    public AuthResponse(String userId, String email, String name, String role, String schoolId) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.role = role;
        this.schoolId = schoolId;
    }

    // getters
}
