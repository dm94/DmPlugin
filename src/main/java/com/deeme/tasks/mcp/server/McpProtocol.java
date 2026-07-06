package com.deeme.tasks.mcp.server;

import com.google.gson.*;

import com.deeme.tasks.mcp.resources.McpResource;
import com.deeme.tasks.mcp.tools.McpTool;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class McpProtocol {

    private static final String PROTOCOL_VERSION = "2025-03-26";

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final Map<String, McpResource> resources = new LinkedHashMap<>();
    private final Map<String, McpTool> tools = new LinkedHashMap<>();
    private final Map<String, List<Consumer<String>>> subscribers = new ConcurrentHashMap<>();
    private final String serverName;
    private final String serverVersion;

    private Consumer<String> broadcaster = json -> {
    };

    private boolean initialized = false;

    public McpProtocol(String serverName, String serverVersion) {
        this.serverName = serverName;
        this.serverVersion = serverVersion;
    }

    public void registerResource(McpResource resource) {
        resources.put(resource.getUri(), resource);
    }

    public Map<String, McpResource> getResources() {
        return resources;
    }

    public void registerTool(McpTool tool) {
        tools.put(tool.getName(), tool);
    }

    public void setBroadcaster(Consumer<String> broadcaster) {
        this.broadcaster = broadcaster != null ? broadcaster : json -> {
        };
    }

    public void notifyResourceUpdated(String uri) {
        JsonObject params = new JsonObject();
        params.addProperty("uri", uri);
        JsonObject notif = new JsonObject();
        notif.addProperty("jsonrpc", "2.0");
        notif.addProperty("method", "notifications/resources/updated");
        notif.add("params", params);
        broadcaster.accept(gson.toJson(notif));
    }

    public String handleMessage(String json) {
        try {
            JsonObject msg = gson.fromJson(json, JsonObject.class);
            if (msg == null || !"2.0".equals(getString(msg, "jsonrpc")))
                return jsonRpcError(null, -32600, "Invalid Request: not valid JSON-RPC 2.0");

            String method = getString(msg, "method");
            JsonElement idElem = msg.get("id");
            String id = idElem != null && !idElem.isJsonNull() ? idElem.toString() : null;
            boolean isNotification = id == null;

            if (method == null && !isNotification)
                return jsonRpcError(id, -32600, "Invalid Request: missing method");

            String response = dispatch(method, msg.get("params"), id);

            if (isNotification)
                return "";
            return response != null ? response : jsonRpcError(id, -32601, "Method not found: " + method);
        } catch (JsonSyntaxException e) {
            return jsonRpcError(null, -32700, "Parse error: " + e.getMessage());
        } catch (Exception | LinkageError e) {
            return jsonRpcError(null, -32603, "Internal error: " + e.getMessage());
        }
    }

    private String dispatch(String method, JsonElement params, String id) {
        if (!initialized && !"initialize".equals(method) && !"notifications/initialized".equals(method))
            return jsonRpcError(id, -32000, "Server not initialized");

        switch (method) {
            case "initialize":
                return handleInitialize(params, id);
            case "notifications/initialized":
                this.initialized = true;
                return null;
            case "ping":
                return jsonRpcResult(id, new JsonObject());
            case "resources/list":
                return handleResourcesList(id);
            case "resources/read":
                return handleResourcesRead(params, id);
            case "resources/subscribe":
                return handleResourcesSubscribe(params, id);
            case "resources/unsubscribe":
                return handleResourcesUnsubscribe(params, id);
            case "tools/list":
                return handleToolsList(id);
            case "tools/call":
                return handleToolsCall(params, id);
            case "notifications/cancelled":
                return null;
            default:
                return null;
        }
    }

    private String handleInitialize(JsonElement params, String id) {
        JsonObject caps = new JsonObject();
        JsonObject resCaps = new JsonObject();
        resCaps.add("subscribe", new JsonPrimitive(true));
        resCaps.add("listChanged", new JsonPrimitive(false));
        caps.add("resources", resCaps);
        JsonObject toolCaps = new JsonObject();
        toolCaps.add("listChanged", new JsonPrimitive(false));
        caps.add("tools", toolCaps);

        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", serverName);
        serverInfo.addProperty("version", serverVersion);

        JsonObject result = new JsonObject();
        result.addProperty("protocolVersion", PROTOCOL_VERSION);
        result.add("capabilities", caps);
        result.add("serverInfo", serverInfo);
        return jsonRpcResult(id, result);
    }

    private String handleResourcesList(String id) {
        JsonArray arr = new JsonArray();
        for (McpResource res : resources.values()) {
            JsonObject r = new JsonObject();
            r.addProperty("uri", res.getUri());
            r.addProperty("name", res.getName());
            r.addProperty("description", res.getDescription());
            if (res.getMimeType() != null)
                r.addProperty("mimeType", res.getMimeType());
            arr.add(r);
        }
        JsonObject result = new JsonObject();
        result.add("resources", arr);
        return jsonRpcResult(id, result);
    }

    private String handleResourcesRead(JsonElement params, String id) {
        if (params == null || !params.isJsonObject())
            return jsonRpcError(id, -32602, "Invalid params");

        String uri = getString(params.getAsJsonObject(), "uri");
        if (uri == null)
            return jsonRpcError(id, -32602, "Missing uri");

        McpResource res = resolveResource(uri);
        if (res == null)
            return jsonRpcError(id, -32602, "Resource not found: " + uri);

        JsonObject content = new JsonObject();
        content.addProperty("uri", uri);
        if (res.getMimeType() != null)
            content.addProperty("mimeType", res.getMimeType());
        content.addProperty("text", res.read(uri));

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject result = new JsonObject();
        result.add("contents", contents);
        return jsonRpcResult(id, result);
    }

    private McpResource resolveResource(String uri) {
        McpResource res = resources.get(uri);
        if (res != null)
            return res;

        for (Map.Entry<String, McpResource> entry : resources.entrySet()) {
            String key = entry.getKey();
            if (uri.startsWith(key) && uri.length() > key.length()) {
                char next = uri.charAt(key.length());
                if (next == '/' || next == '?') {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String handleResourcesSubscribe(JsonElement params, String id) {
        if (params == null || !params.isJsonObject())
            return jsonRpcError(id, -32602, "Invalid params");

        String uri = getString(params.getAsJsonObject(), "uri");
        if (uri == null)
            return jsonRpcError(id, -32602, "Missing uri");
        if (resolveResource(uri) == null)
            return jsonRpcError(id, -32602, "Resource not found: " + uri);

        subscribers.computeIfAbsent(uri, k -> new ArrayList<>());
        return jsonRpcResult(id, new JsonObject());
    }

    private String handleResourcesUnsubscribe(JsonElement params, String id) {
        if (params == null || !params.isJsonObject())
            return jsonRpcError(id, -32602, "Invalid params");

        String uri = getString(params.getAsJsonObject(), "uri");
        if (uri == null)
            return jsonRpcError(id, -32602, "Missing uri");

        subscribers.remove(uri);
        return jsonRpcResult(id, new JsonObject());
    }

    private String handleToolsList(String id) {
        JsonArray arr = new JsonArray();
        for (McpTool tool : tools.values()) {
            JsonObject t = new JsonObject();
            t.addProperty("name", tool.getName());
            t.addProperty("description", tool.getDescription());
            t.add("inputSchema", tool.getInputSchema());
            arr.add(t);
        }
        JsonObject result = new JsonObject();
        result.add("tools", arr);
        return jsonRpcResult(id, result);
    }

    private String handleToolsCall(JsonElement params, String id) {
        if (params == null || !params.isJsonObject())
            return jsonRpcError(id, -32602, "Invalid params");

        JsonObject p = params.getAsJsonObject();
        String name = getString(p, "name");
        if (name == null)
            return jsonRpcError(id, -32602, "Missing tool name");

        McpTool tool = tools.get(name);
        if (tool == null)
            return jsonRpcError(id, -32602, "Tool not found: " + name);

        JsonElement args = p.get("arguments");
        Map<String, Object> argMap = new HashMap<>();
        if (args != null && args.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : args.getAsJsonObject().entrySet()) {
                argMap.put(e.getKey(), jsonToJava(e.getValue()));
            }
        }

        try {
            String result = tool.call(argMap);
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            content.addProperty("type", "text");
            content.addProperty("text", result);
            contents.add(content);

            JsonObject res = new JsonObject();
            res.add("content", contents);
            return jsonRpcResult(id, res);
        } catch (Exception | LinkageError e) {
            return jsonRpcError(id, -32603, "Tool execution failed: " + e.getMessage());
        }
    }

    private String jsonRpcResult(String id, JsonObject result) {
        JsonObject msg = new JsonObject();
        msg.addProperty("jsonrpc", "2.0");
        msg.add("result", result);
        if (id != null)
            msg.add("id", parseJson(id));
        return gson.toJson(msg);
    }

    private String jsonRpcError(String id, int code, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);

        JsonObject msg = new JsonObject();
        msg.addProperty("jsonrpc", "2.0");
        msg.add("error", error);
        if (id != null)
            msg.add("id", parseJson(id));
        return gson.toJson(msg);
    }

    private JsonElement parseJson(String json) {
        try {
            return gson.fromJson(json, JsonElement.class);
        } catch (JsonSyntaxException e) {
            return new JsonPrimitive(json);
        }
    }

    private String getString(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        return e != null && !e.isJsonNull() ? e.getAsString() : null;
    }

    private Object jsonToJava(JsonElement el) {
        if (el == null || el.isJsonNull())
            return null;
        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isBoolean())
                return p.getAsBoolean();
            if (p.isNumber()) {
                double d = p.getAsDouble();
                if (d == Math.floor(d) && !Double.isInfinite(d))
                    return (long) d;
                return d;
            }
            return p.getAsString();
        }
        if (el.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonElement e : el.getAsJsonArray())
                list.add(jsonToJava(e));
            return list;
        }
        if (el.isJsonObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet())
                map.put(e.getKey(), jsonToJava(e.getValue()));
            return map;
        }
        return el.toString();
    }
}
