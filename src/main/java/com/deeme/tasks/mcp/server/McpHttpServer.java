package com.deeme.tasks.mcp.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import com.deeme.tasks.mcp.McpConfig;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class McpHttpServer {

    private final McpConfig config;
    private final McpProtocol protocol;
    private HttpServer server;
    private ExecutorService executor;
    private final List<SseConnection> connections = new ArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    public McpHttpServer(McpConfig config, McpProtocol protocol) {
        this.config = config;
        this.protocol = protocol;
    }

    public String getHost() {
        return config.host;
    }

    public int getPort() {
        return config.port;
    }

    public McpProtocol getProtocol() {
        return protocol;
    }

    public int getConnectionCount() {
        synchronized (connections) {
            return connections.size();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public synchronized void start() {
        if (running.get())
            return;
        int port = config.port;
        int maxAttempts = 10;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                executor = Executors.newCachedThreadPool();
                server = HttpServer.create(new InetSocketAddress(config.host, port), 0);
                server.createContext("/mcp", this::handleConnection);
                server.setExecutor(executor);
                server.start();
                running.set(true);
                protocol.setBroadcaster(this::broadcast);
                if (port != config.port) {
                    System.out.println(
                            "[MCP Bridge] Port " + config.port + " was in use, using port " + port + " instead");
                }
                System.out.println("[MCP Bridge] Server started on http://" + config.host + ":" + port + "/mcp");
                return;
            } catch (IOException e) {
                if (attempt < maxAttempts - 1) {
                    port++;
                } else {
                    System.err.println("[MCP Bridge] Failed to start server: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void stop() {
        if (!running.get())
            return;
        running.set(false);
        synchronized (connections) {
            for (SseConnection conn : connections)
                conn.close();
            connections.clear();
        }
        if (server != null)
            server.stop(1);
        if (executor != null)
            executor.shutdown();
        System.out.println("[MCP Bridge] Server stopped");
    }

    private void handleConnection(HttpExchange exchange) {
        try {
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                handleCorsPreflight(exchange);
            } else if ("GET".equals(method) && isSseRequest(exchange)) {
                handleSse(exchange);
            } else if ("POST".equals(method)) {
                handlePost(exchange);
            } else {
                sendError(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            System.err.println("[MCP Bridge] Error handling request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleCorsPreflight(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().add("Access-Control-Max-Age", "86400");
        exchange.sendResponseHeaders(204, -1);
    }

    private boolean isSseRequest(HttpExchange exchange) {
        String accept = exchange.getRequestHeaders().getFirst("Accept");
        return accept != null && accept.contains("text/event-stream");
    }

    private void handleSse(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().add("Cache-Control", "no-cache");
        exchange.getResponseHeaders().add("Connection", "keep-alive");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, 0);

        SseConnection conn = new SseConnection(exchange.getResponseBody());
        synchronized (connections) {
            connections.add(conn);
        }

        conn.sendEvent("endpoint", "/mcp");

        executor.execute(() -> {
            try {
                conn.readLoop();
            } catch (IOException e) {
                // client disconnected
            } finally {
                conn.close();
                synchronized (connections) {
                    connections.remove(conn);
                }
            }
        });
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        String body;
        try (InputStream is = exchange.getRequestBody();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int read;
            while ((read = is.read(buf)) != -1)
                baos.write(buf, 0, read);
            body = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        }

        String responseJson = protocol.handleMessage(body);

        byte[] respBytes = responseJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("Connection", "close");
        exchange.sendResponseHeaders(200, respBytes.length);
        exchange.getResponseBody().write(respBytes);
        exchange.getResponseBody().close();
    }

    private void sendError(HttpExchange exchange, int code, String msg) throws IOException {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private void broadcast(String json) {
        synchronized (connections) {
            for (SseConnection conn : connections) {
                try {
                    conn.sendMessage(json);
                } catch (IOException e) {
                    conn.close();
                }
            }
            connections.removeIf(c -> c.isClosed());
        }
    }

    public static class SseConnection {
        private final OutputStream out;
        private final ByteArrayOutputStream incoming = new ByteArrayOutputStream();
        private final CountDownLatch closeLatch = new CountDownLatch(1);
        private volatile boolean closed;

        public SseConnection(OutputStream out) {
            this.out = out;
        }

        public synchronized void sendEvent(String event, String data) throws IOException {
            if (closed)
                return;
            out.write(("event: " + event + "\n").getBytes(StandardCharsets.UTF_8));
            for (String line : data.split("\n")) {
                out.write(("data: " + line + "\n").getBytes(StandardCharsets.UTF_8));
            }
            out.write("\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
        }

        public void sendMessage(String json) throws IOException {
            sendEvent("message", json);
        }

        public void readLoop() throws IOException {
            try {
                closeLatch.await();
            } catch (InterruptedException e) {
                // interrupted on close
            }
        }

        public void close() {
            closed = true;
            closeLatch.countDown();
            try {
                out.close();
            } catch (IOException ignored) {
            }
        }

        public boolean isClosed() {
            return closed;
        }
    }
}
