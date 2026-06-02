package com.example.disaster_ar.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(
            ApiException e,
            HttpServletRequest request
    ) {
        log.warn("ApiException [{}] {} {}", request.getRequestURI(), e.getCode(), e.getMessage(), e);

        return ResponseEntity.status(e.getStatus()).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", e.getStatus().value(),
                "error", e.getStatus().getReasonPhrase(),
                "code", e.getCode(),
                "message", e.getMessage() != null ? e.getMessage() : "요청을 처리할 수 없습니다.",
                "path", request.getRequestURI()
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolation(
            DataIntegrityViolationException e,
            HttpServletRequest request
    ) {
        String message = e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage()
                : e.getMessage();

        String lower = message != null ? message.toLowerCase(Locale.ROOT) : "";

        if (lower.contains("uq_schools_name") || lower.contains("school_name")) {
            return conflictBody(
                    "DUPLICATE_SCHOOL_NAME",
                    "이미 존재하는 학교 이름입니다.",
                    request
            );
        }

        if (lower.contains("uq_ibeacon_triplet_per_school")
                || lower.contains("uq_beacon_no_per_school_floor")
                || lower.contains("uuid") && lower.contains("major") && lower.contains("minor")
                || lower.contains("beacon_no")) {
            return conflictBody(
                    "DUPLICATE_BEACON",
                    "이미 등록된 비콘입니다.",
                    request
            );
        }

        log.warn("DataIntegrityViolation [{}] {}", request.getRequestURI(), message, e);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 409,
                "error", "Conflict",
                "code", "DATA_INTEGRITY_CONFLICT",
                "message", "이미 사용 중인 데이터이거나 참조 관계가 있어 처리할 수 없습니다.",
                "path", request.getRequestURI()
        ));
    }

    private ResponseEntity<?> conflictBody(
            String code,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 409,
                "error", "Conflict",
                "code", code,
                "message", message,
                "path", request.getRequestURI()
        ));
    }

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        log.warn("Invalid request body [{}] {}", request.getRequestURI(), e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 400,
                "error", "Bad Request",
                "code", "INVALID_JSON_REQUEST",
                "message", "요청 JSON 형식 또는 인코딩을 확인해 주세요.",
                "path", request.getRequestURI()
        ));
    }
}