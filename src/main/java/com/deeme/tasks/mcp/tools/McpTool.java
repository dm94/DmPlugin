package com.deeme.tasks.mcp.tools;

import com.google.gson.JsonObject;

import java.util.Map;

public interface McpTool {
    String getName();

    String getDescription();

    JsonObject getInputSchema();

    String call(Map<String, Object> args) throws Exception;
}
