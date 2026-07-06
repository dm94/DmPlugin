package com.deeme.tasks.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.deeme.tasks.mcp.util.Json;
import eu.darkbot.api.game.stats.Stats;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.StatsAPI;

public class StatsResource implements McpResource {

    private final StatsAPI stats;
    private final BotAPI bot;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public StatsResource(StatsAPI stats, BotAPI bot) {
        this.stats = stats;
        this.bot = bot;
    }

    @Override
    public String getUri() {
        return "bot://stats";
    }

    @Override
    public String getName() {
        return "Bot Statistics";
    }

    @Override
    public String getDescription() {
        return "Gameplay statistics: credits, uridium, experience, honor, cargo, performance";
    }

    @Override
    public String read(String uri) {
        JsonObject obj = new JsonObject();

        JsonObject general = new JsonObject();
        addStat(general, "credits", Stats.General.CREDITS);
        addStat(general, "uridium", Stats.General.URIDIUM);
        addStat(general, "experience", Stats.General.EXPERIENCE);
        addStat(general, "honor", Stats.General.HONOR);
        addStat(general, "cargo", Stats.General.CARGO);
        addStat(general, "max_cargo", Stats.General.MAX_CARGO);
        obj.add("general", general);

        JsonObject performance = new JsonObject();
        Json.put(performance, "ping", stats.getPing());
        Json.put(performance, "tick_current", Math.round(bot.getTickTime() * 100.0) / 100.0);
        obj.add("performance", performance);

        if (stats.getRunningTime() != null) {
            Json.put(obj, "running_time_seconds", stats.getRunningTime().getSeconds());
        }

        StatsAPI.Stat credits = stats.getStat(Stats.General.CREDITS);
        StatsAPI.Stat uridium = stats.getStat(Stats.General.URIDIUM);
        StatsAPI.Stat exp = stats.getStat(Stats.General.EXPERIENCE);
        StatsAPI.Stat honor = stats.getStat(Stats.General.HONOR);

        JsonObject earned = new JsonObject();
        if (credits != null)
            Json.put(earned, "credits", credits.getEarned());
        if (uridium != null)
            Json.put(earned, "uridium", uridium.getEarned());
        if (exp != null)
            Json.put(earned, "experience", exp.getEarned());
        if (honor != null)
            Json.put(earned, "honor", honor.getEarned());
        obj.add("earned_session", earned);

        return gson.toJson(obj);
    }

    private void addStat(JsonObject obj, String key, StatsAPI.Key statKey) {
        StatsAPI.Stat stat = stats.getStat(statKey);
        if (stat != null) {
            Json.put(obj, key, (long) stat.getCurrent());
        }
    }
}
