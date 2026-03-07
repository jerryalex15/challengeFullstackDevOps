package com.challenger.jerry.controller;

import com.challenger.jerry.dto.UserResponse;
import com.challenger.jerry.service.UserInfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/users")
public class UserController {

    private final UserInfoService userInfoService;

    public UserController(UserInfoService userInfoService){
        this.userInfoService = userInfoService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUSer(){
        return ResponseEntity.ok(userInfoService.getCurrentUser());
    }
}
