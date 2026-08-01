package com.allocra.app.web;

import com.allocra.common.error.InfeasibleBookingException;
import com.allocra.common.error.NotAuthorizedException;
import com.allocra.common.error.ReservationConflictException;
import com.allocra.common.error.ResourceNotFoundException;
import com.allocra.identity.TokenVerifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain exceptions to RFC-7807 problem responses (PRD-BKG-006). A
 * reservation conflict — the key concurrency outcome — is a clear 409
 * (PRD-RSV-004).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ReservationConflictException.class)
	public ProblemDetail onConflict(ReservationConflictException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
	}

	@ExceptionHandler(InfeasibleBookingException.class)
	public ProblemDetail onInfeasible(InfeasibleBookingException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
	}

	@ExceptionHandler(NotAuthorizedException.class)
	public ProblemDetail onNotAuthorized(NotAuthorizedException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ProblemDetail onNotFound(ResourceNotFoundException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(TokenVerifier.InvalidTokenException.class)
	public ProblemDetail onInvalidToken(TokenVerifier.InvalidTokenException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail onBadRequest(IllegalArgumentException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
	}
}
