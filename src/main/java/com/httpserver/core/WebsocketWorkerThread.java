package com.httpserver.core;

import com.httpserver.util.Duck;
import com.httpserver.ws.Opcode;
import com.httpserver.ws.WebSocketFrame;
import com.httpserver.ws.WebSocketParser;
import com.httpserver.ws.WebSocketParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebsocketWorkerThread extends Thread {
    private final static Logger LOGGER = LoggerFactory.getLogger(WebsocketWorkerThread.class);
    private final Socket socket;
    private final WebSocketParser parser = new WebSocketParser();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final byte[] emptyPayload = new byte[0];

    public WebsocketWorkerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            LOGGER.info("WebSocket worker thread started.");
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            // Keep sending ping frame to the client every 1 sec
            executor.scheduleAtFixedRate(() -> {
                WebSocketFrame pingFrame = new WebSocketFrame(true, Opcode.PING, emptyPayload);
                try {
                    outputStream.write(pingFrame.generateFrameBytes());
                } catch (IOException | WebSocketParsingException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("Sent ping to the client");
            }, 0, 5, TimeUnit.SECONDS);

            while (!Thread.currentThread().isInterrupted()) {
                WebSocketFrame clientFrame = parser.parseWebsocketFrame(inputStream);

                WebSocketFrame serverFrame;
                if (clientFrame.getOpcode() == Opcode.TEXT) {
                    LOGGER.info("Payload string: {}", clientFrame.getPayloadAsString());
                    // Build a response to the client
                    final byte[] serverPayload = Duck.Say(clientFrame.getPayload());
                    serverFrame = new WebSocketFrame(true, Opcode.TEXT, serverPayload);
                    outputStream.write(serverFrame.generateFrameBytes());
                } else if (clientFrame.getOpcode() == Opcode.CLOSE) {
                    LOGGER.info("Close frame received.");
                    serverFrame = new WebSocketFrame(true, Opcode.CLOSE, emptyPayload);
                    outputStream.write(serverFrame.generateFrameBytes());
                    break;
                } else if (clientFrame.getOpcode() == Opcode.PONG) {
                    LOGGER.info("Pong frame received.");
                } else {
                    throw new WebSocketParsingException("Unknown opcode " + clientFrame.getOpcode());
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error in WebSocket worker thread: ", e);
        } catch (WebSocketParsingException e) {
            try {
                socket.close();
                executor.shutdown();
                LOGGER.info("Socket closed by WebSocketParsingException");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } finally {
            try {
                socket.close();
                executor.shutdown();
                LOGGER.info("Socket closed");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
