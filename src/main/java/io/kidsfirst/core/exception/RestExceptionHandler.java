package io.kidsfirst.core.exception;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.json.simple.JSONObject;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private Environment env;

    public RestExceptionHandler(Environment env){
        this.env = env;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        logException(request, ex);
        return buildResponseEntity(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ClassCastException.class)
    protected ResponseEntity<Object> handleClassCastException(ClassCastException ex, WebRequest request) {
        logException(request, ex);
        return buildResponseEntity(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalAccessException.class)
    protected ResponseEntity<Object> handleNotFoundException(NotFoundException ex, WebRequest request) {
        logException(request, ex);
        return buildResponseEntity(ex, HttpStatus.NOT_FOUND);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logException(request, ex);
        return buildResponseEntity(ex, status);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logException(request, ex);
        return buildResponseEntity(ex, status);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logException(request, ex);
        return buildResponseEntity(ex, status);
    }

    @Override
    protected ResponseEntity<Object> handleMissingPathVariable(MissingPathVariableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logException(request, ex);
        return buildResponseEntity(ex, status);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logException(request, ex);
        return buildResponseEntity(ex, status);
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logException(request, ex);
        return buildResponseEntity(ex, status);
    }

    @Override
    protected ResponseEntity<Object> handleConversionNotSupported(ConversionNotSupportedException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logException(request, ex);
        return buildResponseEntity(ex, status);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logException(request, ex);
        return buildResponseEntity(ex, status);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logException(request, ex);
        return buildResponseEntity(ex, status);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logException(request, ex);
        return buildResponseEntity(ex, status);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logException(request, ex);
        return buildResponseEntity(ex, status);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestPartException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logException(request, ex);
        return buildResponseEntity(ex, status);
    }

    @Override
    protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logException(request, ex);
        return buildResponseEntity(ex, status);
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logException(request, ex);
        return buildResponseEntity(ex, status);
    }

    @Override
    protected ResponseEntity<Object> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logException(request, ex);
        return buildResponseEntity(ex, status);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logException(request, ex);
        return buildResponseEntity(ex, status);
    }

    private ResponseEntity<Object> buildResponseEntity(Exception ex, HttpStatus status) {
        return ResponseEntity.status(status).body(formatException(ex));
    }

    private void logException(WebRequest webRequest, Throwable e) {
        if(webRequest instanceof ServletWebRequest){
            HttpServletRequest request = ((ServletWebRequest)webRequest).getRequest();

            StringBuilder msg = new StringBuilder();
            msg.append(request.getMethod()).append(" ");
            msg.append(request.getRequestURI());

            String queryString = request.getQueryString();
            if (queryString != null) {
                msg.append('?').append(queryString);
            }

            String client = request.getRemoteAddr();
            if (StringUtils.hasLength(client)) {
                msg.append(",\n\tclient=").append(client);
            }

            String user = request.getRemoteUser();
            if (user != null) {
                msg.append(",\n\tuser=").append(user);
            }

            HttpHeaders headers = new ServletServerHttpRequest(request).getHeaders();
            msg.append(",\n\theaders=").append(headers);

            ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
            if (wrapper != null) {
                String payload = "[unknown]";
                byte[] buf = wrapper.getContentAsByteArray();
                if (buf.length > 0) {
                    try {
                        payload = new String(buf, 0, buf.length, wrapper.getCharacterEncoding());
                    } catch (Exception ex) {
                        // Unknown payload
                    }
                }
                msg.append(",\n\tpayload=").append(payload);
            }

            log.error("Error occurred :\ninput=\n\n" + msg.toString() + "\n\nStack trace=", e);
        }
    }

    private JSONObject formatException(Exception e) {
        val output = new JSONObject();
        output.put("error", e.getMessage());

        // Add Stack Trace if environment states debug
        val debug = this.env.getProperty("application.debug", this.env.getProperty("debug"));
        if (debug != null && debug.equalsIgnoreCase("true")) {
            val sb = new StringBuffer();
            output.put("exception", e.getClass().getSimpleName());
            for (StackTraceElement ste : e.getStackTrace()) {
                sb.append(ste.getClassName() + "." + ste.getMethodName() + ": " + ste.getLineNumber() + "\n");
            }
            output.put("stack", sb.toString());

        }

        return output;
    }

}