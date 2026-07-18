# AGENTS.md — MCP Bridge Feature

## Overview

**MCP Bridge** is a feature of [DmPlugin](../../../../../README.md) that exposes DarkBot's runtime state and control surface over the [Model Context Protocol (MCP)](https://agents.md/). External AI agents read live bot telemetry and act through a local HTTP + SSE endpoint speaking JSON-RPC 2.0.

- **Language:** Java 11 (source & target)
- **Build:** Part of DmPlugin's Gradle build (`build.gradle.kts` at project root)
- **Key deps:** DarkBotAPI `0.9.5` (compile-only), Gson `2.10.1`
- **Runtime:** JVM bundled with DarkBot; plugin loaded by DarkBot's plugin manager, not standalone
- **MCP protocol version:** `2025-03-26` (in `McpProtocol.PROTOCOL_VERSION`)
- **Default endpoint:** `http://127.0.0.1:9876/mcp`

## Architecture

All sources under `src/main/java/com/deeme/tasks/mcp/`. Single feature `McpBridge` at the root.

```
McpBridge            @Feature + Task + Configurable<McpConfig> + Installable. Wires APIs, starts/stops server.
├─ McpConfig         Host/port config (port 1024-65535, default 9876).
├─ StatusPanelEditor Swing panel in DarkBot UI: status label, connection count, restart button.
├─ server/
│  ├─ McpHttpServer  JDK HttpServer on /mcp. SSE for streaming, POST for JSON-RPC. Port auto-increment (10 tries).
│  └─ McpProtocol    JSON-RPC 2.0 dispatcher. Registers resources & tools, dispatches initialize/ping/subscribe/call.
├─ conditions/
│  └─ ConditionSchemaResource  Reflects DarkBot's condition registry into a schema (uses java.lang.invoke, not java.lang.reflect).
├─ resources/        McpResource interface implementations:
│  ├─ BotResource          mcp://bot/state
│  ├─ HeroResource         mcp://hero/info
│  ├─ StatsResource        mcp://stats/session
│  ├─ ModuleResource       bot://module
│  ├─ PluginResource       mcp://plugins/list
│  ├─ ProfileListResource  bot://profiles
│  ├─ ConfigValueResource  bot://config/value?path=...
│  ├─ ConfigTreeResource   bot://config/tree
│  ├─ InspectResource      bot://inspect?root=...&path=...&max_depth=...&max_items=...
│  ├─ NpcConfigResource    bot://config/npc
│  ├─ EntitiesResource     mcp://entities — live NPCs, players, pets, boxes, mines, portals
│  ├─ PetResource          mcp://pet — PET gear, locator, stats
│  ├─ GroupResource        mcp://group — group members, invites
│  ├─ RepairResource       mcp://repair — deaths, last destroyer, revive locations
│  ├─ BoosterResource      mcp://boosters — active boosters with amount % and remaining time
│  ├─ InventoryResource    mcp://inventory — items in inventory
│  ├─ NpcEventResource     mcp://npc_event — NPC event status (generic + agatus)
│  ├─ OreResource          mcp://ores — owned amounts and upgrade slots
│  └─ ConditionSchemaResource  conditions://schema
├─ tools/            McpTool interface implementations:
│  ├─ BotControlTool    toggle_pause — pause/resume bot
│  ├─ PluginReloadTool  reload_plugins — reload all plugins (uses java.lang.invoke)
│  ├─ SetConfigTool     set_config — update config value by dot-path
│  ├─ SetNpcConfigTool  set_npc_config — configure NPC settings (kill, priority, radius)
│  ├─ SetProfileTool    set_profile — switch config profile
│  ├─ ResourceTool      resource — resource access as a tool (fallback for MCP clients)
│  ├─ SetPetGearTool    set_pet — enable/disable PET and/or set its gear
│  ├─ ResetDeathsTool   reset_deaths — zero the hero death counter
│  ├─ MoveToTool        move — goto x y | random | stop
│  ├─ ValidateConditionTool  validate_condition — validate condition DSL string
│  └─ BuildConditionTool     build_condition — build condition DSL from JSON tree
└─ util/
   ├─ Json              Gson helpers (no nulls, typed puts, size counter for ProGuard-stripped methods).
   ├─ ObjectInspector    Reflection-based runtime object snapshotter (delegates to Gson, not java.lang.reflect).
   └─ ObjectInspectorSelfCheck  Runnable self-check (no JUnit).
```

**Extension points:** implement `McpResource` or `McpTool`, register in `McpBridge` constructor — no auto-discovery.

## CRITICAL: PluginClassLoader blocks java.lang.reflect.\*

DarkBot's `PluginClassLoader` blocks **all bytecode references** to `java.lang.reflect.*` from plugin classes. Calling `Class.getMethod()`, `Class.getField()`, `Class.getDeclaredField()`, etc., produces bytecode whose method descriptor references `java.lang.reflect.Method`/`Field` in the constant pool, triggering `NoClassDefFoundError`.

**Use `java.lang.invoke.MethodHandle` instead** (not blocked). This is the approved reflection mechanism:

```java
// ❌ BROKEN — triggers NoClassDefFoundError: java/lang/reflect/Method
Class<?> clazz = Class.forName("...");
Object m = clazz.getMethod("doSomething", String.class);
m.getClass().getMethod("invoke", Object.class, Object[].class).invoke(m, null, args);

// ✅ WORKS — uses java.lang.invoke
MethodHandles.Lookup lookup = MethodHandles.lookup();
MethodHandle mh = lookup.findStatic(clazz, "doSomething",
    MethodType.methodType(void.class, String.class));
mh.invoke(args);
```

**Existing tools using the correct pattern:**

- `PluginReloadTool` — uses `MethodHandle.findGetter` / `findVirtual`
- `ObjectInspector` — delegates all reflection to Gson (parent classloader)
- `ValidateConditionTool` (fixed) — uses `findStatic` / `findVirtual` / `findGetter`
- `ConditionSchemaResource` (fixed) — uses `findStaticGetter` / `findGetter` / `findVirtual`

**Gotchas:**

- `MethodHandle.invoke()` and `invokeWithArguments()` throw `Throwable` (not `Exception`). Wrap with your own `invoke(MethodHandle, Object...)` helper that re-throws checked exceptions properly (see `ValidateConditionTool` or `ConditionSchemaResource`).
- `findStaticGetter` / `findGetter` only work for accessible fields. For non-public fields, use `MethodHandles.privateLookupIn()` (Java 9+). If that also fails, the field is inaccessible without `java.lang.reflect`.

## Build & Deploy

MCP Bridge is built as part of DmPlugin. From the project root:

```bash
# Windows
gradlew.bat build            # compile + jar → build/libs/DmPlugin-2.2.0.jar
gradlew.bat copyFile         # → DmPlugin.jar (deployable name)
gradlew.bat signFile         # runs sign.bat (jarsigner, requires .keystore + USERKEY/KEYPASS)

# Deploy: copy DmPlugin.jar to DarkBot's plugins/ folder
```

No hot reload. Rebuild + restart DarkBot to test changes. Server starts lazily on first `onBackgroundTick()` after install.

## Testing

No test framework. Manual smoke test with curl:

```bash
curl -X POST http://127.0.0.1:9876/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'
```

Then `tools/list`, `tools/call` with `{"name":"toggle_pause","arguments":{}}`, or `resources/read`, `resources/list`.

Self-check runner (no JUnit): `ObjectInspectorSelfCheck` — `java -ea -cp ... com.deeme.tasks.mcp.util.ObjectInspectorSelfCheck`

## Code Style

- One public class per file, filename matches classname.
- PascalCase classes, camelCase methods/variables, `UPPER_SNAKE` constants.
- Use `com.deeme.tasks.mcp.util.Json` helpers for typed puts to `JsonObject`.
- Catch specific exceptions; only bare `Exception` at the top-level dispatcher boundary in `McpProtocol.handleMessage`.
- UTF-8 everywhere (`options.encoding = "UTF-8"`).
- No Lombok annotations where plain Java is equally short.

## Tools Reference

| Tool                 | Description                                                             |
| -------------------- | ----------------------------------------------------------------------- |
| `toggle_pause`       | Pause/resume bot (BotAPI)                                               |
| `get_config`         | Read config by dot-separated path                                       |
| `set_config`         | Update config value (auto-converts type)                                |
| `set_npc_config`     | Configure NPC settings (kill, priority, radius)                         |
| `explore_config`     | Navigate config tree                                                    |
| `list_profiles`      | List config profiles                                                    |
| `set_profile`        | Switch active profile                                                   |
| `list_plugins`       | List loaded plugins                                                     |
| `reload_plugins`     | Reload all plugins (uses `java.lang.invoke` to access `PluginHandler`)  |
| `inspect_object`     | Deep-inspect DarkBot runtime objects                                    |
| `resource`           | Resource access as a tool (fallback for non-resource-aware MCP clients) |
| `validate_condition` | Validate condition DSL string (uses `java.lang.invoke`)                 |
| `build_condition`    | Build condition DSL from structured JSON tree                           |

## Resources Reference

| URI                               | Description                                          |
| --------------------------------- | ---------------------------------------------------- |
| `mcp://bot/state`                 | Bot running state, module status, map info           |
| `mcp://hero/info`                 | Hero stats, ship, cargo                              |
| `mcp://stats/session`             | Session statistics                                   |
| `bot://module`                    | Current module: name, status, running state          |
| `mcp://plugins/list`              | Loaded plugins                                       |
| `bot://profiles`                  | Config profiles                                      |
| `bot://config/value?path=...`     | Config value reader                                  |
| `bot://config/tree`               | Config tree navigator (supports path drilling)       |
| `bot://config/npc`                | NPC configuration (list all or get by name)          |
| `bot://inspect?root=...&path=...` | Object inspector (supports `max_depth`, `max_items`) |
| `conditions://schema`             | Condition DSL schema (all types, values, enums)      |

## Adding a new tool / resource

1. Implement `McpTool` or `McpResource` in the appropriate package.
2. **Always use `java.lang.invoke.MethodHandle`** (not `java.lang.reflect.*`) for any reflection.
3. Register in `McpBridge` constructor: `protocol.registerTool(new FooTool(...))`.
4. If the tool needs a DarkBot API, inject via the `McpBridge` constructor (available: Bot/Hero/Stats/Extensions/StarSystem/Config/PluginAPI).

## Security

- Server binds to `127.0.0.1` by default. Do not change default to `0.0.0.0`.
- CORS allows `*` origin (required for browser-based MCP clients).
- No auth layer — keep it on loopback.
- `McpConfig.port` validated `[1024, 65535]` via `@Number`; don't weaken.
2. **Always use `java.lang.invoke.MethodHandle`** (not `java.lang.reflect.*`) for any reflection.
3. Register in `McpBridge` constructor: `protocol.registerTool(new FooTool(...))`.
4. If the tool needs a DarkBot API, inject via the `McpBridge` constructor (available: Bot/Hero/Stats/Extensions/StarSystem/Config/PluginAPI).

## Security

- Server binds to `127.0.0.1` by default. Do not change default to `0.0.0.0`.
- CORS allows `*` origin (required for browser-based MCP clients).
- No auth layer — keep it on loopback.
- `McpConfig.port` validated `[1024, 65535]` via `@Number`; don't weaken.
