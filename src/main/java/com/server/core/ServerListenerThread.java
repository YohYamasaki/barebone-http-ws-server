package com.server.core;

import com.server.core.io.WebRootHandler;
import com.server.core.io.WebRootNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Thread handles TCP socket connection.
 */
public class ServerListenerThread extends Thread {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerListenerThread.class);
    private final ServerSocket serverSocket;
    private final WebRootHandler webRootHandler;

    public ServerListenerThread(int port, String webroot) throws IOException, WebRootNotFoundException {
        this.webRootHandler = new WebRootHandler(webroot);
        this.serverSocket = new ServerSocket(port);
    }

    @Override
    public void run() {
        super.run();

        try {
            while (serverSocket.isBound() && !serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                LOGGER.info("Connection accepted: ", socket.getInetAddress());
                HttpConnectionWorkerThread workerThread = new HttpConnectionWorkerThread(socket, webRootHandler);
                workerThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage(), e);
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOGGER.warn("Failed to close socket: ", e);
            }
        }
    }
}
