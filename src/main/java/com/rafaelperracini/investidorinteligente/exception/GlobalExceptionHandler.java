package com.rafaelperracini.investidorinteligente.exception;

import com.rafaelperracini.investidorinteligente.config.RateLimiterConfig;
import com.rafaelperracini.investidorinteligente.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AnaliseNaoEncontradaException.class)
    public ResponseEntity<ErrorResponse> handleAnaliseNaoEncontrada(
            AnaliseNaoEncontradaException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(CotacaoIndisponivelException.class)
    public ResponseEntity<ErrorResponse> handleCotacaoIndisponivel(
            CotacaoIndisponivelException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, mensagem, request);
    }

    @ExceptionHandler(RateLimiterConfig.RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(
            RateLimiterConfig.RateLimitExceededException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, e.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception e, HttpServletRequest request) {
        log.error("Erro inesperado: {}", e.getMessage(), e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno do servidor", request);
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status, String mensagem, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(), status.value(), mensagem, request.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }
}
