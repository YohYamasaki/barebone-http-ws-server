package com.server.util;

/**
 * Generate a "Duck says" byte array!
 */
public class Duck {
    public static byte[] Say(byte[] originalBytes) {
        byte[] appendStringBytes = "（ ".getBytes();
        byte[] prependStringBytes = " ）Oo｡. \uD83E\uDD86".getBytes();
        int serverPayloadLength = appendStringBytes.length + originalBytes.length + prependStringBytes.length;

        byte[] resBytes = new byte[serverPayloadLength];
        for (int i = 0; i < serverPayloadLength; i++) {
            if (i < appendStringBytes.length) {
                resBytes[i] = appendStringBytes[i];
            } else if (i < appendStringBytes.length + originalBytes.length) {
                resBytes[i] = originalBytes[i - appendStringBytes.length];
            } else {
                resBytes[i] = prependStringBytes[i - appendStringBytes.length - originalBytes.length];
            }
        }
        return resBytes;
    }

}
