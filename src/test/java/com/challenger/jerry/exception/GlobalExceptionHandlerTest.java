package com.challenger.jerry.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    @Test
    void shouldReturnUnauthorizedWhenInvalidRefreshToken() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        InvalidRefreshTokenException exception = new InvalidRefreshTokenException("Token expired");

        ResponseEntity<String> response = handler.handleInvalidRefreshToken(exception);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Token expired", response.getBody());
    }
}