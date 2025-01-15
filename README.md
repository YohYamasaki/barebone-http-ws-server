# barebone-http-ws-server

A toy server that speaks HTTP and WebSocket protocol. Built for learning purposes.

## Features

- Responds to HTTP GET request and serves requested content from webroot

- Process WebSocket handshake (request-target will be disregarded)

- When receiving a text message from a client, return the same text with decorations

- Ping the client after establishing a WebSocket connection

- Close frame handling

## Not implemented

- Other HTTP request methods from GET

- Message fragmentation of WebSocket

- Handling of any WebSocket frame payload length greater than more than int holdable bytes in Java