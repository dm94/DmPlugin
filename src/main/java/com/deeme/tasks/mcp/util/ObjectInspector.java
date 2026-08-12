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

public class ObjectInspector {

    private static final String REASON_KEY = "reason";

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
        Json.put(result, "root", rootName);
        Json.put(result, "path", normalizePath(path));
        Json.put(result, "type", root != null ? root.getClass().getName() : "null");

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
                current = nthElement(current.getAsJsonArray(), parseIndex(segment));
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

    private JsonElement pruneArray(JsonArray array, int depth) {
        int size = countElements(array);
        if (depth >= maxDepth) {
            return depthMarker(size);
        }
        JsonArray pruned = new JsonArray();
        int included = 0;
        for (JsonElement el : array) {
            if (included >= maxItems)
                break;
            pruned.add(prune(el, depth + 1));
            included++;
        }
        if (size > maxItems) {
            pruned.add(truncationMarker(size, included));
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
        Json.put(marker, REASON_KEY, "max_depth");
        Json.put(marker, "child_count", childCount);
        return marker;
    }

    private JsonObject truncationMarker(int total, int included) {
        JsonObject marker = new JsonObject();
        marker.add("truncated", new JsonPrimitive(true));
        Json.put(marker, "included", included);
        Json.put(marker, "total", total);
        return marker;
    }

    private JsonObject errorNode(String kind, String message) {
        JsonObject node = new JsonObject();
        Json.put(node, "kind", kind);
        Json.put(node, "message", message);
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

    private static JsonElement nthElement(JsonArray array, int index) {
        int i = 0;
        for (JsonElement el : array) {
            if (i == index)
                return el;
            i++;
        }
        throw new IndexOutOfBoundsException("Index " + index + " out of bounds (size=" + i + ")");
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
                    out.name(REASON_KEY).value(reason);
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
                    out.name(REASON_KEY).value("inaccessible");
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
