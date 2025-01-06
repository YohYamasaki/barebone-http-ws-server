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
    private final static Logger LOGGER = LoggerFactory.getLogger(ServerListenerThread.class);
    private final Socket socket;
    private final HttpParser httpParser = new HttpParser();
    private final WebRootHandler webRootHandler;

    public HttpConnectionWorkerThread(Socket socket, WebRootHandler webRootHandler) {
        this.socket = socket;
        this.webRootHandler = webRootHandler;
    }

    @Override
    public void run() {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            HttpRequest request = httpParser.parseHttpRequest(inputStream);
            if (request.isWebsocketUpgrade()) {
                // Switch to websocket connection
            } else {
                HttpResponse response = handleRequest(request);
                outputStream.write(response.getResponseBytes());
            }

            LOGGER.info("Connection finished");
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (HttpParsingException e) {
            LOGGER.info("Bag Request", e);
            HttpResponse response = new HttpResponse.Builder()
                    .httpVersion(HttpVersion.HTTP_1_1.LITERAL)
                    .statusCode(e.getErrorCode())
                    .build();
            try {
                outputStream.write(response.getResponseBytes());
            } catch (IOException ex) {
                LOGGER.error("Problem with communication", e);
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private HttpResponse handleRequest(HttpRequest request) {
        switch (request.getMethod()) {
            case GET:
                LOGGER.info(" * GET Request");
                return handleGetRequest(request, true);
            case HEAD:
                LOGGER.info(" * HEAD Request");
                return handleGetRequest(request, false);
            default:
                return new HttpResponse.Builder()
                        .httpVersion(request.getBestCompatibleHttpVersion().LITERAL)
                        .statusCode(HttpStatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED)
                        .build();
        }
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
}
