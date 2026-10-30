package az.baku.managesystemback.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ApiErrorResponse {

    private final String message;
    private final Map<String, List<String>> errors;
}
