package com.httpserver.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class WebSocketParser {
    private final static Logger LOGGER = LoggerFactory.getLogger(WebSocketParser.class);

    public WebSocketFrame parseWebsocketFrame(InputStream inputStream) throws WebSocketParsingException, IOException {
        // First byte: FIN flag, opcode
        final int firstByte = inputStream.read();
        boolean fin = (firstByte & 0b10000000) != 0;
        Opcode opcode = Opcode.fromCode(firstByte & 0b00001111);

        // Second byte: MASK flag, payload length
        final int secondByte = inputStream.read();
        boolean mask = (secondByte & 0b10000000) != 0;
        if (!mask) {
            throw new WebSocketParsingException("Frame from client must be masked.");
        }
        int payloadLength = (secondByte & 0b01111111);

        // Handle extended payload length
        if (payloadLength == 126 || payloadLength == 127) {
            int extendedPayloadLength = 0;
            for (int i = 0; i < 2; i++) {
                final int b = inputStream.read();
                extendedPayloadLength += b;
            }

            if (payloadLength == 127) {
                for (int i = 0; i < 2; i++) {
                    final int b = inputStream.read();
                    extendedPayloadLength += b;
                }
            }
            payloadLength = extendedPayloadLength;
        }

        // Masking key
        byte[] maskingKey = new byte[4];
        int bytesRead = inputStream.read(maskingKey);
        if (bytesRead != 4) {
            throw new WebSocketParsingException("Failed to read masking key");
        }

        // Get payload and decode by the masking key
        byte[] decodedPayload = new byte[payloadLength];
        for (int i = 0; i < payloadLength; i++) {
            final byte b = (byte) inputStream.read();
            if (b == -1) {
                throw new WebSocketParsingException("Unexpected end of stream");
            }
            final int maskByte = maskingKey[i % 4];
            decodedPayload[i] = (byte) (b ^ maskByte);
        }

        return new WebSocketFrame(fin, opcode, decodedPayload);
    }
}
