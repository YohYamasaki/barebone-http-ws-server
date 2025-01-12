package com.server.http;

public enum HttpHeaderFieldName {
    CONTENT_TYPE("Content-Type"),
    CONTENT_LENGTH("Content-Length");
    public final String headerName;

    HttpHeaderFieldName(String headerName) {
        this.headerName = headerName;
    }
}
