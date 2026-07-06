package com.deeme.tasks.mcp.resources;

public interface McpResource {
    String getUri();

    String getName();

    String getDescription();

    default String getMimeType() {
        return "application/json";
    }

    String read(String uri);
}
