package com.deeme.tasks.mcp.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public final class Json {

    private Json() {
    }

    public static void put(JsonObject o, String key, boolean value) {
        o.add(key, new JsonPrimitive(value));
    }

    public static void put(JsonObject o, String key, Number value) {
        o.add(key, new JsonPrimitive(value));
    }

    public static void put(JsonObject o, String key, JsonElement value) {
        o.add(key, value);
    }

    public static int size(JsonArray array) {
        int count = 0;
        for (JsonElement ignored : array) {
            count++;
        }
        return count;
    }
}
