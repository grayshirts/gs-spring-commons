package ar.com.grayshirts.commons.spring.web;

import ar.com.grayshirts.commons.exception.BusinessException;
import ar.com.grayshirts.commons.exception.ResourceNotFoundException;
import ar.com.grayshirts.commons.type.RestErrorResponse;
import ar.com.grayshirts.commons.type.RestResponse;
import ar.com.grayshirts.commons.type.RestValidationErrorsResponse;
import ar.com.grayshirts.commons.type.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import javax.security.auth.login.AccountLockedException;
import javax.servlet.http.HttpServletRequest;
import java.nio.file.AccessDeniedException;
import java.security.DigestException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import static org.springframework.http.HttpStatus.*;
import static com.google.common.base.CaseFormat.*;


public abstract class BaseController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired(required = false)
    protected MessageSource messageSource;

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<RestResponse> handleException(HttpServletRequest req, Throwable ex) {
        logger.error("Error executing {} {}", req.getMethod(), req.getRequestURI(), ex);

        HttpStatus httpStatus = INTERNAL_SERVER_ERROR;              // 500
        String errorMsg = ex.getMessage()!=null ? ex.getMessage() : "Internal Error";

        return new ResponseEntity(new RestErrorResponse(httpStatus.value(), getMsg(errorMsg), exceptionToErrorCode(ex)), httpStatus);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> handleBusinessException(HttpServletRequest req, BusinessException ex) {
        logger.debug("Business error executing {} {}", req.getMethod(), req.getRequestURI(), ex);

        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;             // 400

        return new ResponseEntity(new RestErrorResponse(httpStatus.value(), getMsg(ex.getMessage()), exceptionToErrorCode(ex)), httpStatus);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleResourceNotFoundException(HttpServletRequest req, ResourceNotFoundException ex) {
        logger.debug("Resource not found executing {} {}", req.getMethod(), req.getRequestURI(), ex);

        HttpStatus httpStatus = HttpStatus.NOT_FOUND;             // 404

        return new ResponseEntity(new RestErrorResponse(httpStatus.value(), getMsg(ex.getMessage()), exceptionToErrorCode(ex)), httpStatus);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RestResponse> handleIllegalArgumentException(HttpServletRequest req, IllegalArgumentException ex) {
        logger.warn("Argument error executing {} {}", req.getMethod(), req.getRequestURI(), ex);

        HttpStatus httpStatus = HttpStatus.UNPROCESSABLE_ENTITY;    // 422

        return new ResponseEntity(new RestErrorResponse(httpStatus.value(), getMsg(ex.getMessage()), exceptionToErrorCode(ex)), httpStatus);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RestResponse> handleAccessDeniedException(HttpServletRequest req, AccessDeniedException ex) {
        logger.warn("Access forbidden executing {} {}", req.getMethod(), req.getRequestURI(), ex);

        HttpStatus httpStatus = HttpStatus.UNAUTHORIZED;            // 401

        return new ResponseEntity(new RestErrorResponse(httpStatus.value(), getMsg(ex.getMessage()), exceptionToErrorCode(ex)), httpStatus);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResponse> handleArgumentNotValidException(HttpServletRequest req, MethodArgumentNotValidException ex) {
        logger.debug("Argument error executing {} {} : {}", req.getMethod(), req.getRequestURI(), ex.getMessage());

        HttpStatus httpStatus = UNPROCESSABLE_ENTITY;               // 422

        return new ResponseEntity(new RestValidationErrorsResponse(
            httpStatus.value(), getMsg("Validations failed."), exceptionToErrorCode(ex), processFieldErrors(ex)), httpStatus);
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<RestResponse> handleMissingParameter(HttpServletRequest req, ServletRequestBindingException ex) {
        logger.warn("Binding error executing {} {}", req.getMethod(), req.getRequestURI(), ex);

        HttpStatus httpStatus = HttpStatus.UNPROCESSABLE_ENTITY;    // 422

        return new ResponseEntity<>(new RestErrorResponse(httpStatus.value(), getMsg(ex.getMessage()), exceptionToErrorCode(ex)), httpStatus);
    }

    @ExceptionHandler(GeneralSecurityException.class)
    public ResponseEntity<RestResponse> handleGeneralSecurityException(HttpServletRequest req, GeneralSecurityException ex) {
        if (ex instanceof DigestException /* || ... */) {
            logger.error("Security error executing {} {}", req.getMethod(), req.getRequestURI(), ex);
            return new ResponseEntity<>(new RestErrorResponse(
                INTERNAL_SERVER_ERROR.value(), getMsg(ex.getMessage()), exceptionToErrorCode(ex)), INTERNAL_SERVER_ERROR);  // 500
        }
        if (ex instanceof AccountLockedException) {
            logger.warn("Trying to access a locked account at {} {}", req.getMethod(), req.getRequestURI(), ex);
        } else {
            logger.debug("Security error executing {} {}", req.getMethod(), req.getRequestURI(), ex);
        }
        return new ResponseEntity<>(new RestErrorResponse(
            UNAUTHORIZED.value(), getMsg(ex.getMessage()), exceptionToErrorCode(ex)), UNAUTHORIZED);                    // 401
    }

    protected MessageSource getMessageSource() {
        return messageSource;
    }

    protected String getMsg(String key) {
        if (getMessageSource()!=null && key!=null) {
            return getMessageSource().getMessage(key, null, key, LocaleContextHolder.getLocale());
        }
        return key;
    }

    protected String getMsg(String key, Object[] args) {
        if (getMessageSource()!=null && key!=null) {
            return getMessageSource().getMessage(key, args, key, LocaleContextHolder.getLocale());
        }
        return key;
    }

    /**
     * Returns a code string of a given exception. Eg.:
     * MethodArgumentNotValidException --> method_argument_not_valid
     */
    protected String exceptionToErrorCode(Throwable e) {
        if (e==null) return null;
        String errorName = UPPER_CAMEL.to(LOWER_UNDERSCORE, e.getClass().getSimpleName().replace("Exception", ""));
        if (!errorName.contains("_")) errorName += "_error";
        return errorName;
    }

    protected List<ValidationError> processFieldErrors(MethodArgumentNotValidException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        List<ValidationError> errors = new ArrayList<>(fieldErrors.size());
        for (FieldError fieldError: fieldErrors) {
            String localizedErrorMessage = resolveLocalizedErrorMessage(fieldError);
            errors.add(new ValidationError(fieldError.getField(), localizedErrorMessage));
        }
        return errors;
    }

    protected String resolveLocalizedErrorMessage(FieldError fieldError) {
        if (getMessageSource()!=null) {
            return getMessageSource().getMessage(fieldError, LocaleContextHolder.getLocale());
        }
        return fieldError.getDefaultMessage();
    }
}
