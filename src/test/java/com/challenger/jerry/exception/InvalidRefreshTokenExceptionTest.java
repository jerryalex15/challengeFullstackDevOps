package com.challenger.jerry.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidRefreshTokenExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        String message = "Invalid refresh token";
        InvalidRefreshTokenException exception = new InvalidRefreshTokenException(message);

        assertEquals(message, exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception);
    }
}