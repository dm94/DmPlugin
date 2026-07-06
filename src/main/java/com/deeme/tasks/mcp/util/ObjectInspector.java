package com.deeme.tasks.mcp.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Snapshots DarkBot runtime objects to JSON for AI-assisted inspection.
 *
 * ponytail: Two DarkBot constraints shape this design.
 * 1. PluginClassLoader blocks java.lang.reflect.* in plugin bytecode, so all
 * reflection is delegated to Gson, which runs under DarkBot's parent loader.
 * 2. DarkBot runs on JDK 17 without --add-opens=java.desktop, so Gson cannot
 * reflect into JDK types like java.awt.Color. A catch-all TypeAdapterFactory
 * wraps every adapter: on any failure it emits an error marker instead of
 * aborting the whole snapshot. The same factory bounds recursion depth,
 * which also makes cyclic graphs safe (a cycle hits the ceiling, not the
 * stack). Breadth is capped by post-pruning the serialized JSON tree.
 * Only Gson methods DarkBot itself uses are called (registerTypeAdapterFactory,
 * toJsonTree) — ProGuard strips the rest from the bundled Gson.
 */
public class ObjectInspector {

    private final int maxDepth;
    private final int maxItems;

    public ObjectInspector(int maxDepth, int maxItems) {
        this.maxDepth = maxDepth;
        this.maxItems = maxItems;
    }

    public JsonObject inspect(String rootName, Object root, String path) {
        SafeFactory factory = new SafeFactory(maxDepth);
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeAdapterFactory(factory)
                .create();

        JsonObject result = new JsonObject();
        result.addProperty("root", rootName);
        result.addProperty("path", normalizePath(path));
        result.addProperty("type", root != null ? root.getClass().getName() : "null");

        try {
            JsonElement tree = gson.toJsonTree(root);
            result.add("value", prune(navigate(tree, path), 0));
        } catch (StackOverflowError e) {
            result.add("value", errorNode("cycle_or_depth", "Object graph too deep or cyclic"));
        } catch (Exception e) {
            result.add("value", errorNode("error", e.getMessage()));
        }
        return result;
    }

    private JsonElement navigate(JsonElement tree, String path) {
        JsonElement current = tree;
        for (String segment : splitPath(path)) {
            if (current == null || current.isJsonNull()) {
                throw new IllegalArgumentException("Cannot resolve '" + segment + "' from null");
            }
            if (current.isJsonObject()) {
                current = current.getAsJsonObject().get(segment);
            } else if (current.isJsonArray()) {
                current = current.getAsJsonArray().get(parseIndex(segment));
            } else {
                throw new IllegalArgumentException("Cannot navigate into primitive at '" + segment + "'");
            }
            if (current == null) {
                throw new IllegalArgumentException("Path segment not found: " + segment);
            }
        }
        return current != null ? current : JsonNull.INSTANCE;
    }

    private JsonElement prune(JsonElement element, int depth) {
        if (element == null || element.isJsonNull()) {
            return JsonNull.INSTANCE;
        }
        if (element.isJsonArray()) {
            return pruneArray(element.getAsJsonArray(), depth);
        }
        if (element.isJsonObject()) {
            return pruneObject(element.getAsJsonObject(), depth);
        }
        return element;
    }

    // ponytail: ProGuard strips JsonArray.size() and JsonObject.size() from
    // DarkBot's bundled Gson. entrySet().size() is safe (JDK Set), arrays are
    // counted by iteration — same pattern as the existing Json.size() helper.
    private JsonElement pruneArray(JsonArray array, int depth) {
        int size = countElements(array);
        if (depth >= maxDepth) {
            return depthMarker(size);
        }
        JsonArray pruned = new JsonArray();
        int limit = Math.min(size, maxItems);
        for (int i = 0; i < limit; i++) {
            pruned.add(prune(array.get(i), depth + 1));
        }
        if (size > maxItems) {
            pruned.add(truncationMarker(size, limit));
        }
        return pruned;
    }

    private JsonElement pruneObject(JsonObject object, int depth) {
        int size = object.entrySet().size();
        if (depth >= maxDepth) {
            return depthMarker(size);
        }
        JsonObject pruned = new JsonObject();
        int count = 0;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (count >= maxItems) {
                break;
            }
            pruned.add(entry.getKey(), prune(entry.getValue(), depth + 1));
            count++;
        }
        if (size > maxItems) {
            pruned.add("__truncated__", truncationMarker(size, count));
        }
        return pruned;
    }

    private static int countElements(JsonArray array) {
        int count = 0;
        for (JsonElement ignored : array) {
            count++;
        }
        return count;
    }

    private JsonObject depthMarker(int childCount) {
        JsonObject marker = new JsonObject();
        marker.add("truncated", new JsonPrimitive(true));
        marker.addProperty("reason", "max_depth");
        marker.addProperty("child_count", childCount);
        return marker;
    }

    private JsonObject truncationMarker(int total, int included) {
        JsonObject marker = new JsonObject();
        marker.add("truncated", new JsonPrimitive(true));
        marker.addProperty("included", included);
        marker.addProperty("total", total);
        return marker;
    }

    private JsonObject errorNode(String kind, String message) {
        JsonObject node = new JsonObject();
        node.addProperty("kind", kind);
        node.addProperty("message", message);
        return node;
    }

    private List<String> splitPath(String path) {
        List<String> segments = new ArrayList<>();
        String normalized = normalizePath(path);
        if (normalized.isEmpty()) {
            return segments;
        }
        for (String segment : normalized.split("\\.")) {
            if (!segment.isEmpty()) {
                segments.add(segment);
            }
        }
        return segments;
    }

    private String normalizePath(String path) {
        return Optional.ofNullable(path).orElse("").trim();
    }

    private int parseIndex(String segment) {
        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected numeric index but got: " + segment, e);
        }
    }

    /**
     * Wraps every Gson adapter to bound recursion depth and swallow reflection
     * failures (JPMS-blocked types like java.awt.Color, cyclic graphs, etc.).
     * A failure emits an inline error marker so the rest of the snapshot
     * still serializes.
     */
    private static final class SafeFactory implements TypeAdapterFactory {

        private final int maxDepth;
        private int depth = 0;

        SafeFactory(int maxDepth) {
            this.maxDepth = maxDepth;
        }

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            try {
                return buildSafeAdapter(gson, type);
            } catch (Exception e) {
                // ponytail: Adapter creation can fail when ReflectiveTypeAdapterFactory
                // tries to access JPMS-blocked fields (e.g. java.awt.Color.value).
                // Returning a marker adapter lets the parent object still serialize.
                return markerAdapter(type.getRawType().getName());
            }
        }

        private <T> TypeAdapter<T> buildSafeAdapter(Gson gson, TypeToken<T> type) {
            TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    if (value == null || isLeaf(value)) {
                        delegate.write(out, value);
                        return;
                    }
                    if (depth >= maxDepth) {
                        writeMarker(out, value, "max_depth");
                        return;
                    }
                    depth++;
                    try {
                        delegate.write(out, value);
                    } catch (Exception | StackOverflowError e) {
                        writeMarker(out, value, "inaccessible");
                    } finally {
                        depth--;
                    }
                }

                @Override
                public T read(JsonReader in) throws IOException {
                    return delegate.read(in);
                }

                private void writeMarker(JsonWriter out, Object value, String reason) throws IOException {
                    out.beginObject();
                    out.name("truncated").value(true);
                    out.name("reason").value(reason);
                    out.name("type").value(value.getClass().getName());
                    out.endObject();
                }
            };
        }

        private static <T> TypeAdapter<T> markerAdapter(String typeName) {
            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    out.beginObject();
                    out.name("truncated").value(true);
                    out.name("reason").value("inaccessible");
                    out.name("type").value(value != null ? value.getClass().getName() : typeName);
                    out.endObject();
                }

                @Override
                public T read(JsonReader in) {
                    return null;
                }
            };
        }

        private static boolean isLeaf(Object value) {
            return value instanceof String
                    || value instanceof Number
                    || value instanceof Boolean
                    || value instanceof Character
                    || value instanceof Enum<?>;
        }
    }
}
