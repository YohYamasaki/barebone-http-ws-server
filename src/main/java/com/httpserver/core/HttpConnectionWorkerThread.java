package com.httpserver.core;

import com.httpserver.core.io.ReadFileException;
import com.httpserver.core.io.WebRootHandler;
import com.httpserver.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class HttpConnectionWorkerThread extends Thread {
    private final static Logger LOGGER = LoggerFactory.getLogger(HttpConnectionWorkerThread.class);
    private final Socket socket;
    private final HttpParser httpParser = new HttpParser();
    private final WebRootHandler webRootHandler;
    private boolean isWebsocketConnection = false;

    public HttpConnectionWorkerThread(Socket socket, WebRootHandler webRootHandler) {
        this.socket = socket;
        this.webRootHandler = webRootHandler;
    }

    @Override
    public void run() {
        InputStream inputStream;
        OutputStream outputStream;
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            HttpRequest request = httpParser.parseHttpRequest(inputStream);
            if (request.isWebsocketUpgrade()) {
                LOGGER.info("WebSocket Upgrade Request detected.");
                // WebSocketハンドシェイク処理
                HttpResponse handshakeResponse = handleWebSocketUpgradeRequest(request);
                outputStream.write(handshakeResponse.getResponseBytes());

                // WebSocketの処理スレッドを起動
                WebsocketWorkerThread websocketWorkerThread = new WebsocketWorkerThread(socket);
                websocketWorkerThread.start();
                isWebsocketConnection = true;
            } else {
                // 通常のHTTPリクエストを処理
                HttpResponse response = handleRequest(request);
                outputStream.write(response.getResponseBytes());
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

    private HttpResponse handleRequest(HttpRequest request) throws HttpParsingException {
        return switch (request.getMethod()) {
            case GET -> {
                LOGGER.info(" * GET Request");
                yield handleGetRequest(request, true);
            }
            case HEAD -> {
                LOGGER.info(" * HEAD Request");
                yield handleGetRequest(request, false);
            }
        };

    }

    private HttpResponse handleGetRequest(HttpRequest request, boolean setMessageBody) {
        try {
            HttpResponse.Builder builder = new HttpResponse.Builder()
                    .httpVersion(request.getBestCompatibleHttpVersion().LITERAL)
                    .statusCode(HttpStatusCode.OK)
                    .addHeader(HttpHeaderName.CONTENT_TYPE.headerName, webRootHandler.getFileMimeType(request.getRequestTarget()));
            if (setMessageBody) {
                byte[] messageBody = webRootHandler.getFileByteArrayData(request.getRequestTarget());
                builder.addHeader(HttpHeaderName.CONTENT_LENGTH.headerName, String.valueOf(messageBody.length))
                        .messageBody(messageBody);
            }
            return builder.build();
        } catch (FileNotFoundException e) {
            return new HttpResponse.Builder()
                    .httpVersion(request.getBestCompatibleHttpVersion().LITERAL)
                    .statusCode(HttpStatusCode.CLIENT_ERROR_404_NOT_FOUND)
                    .build();
        } catch (ReadFileException e) {
            return new HttpResponse.Builder()
                    .httpVersion(request.getBestCompatibleHttpVersion().LITERAL)
                    .statusCode(HttpStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    private HttpResponse handleWebSocketUpgradeRequest(HttpRequest request) throws HttpParsingException {
        return new HttpResponse.Builder()
                .httpVersion(request.getBestCompatibleHttpVersion().LITERAL)
                .statusCode(HttpStatusCode.WEBSOCKET_UPGRADE)
                .addHeader("Upgrade", "websocket")
                .addHeader("Connection", "Upgrade")
                .addHeader("Sec-WebSocket-Accept", request.getSecWebsocketAcceptValue())
                .build();
    }
}
