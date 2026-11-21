package com.shopmall.common.web;

import com.shopmall.common.api.ApiResponse;
import com.shopmall.common.error.BizException;
import com.shopmall.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException exception) {
        return response(exception.getErrorCode(), exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidationException(BindException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError == null
                ? ErrorCode.PARAM_ERROR.getMessage()
                : fieldError.getField() + ": " + fieldError.getDefaultMessage();
        return response(ErrorCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception) {
        return response(ErrorCode.BODY_INVALID);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeaderException(
            MissingRequestHeaderException exception) {
        return response(ErrorCode.CLIENT_TYPE_INVALID);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException exception) {
        return response(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        log.error("Unhandled exception", exception);
        return response(ErrorCode.SYSTEM_ERROR);
    }

    private ResponseEntity<ApiResponse<Void>> response(ErrorCode errorCode) {
        return response(errorCode, errorCode.getMessage());
    }

    private ResponseEntity<ApiResponse<Void>> response(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode, message));
    }
}
