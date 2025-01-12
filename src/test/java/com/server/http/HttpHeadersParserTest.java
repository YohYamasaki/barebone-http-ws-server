package com.server.http;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HttpHeadersParserTest {
    private HttpParser httpParser;
    private Method parseHeadersMethod;

    @BeforeAll
    public void beforeClass() throws NoSuchMethodException {
        httpParser = new HttpParser();
        Class<HttpParser> cls = HttpParser.class;
        parseHeadersMethod = cls.getDeclaredMethod("parseHeaderFields", InputStreamReader.class, HttpRequest.class);
        parseHeadersMethod.setAccessible(true);
    }

    @Test
    public void simpleSingleHeader() throws InvocationTargetException, IllegalAccessException {
        HttpRequest request = new HttpRequest();
        parseHeadersMethod.invoke(
                httpParser,
                generateSimpleSingleHeaderField(),
                request);
        assertEquals(1, request.getHeaderFieldNames().size());
        assertEquals("localhost:8080", request.getHeaderFields("host"));
    }

    @Test
    public void multipleHeaderFields() throws InvocationTargetException, IllegalAccessException {
        HttpRequest request = new HttpRequest();
        parseHeadersMethod.invoke(
                httpParser,
                generateMultipleHeaderFields(),
                request);
        assertEquals(10, request.getHeaderFieldNames().size());
        assertEquals("localhost:8080", request.getHeaderFields("host"));
    }

    @Test
    public void errorSpaceBeforeColonHeader() throws IllegalAccessException {
        HttpRequest request = new HttpRequest();
        try {
            parseHeadersMethod.invoke(
                    httpParser,
                    generateSpaceBeforeColonErrorHeaderFields(),
                    request);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof HttpParsingException) {
                assertEquals(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST, ((HttpParsingException) e.getCause()).getErrorCode());
            }
        }
    }

    @Test
    public void validWebsocketUpgradeHeader() throws InvocationTargetException, IllegalAccessException {
        HttpRequest request = new HttpRequest();
        parseHeadersMethod.invoke(
                httpParser,
                generateValidWebsocketUpgradeHeaderFields(),
                request);
        assertTrue(request.isWebsocketHandshake());
    }

    @Test
    public void noUpgradeUpgradeWebsocketUpgradeHeader() throws InvocationTargetException, IllegalAccessException {
        HttpRequest request = new HttpRequest();
        parseHeadersMethod.invoke(
                httpParser,
                generateNoUpgradeWebsocketUpgradeHeaderFields(),
                request);
        assertFalse(request.isWebsocketHandshake());
    }

    @Test
    public void invalidSecKeyWebsocketUpgradeHeader() throws InvocationTargetException, IllegalAccessException {
        HttpRequest request = new HttpRequest();
        parseHeadersMethod.invoke(
                httpParser,
                generateInvalidSecKeyWebsocketUpgradeHeaderFields(),
                request);
        assertFalse(request.isWebsocketHandshake());
    }

    private InputStreamReader generateSimpleSingleHeaderField() {
        String rawData = "Host: localhost:8080\r\n";
        InputStream inputStream = new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
        return new InputStreamReader(inputStream, StandardCharsets.US_ASCII);
    }

    private InputStreamReader generateMultipleHeaderFields() {
        String rawData = "Host: localhost:8080\r\n" +
                "Connection: keep-alive\r\n" +
                "Upgrade-Insecure-Requests: 1\r\n" +
                "User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36\r\n" +
                "Sec-Fetch-User: ?1\r\n" +
                "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3\r\n" +
                "Sec-Fetch-Site: none\r\n" +
                "Sec-Fetch-Mode: navigate\r\n" +
                "Accept-Encoding: gzip, deflate, br\r\n" +
                "Accept-Language: en-US,en;q=0.9,es;q=0.8,pt;q=0.7,de-DE;q=0.6,de;q=0.5,la;q=0.4\r\n" +
                "\r\n";
        InputStream inputStream = new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
        return new InputStreamReader(inputStream, StandardCharsets.US_ASCII);
    }

    private InputStreamReader generateSpaceBeforeColonErrorHeaderFields() {
        String rawData = "Host : localhost:8080\r\n\r\n";
        InputStream inputStream = new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
        return new InputStreamReader(inputStream, StandardCharsets.US_ASCII);
    }

    private InputStreamReader generateValidWebsocketUpgradeHeaderFields() {
        String rawData =
                "Host: server.example.com\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
                        "Origin: http://example.com\r\n" +
                        "Sec-WebSocket-Version: 13\r\n\r\n";
        InputStream inputStream = new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
        return new InputStreamReader(inputStream, StandardCharsets.US_ASCII);
    }

    private InputStreamReader generateNoUpgradeWebsocketUpgradeHeaderFields() {
        String rawData =
                "Host: server.example.com\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
                        "Origin: http://example.com\r\n" +
                        "Sec-WebSocket-Version: 13\r\n\r\n";
        InputStream inputStream = new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
        return new InputStreamReader(inputStream, StandardCharsets.US_ASCII);
    }

    private InputStreamReader generateInvalidSecKeyWebsocketUpgradeHeaderFields() {
        String rawData =
                "Host: server.example.com\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Key: dGhlIH\r\n" +
                        "Origin: http://example.com\r\n" +
                        "Sec-WebSocket-Version: 13\r\n\r\n";
        InputStream inputStream = new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
        return new InputStreamReader(inputStream, StandardCharsets.US_ASCII);
    }
}