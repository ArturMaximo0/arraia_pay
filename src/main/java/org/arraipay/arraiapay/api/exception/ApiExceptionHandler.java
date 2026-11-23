package org.arraipay.arraiapay.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.arraipay.arraiapay.api.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(DominioException.class)
    public ResponseEntity<ApiErrorResponse> dominio(DominioException exception, HttpServletRequest request) {
        HttpStatus status;
        if ("E09".equals(exception.getCodigo())) status = HttpStatus.FORBIDDEN;
        else if ("E12".equals(exception.getCodigo())) status = HttpStatus.CONFLICT;
        else if ("E23".equals(exception.getCodigo())) status = HttpStatus.UNAUTHORIZED;
        else if ("E22".equals(exception.getCodigo())) status = HttpStatus.SERVICE_UNAVAILABLE;
        else if ("E01".equals(exception.getCodigo()) || "E16".equals(exception.getCodigo()) || "E08".equals(exception.getCodigo())) status = HttpStatus.NOT_FOUND;
        else status = HttpStatus.BAD_REQUEST;
        ApiErrorResponse body = new ApiErrorResponse(
                exception.getCodigo(), exception.getMessage(), null,
                OffsetDateTime.now(), request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
