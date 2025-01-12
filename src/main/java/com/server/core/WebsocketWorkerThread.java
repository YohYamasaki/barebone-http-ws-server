package com.server.core;

import com.server.util.Duck;
import com.server.ws.Opcode;
import com.server.ws.WebSocketFrame;
import com.server.ws.WebSocketParser;
import com.server.ws.WebSocketParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Thread handles WebSocket messages.
 */
public class WebsocketWorkerThread extends Thread {
    private final static Logger LOGGER = LoggerFactory.getLogger(WebsocketWorkerThread.class);
    private final Socket socket;
    private final WebSocketParser parser = new WebSocketParser();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    InputStream inputStream;
    OutputStream outputStream;

    public WebsocketWorkerThread(Socket socket) throws IOException {
        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
    }

    @Override
    public void run() {
        try {
            LOGGER.info("WebSocket worker thread started.");
            sendPing();
            while (!Thread.currentThread().isInterrupted()) {
                WebSocketFrame clientFrame = parser.parseWebsocketFrame(inputStream);
                handleFrame(clientFrame);
                if (clientFrame.getOpcode() == Opcode.CLOSE) break;
            }
        } catch (IOException e) {
            LOGGER.error("Error in WebSocket worker thread: ", e);
        } catch (WebSocketParsingException e) {
            LOGGER.error("WebSocket parsing error: ", e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Send WebSocket frame and flush the output stream.
     *
     * @param frame frame to be sent
     */
    private void sendFrame(WebSocketFrame frame) throws IOException, WebSocketParsingException {
        outputStream.write(frame.generateFrameBytes());
        outputStream.flush();
    }

    /**
     * Set the scheduler to send a ping frame to the client every 5 sec.
     */
    private void sendPing() {
        executor.scheduleAtFixedRate(() -> {
            WebSocketFrame pingFrame = new WebSocketFrame.Builder().fin(true).opcode(Opcode.PING).build();
            try {
                sendFrame(pingFrame);
            } catch (IOException | WebSocketParsingException e) {
                throw new RuntimeException(e);
            }
            LOGGER.info("Ping frame sent.");
        }, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * Handle the WebSocket frame sent by the client and respond to it based on the opcode.
     *
     * @param clientFrame WebSocket frame from the client
     */
    private void handleFrame(WebSocketFrame clientFrame) throws WebSocketParsingException, IOException {
        WebSocketFrame serverFrame = null;
        switch (clientFrame.getOpcode()) {
            case Opcode.TEXT -> {
                LOGGER.info("Payload string: {}", clientFrame.getPayloadAsString());
                // Build a "duck say" payload based on the client frame payload
                final byte[] serverPayload = Duck.Say(clientFrame.getPayload());
                serverFrame = new WebSocketFrame.Builder().fin(true).opcode(Opcode.TEXT).payload(serverPayload).build();
            }
            case Opcode.CLOSE -> {
                LOGGER.info("Close frame received.");
                // Build a close response frame
                serverFrame = new WebSocketFrame.Builder().fin(true).opcode(Opcode.CLOSE).build();
            }
            case Opcode.PONG -> LOGGER.info("Pong frame received.");
            default -> throw new WebSocketParsingException("Unknown opcode " + clientFrame.getOpcode());
        }

        if (serverFrame != null) {
            sendFrame(serverFrame);
        }
    }
}
