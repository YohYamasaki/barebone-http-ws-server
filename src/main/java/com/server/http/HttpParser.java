package com.server.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Parser of an HTTP request from an input stream.
 */
public class HttpParser {
    private static final int SP = 0x20; // 32
    private static final int CR = 0x0D; // 30
    private static final int LF = 0x0A; // 10

    /**
     * Main entry for parsing an HTTP request. Parse HTTP header request line and header fields to create an HttpRequest object.
     *
     * @param inputStream input stream of a TCP socket
     * @return parsed HttpRequest object
     */
    public HttpRequest parseHttpRequest(InputStream inputStream) throws HttpParsingException {
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.US_ASCII);
        HttpRequest request = new HttpRequest();
        try {
            parseRequestLine(reader, request);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            parseHeaderFields(reader, request);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return request;
    }

    /**
     * Parse HTTP Request line and update the provided HTTPRequest object.
     *
     * @param reader  input stream reader from the TCP socket input stream
     * @param request HttpRequest object to be updated
     */
    private void parseRequestLine(InputStreamReader reader, HttpRequest request) throws IOException, HttpParsingException {
        StringBuilder processingDataBuffer = new StringBuilder();

        boolean methodParsed = false;
        boolean requestTargetParsed = false;

        int b;
        while ((b = reader.read()) != -1) {
            if (b == SP) { // Tokenise the request line by SP
                if (!methodParsed) {
                    request.setMethod(processingDataBuffer.toString());
                    methodParsed = true;
                } else if (!requestTargetParsed) {
                    request.setRequestTarget(processingDataBuffer.toString());
                    requestTargetParsed = true;
                } else {
                    throw new HttpParsingException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                }
                processingDataBuffer.delete(0, processingDataBuffer.length());
            } else if (b == CR) { // End of the request line
                b = reader.read();
                // Line feed must come after carriage return
                if (b != LF || !methodParsed || !requestTargetParsed) {
                    throw new HttpParsingException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                }

                // HTTP version is placed right before the CRLF in the first line
                try {
                    request.setHttpVersion(processingDataBuffer.toString());
                } catch (BadHttpVersionException e) {
                    throw new HttpParsingException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                }

                return;
            } else {
                processingDataBuffer.append((char) b);
                if (!methodParsed && processingDataBuffer.length() > HttpMethod.MAX_LENGTH) {
                    throw new HttpParsingException(HttpStatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED);
                }
            }
        }
    }

    /**
     * Parse HTTP header fields and update the provided HTTPRequest object.
     *
     * @param reader  input stream reader from the TCP socket input stream
     * @param request HttpRequest object to be updated
     */
    private void parseHeaderFields(InputStreamReader reader, HttpRequest request) throws IOException, HttpParsingException {
        StringBuilder processingDataBuffer = new StringBuilder();
        boolean crlfFound = false;

        int b;
        while ((b = reader.read()) >= 0) {
            if (b == CR) {
                b = reader.read();
                // Line feed must come after carriage return
                if (b != LF) {
                    throw new HttpParsingException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                }
                // Two CRLF received, end of Headers section
                if (crlfFound) return;
                // Handle
                crlfFound = true;
                processSingleHeaderField(processingDataBuffer.toString(), request);
                processingDataBuffer.delete(0, processingDataBuffer.length());
            } else {
                crlfFound = false;
                processingDataBuffer.append((char) b);
            }
        }
    }

    /**
     * Add header field and value pair to the provided HttpRequest object.
     *
     * @param rawFieldLine raw HTTP header field line
     * @param request      HttpRequest object to be updated
     */
    private void processSingleHeaderField(String rawFieldLine, HttpRequest request) throws HttpParsingException {
        String[] headerFieldParts = rawFieldLine.split(":", 2);
        if (headerFieldParts.length != 2) {
            throw new HttpParsingException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
        }

        String fieldName = headerFieldParts[0].trim();
        String fieldValue = headerFieldParts[1].trim();
        request.addHeaderField(fieldName, fieldValue);
    }
}
