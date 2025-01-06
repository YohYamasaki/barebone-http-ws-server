package com.httpserver.http;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public abstract class HttpMessage {
    private final HashMap<String, String> headerFields = new HashMap<>();
    private byte[] messageBody = new byte[0];

    public String toString() {
        String res = "";
        for (Map.Entry<String, String> entry : headerFields.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            res = String.format("%s%s: %s\n", res, key, value);
        }
        return res;
    }

    public Set<String> getHeaderFieldNames() {
        return headerFields.keySet();
    }

    @Nullable
    public String getHeaderFields(String headerName) {
        return headerFields.get(headerName.toLowerCase());
    }

    void addHeaderField(String fieldName, String fieldValue) {
        headerFields.put(fieldName.toLowerCase(), fieldValue);
    }

    public byte[] getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(byte[] messageBody) {
        this.messageBody = messageBody;
    }
}
