package com.server.http;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.Base64;

/**
 * A class to hold HTTP request data.
 */
public class HttpRequest extends HttpMessage {
    private HttpMethod method;
    private String requestTarget;
    private HttpVersion httpVersion;

    public HttpMethod getMethod() {
        return method;
    }

    /**
     * Set HTTP method by string.
     *
     * @param methodName method name as string
     */
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

    public HttpVersion getHttpVersion() {
        return httpVersion;
    }

    /**
     * Set HTTP version by string.
     *
     * @param httpVersionString HTTP version as string
     */
    public void setHttpVersion(String httpVersionString) throws HttpParsingException {
        this.httpVersion = HttpVersion.fromString(httpVersionString);
    }

    public String getRequestTarget() {
        return requestTarget;
    }

    public void setRequestTarget(String requestTarget) throws HttpParsingException {
        if (requestTarget == null || requestTarget.isEmpty()) {
            throw new HttpParsingException(HttpStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR);
        }
        this.requestTarget = requestTarget;
    }

    /**
     * Check if the HTTP request is a WebSocket handshake.
     *
     * @return True if it is a WebSocket handshake
     */
    public boolean isWebsocketHandshake() {
        final String websocketKeyValue = getHeaderFields("Sec-WebSocket-Key");
        return hasHeaderField("Host")
                && hasHeaderValue("Upgrade", "websocket")
                && hasHeaderValue("Connection", "Upgrade")
                && hasHeaderField("Origin")
                && hasHeaderField("Sec-WebSocket-Key")
                && websocketKeyValue != null
                && websocketKeyValue.length() == 24 // Base64 encoded 16 byte nonce should have 24 characters
                && "13".equals(getHeaderFields("Sec-WebSocket-Version"));
    }

    /**
     * Check that the HTTP request has a header field with the provided field name.
     *
     * @param fieldName Target header field name
     * @return True if the request has header field
     */
    private boolean hasHeaderField(String fieldName) {
        return getHeaderFields(fieldName) != null;
    }


    /**
     * Check that the HTTP request has a header field with the specified value in the provided field name.
     *
     * @param fieldName Target header field name
     * @param value     Target header field value
     * @return True if the request has the value in the header field
     */
    private boolean hasHeaderValue(String fieldName, String value) {
        String headerValue = getHeaderFields(fieldName);
        return headerValue != null && headerValue.contains(value);
    }

    /**
     * Generate the value for the `Sec-WebSocket-Accept` header field of the WebSocket server handshake.
     *
     * @return Handshake header field value for `Sec-WebSocket-Accept`
     */
    public String generateSecWebsocketAcceptFieldValue() throws HttpParsingException {
        final String websocketKeyValue = getHeaderFields("Sec-WebSocket-Key");
        if (websocketKeyValue == null || websocketKeyValue.length() != 24) {
            // Base64 encoded 16 byte nonce should have 24 characters
            throw new HttpParsingException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
        }
        final byte[] hash = DigestUtils.sha1(websocketKeyValue + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11");
        return Base64.getEncoder().encodeToString(hash);
    }
}
