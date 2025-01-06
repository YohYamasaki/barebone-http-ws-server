package com.httpserver.http;

import java.util.Objects;

public class HttpRequest extends HttpMessage {
    private HttpMethod method;
    private String requestTarget;
    private String originalHttpVersion; // literal from the request
    private HttpVersion bestCompatibleHttpVersion;

    public HttpRequest() {
    }

    public HttpMethod getMethod() {
        return method;
    }

    void setMethod(String methodName) throws HttpParsingException {
        for (HttpMethod method : HttpMethod.values()) {
            if (methodName.equals(method.name())) {
                this.method = method;
                return;
            }
        }
        throw new HttpParsingException(
                HttpStatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED
        );
    }

    public HttpVersion getBestCompatibleHttpVersion() {
        return bestCompatibleHttpVersion;
    }

    public String getOriginalHttpVersion() {
        return originalHttpVersion;
    }

    public String getRequestTarget() {
        return requestTarget;
    }

    public void setRequestTarget(String requestTarget) throws HttpParsingException {
        if (requestTarget == null || requestTarget.length() == 0) {
            throw new HttpParsingException(HttpStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR);
        }
        this.requestTarget = requestTarget;
    }

    public void setHttpVersion(String originalHttpVersion) throws HttpParsingException, BadHttpVersionException {
        this.originalHttpVersion = originalHttpVersion;
        this.bestCompatibleHttpVersion = HttpVersion.getBestCompatibleVersion(originalHttpVersion);
        if (this.bestCompatibleHttpVersion == null) {
            throw new HttpParsingException(HttpStatusCode.SERVER_ERROR_505_HTTP_VERSION_NOT_SUPPORTED);
        }
    }

    // TODO: refactor code a bit & add check for Sec-WebSocket-Key value length
    public boolean isWebsocketUpgrade() {
        final String hostValue = getHeaderFields("Host");
        final String upgradeValue = getHeaderFields("Upgrade");
        final String connectionValue = getHeaderFields("Connection");
        final String originValue = getHeaderFields("Origin");
        final String secWebSocketKeyValue = getHeaderFields("Sec-WebSocket-Key");
        final String secWebSocketVersionValue = getHeaderFields("Sec-WebSocket-Version");

        return hostValue != null
                && upgradeValue != null && upgradeValue.contains("websocket")
                && connectionValue != null && connectionValue.contains("Upgrade")
                && originValue != null
                && secWebSocketKeyValue != null
                && Objects.equals(secWebSocketVersionValue, "13");
    }
}
