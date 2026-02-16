package com.challenger.jerry.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class RegisterResponse {
    private Long id;
    private String email;
    private String fullName;
    private String roles;
}