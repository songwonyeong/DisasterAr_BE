package com.example.disaster_ar.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(
            IllegalArgumentException e,
            HttpServletRequest request
    ) {
        log.warn("BadRequest [{}] {}", request.getRequestURI(), e.getMessage(), e);

        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 400,
                "error", "Bad Request",
                "message", e.getMessage() != null ? e.getMessage() : "잘못된 요청입니다.",
                "path", request.getRequestURI()
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(
            IllegalStateException e,
            HttpServletRequest request
    ) {
        log.warn("Conflict [{}] {}", request.getRequestURI(), e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 409,
                "error", "Conflict",
                "message", e.getMessage() != null ? e.getMessage() : "현재 상태에서 처리할 수 없습니다.",
                "path", request.getRequestURI()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleEtc(
            Exception e,
            HttpServletRequest request
    ) {
        log.error("InternalServerError [{}]", request.getRequestURI(), e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 500,
                "error", "Internal Server Error",
                "message", "서버 내부 오류가 발생했습니다.",
                "path", request.getRequestURI()
        ));
    }
}