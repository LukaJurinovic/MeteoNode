package com.example.meteonode.model.dto.response;

public record AuthResponse(String accessToken, String refreshToken, String username, String role) {}
