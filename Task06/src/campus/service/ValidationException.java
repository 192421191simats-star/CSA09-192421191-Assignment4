package campus.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Input the user can correct; carries one message per invalid field, in form order. */
public class ValidationException extends Exception {
    private final Map<String, String> fieldErrors;
    public ValidationException(Map<String, String> fieldErrors) {
        super(String.join(" ", fieldErrors.values()));
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }
    public Map<String, String> fieldErrors() { return fieldErrors; }
}
