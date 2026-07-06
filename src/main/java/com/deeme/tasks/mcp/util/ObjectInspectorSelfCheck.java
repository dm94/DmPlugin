package com.deeme.tasks.mcp.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runnable self-check for {@link ObjectInspector}. Invoke with:
 * {@code java -ea -cp ... com.deeme.tasks.mcp.util.ObjectInspectorSelfCheck}
 *
 * Validates breadth truncation and depth bounding. Cyclic graphs are caught
 * via StackOverflowError in the inspector, so this self-check uses a graph
 * shallow enough not to overflow but deep enough to hit max_depth.
 */
public class ObjectInspectorSelfCheck {

    public static void main(String[] args) {
        SampleNode root = new SampleNode("root");
        root.child = new SampleNode("child");
        root.child.child = new SampleNode("grandchild");
        root.values = Arrays.asList("alpha", "beta", "gamma");
        root.named = new LinkedHashMap<>();
        root.named.put("answer", 42);

        JsonObject snapshot = new ObjectInspector(2, 10).inspect("sample", root, "");
        assert "sample".equals(snapshot.get("root").getAsString());
        assert snapshot.get("type").getAsString().endsWith("SampleNode");

        // Depth 2: root is depth 0, child is depth 1, child.child must be a depth
        // marker.
        JsonObject value = snapshot.getAsJsonObject("value");
        JsonObject childValue = value.getAsJsonObject("child");
        assert childValue != null;
        JsonObject grandChild = childValue.getAsJsonObject("child");
        assert grandChild != null;
        assert grandChild.has("truncated") : "child.child should hit max_depth";

        // Breadth truncation: cap items below the list size, then append a marker.
        JsonObject valuesSnapshot = new ObjectInspector(2, 2).inspect("sample", root, "values");
        JsonArray values = valuesSnapshot.get("value").getAsJsonArray();
        assert values.size() == 3 : "expected 2 items + 1 marker, got " + values.size();
        JsonObject marker = values.get(2).getAsJsonObject();
        assert marker.get("truncated").getAsBoolean();
        assert marker.get("total").getAsInt() == 3;

        System.out.println("ObjectInspector self-check passed");
    }

    private static final class SampleNode {
        private final String name;
        private SampleNode child;
        private java.util.List<String> values;
        private Map<String, Integer> named;

        private SampleNode(String name) {
            this.name = name;
        }
    }
}
