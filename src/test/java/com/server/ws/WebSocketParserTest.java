package com.server.ws;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WebSocketParserTest {
    private WebSocketParser parser;

    @BeforeAll
    public void beforeClass() {
        parser = new WebSocketParser();
    }

    @Test
    public void shortSizedPayloadFrame() {
        try {
            final InputStream inputStream = generateShortSizedPayloadFrame();
            final WebSocketFrame frame = parser.parseWebsocketFrame(inputStream);
            assertEquals(Opcode.TEXT, frame.getOpcode());
            assertEquals("123abc", frame.getPayloadAsString());
        } catch (IOException | WebSocketParsingException e) {
            fail();
        }
    }

    @Test
    public void mediumSizedPayloadFrame() {
        try {
            final InputStream inputStream = generateMediumSizedPayloadFrame();
            final WebSocketFrame frame = parser.parseWebsocketFrame(inputStream);

            String payloadString = "a".repeat(1000);
            assertEquals(Opcode.TEXT, frame.getOpcode());
            assertEquals(payloadString, frame.getPayloadAsString());
        } catch (IOException | WebSocketParsingException e) {
            fail();
        }
    }

    @Test
    public void largeSizedPayloadFrame() {
        try {
            final InputStream inputStream = generateLargeSizedPayloadFrame();
            final WebSocketFrame frame = parser.parseWebsocketFrame(inputStream);

            String payloadString = "a".repeat(100000);
            assertEquals(Opcode.TEXT, frame.getOpcode());
            assertEquals(payloadString, frame.getPayloadAsString());
        } catch (IOException | WebSocketParsingException e) {
            fail();
        }
    }

    @Test
    public void notMaskedPayloadFrame() throws WebSocketParsingException {
        InputStream inputStream = generateNotMaskedPayloadFrame();
        WebSocketParsingException exception = assertThrows(WebSocketParsingException.class, () -> {
            parser.parseWebsocketFrame(inputStream);
        });
        assertEquals("Frame from client must be masked.", exception.getMessage());
    }

    @Test
    public void validCloseFrame() {
        try {
            final InputStream inputStream = generateCloseFrame();
            final WebSocketFrame frame = parser.parseWebsocketFrame(inputStream);
            assertEquals(Opcode.CLOSE, frame.getOpcode());
        } catch (IOException | WebSocketParsingException e) {
            fail();
        }
    }

    @Test
    public void validTestPongFrame() {
        try {
            final InputStream inputStream = generatePongFrame();
            final WebSocketFrame frame = parser.parseWebsocketFrame(inputStream);
            assertEquals(Opcode.PONG, frame.getOpcode());
        } catch (IOException | WebSocketParsingException e) {
            fail();
        }
    }

    InputStream generateShortSizedPayloadFrame() throws WebSocketParsingException {
        String payloadString = "123abc";
        WebSocketFrame frame = new WebSocketFrame.Builder().fin(true).mask(true).opcode(Opcode.TEXT).payload(payloadString.getBytes()).build();
        return new ByteArrayInputStream(frame.generateFrameBytes());
    }

    InputStream generateMediumSizedPayloadFrame() throws WebSocketParsingException {
        WebSocketFrame frame = new WebSocketFrame.Builder().fin(true).mask(true).opcode(Opcode.TEXT).payload("a".repeat(1000).getBytes()).build();
        return new ByteArrayInputStream(frame.generateFrameBytes());
    }

    InputStream generateLargeSizedPayloadFrame() throws WebSocketParsingException {
        WebSocketFrame frame = new WebSocketFrame.Builder().fin(true).mask(true).opcode(Opcode.TEXT).payload("a".repeat(100000).getBytes()).build();
        return new ByteArrayInputStream(frame.generateFrameBytes());
    }

    InputStream generateNotMaskedPayloadFrame() throws WebSocketParsingException {
        String payloadString = "123abc";
        WebSocketFrame frame = new WebSocketFrame.Builder().fin(true).mask(false).opcode(Opcode.TEXT).payload(payloadString.getBytes()).build();
        return new ByteArrayInputStream(frame.generateFrameBytes());
    }

    InputStream generateCloseFrame() throws WebSocketParsingException {
        WebSocketFrame frame = new WebSocketFrame.Builder().fin(true).mask(true).opcode(Opcode.CLOSE).build();
        return new ByteArrayInputStream(frame.generateFrameBytes());
    }

    InputStream generatePongFrame() throws WebSocketParsingException {
        WebSocketFrame frame = new WebSocketFrame.Builder().fin(true).mask(true).opcode(Opcode.PONG).build();
        return new ByteArrayInputStream(frame.generateFrameBytes());
    }
}