package az.baku.managesystemback.exception;

import az.baku.managesystemback.dto.response.ApiErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException ignored
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                "Validation failed.",
                Map.of(
                        "avatar",
                        List.of("Avatar must be 5 MB or smaller.")
                )
        );

        return ResponseEntity
                .status(HttpStatusCode.valueOf(413))
                .body(body);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPart(
            MissingServletRequestPartException ignored
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                "Validation failed.",
                Map.of(
                        "avatar",
                        List.of("Choose an image to upload.")
                )
        );

        return ResponseEntity
                .status(HttpStatusCode.valueOf(422))
                .body(body);
    }
}
