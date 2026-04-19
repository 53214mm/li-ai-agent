package com.li.liaiagent.controller;

import com.li.liaiagent.advisor.exception.DocumentNotFoundException;
import com.li.liaiagent.advisor.exception.ProhibitedWordException;
import com.li.liaiagent.advisor.exception.QueryTimeoutException;
import com.li.liaiagent.advisor.exception.SimilarityTooLowException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDocumentNotFound(DocumentNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(SimilarityTooLowException.class)
    public ResponseEntity<ErrorResponse> handleSimilarityTooLow(SimilarityTooLowException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "SIMILARITY_TOO_LOW", ex.getMessage(), request);
    }

    @ExceptionHandler(QueryTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleQueryTimeout(QueryTimeoutException ex, HttpServletRequest request) {
        return build(HttpStatus.REQUEST_TIMEOUT, "QUERY_TIMEOUT", ex.getMessage(), request);
    }

    @ExceptionHandler(ProhibitedWordException.class)
    public ResponseEntity<ErrorResponse> handleProhibitedWord(ProhibitedWordException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "PROHIBITED_WORD", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOtherExceptions(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "服务内部异常，请稍后重试", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(code, message, request.getRequestURI(), Instant.now()));
    }

    private record ErrorResponse(String code, String message, String path, Instant timestamp) {
    }
}
