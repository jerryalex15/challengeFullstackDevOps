package com.challenger.jerry.dto;

import com.challenger.jerry.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class RegisterResponse {
    private Long id;
    private String email;
    private String fullName;
    private List<String> roles;
}