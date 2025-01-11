package com.httpserver.ws;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class WebSocketFrame {
    private final Opcode opcode;
    private final byte[] payload;
    private final boolean fin;

    public WebSocketFrame(boolean fin, Opcode opcode, byte[] payload) {
        this.fin = fin;
        this.opcode = opcode;
        this.payload = payload;
    }

    public static String toBitString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            for (int i = 7; i >= 0; i--) {
                builder.append((b >> i) & 1);
            }
            builder.append(" ");
        }
        return builder.toString();
    }

    public byte[] getPayload() {
        return payload;
    }

    public String getPayloadAsString() {
        return new String(payload, StandardCharsets.UTF_8);
    }

    public byte[] generateFrameBytes() throws WebSocketParsingException {
        ArrayList<Byte> bytes = new ArrayList<>();
        // First byte: FIN flag, opcode
        byte firstByte = fin ? (byte) 0b10000000 : (byte) 0b00000000;
        bytes.add((byte) (firstByte | opcode.code));

        // Second byte: MASK flag, payload length
        // payload from server side must not be masked
        byte secondByte = (byte) 0b00000000;
        final int length = payload.length;
        if (length <= 125) {
            bytes.add((byte) (secondByte | payload.length));
        } else if (length <= 65535) { // 2^16 - 1
            bytes.add((byte) (secondByte | 126));
            bytes.add((byte) ((length >> 8) & 0xFF));
            bytes.add((byte) (length & 0xFF));
        } else {
            // As the max. possible length of payload is 2^64, we omit the payload length check for it
            bytes.add((byte) (secondByte | 127));
            for (int i = 7; i >= 0; i--) {
                bytes.add((byte) ((length >> (i * 8)) & 0xFF));
            }
        }

        // No masking key needed because of server frame
        for (byte b : payload) {
            bytes.add(b);
        }

        byte[] res = new byte[bytes.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = bytes.get(i);
        }
        return res;
    }

    public Opcode getOpcode() {
        return opcode;
    }
}