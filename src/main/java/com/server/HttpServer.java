package com.server;

import com.server.core.ServerListenerThread;
import com.server.core.io.WebRootNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Driver class for the http server.
 */
public class HttpServer {

    private final static Logger LOGGER = LoggerFactory.getLogger(HttpServer.class);

    public static void main(String[] args) {
        LOGGER.info("server start at http://localhost:8080");
        try {
            ServerListenerThread serverListenerThread = new ServerListenerThread(8080, "webroot");
            serverListenerThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WebRootNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
