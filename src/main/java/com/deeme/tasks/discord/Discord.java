package com.deeme.tasks.discord;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemeplus.utils.Backpage;
import com.github.manolo8.darkbot.utils.Time;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BackpageAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.GroupAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.I18nAPI;
import eu.darkbot.api.managers.RepairAPI;
import eu.darkbot.api.managers.StatsAPI;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Task;

import java.text.DecimalFormat;
import java.util.Arrays;

@Feature(name = "Discord (Obsolete)", description = "Use LeanPlugin")
public class Discord implements Task, Configurable<DiscordConfig> {
    protected final PluginAPI api;
    protected final StatsAPI stats;
    protected final BotAPI bot;
    protected final BackpageAPI backpage;
    protected final HeroAPI heroapi;
    protected final GroupAPI group;
    protected final RepairAPI repair;
    protected final I18nAPI i18n;

    private DiscordConfig discordConfig;
    private long nextSend = 0;
    private final DecimalFormat formatter = new DecimalFormat("###,###,###");
    private double lastUridium = 0;
    private double lastCargo = 0;
    private boolean firstTick = true;

    public Discord(PluginAPI api, BotAPI bot, AuthAPI auth, StatsAPI stats, I18nAPI i18n) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog(auth.getAuthId());

        this.api = api;
        this.bot = bot;
        this.stats = stats;
        this.i18n = i18n;
        this.backpage = api.getAPI(BackpageAPI.class);
        this.heroapi = api.getAPI(HeroAPI.class);
        this.group = api.getAPI(GroupAPI.class);
        this.repair = api.getAPI(RepairAPI.class);
    }

    @Override
    public void setConfig(ConfigSetting<DiscordConfig> arg0) {
        this.discordConfig = arg0.getValue();
    }

    @Override
    public void onTickTask() {
        tick();
    }

    private void tick() {
        if (firstTick) {
            firstTick = false;
            sendInfoHelp();
        } else if (this.nextSend <= System.currentTimeMillis()) {
            sendStatistics();
            nextSend = (60000L * discordConfig.intervalMessage) + System.currentTimeMillis();
        }
    }

    public void setConfig(DiscordConfig discordConfig) {
        this.discordConfig = discordConfig;
    }

    private String getAuthorPart() {
        String help = "";
        help += "\"author\": {";
        help += "\"name\": \"Dm94Dani's Projects Discord\",";
        help += "\"url\": \"" + Utils.DISCORD_URL + "\",";
        help += "\"icon_url\": \"https://raw.githubusercontent.com/dm94/dm94/master/FENIX-dm94dani.png\"";
        help += "}";

        return help;
    }

    private void sendInfoHelp() {
        String help = "{";
        help += " \"embeds\": [";
        help += "{";
        help += "\"title\": \"This task is obsolete use LeanPlugin\",";
        help += "\"description\": \"[DarkBot official Discord](https://discord.gg/vXAKu9r)\"";
        help += "}";
        help += "]";
        help += "}";

        if (discordConfig.discordWebHook == null || discordConfig.discordWebHook.isEmpty()) {
            return;
        }
        Backpage.sendMessage(help, discordConfig.discordWebHook);
    }

    private void sendStatistics() {
        int color = 8311585;

        if (lastUridium == stats.getTotalUridium() || stats.getEarnedUridium() < 1000) {
            color = 13632027;
        } else if (stats.getEarnedUridium() < 5000) {
            color = 16098851;
        } else if (stats.getEarnedUridium() < 10000) {
            color = 16312092;
        }

        String best = "{";
        best += " \"embeds\": [";
        best += "{";
        best += "\"title\": \"" + heroapi.getEntityInfo().getUsername() + "\",";
        best += " \"description\": \"" + "Total Uridium: " + formatter.format(stats.getTotalUridium())
                + " | Total Credits: " + formatter.format(stats.getTotalCredits()) + "\",";
        best += "\"color\": " + color + ",";
        best += "\"footer\": {";
        best += "\"text\": \"" + i18n.get(bot.isRunning() ? "gui.map.running" : "gui.map.waiting") + " " +
                Time.toString(stats.getRunningTime().toMillis()) + " | " +
                stats.getPing() + " ping" + " | " +
                "SID Status: " + backpage.getSidStatus() + "\"";
        best += "},";
        best += getAuthorPart();
        best += ",";
        best += "\"fields\": [";
        best += "{" +
                " \"name\": \"Map\"," +
                " \"value\": \"" + heroapi.getMap().getName() + "\"," +
                " \"inline\": true" +
                "},";
        best += "{" +
                " \"name\": \"Group size\",";
        if (group.hasGroup()) {
            best += " \"value\": \"" + group.getSize() + "\",";
        } else {
            best += " \"value\": \" No Group \",";
        }
        best += " \"inline\": true" +
                "},";
        best += "{" +
                " \"name\": \"Credits\"," +
                " \"value\": \"" + stats.getEarnedCredits() + "\","
                +
                " \"inline\": true" +
                "},";
        best += "{" +
                " \"name\": \"Uridium\"," +
                " \"value\": \"" + stats.getEarnedUridium() + "\","
                +
                " \"inline\": true" +
                "},";
        best += "{" +
                " \"name\": \"Experiencie\"," +
                " \"value\": \"" + stats.getEarnedExperience()
                + "\"," +
                " \"inline\": true" +
                "},";
        best += "{" +
                " \"name\": \"Honor\"," +
                " \"value\": \"" + stats.getEarnedHonor() + "\"," +
                " \"inline\": true" +
                "},";
        best += "{" +
                " \"name\": \"Cargo\"," +
                " \"value\": \"" + stats.getCargo() + "/" + stats.getMaxCargo() + "\"," +
                " \"inline\": true" +
                "},";
        best += "{" +
                " \"name\": \"Deaths\"," +
                " \"value\": \"" + repair.getDeathAmount() + "\"," +
                " \"inline\": true" +
                "},";
        best += "{" +
                " \"name\": \"Module\"," +
                " \"value\": \"" + bot.getModule().getClass().getSimpleName() + "\"," +
                " \"inline\": true" +
                "},";
        best += "{" +
                " \"name\": \"Module status\"," +
                " \"value\": \"" + bot.getModule().getStatus() + "\"," +
                " \"inline\": false" +
                "}";
        best += "]";
        best += "}";

        if (discordConfig.sendSidLink) {
            best += ",{";
            String sid = backpage.getSid();
            String instance = backpage.getInstanceURI().toString();
            if (sid == null || sid.isEmpty() || instance == null || instance.isEmpty())
                return;
            String url = instance + "?dosid=" + sid;
            best += "\"description\": \"[SID Login](" + url + ")\"";
            best += "}";
        }
        if (lastUridium == stats.getTotalUridium() && lastCargo == stats.getCargo()) {
            best += ",{";
            best += "\"title\": \"Bot stopped\",";
            best += "\"description\": \"@here Bot stopped\"";
            best += "}";
        }

        best += "]";
        best += "}";

        lastCargo = stats.getCargo();
        lastUridium = stats.getTotalUridium();

        if (discordConfig.discordWebHook == null || discordConfig.discordWebHook.isEmpty()) {
            return;
        }
        Backpage.sendMessage(best, discordConfig.discordWebHook);

    }

}
