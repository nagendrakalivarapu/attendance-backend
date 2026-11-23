package com.smartattendance.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatusCode status,
            String error,
            String message) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("timestamp", Instant.now());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);

        return ResponseEntity.status(status).body(response);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String message) {

        return buildResponse(
                status,
                status.getReasonPhrase(),
                message
        );
    }

    // OTP verification errors
    @ExceptionHandler(OtpVerificationException.class)
    public ResponseEntity<Map<String, Object>> handleOtpException(
            OtpVerificationException ex) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid OTP request."
        );
    }

    // Response status exceptions
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request) {

        HttpStatusCode status = ex.getStatusCode();

        HttpStatus resolvedStatus =
                HttpStatus.resolve(status.value());

        String error = resolvedStatus != null
                ? resolvedStatus.getReasonPhrase()
                : "HTTP Error";

        String path = request.getServletPath();

        String message;

        // Hide account-related information in OTP endpoints.
        if (path.equals("/api/auth/verify-otp")
                && (status.value() == 401 || status.value() == 403)) {

            message = "Unable to verify OTP. Please check your details.";

        } else if (path.equals("/api/auth/resend-otp")
                && (status.value() == 401 || status.value() == 403)) {

            message = "Unable to process OTP request.";

        } else {

            message = ex.getReason() != null
                    ? ex.getReason()
                    : "The request could not be processed.";
        }

        return buildResponse(status, error, message);
    }

    // Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request data."
        );
    }

    // Malformed JSON
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedRequest(
            HttpMessageNotReadableException ex) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid or malformed request body."
        );
    }

    // Invalid login credentials
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password."
        );
    }

    // Disabled accounts
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabledAccount(
            DisabledException ex) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password."
        );
    }

    // Authentication failures
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException ex) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Authentication failed."
        );
    }

    // Authorization failures
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex) {

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action."
        );
    }

    // Unexpected runtime exceptions
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex) {

        log.error(
                "Unexpected runtime exception: {}",
                ex.getClass().getSimpleName(),
                ex
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later."
        );
    }

    // Unexpected exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(
            Exception ex) {

        log.error(
                "Unexpected application exception: {}",
                ex.getClass().getSimpleName(),
                ex
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later."
        );
    }
}
