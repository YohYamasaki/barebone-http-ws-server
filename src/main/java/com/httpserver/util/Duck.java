package com.httpserver.util;

public class Duck {
    public static byte[] Say(byte[] payload) {
        byte[] appendStringBytes = "（ ".getBytes();
        byte[] prependStringByte = " ）Oo｡. \uD83E\uDD86".getBytes();
        int serverPayloadLength = appendStringBytes.length + payload.length + prependStringByte.length;

        byte[] resBytes = new byte[serverPayloadLength];
        for (int i = 0; i < serverPayloadLength; i++) {
            if (i < appendStringBytes.length) {
                resBytes[i] = appendStringBytes[i];
            } else if (i < appendStringBytes.length + payload.length) {
                resBytes[i] = payload[i - appendStringBytes.length];
            } else {
                resBytes[i] = prependStringByte[i - appendStringBytes.length - payload.length];
            }
        }
        return resBytes;
    }

}
