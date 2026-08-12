package com.deeme.tasks.mcp.tools;

import com.deeme.tasks.mcp.util.Json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import com.deeme.tasks.mcp.resources.McpResource;

import java.util.Map;

public class ResourceTool implements McpTool {

    private static final String DESCRIPTION_KEY = "description";

    private final Map<String, McpResource> resources;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public ResourceTool(Map<String, McpResource> resources) {
        this.resources = resources;
    }

    @Override
    public String getName() {
        return "resource";
    }

    @Override
    public String getDescription() {
        return "Access bot resources. Use action 'list' to enumerate all available resources with their URIs, "
                + "or action 'read' with a specific URI to get the resource content. "
                + "This is a tool-based fallback for MCP clients that don't fully support resources.";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        Json.put(schema, "type", "object");

        JsonObject props = new JsonObject();

        JsonObject actionProp = new JsonObject();
        Json.put(actionProp, "type", "string");
        Json.put(actionProp, DESCRIPTION_KEY, "Action: 'list' to enumerate resources, 'read' to get a resource by URI");
        JsonArray enum_ = new JsonArray();
        enum_.add(new JsonPrimitive("list"));
        enum_.add(new JsonPrimitive("read"));
        actionProp.add("enum", enum_);
        props.add("action", actionProp);

        JsonObject uriProp = new JsonObject();
        Json.put(uriProp, "type", "string");
        Json.put(uriProp, DESCRIPTION_KEY, "Resource URI to read (required when action is 'read')");
        props.add("uri", uriProp);

        schema.add("properties", props);

        JsonArray required = new JsonArray();
        required.add(new JsonPrimitive("action"));
        schema.add("required", required);

        return schema;
    }

    @Override
    public String call(Map<String, Object> args) throws Exception {
        String action = (String) args.get("action");
        if (action == null)
            throw new IllegalArgumentException("Missing required parameter 'action'");

        switch (action) {
            case "list":
                return handleList();
            case "read": {
                String uri = (String) args.get("uri");
                if (uri == null)
                    throw new IllegalArgumentException("Missing required parameter 'uri' for action 'read'");
                return handleRead(uri);
            }
            default:
                throw new IllegalArgumentException("Unknown action: " + action + ". Supported: list, read");
        }
    }

    private String handleList() {
        JsonArray arr = new JsonArray();
        for (McpResource res : resources.values()) {
            JsonObject r = new JsonObject();
            Json.put(r, "uri", res.getUri());
            Json.put(r, "name", res.getName());
            Json.put(r, DESCRIPTION_KEY, res.getDescription());
            if (res.getMimeType() != null)
                Json.put(r, "mimeType", res.getMimeType());
            arr.add(r);
        }
        JsonObject result = new JsonObject();
        result.add("resources", arr);
        return gson.toJson(result);
    }

    private String handleRead(String uri) throws Exception {
        McpResource res = resources.get(uri);
        if (res != null)
            return res.read(uri);

        for (Map.Entry<String, McpResource> entry : resources.entrySet()) {
            String key = entry.getKey();
            if (uri.startsWith(key) && uri.length() > key.length()) {
                char next = uri.charAt(key.length());
                if (next == '/' || next == '?') {
                    return entry.getValue().read(uri);
                }
            }
        }

        throw new IllegalArgumentException("Resource not found: " + uri);
    }
}
