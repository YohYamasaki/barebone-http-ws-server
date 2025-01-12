package com.server.ws;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * An object to hold WebSocket Frame data.
 */
public class WebSocketFrame {
    private final boolean fin;
    private final boolean mask;
    private final Opcode opcode;
    private final byte[] payload;

    private WebSocketFrame(Builder builder) {
        this.fin = builder.fin;
        this.mask = builder.mask;
        this.opcode = builder.opcode;
        this.payload = builder.payload;
    }


    public Opcode getOpcode() {
        return opcode;
    }

    public byte[] getPayload() {
        return payload;
    }

    public String getPayloadAsString() {
        return new String(payload, StandardCharsets.UTF_8);
    }

    /**
     * Generate a byte array of the WebSocket frame.
     *
     * @return a byte array of the WebSocket frame
     */
    public byte[] generateFrameBytes() {
        ArrayList<Byte> bytes = new ArrayList<>();
        // First byte: FIN flag(1st bit), opcode(5th - 8th bits)
        // 2nd - 4th bits are for RSV, which are not used
        byte firstByte = (byte) (fin ? 0b10000000 : 0b00000000);
        bytes.add((byte) (firstByte | opcode.code));

        // Second byte: MASK flag(1st bit), payload length(2nd - 8th bits)
        // payload from server side must not be masked
        byte secondByte = (byte) (mask ? 0b10000000 : 0b00000000);
        final int length = payload.length;
        if (length <= 125) {
            bytes.add((byte) (secondByte | payload.length));
        } else if (length <= 65535) { // 2^16 - 1
            bytes.add((byte) (secondByte | 126));
            // Insert 2 bytes for extended payload length
            bytes.add((byte) ((length >> 8) & 0xFF));
            bytes.add((byte) (length & 0xFF));
        } else {
            // As the max. possible length of payload is 2^64, we omit the payload length check for it
            // Insert 8 bytes for extended payload length
            bytes.add((byte) (secondByte | 127));
            for (int i = 7; i >= 0; i--) {
                bytes.add((byte) ((length >> (i * 8)) & 0xFF));
            }
        }

        // Add masking bytes
        // Server must not mask the payload, so this is only for the testing purpose
        if (mask) {
            for (int i = 0; i < 4; i++) bytes.add((byte) 0x00);
        }

        // Append payload
        for (byte b : payload) bytes.add(b);

        // Convert the arraylist to array
        byte[] res = new byte[bytes.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = bytes.get(i);
        }
        return res;
    }

    /**
     * Builder of a WebSocketFrame object.
     */
    public static class Builder {
        private boolean fin = true;
        private boolean mask = false; // mask should be false as default since server cannot send masked payload
        private Opcode opcode;
        private byte[] payload;

        public Builder fin(boolean fin) {
            this.fin = fin;
            return this;
        }

        public Builder opcode(Opcode opcode) {
            this.opcode = opcode;
            return this;
        }

        // only for testing purpose
        public Builder mask(boolean mask) {
            this.mask = mask;
            return this;
        }

        public Builder payload(byte[] payload) {
            this.payload = payload;
            return this;
        }

        public WebSocketFrame build() {
            if (opcode == null) {
                throw new NullPointerException();
            }
            if (payload == null) {
                payload = new byte[0];
            }
            return new WebSocketFrame(this);
        }

    }
}