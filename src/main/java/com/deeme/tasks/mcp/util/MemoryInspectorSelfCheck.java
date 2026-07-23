package com.deeme.tasks.mcp.util;

import java.util.Optional;

/**
 * Runnable self-check for {@link MemoryInspector#parseAddress(String)}.
 * Does not require DarkBot on the classpath — only validates the
 * address parser, which is the only part of the inspector that runs
 * without the GameAPI.
 *
 * Invoke with:
 * {@code java -ea -cp ... com.deeme.tasks.mcp.util.MemoryInspectorSelfCheck}
 */
public class MemoryInspectorSelfCheck {

    public static void main(String[] args) {
        assertEquals(0x1234L, parse("0x1234"));
        assertEquals(0xDEADBEEFL, parse("0xDEADBEEF"));
        assertEquals(0xDEADBEEFL, parse("0Xdeadbeef"));
        assertEquals(0xFFFFFFFFFFFFFFFFL, parse("0xFFFFFFFFFFFFFFFF"));
        assertEquals(1234L, parse("1234"));
        assertEquals(0L, parse("0x0"));
        assertEquals(0L, parse("0"));

        assert !MemoryInspector.parseAddress("hello").isPresent() : "garbage must be rejected";
        assert !MemoryInspector.parseAddress("0xZZ").isPresent() : "non-hex must be rejected";
        assert !MemoryInspector.parseAddress("").isPresent() : "empty must be rejected";
        assert !MemoryInspector.parseAddress("   ").isPresent() : "blank must be rejected";
        assert !MemoryInspector.parseAddress(null).isPresent() : "null must be rejected";
        assert !MemoryInspector.parseAddress("0xFFFFFFFFFFFFFFFFF").isPresent() : "overflow must be rejected";

        System.out.println("MemoryInspector self-check passed");
    }

    private static long parse(String text) {
        Optional<Long> result = MemoryInspector.parseAddress(text);
        assert result.isPresent() : "expected address to parse: " + text;
        return result.get();
    }

    private static void assertEquals(long expected, long actual) {
        assert expected == actual : "expected 0x" + Long.toHexString(expected)
                + " got 0x" + Long.toHexString(actual);
    }
}
