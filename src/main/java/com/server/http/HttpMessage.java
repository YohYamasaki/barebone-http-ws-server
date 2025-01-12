package com.server.http;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Abstract class for HTTP response and request.
 */
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

    public byte[] getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(byte[] messageBody) {
        this.messageBody = messageBody;
    }

    /**
     * Add new header field.
     *
     * @param fieldName  Header field name
     * @param fieldValue Header field value
     */
    void addHeaderField(String fieldName, String fieldValue) {
        headerFields.put(fieldName.toLowerCase(), fieldValue);
    }

    /**
     * Get header field value by field name.
     *
     * @param fieldName Target header field name
     * @return Header field value
     */
    @Nullable
    public String getHeaderFields(String fieldName) {
        return headerFields.get(fieldName.toLowerCase());
    }
}
