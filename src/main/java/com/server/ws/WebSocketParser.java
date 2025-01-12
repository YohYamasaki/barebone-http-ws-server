package com.server.ws;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parser of the WebSocket frame.
 */
public class WebSocketParser {
    /**
     * The entry point of the WebSocket parser. Does not support following features.
     * <ul>
     * <li>Message fragmentation</li>
     * <li>Bytes of payload longer than the int-holdable length</li>
     * </ul>
     *
     * @param inputStream input stream from the TCP socket
     * @return Parsed WebSocketFrame object
     */
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
        if (payloadLength == 126) {
            byte[] extendedPayloadLengthBytes = new byte[2];
            inputStream.read(extendedPayloadLengthBytes, 0, 2);
            payloadLength = ((extendedPayloadLengthBytes[0] & 0xFF) << 8) |
                    (extendedPayloadLengthBytes[1] & 0xFF);
        } else if (payloadLength == 127) {
            byte[] extendedPayloadLengthBytes = new byte[8];
            inputStream.read(extendedPayloadLengthBytes, 0, 8);
            payloadLength = 0;
            for (byte b : extendedPayloadLengthBytes) {
                payloadLength = (payloadLength << 8) | (b & 0xFF);
            }
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

        return new WebSocketFrame.Builder().fin(fin).opcode(opcode).payload(decodedPayload).build();
    }
}
