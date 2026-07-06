# DarkBot-MCP (MCP Bridge)

A [DarkBot](https://github.com/darkbot-reloaded/DarkBot) plugin that exposes the bot's runtime state and control surface over the [Model Context Protocol (MCP)](https://modelcontextprotocol.io/). Enables external AI agents to read live bot telemetry and execute actions through a local HTTP + SSE endpoint speaking JSON-RPC 2.0.

## Features

- **Read-only resources** — query bot state, hero info, stats, loaded plugins
- **Actionable tools** — control bot (pause/resume), read/write config, inspect runtime objects, switch profiles, reload plugins
- **SSE streaming** — subscribe to state change notifications
- **Object inspector** — reflect into DarkBot internals for deep debugging
- **Configurable** — host/port via DarkBot UI (default `127.0.0.1:9876/mcp`)

## Requirements

- DarkBot `1.131.6 beta 3` or later
- Java 11+

## Build

```bash
git clone https://github.com/dm94/DarkBot-MCP.git
cd DarkBot-MCP

# Build the plugin
./gradlew.bat build

# Produce deployable jar
./gradlew.bat copyFile

# (Optional) Sign the jar
./gradlew.bat signFile
```

Output: `build/McpBridge.jar`

## Install

Copy `McpBridge.jar` into DarkBot's `plugins/` folder and restart the bot.

## Usage

With DarkBot running and the plugin loaded, the MCP endpoint is available at `http://127.0.0.1:9876/mcp`.

### Smoke test

```bash
curl -X POST http://127.0.0.1:9876/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'
```

### List available tools

```bash
curl -X POST http://127.0.0.1:9876/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
```

### Call a tool

```bash
curl -X POST http://127.0.0.1:9876/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"toggle_pause","arguments":{}}}'
```

### Read a resource

```bash
curl -X POST http://127.0.0.1:9876/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":4,"method":"resources/read","params":{"uri":"mcp://bot/state"}}'
```

## Tools

| Tool             | Description                               |
| ---------------- | ----------------------------------------- |
| `toggle_pause`   | Pause/resume the bot                      |
| `get_config`     | Read a config value by dot-separated path |
| `set_config`     | Update a config value                     |
| `explore_config` | Navigate the config tree                  |
| `list_profiles`  | List available config profiles            |
| `set_profile`    | Switch active config profile              |
| `list_plugins`   | List loaded plugins                       |
| `reload_plugins` | Reload all plugins                        |
| `inspect_object` | Deep-inspect DarkBot runtime objects      |

## Resources

| URI                   | Description                                |
| --------------------- | ------------------------------------------ |
| `mcp://bot/state`     | Bot running state and config module status |
| `mcp://hero/info`     | Hero stats, ship, cargo                    |
| `mcp://stats/session` | Session statistics                         |
| `mcp://plugins/list`  | Loaded plugins list                        |

## Architecture

```
McpPlugin          Feature entry, wires DarkBot APIs, starts/stops HTTP server
├─ McpConfig       Host/port config (port 1024-65535, default 9876)
├─ StatusPanelEditor  Adds MCP status to DarkBot UI
├─ server/
│  ├─ McpHttpServer   JDK HttpServer on /mcp (SSE + POST)
│  └─ McpProtocol     JSON-RPC 2.0 dispatcher
├─ resources/       McpResource interface implementations
├─ tools/           McpTool interface implementations
└─ util/
   ├─ Json          Gson helpers (no nulls, typed puts)
   └─ ObjectInspector  Reflection-based runtime inspector
```

## Extending

Implement `McpResource` or `McpTool` and register it in `McpPlugin`'s constructor.

## Security

- Binds to `127.0.0.1` by default
- No authentication layer — keep it on loopback
- CORS allows `*` origin (required for browser-based MCP clients)
