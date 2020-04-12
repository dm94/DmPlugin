package com.deeme.types.backpage;

import com.deeme.types.VersionJson;
import com.github.manolo8.darkbot.extensions.features.FeatureDefinition;
import com.github.manolo8.darkbot.extensions.util.Version;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.github.manolo8.darkbot.Main.GSON;

public class Utils {
    private static final Type VERSIONJSON_LIST = new TypeToken<List<VersionJson>>(){}.getType();
    private static final JsonParser JSON_PARSER = new JsonParser();

    public static void sendMessage(String message, String url) {
        if (message == null || message.isEmpty() || url == null || url.isEmpty()) {
            return;
        }

        try {
            HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");

            OutputStream os = conn.getOutputStream();
            os.write(message.getBytes(StandardCharsets.UTF_8));
            os.close();
            conn.getInputStream();
            conn.disconnect();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static VersionJson updateLastVersion() {
        VersionJson lastVersion = null;
        String params = "https://gist.githubusercontent.com/dm94/58c42d0a5957a300bbacd59dc7cbb752/raw/DmPlugin.json";

        try (InputStreamReader in = new InputStreamReader((new URL(params)).openStream())) {
            lastVersion = GSON.fromJson(in, VersionJson.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lastVersion;
    }

    public static List<VersionJson> getVersions() {
        String params = "https://gist.githubusercontent.com/dm94/58c42d0a5957a300bbacd59dc7cbb752/raw/DmPlugin.json";
        List<VersionJson> allVersions = new ArrayList<>();

        try (InputStreamReader in = new InputStreamReader((new URL(params)).openStream())) {
            JsonObject data = JSON_PARSER.parse(in).getAsJsonObject().getAsJsonObject("data");
            allVersions = GSON.fromJson(data.get("versions"), VERSIONJSON_LIST);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return allVersions;
    }

    public static VersionJson getLastValidVersion(List<VersionJson> allVersions, Version botVersion) {

        for (VersionJson versionJson : allVersions) {
            if (new Version(versionJson.getMinVersion()).compareTo(botVersion) == 0 || new Version(versionJson.getMinVersion()).compareTo(botVersion) > 0) {
                return versionJson;
            }
        }

        return null;
    }

    public static boolean newVersionAvailable(VersionJson latestVersion, FeatureDefinition plugin) {
        if (latestVersion == null || plugin == null) return false;

        Version lastVersion = new Version(latestVersion.getVersionNumber());

        return lastVersion.compareTo(plugin.getPlugin().getDefinition().version) > 0;
    }
}
