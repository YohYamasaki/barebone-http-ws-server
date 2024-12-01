package com.httpserver.http;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public abstract class HttpMessage {
    private final HashMap<String, String> headers = new HashMap<>();
    private byte[] messageBody = new byte[0];

    public String toString() {
        String res = "";
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            res = String.format("%s%s: %s\n", res, key, value);
        }
        return res;
    }

    public Set<String> getHeaderNames() {
        return headers.keySet();
    }

    public String getHeader(String headerName) {
        return headers.get(headerName.toLowerCase());
    }

    void addHeader(String headerName, String headerField) {
        headers.put(headerName.toLowerCase(), headerField);
    }

    public byte[] getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(byte[] messageBody) {
        this.messageBody = messageBody;
    }
}
