package dev.noren.staybooking.bootstrap.web.parser;

import dev.noren.staybooking.bootstrap.web.exception.InvalidVersionHeaderException;
import dev.noren.staybooking.bootstrap.web.exception.PreconditionRequiredException;
import org.springframework.stereotype.Component;

@Component
public class VersionHeader {

    public long parseIfMatch(String value) {
        if (value == null || value.isBlank()) {
            throw new PreconditionRequiredException("If-Match header is required");
        }

        String etag = value.trim();

        if (etag.equals("*")
                || etag.startsWith("W/")
                || !etag.startsWith("\"")
                || !etag.endsWith("\"")) {
            throw new InvalidVersionHeaderException("If-Match must contain one strong quoted version ETag");
        }

        long version;
        try {
            version = Long.parseLong(etag.substring(1, etag.length() - 1));
        } catch (NumberFormatException ex) {
            throw new InvalidVersionHeaderException("If-Match must be a number");
        }

        if (version < 0 || !etag.equals(toEtag(version))) {
            throw new InvalidVersionHeaderException("If-Match must be a non-negative integer version ETag");
        }
        return version;
    }

    public String toEtag(long version) {
        return "\"" + version + "\"";
    }

}
