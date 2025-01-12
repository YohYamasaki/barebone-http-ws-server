package com.server.ws;

public enum Opcode {
    CONTINUE(0x0), TEXT(0x1), BINARY(0x2), CLOSE(0x8), PING(0x9), PONG(0xA);

    public final int code;

    Opcode(int code) {
        this.code = code;
    }

    public static Opcode fromCode(int code) {
        for (Opcode op : values()) {
            if (op.code == code) return op;
        }
        throw new IllegalArgumentException("Unknown opcode: " + code);
    }
}
