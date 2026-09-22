package com.marceloaleixo.melvora.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(
            MethodArgumentNotValidException ex) {

        Map<String, String> fields = new LinkedHashMap<>();

        ex.getBindingResult()
            .getFieldErrors()
            .forEach(error ->
                fields.putIfAbsent(
                    error.getField(),
                    error.getDefaultMessage()
                )
            );

        return ResponseEntity.badRequest()
            .body(new ApiError(
                "VALIDATION_ERROR",
                "Dados inválidos.",
                fields
            ));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> methodValidation(
            HandlerMethodValidationException ex) {

        return ResponseEntity.badRequest()
            .body(new ApiError(
                "VALIDATION_ERROR",
                "Parâmetros inválidos.",
                null
            ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> constraintViolation(
            ConstraintViolationException ex) {

        return ResponseEntity.badRequest()
            .body(new ApiError(
                "VALIDATION_ERROR",
                "Parâmetros inválidos.",
                null
            ));
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ApiError> business(
            RegraNegocioException ex) {

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new ApiError(
                "BUSINESS_ERROR",
                ex.getMessage(),
                null
            ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> notFound(
            ResourceNotFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiError(
                "NOT_FOUND",
                ex.getMessage(),
                null
            ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> conflict(
            DataIntegrityViolationException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ApiError(
                "DATA_CONFLICT",
                "Não foi possível concluir a operação por conflito de dados.",
                null
            ));
    }


    @ExceptionHandler(TenantContextException.class)
    public ResponseEntity<ApiError> tenantContext(
            TenantContextException ex) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ApiError(
                "TENANT_CONTEXT_ERROR",
                "A operação não pode ser realizada no contexto da empresa atual.",
                null
            ));
    }

}
