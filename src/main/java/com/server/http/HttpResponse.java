package com.server.http;

/**
 * An object to hold HTTP response data.
 */
public class HttpResponse extends HttpMessage {
    private final String httpVersion;
    private final HttpStatusCode statusCode;

    private HttpResponse(HttpResponse.Builder builder) {
        this.httpVersion = builder.httpVersion;
        this.statusCode = builder.statusCode;
        builder.getHeaderFieldNames().forEach(name -> {
            this.addHeaderField(name, builder.getHeaderFields(name));
        });
        this.setMessageBody(builder.getMessageBody());
    }

    public String getReasonPhrase() {
        if (statusCode != null) {
            return statusCode.MESSAGE;
        }
        return null;
    }

    /**
     * Generate a byte array to be sent to clients.
     *
     * @return Byte array of the response data
     */
    public byte[] generateResponseBytes() {
        StringBuilder responseBuilder = new StringBuilder();
        String CRLF = "\r\n";

        responseBuilder.append(httpVersion)
                .append(" ")
                .append(statusCode.STATUS_CODE)
                .append(" ")
                .append(getReasonPhrase())
                .append(CRLF);
        for (String headerName : getHeaderFieldNames()) {
            responseBuilder.append(headerName)
                    .append(": ")
                    .append(getHeaderFields(headerName))
                    .append(CRLF);
        }
        responseBuilder.append(CRLF);
        byte[] responseBytes = responseBuilder.toString().getBytes();
        if (getMessageBody().length == 0)
            return responseBytes;
        byte[] responseWithBody = new byte[responseBytes.length + getMessageBody().length];
        System.arraycopy(responseBytes, 0, responseWithBody, 0, responseBytes.length);
        System.arraycopy(getMessageBody(), 0, responseWithBody, responseBytes.length, getMessageBody().length);
        return responseWithBody;
    }

    /**
     * Builder of an HTTP response object.
     */
    public static class Builder extends HttpMessage {
        private String httpVersion;
        private HttpStatusCode statusCode;

        public Builder httpVersion(String httpVersion) {
            this.httpVersion = httpVersion;
            return this;
        }

        public Builder statusCode(HttpStatusCode statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder addHeader(String headerName, String headerField) {
            this.addHeaderField(headerName, headerField);
            return this;
        }

        public Builder messageBody(byte[] messageBody) {
            this.setMessageBody(messageBody);
            return this;
        }

        public HttpResponse build() {
            return new HttpResponse(this);
        }
    }
}