package com.server.core;

import com.server.core.io.ReadFileException;
import com.server.core.io.WebRootHandler;
import com.server.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Thread handles HTTP request. GET request & WebSocket protocol upgrade handshake are supported.
 */
public class HttpConnectionWorkerThread extends Thread {
    private final static Logger LOGGER = LoggerFactory.getLogger(HttpConnectionWorkerThread.class);
    private final Socket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final HttpParser httpParser = new HttpParser();
    private final WebRootHandler webRootHandler;
    private boolean isWebsocketConnection = false;

    public HttpConnectionWorkerThread(Socket socket, WebRootHandler webRootHandler) throws IOException {
        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
        this.webRootHandler = webRootHandler;
    }

    @Override
    public void run() {
        try {
            HttpRequest request = httpParser.parseHttpRequest(inputStream);
            if (request.isWebsocketHandshake()) {
                // Try to switch protocol to WebSocket
                LOGGER.info("WebSocket Upgrade Request detected.");
                handleWebSocketUpgradeRequest(request);
                isWebsocketConnection = true;
            } else {
                // Handle normal HTTP request
                HttpResponse response = handleGetRequest(request);
                sendResponse(response);
            }
        } catch (IOException | HttpParsingException e) {
            LOGGER.error("Error processing request: ", e);
        } finally {
            if (!isWebsocketConnection) {
                try {
                    socket.close();
                    LOGGER.info("Socket closed");
                } catch (IOException e) {
                    LOGGER.error("Error closing socket", e);
                }
            }
        }
    }

    /**
     * Send HTTP response and flush the output stream.
     *
     * @param response HTTP response to send
     */
    private void sendResponse(HttpResponse response) throws IOException {
        outputStream.write(response.generateResponseBytes());
        outputStream.flush();
    }

    /**
     * Handle GET request and return HTTP response with or without body.
     *
     * @param request Whether the response includes the body
     * @return HTTP response for the GET request
     */
    private HttpResponse handleGetRequest(HttpRequest request) {
        try {
            HttpResponse.Builder builder = new HttpResponse.Builder()
                    .httpVersion(request.getHttpVersion().literal)
                    .statusCode(HttpStatusCode.OK)
                    .addHeader(HttpHeaderFieldName.CONTENT_TYPE.headerName, webRootHandler.getFileMimeType(request.getRequestTarget()));
            byte[] messageBody = webRootHandler.getFileByteArrayData(request.getRequestTarget());
            builder.addHeader(HttpHeaderFieldName.CONTENT_LENGTH.headerName, String.valueOf(messageBody.length))
                    .messageBody(messageBody);
            return builder.build();
        } catch (FileNotFoundException e) {
            return new HttpResponse.Builder()
                    .httpVersion(request.getHttpVersion().literal)
                    .statusCode(HttpStatusCode.CLIENT_ERROR_404_NOT_FOUND)
                    .build();
        } catch (ReadFileException e) {
            return new HttpResponse.Builder()
                    .httpVersion(request.getHttpVersion().literal)
                    .statusCode(HttpStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    /**
     * Handle WebSocket upgrade request. Start a worker thread to listen WebSocket frames.
     *
     * @param request WebSocket handshake from the client
     */
    private void handleWebSocketUpgradeRequest(HttpRequest request) throws HttpParsingException, IOException {
        HttpResponse handshakeResponse = new HttpResponse.Builder()
                .httpVersion(request.getHttpVersion().literal)
                .statusCode(HttpStatusCode.WEBSOCKET_UPGRADE)
                .addHeader("Upgrade", "websocket")
                .addHeader("Connection", "Upgrade")
                .addHeader("Sec-WebSocket-Accept", request.generateSecWebsocketAcceptFieldValue())
                .build();
        sendResponse(handshakeResponse);
        WebsocketWorkerThread websocketWorkerThread = new WebsocketWorkerThread(socket);
        websocketWorkerThread.start();
    }
}
