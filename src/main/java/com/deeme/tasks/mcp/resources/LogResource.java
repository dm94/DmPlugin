package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Read-only view of DarkBot's log folder.
 *
 * DarkBot writes its session log to {@code logs/<START_TIME>.log} via
 * {@code com.github.manolo8.darkbot.utils.LogUtils}. The log folder and
 * current session name are exposed as {@code public static final} on
 * that class. They are read here through {@link java.lang.invoke.MethodHandle}
 * (the plugin classloader blocks {@code java.lang.reflect.*}).
 *
 * <p>
 * URI: {@code mcp://log}
 * </p>
 *
 * <p>
 * Query parameters:
 * </p>
 * <ul>
 * <li>{@code tail=N} - last N lines, default 200, max 5000</li>
 * <li>{@code file=YYYY-MM-DD_HH-mm-ss_SSS} - specific session log
 * (the {@code .log} suffix is optional). Default: current session.</li>
 * <li>{@code pattern=substr} - case-insensitive substring filter</li>
 * <li>{@code list=true} - ignore the others and return the list of
 * available {@code logs/*.log} files with size and mtime</li>
 * </ul>
 */
public class LogResource implements McpResource {

  private static final String LOG_UTILS_CLASS = "com.github.manolo8.darkbot.utils.LogUtils";

  private static final int DEFAULT_TAIL = 200;
  private static final int MAX_TAIL = 5000;
  // Hard cap on the number of bytes read from the file end. Keeps
  // the response bounded for huge logs; user can still use `tail` to
  // shrink the returned line count further.
  private static final int MAX_BYTES = 1_048_576; // 1 MiB

  private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
  private final Resolved resolved = Resolved.lazyResolve();

  @Override
  public String getUri() {
    return "mcp://log";
  }

  @Override
  public String getName() {
    return "DarkBot Log";
  }

  @Override
  public String getDescription() {
    return "Read DarkBot session log files. Default returns the last 200 lines of the current "
        + "session log. Use tail=N (1-5000) to change line count, file=YYYY-MM-DD_HH-mm-ss_SSS "
        + "for a specific session, pattern=substr to filter lines (case-insensitive), or "
        + "list=true to enumerate available logs.";
  }

  @Override
  public String read(String uri) {
    Map<String, String> params = parseQuery(uri);

    if ("true".equalsIgnoreCase(params.get("list"))) {
      return listLogs();
    }

    if (resolved == null) {
      return gson.toJson(error("darkbot_unavailable",
          "LogUtils class is not reachable from this plugin"));
    }

    int tail = boundedInteger(params.get("tail"), DEFAULT_TAIL, 1, MAX_TAIL);
    String pattern = params.get("pattern");
    if (pattern != null && pattern.isEmpty()) {
      pattern = null;
    }

    Path logFile = resolveLogFile(params.get("file"));
    if (logFile == null) {
      return gson.toJson(error("log_folder_missing",
          "Cannot resolve DarkBot logs folder"));
    }

    JsonObject result = new JsonObject();
    result.addProperty("file", logFile.getFileName().toString());
    result.addProperty("path", logFile.toString());

    if (!Files.exists(logFile)) {
      result.addProperty("error", "Log file not found");
      return gson.toJson(result);
    }

    long size;
    try {
      size = Files.size(logFile);
    } catch (IOException e) {
      return gson.toJson(error("size_error", e.getMessage()));
    }
    result.addProperty("size_bytes", size);

    List<String> lines = readTail(logFile, tail, pattern);
    result.addProperty("returned_lines", lines.size());
    result.addProperty("tail", tail);
    if (pattern != null) {
      result.addProperty("pattern", pattern);
    }

    JsonArray arr = new JsonArray();
    for (String line : lines) {
      arr.add(new JsonPrimitive(line));
    }
    result.add("lines", arr);
    return gson.toJson(result);
  }

  private String listLogs() {
    JsonObject result = new JsonObject();

    if (resolved == null) {
      return gson.toJson(error("darkbot_unavailable",
          "LogUtils class is not reachable from this plugin"));
    }

    Path folder;
    try {
      folder = (Path) resolved.logFolder.invoke();
    } catch (Throwable t) {
      return gson.toJson(error("log_folder_missing", t.getMessage()));
    }
    if (folder == null) {
      return gson.toJson(error("log_folder_missing", "LOG_FOLDER is null"));
    }

    result.addProperty("folder", folder.toString());
    JsonArray arr = new JsonArray();

    if (Files.exists(folder)) {
      try (Stream<Path> stream = Files.list(folder)) {
        stream.filter(p -> p.getFileName().toString().endsWith(".log"))
            .sorted(Comparator.comparing(Path::getFileName, Comparator.reverseOrder()))
            .forEach(p -> arr.add(fileInfo(p)));
      } catch (IOException e) {
        return gson.toJson(error("list_error", e.getMessage()));
      }
    }
    result.add("files", arr);
    return gson.toJson(result);
  }

  private JsonObject fileInfo(Path p) {
    JsonObject obj = new JsonObject();
    obj.addProperty("name", p.getFileName().toString());
    try {
      obj.addProperty("size_bytes", Files.size(p));
    } catch (IOException e) {
      obj.addProperty("size_bytes", -1);
    }
    try {
      obj.addProperty("modified", Files.getLastModifiedTime(p).toString());
    } catch (IOException e) {
      obj.addProperty("modified", "");
    }
    return obj;
  }

  private Path resolveLogFile(String file) {
    try {
      Path folder = (Path) resolved.logFolder.invoke();
      if (folder == null) {
        return null;
      }
      if (file == null || file.isEmpty()) {
        String start = (String) resolved.startTime.invoke();
        if (start == null || start.isEmpty()) {
          return null;
        }
        return folder.resolve(start + ".log");
      }
      return folder.resolve(file.endsWith(".log") ? file : file + ".log");
    } catch (Throwable t) {
      return null;
    }
  }

  private List<String> readTail(Path file, int n, String pattern) {
    List<String> result = new ArrayList<>();
    long length;
    try {
      length = Files.size(file);
    } catch (IOException e) {
      return result;
    }
    if (length == 0) {
      return result;
    }

    long from = Math.max(0, length - MAX_BYTES);
    byte[] buffer = new byte[(int) (length - from)];
    try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
      raf.seek(from);
      raf.readFully(buffer);
    } catch (IOException e) {
      return result;
    }

    String text = new String(buffer, StandardCharsets.UTF_8);
    // If we started mid-file, drop the partial first line.
    int startIdx = 0;
    if (from > 0) {
      int nl = text.indexOf('\n');
      startIdx = nl >= 0 ? nl + 1 : text.length();
    }
    if (startIdx >= text.length()) {
      return result;
    }

    String[] lines = text.substring(startIdx).split("\\r?\\n", -1);
    // text may end with a trailing newline; split keeps an empty trailing element.
    int total = lines.length;
    if (total > 0 && lines[total - 1].isEmpty()) {
      total--;
    }
    int begin = Math.max(0, total - n);
    String needle = pattern == null ? null : pattern.toLowerCase();
    for (int i = begin; i < total; i++) {
      String line = lines[i];
      if (needle == null || line.toLowerCase().contains(needle)) {
        result.add(line);
      }
    }
    return result;
  }

  private JsonObject error(String code, String message) {
    JsonObject err = new JsonObject();
    err.addProperty("code", code);
    err.addProperty("message", message == null ? "" : message);
    JsonObject obj = new JsonObject();
    obj.addProperty("error", code);
    obj.add("details", err);
    return obj;
  }

  private static Map<String, String> parseQuery(String uri) {
    Map<String, String> params = new HashMap<>();
    int qmark = uri.indexOf('?');
    if (qmark < 0) {
      return params;
    }
    for (String pair : uri.substring(qmark + 1).split("&")) {
      int eq = pair.indexOf('=');
      if (eq > 0) {
        params.put(pair.substring(0, eq), pair.substring(eq + 1));
      }
    }
    return params;
  }

  private static int boundedInteger(String value, int defaultValue, int minimum, int maximum) {
    if (value == null || value.isEmpty()) {
      return defaultValue;
    }
    try {
      int parsed = Integer.parseInt(value);
      if (parsed < minimum || parsed > maximum) {
        return defaultValue;
      }
      return parsed;
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static final class Resolved {
    final MethodHandle logFolder;
    final MethodHandle startTime;

    private Resolved(MethodHandle logFolder, MethodHandle startTime) {
      this.logFolder = logFolder;
      this.startTime = startTime;
    }

    static Resolved lazyResolve() {
      try {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Class<?> logUtils = Class.forName(LOG_UTILS_CLASS);
        return new Resolved(
            findStaticGetter(lookup, logUtils, "LOG_FOLDER", Path.class),
            findStaticGetter(lookup, logUtils, "START_TIME", String.class));
      } catch (Throwable t) {
        return null;
      }
    }

    private static MethodHandle findStaticGetter(MethodHandles.Lookup lookup,
        Class<?> refc, String name, Class<?> type) {
      try {
        return lookup.findStaticGetter(refc, name, type);
      } catch (IllegalAccessException | NoSuchFieldException e) {
        try {
          return MethodHandles.privateLookupIn(refc, lookup)
              .findStaticGetter(refc, name, type);
        } catch (IllegalAccessException | NoSuchFieldException ex) {
          throw new IllegalStateException(
              "Cannot access " + refc.getName() + "." + name, ex);
        }
      }
    }
  }
}
