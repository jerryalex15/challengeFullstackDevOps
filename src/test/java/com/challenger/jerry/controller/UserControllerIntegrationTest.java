package com.challenger.jerry.controller;

import com.challenger.jerry.DatabaseContainer.DatabaseInstanceTest;
import com.challenger.jerry.entity.Role;
import com.challenger.jerry.entity.UserInfo;
import com.challenger.jerry.repository.RoleRepository;
import com.challenger.jerry.repository.UserInfoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class UserControllerIntegrationTest extends DatabaseInstanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    UserInfoRepository userInfoRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Test
    @WithMockUser(username = "test@mail.com") // ← simule user connecté
    void shouldReturnMeInfo() throws Exception {

        // GIVEN — on doit avoir un user en base
        Role roleUser = Role.builder()
                .id(1L).name("ROLE_USER").build();
        roleRepository.save(roleUser);

        UserInfo user = UserInfo.builder()
                .email("test@mail.com")
                .fullName("Jerry")
                .password("password")
                .roles(Set.of(roleUser))
                .build();
        userInfoRepository.save(user);

        // WHEN + THEN
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@mail.com"))
                .andExpect(jsonPath("$.fullName").value("Jerry"));
    }

    @Test
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "unknown@mail.com")
    void shouldReturnUserNotFound() throws Exception {

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().is4xxClientError());
    }
}
