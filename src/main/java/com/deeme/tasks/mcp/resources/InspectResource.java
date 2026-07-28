package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.deeme.tasks.mcp.util.MemoryInspector;

import java.util.HashMap;
import java.util.Map;

public class InspectResource implements McpResource {

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    @Override
    public String getUri() {
        return "bot://inspect";
    }

    @Override
    public String getName() {
        return "Object Inspector";
    }

    @Override
    public String getDescription() {
        return "Inspect DarkBot runtime objects by memory address, similar to "
                + "DarkBot's ObjectInspector address box. Use address=0x... (or "
                + "decimal) to read the object's slots directly out of process "
                + "memory. Useful for AI-assisted debugging and discovery.";
    }

    @Override
    public String read(String uri) {
        String address = parseQuery(uri).get("address");
        JsonObject result = new MemoryInspector().inspect(address);
        return gson.toJson(result);
    }

    private Map<String, String> parseQuery(String uri) {
        Map<String, String> params = new HashMap<>();
        int qmark = uri.indexOf('?');
        if (qmark < 0) {
            return params;
        }
        String query = uri.substring(qmark + 1);
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                params.put(pair.substring(0, eq), pair.substring(eq + 1));
            }
        }
        return params;
    }
}
