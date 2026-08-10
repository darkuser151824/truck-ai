package com.example.truck_ai.template;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private String status;
    private int statusCode;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(HttpStatus httpStatus, String message, T data) {
        return new ApiResponse<>("OK", httpStatus.value(), message, data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> failure(HttpStatus httpStatus, String message) {
        return new ApiResponse<>("FAILED", httpStatus.value(), message, null, LocalDateTime.now());
    }
}
