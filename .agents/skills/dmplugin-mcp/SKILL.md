---
name: "dmplugin-mcp"
description: "Drives the DmPlugin MCP Bridge (JSON-RPC 2.0 over HTTP+SSE) to read DarkBot state and invoke actions. Invoke when the user asks to control/inspect DarkBot, query the bot, run a tool, or read a resource via MCP. Requires a live DarkBot instance; after reload_plugins the MCP must be re-initialized."
---

# DmPlugin MCP Bridge

Local MCP server exposed by [DmPlugin](d:\Github\DmPlugin\src\main\java\com\deeme\tasks\mcp\AGENTS.md) inside DarkBot. Speaks JSON-RPC 2.0 over HTTP, with SSE for streaming.

- **Endpoint:** `http://127.0.0.1:9876/mcp` (port configurable 1024-65535; auto-increments up to 10 tries if busy)
- **Protocol version:** `2025-03-26`
- **Authoritative docs:** [AGENTS.md](d:\Github\DmPlugin\src\main\java\com\deeme\tasks\mcp\AGENTS.md) — architecture, full tools/resources tables, extension rules, and the `java.lang.invoke` reflection constraint. Read it before extending the bridge.

## Hard requirements

1. **DarkBot must be running with DmPlugin loaded.** No process = no endpoint. Always probe before any tool call.
2. **After `reload_plugins` the MCP is recreated from scratch.** The old socket dies, the new `McpBridge` instance starts a fresh `McpHttpServer` on the next `onBackgroundTick()`. The protocol handler resets its `initialized` flag, so you **must re-run the full handshake** — see "Lifecycle" below.

## Quick probe

```bash
curl -sS -X POST http://127.0.0.1:9876/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'
```

- `200` with `serverInfo` → alive.
- `Connection refused` / timeout → DarkBot not running, plugin not loaded, or server still booting after a reload. Stop and tell the user to start DarkBot; do not retry blindly.
- `-32000 Server not initialized` → handshake lost (typical after a reload). Re-initialize.

## Lifecycle: connect, reload, reconnect

The protocol requires `initialize` → `notifications/initialized` before any other method. Subscriptions and tool calls against a non-initialized server return `-32000`.

**Normal connect:**
1. `initialize` with `{}` params.
2. Send `notifications/initialized` (no `id`).
3. `tools/list` / `resources/list` to discover what is available.
4. `resources/subscribe` for any URI you want SSE updates on.

**After `reload_plugins` (or any plugin reload, port change, or DarkBot restart):**
1. The current HTTP/SSE connection is dead. Close it.
2. Probe the endpoint with `initialize`. If it fails, wait briefly and retry — the new `McpHttpServer` is started lazily on the first `onBackgroundTick()` of the reloaded `McpBridge`, so there is a small window where the port is unbound.
3. On success, **redo the full handshake** (initialize + `notifications/initialized`).
4. Re-subscribe to any resources you were tracking. Re-list tools if the plugin set may have changed.
5. Tell the user: "Plugins reloaded, MCP re-initialized."

The `McpBridge.uninstall()` stops the server, and the reloaded feature rebuilds it with a brand-new `McpProtocol` instance, so internal state (subscriptions, `initialized` flag) is wiped. There is no in-place refresh.

## Discovering capabilities

Always read the live list rather than hard-coding — the bridge is extended in `McpBridge` constructor without auto-discovery.

```
{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
{"jsonrpc":"2.0","id":3,"method":"resources/list","params":{}}
```

Current tool and resource URIs are listed in the [AGENTS.md](d:\Github\DmPlugin\src\main\java\com\deeme\tasks\mcp\AGENTS.md) reference tables.

## Calling a tool

```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "method": "tools/call",
  "params": {"name": "toggle_pause", "arguments": {}}
}
```

Arguments are decoded by `McpProtocol.jsonToJava` — booleans, numbers (int/double auto), strings, arrays, and objects. `null` → Java `null`. Numeric paths into config go as strings (`"general.foo.bar"`).

## Reading a resource

```json
{
  "jsonrpc": "2.0",
  "id": 5,
  "method": "resources/read",
  "params": {"uri": "mcp://bot/state"}
}
```

URIs with query params (`bot://config/value?path=...`, `mcp://entities?type=npc&limit=20`) are resolved by `McpProtocol.resolveResource` against the registered prefix, so pass the full URI including query string.

## Error codes to recognise

| Code | Meaning | Action |
| ---- | ------- | ------ |
| `-32700` | JSON parse error | Check payload |
| `-32600` | Invalid request | Missing/invalid JSON-RPC fields |
| `-32601` | Method not found | Wrong method or pre-init |
| `-32602` | Invalid params | Bad tool name / URI / arg shape |
| `-32603` | Internal error | Tool threw; `data.message` often has the cause |
| `-32000` | Server not initialized | Re-run the handshake (typical after reload) |

## Critical: PluginClassLoader constraint

DarkBot's `PluginClassLoader` rejects any bytecode referencing `java.lang.reflect.*`. If you write or extend tools/resources under `src/main/java/com/deeme/tasks/mcp/`, use `java.lang.invoke.MethodHandle` for reflection. See the [AGENTS.md](d:\Github\DmPlugin\src\main\java\com\deeme\tasks\mcp\AGENTS.md) section "CRITICAL: PluginClassLoader blocks java.lang.reflect.*" for the pattern and the gotchas (`invoke` throws `Throwable`, `findStaticGetter` only sees accessible fields).

## When something goes wrong

- **Endpoint not reachable:** confirm DarkBot is running. The plugin auto-starts the server on the first background tick after install; if it never bound the port, check the DarkBot logs for `[MCP Bridge] Failed to start server`.
- **Tool returns `-32603` after a code change:** the user probably rebuilt and the running JVM is stale. Tell them to restart DarkBot (full restart, not `reload_plugins`, because `reload_plugins` re-instantiates `McpBridge` but does not recompile bytecode already in the jar).
- **Tools list changed after `reload_plugins`:** expected — re-list and update any cached tool names.
- **Port mismatch:** the configured port (DarkBot config → `mcpconfig.port`, 1024-65535) may have been auto-incremented if busy. Read the actual port from the Status panel (`McpBridge.liveServer.getPort()`) or the `[MCP Bridge] Server started on ...` console line.
