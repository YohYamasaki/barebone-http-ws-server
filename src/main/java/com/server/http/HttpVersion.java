package com.server.http;

import java.util.Objects;

public enum HttpVersion {
    HTTP_1_1("HTTP/1.1");

    public final String literal;

    HttpVersion(String LITERAL) {
        this.literal = LITERAL;
    }

    public static HttpVersion fromString(String version) throws HttpParsingException {
        for (HttpVersion versions : values()) {
            if (Objects.equals(versions.literal, version)) return versions;
        }
        throw new HttpParsingException(HttpStatusCode.SERVER_ERROR_505_HTTP_VERSION_NOT_SUPPORTED);
    }
}
