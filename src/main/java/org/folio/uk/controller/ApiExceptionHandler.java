package org.folio.uk.controller;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.logging.log4j.Level.DEBUG;
import static org.apache.logging.log4j.Level.WARN;
import static org.folio.uk.domain.dto.ErrorCode.NOT_FOUND_ERROR;
import static org.folio.uk.domain.dto.ErrorCode.SERVICE_ERROR;
import static org.folio.uk.domain.dto.ErrorCode.UNKNOWN_ERROR;
import static org.folio.uk.domain.dto.ErrorCode.VALIDATION_ERROR;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.folio.cql2pgjson.exception.CQLFeatureUnsupportedException;
import org.folio.spring.cql.CqlQueryValidationException;
import org.folio.uk.domain.dto.Error;
import org.folio.uk.domain.dto.ErrorCode;
import org.folio.uk.domain.dto.ErrorResponse;
import org.folio.uk.domain.dto.Parameter;
import org.folio.uk.exception.RequestValidationException;
import org.folio.uk.exception.UnprocessableEntityException;
import org.folio.uk.integration.keycloak.KeycloakException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Log4j2
@RestControllerAdvice
public class ApiExceptionHandler {

  /**
   * Catches and handles all exceptions for type {@link UnsupportedOperationException}.
   *
   * @param exception {@link UnsupportedOperationException} to process
   * @return {@link ResponseEntity} with {@link ErrorResponse} body
   */
  @ExceptionHandler(UnsupportedOperationException.class)
  public ResponseEntity<ErrorResponse> handleUnsupportedOperationException(UnsupportedOperationException exception) {
    logException(DEBUG, exception);
    return buildResponseEntity(exception, NOT_IMPLEMENTED, SERVICE_ERROR);
  }

  /**
   * Catches and handles all exceptions for type {@link MethodArgumentNotValidException}.
   *
   * @param exception {@link MethodArgumentNotValidException} to process
   * @return {@link ResponseEntity} with {@link ErrorResponse} body
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
    MethodArgumentNotValidException exception) {
    var validationErrors = Optional.of(exception.getBindingResult()).map(Errors::getAllErrors).orElse(emptyList());
    var errorResponse = new ErrorResponse();
    validationErrors.forEach(error ->
      errorResponse.addErrorsItem(new Error()
        .message(error.getDefaultMessage())
        .code(VALIDATION_ERROR.getValue())
        .type(MethodArgumentNotValidException.class.getSimpleName())
        .addParametersItem(new Parameter()
          .key(((FieldError) error).getField())
          .value(String.valueOf(((FieldError) error).getRejectedValue())))));
    errorResponse.totalRecords(errorResponse.getErrors().size());

    return buildResponseEntity(errorResponse, BAD_REQUEST);
  }

  /**
   * Catches and handles all exceptions for type {@link ConstraintViolationException}.
   *
   * @param exception {@link ConstraintViolationException} to process
   * @return {@link ResponseEntity} with {@link ErrorResponse} body
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
    logException(DEBUG, exception);
    var errorResponse = new ErrorResponse();
    exception.getConstraintViolations().forEach(constraintViolation ->
      errorResponse.addErrorsItem(new Error()
        .message(String.format("%s %s", constraintViolation.getPropertyPath(), constraintViolation.getMessage()))
        .code(VALIDATION_ERROR.getValue())
        .type(ConstraintViolationException.class.getSimpleName())));
    errorResponse.totalRecords(errorResponse.getErrors().size());

    return buildResponseEntity(errorResponse, BAD_REQUEST);
  }

  /**
   * Catches and handles all exceptions for type {@link RequestValidationException}.
   *
   * @param exception {@link RequestValidationException} to process
   * @return {@link ResponseEntity} with {@link ErrorResponse} body
   */
  @ExceptionHandler(RequestValidationException.class)
  public ResponseEntity<ErrorResponse> handleRequestValidationException(RequestValidationException exception) {
    var errorResponse = buildValidationError(exception, exception.getErrorParameters());
    return buildResponseEntity(errorResponse, BAD_REQUEST);
  }

  /**
   * Catches and handles all exceptions for type {@link NoSuchElementException}.
   *
   * @param exception {@link NoSuchElementException} to process
   * @return {@link ResponseEntity} with {@link ErrorResponse} body
   */
  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ErrorResponse> handleNoSuchElementException(NoSuchElementException exception) {
    logException(DEBUG, exception);
    return buildResponseEntity(exception, BAD_REQUEST, NOT_FOUND_ERROR);
  }

  /**
   * Catches and handles all exceptions for type {@link UnprocessableEntityException}.
   *
   * @param exception {@link UnprocessableEntityException} to process
   * @return {@link ResponseEntity} with {@link ErrorResponse} body
   */
  @ExceptionHandler(UnprocessableEntityException.class)
  public ResponseEntity<ErrorResponse> handleUnprocessableEntityException(UnprocessableEntityException exception) {
    logException(DEBUG, exception);
    var errors = exception.getErrors().stream()
      .map(e -> new Error()
        .code(e.getCode())
        .message(e.getMessage())).toList();

    return ResponseEntity.status(UNPROCESSABLE_ENTITY)
      .body(new ErrorResponse().errors(errors).totalRecords(errors.size()));
  }

  /**
   * Catches and handles common request validation exceptions.
   *
   * @param exception {@link Exception} object to process
   * @return {@link ResponseEntity} with {@link ErrorResponse} body
   */
  @ExceptionHandler({
    IllegalArgumentException.class,
    CqlQueryValidationException.class,
    MissingRequestHeaderException.class,
    CQLFeatureUnsupportedException.class,
    InvalidDataAccessApiUsageException.class,
    HttpMediaTypeNotSupportedException.class,
    MethodArgumentTypeMismatchException.class,
    FeignException.BadRequest.class
  })
  public ResponseEntity<ErrorResponse> handleValidationExceptions(Exception exception) {
    logException(DEBUG, exception);
    return buildResponseEntity(exception, BAD_REQUEST, VALIDATION_ERROR);
  }

  /**
   * Catches and handles all exceptions for type {@link EntityNotFoundException}, {@link FeignException.NotFound}.
   *
   * @param exception {@link EntityNotFoundException}, {@link FeignException.NotFound} object
   * @return {@link ResponseEntity} with {@link ErrorResponse} body.
   */
  @ExceptionHandler({EntityNotFoundException.class, FeignException.NotFound.class})
  public ResponseEntity<ErrorResponse> handleEntityNotFoundException(Exception exception) {
    logException(DEBUG, exception);
    return buildResponseEntity(exception, NOT_FOUND, NOT_FOUND_ERROR);
  }

  /**
   * Catches and handles all exceptions for type {@link HttpMessageNotReadableException}.
   *
   * @param exception {@link HttpMessageNotReadableException} object
   * @return {@link ResponseEntity} with {@link ErrorResponse} body.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handlerHttpMessageNotReadableException(
    HttpMessageNotReadableException exception) {

    return Optional.ofNullable(exception.getCause())
      .map(Throwable::getCause)
      .filter(IllegalArgumentException.class::isInstance)
      .map(IllegalArgumentException.class::cast)
      .map(this::handleValidationExceptions)
      .orElseGet(() -> {
        logException(DEBUG, exception);
        return buildResponseEntity(exception, BAD_REQUEST, VALIDATION_ERROR);
      });
  }

  /**
   * Catches and handles all exceptions for type {@link MissingServletRequestParameterException}.
   *
   * @param exception {@link MissingServletRequestParameterException} to process
   * @return {@link ResponseEntity} with {@link ErrorResponse} body
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
    MissingServletRequestParameterException exception) {
    logException(DEBUG, exception);
    return buildResponseEntity(exception, BAD_REQUEST, VALIDATION_ERROR);
  }

  /**
   * Catches and handles all exceptions for type {@link KeycloakException}.
   *
   * @param exception {@link KeycloakException} to process
   * @return {@link ResponseEntity} with {@link ErrorResponse} body
   */
  @ExceptionHandler(KeycloakException.class)
  public ResponseEntity<ErrorResponse> handleOutgoingRequestException(Exception exception) {
    logException(WARN, exception);
    var errorParameters = singletonList(new Parameter().key("cause").value(exception.getCause().getMessage()));
    var errorResponse = buildErrorResponse(exception, errorParameters, SERVICE_ERROR);
    return buildResponseEntity(errorResponse, BAD_REQUEST);
  }

  /**
   * Handles all uncaught exceptions.
   *
   * @param exception {@link Exception} object
   * @return {@link ResponseEntity} with {@link ErrorResponse} body.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAllOtherExceptions(Exception exception) {
    logException(WARN, exception);
    return buildResponseEntity(exception, INTERNAL_SERVER_ERROR, UNKNOWN_ERROR);
  }

  private static ErrorResponse buildValidationError(RequestValidationException exception, List<Parameter> parameters) {
    return buildErrorResponse(exception, parameters, exception.getErrorCode());
  }

  private static ErrorResponse buildErrorResponse(Exception exception, List<Parameter> parameters, ErrorCode code) {
    var error = new Error()
      .type(exception.getClass().getSimpleName())
      .code(code.getValue())
      .message(exception.getMessage())
      .parameters(isNotEmpty(parameters) ? parameters : null);
    return new ErrorResponse().errors(List.of(error)).totalRecords(1);
  }

  private static ResponseEntity<ErrorResponse> buildResponseEntity(
    Throwable exception, HttpStatus status, ErrorCode code) {

    var errorResponse = new ErrorResponse()
      .errors(List.of(new Error()
        .message(exception.getMessage())
        .type(exception.getClass().getSimpleName())
        .code(code.getValue())))
      .totalRecords(1);

    return buildResponseEntity(errorResponse, status);
  }

  private static ResponseEntity<ErrorResponse> buildResponseEntity(ErrorResponse errorResponse, HttpStatus status) {
    return ResponseEntity.status(status).body(errorResponse);
  }

  private static void logException(Level level, Exception exception) {
    log.log(level, "Handling exception", exception);
  }
}
