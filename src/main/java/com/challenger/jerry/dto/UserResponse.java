package com.challenger.jerry.dto;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String roles
) {}
