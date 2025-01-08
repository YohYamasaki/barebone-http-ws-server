package com.httpserver.http;

public enum HttpStatusCode {
    //    Client Errors
    CLIENT_ERROR_400_BAD_REQUEST(400, "Bad Request"),
    CLIENT_ERROR_401_METHOD_NOT_ALLOWED(401, "Method Not Allowed"),
    CLIENT_ERROR_414_REQUEST(414, "URL TOo Long"),
    CLIENT_ERROR_404_NOT_FOUND(404, "Not Found"),
    //    Server Errors
    SERVER_ERROR_500_INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    SERVER_ERROR_501_NOT_IMPLEMENTED(501, "Not Implemented"),
    SERVER_ERROR_505_HTTP_VERSION_NOT_SUPPORTED(505, "http version not supported"),
    // HTTP OK
    OK(200, "OK"),
    WEBSOCKET_UPGRADE(101, "Switching Protocols");

    public final int STATUS_CODE;
    public final String MESSAGE;

    HttpStatusCode(int STATUS_CODE, String MESSAGE) {
        this.STATUS_CODE = STATUS_CODE;
        this.MESSAGE = MESSAGE;
    }
}

