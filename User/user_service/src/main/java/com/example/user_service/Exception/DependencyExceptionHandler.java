package com.example.user_service.Exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import feign.FeignException;

@RestControllerAdvice
public class DependencyExceptionHandler {

	@ExceptionHandler(RequestNotPermitted.class)
	public ResponseEntity<ErrorResponse> handleRateLimit(RequestNotPermitted exception) {
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.header(HttpHeaders.RETRY_AFTER, "1")
				.body(new ErrorResponse(
						HttpStatus.TOO_MANY_REQUESTS.value(),
						"Downstream request rate limit exceeded; retry shortly",
						LocalDateTime.now()));
	}

	@ExceptionHandler({CallNotPermittedException.class, FeignException.class})
	public ResponseEntity<ErrorResponse> handleUnavailable(RuntimeException exception) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ErrorResponse(
						HttpStatus.SERVICE_UNAVAILABLE.value(),
						"Downstream service is temporarily unavailable",
						LocalDateTime.now()));
	}

	public record ErrorResponse(int status, String message, LocalDateTime timestamp) {
	}
}
