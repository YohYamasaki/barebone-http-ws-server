package com.httpserver.http;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.Base64;

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

    public boolean isWebsocketUpgrade() {
        final String websocketKeyValue = getHeaderFields("Sec-WebSocket-Key");
        return containsHeader("Host")
                && containsHeaderValue("Upgrade", "websocket")
                && containsHeaderValue("Connection", "Upgrade")
                && containsHeader("Origin")
                && containsHeader("Sec-WebSocket-Key")
                && websocketKeyValue.length() == 24
                && "13".equals(getHeaderFields("Sec-WebSocket-Version"));
    }

    private boolean containsHeader(String headerName) {
        return getHeaderFields(headerName) != null;
    }

    private boolean containsHeaderValue(String headerName, String value) {
        String headerValue = getHeaderFields(headerName);
        return headerValue != null && headerValue.contains(value);
    }

    public String getSecWebsocketAcceptValue() throws HttpParsingException {
        final String websocketKeyValue = getHeaderFields("Sec-WebSocket-Key");
        if (websocketKeyValue == null || websocketKeyValue.length() != 24) {
            throw new HttpParsingException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
        }
        final byte[] hash = DigestUtils.sha1(websocketKeyValue + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11");
        System.out.println();
        return Base64.getEncoder().encodeToString(hash);
    }
}
