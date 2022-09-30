package com.deeme.tasks;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.types.Length;
import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.core.itf.Configurable;
import com.github.manolo8.darkbot.core.itf.InstructionProvider;
import com.github.manolo8.darkbot.core.itf.Task;
import com.github.manolo8.darkbot.core.manager.StatsManager;
import com.github.manolo8.darkbot.extensions.features.Feature;
import com.github.manolo8.darkbot.utils.Time;
import java.text.DecimalFormat;
import java.util.Arrays;

@Feature(name = "Discord", description = "Used to send statistics to discord")
public class Discord implements Task, Configurable<Discord.DiscordConfig>, InstructionProvider {

    private DiscordConfig discordConfig;
    private Main main;
    private StatsManager statsManager;
    private long nextSend = 0;
    private final DecimalFormat formatter = new DecimalFormat("###,###,###");
    private double lastUridium = 0;
    private double lastCargo = 0;
    private boolean firstTick = true;

    @Override
    public void install(Main main) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            return;
        VerifierChecker.checkAuthenticity();

        Utils.showDonateDialog();

        this.main = main;
        this.statsManager = main.statsManager;
    }

    @Override
    public void tick() {

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

    public static class DiscordConfig {

        @Option(value = "Message Interval", description = "How often a message is sent in minutes")
        @Num(min = 10, max = 500)
        public int intervalMessage = 10;

        @Option(value = "Discord WebHook", description = "Link you get when you create a webhook in discord")
        @Length(10)
        public String discordWebHook = null;

        @Option(value = "Discord Name", description = "Name with which the statistics will be sent")
        @Length(5)
        public String discordName = "Stats";

        @Option(value = "Send SID link", description = "WARNING | It will send the link to connect to the account")
        public boolean sendSidLink = false;

        @Option(value = "Send Session stats", description = "Send the statistics of this session")
        public boolean sendSessionStats = false;

    }

    private void sendInfoHelp() {
        String help = "{";
        help += " \"embeds\": [";
        help += "{";
        help += "\"color\": 4886754,";
        help += "\"author\": {";
        help += "\"name\": \"DarkBot official Discord\",";
        help += "\"url\": \"https://discord.gg/vXAKu9r\",";
        help += "\"icon_url\": \"https://cdn.discordapp.com/attachments/562050347217190952/621799025380687892/darkbot.png\"";
        help += "}";
        help += "}";
        help += ",{";
        help += "\"title\": \"WebHook Color Info\",";
        help += "\"color\": 4886754,";
        help += " \"description\": \"The color of the webhook changes depending on the uridium/hour\",";
        help += "\"fields\": [";
        help += "{" +
                " \"name\": \"Uridium < 1000\"," +
                " \"value\": \"Red\"," +
                " \"inline\": true" +
                "}";
        help += ",{" +
                " \"name\": \"Uridium < 5000\"," +
                " \"value\": \"Orange\"," +
                " \"inline\": true" +
                "}";
        help += ",{" +
                " \"name\": \"Uridium < 10000\"," +
                " \"value\": \"Yellow\"," +
                " \"inline\": true" +
                "}";
        help += ",{" +
                " \"name\": \"10000 < Uridium\"," +
                " \"value\": \"Green\"," +
                " \"inline\": true" +
                "}";
        help += "]";
        help += "}";
        help += "]";
        help += "}";

        if (discordConfig.discordWebHook == null || discordConfig.discordWebHook.isEmpty()) {
            return;
        }
        Utils.sendMessage(help, discordConfig.discordWebHook);
    }

    private void sendStatistics() {
        if (discordConfig.discordName == null || discordConfig.discordName.isEmpty()) {
            discordConfig.discordName = "Stats";
        }
        int color = 8311585;

        if (lastUridium == statsManager.getTotalUridium() || statsManager.earnedUridium() < 1000) {
            color = 13632027;
        } else if (statsManager.earnedUridium() < 5000) {
            color = 16098851;
        } else if (statsManager.earnedUridium() < 10000) {
            color = 16312092;
        }

        String best = "{";
        best += " \"embeds\": [";
        best += "{";
        best += "\"title\": \"" + discordConfig.discordName + "\",";
        best += " \"description\": \"" + "Total Uridium: " + formatter.format(statsManager.getTotalUridium())
                + " | Total Credits: " + formatter.format(statsManager.getTotalCredits()) + "\",";
        best += "\"color\": " + color + ",";
        best += "\"footer\": {";
        best += "\"text\": \"" + (main.isRunning() ? "RUNNING " : "WAITING ")
                + Time.toString(statsManager.runningTime()) + " | " +
                main.pingManager.ping + " ping" + " | " +
                "SID Status: " + main.backpage.sidStatus() + "\"";
        best += "},";
        best += "\"author\": {";
        best += "\"name\": \"Dm94 | DmPlugin\",";
        best += "\"url\": \"https://github.com/dm94/\",";
        best += "\"icon_url\": \"https://raw.githubusercontent.com/dm94/dm94/master/FENIX-dm94dani.png\"";
        best += "},";
        best += "\"fields\": [";
        best += "{" +
                " \"name\": \"Map\"," +
                " \"value\": \"" + main.hero.getMap().getName() + "\"," +
                " \"inline\": true" +
                "},";
        best += "{" +
                " \"name\": \"Group size\",";
        if (main.guiManager.group.hasGroup()) {
            best += " \"value\": \"" + main.guiManager.group.group.size + "\",";
        } else {
            best += " \"value\": \" No Group \",";
        }
        best += " \"inline\": true" +
                "},";
        best += "{" +
                " \"name\": \"cre/h\"," +
                " \"value\": \"" + formatter.format(statsManager.earnedCredits()) + "\"," +
                " \"inline\": true" +
                "},";
        best += "{" +
                " \"name\": \"uri/h\"," +
                " \"value\": \"" + formatter.format(statsManager.earnedUridium()) + "\"," +
                " \"inline\": true" +
                "},";
        best += "{" +
                " \"name\": \"exp/h\"," +
                " \"value\": \"" + formatter.format(statsManager.earnedExperience()) + "\"," +
                " \"inline\": true" +
                "},";
        best += "{" +
                " \"name\": \"hon/h\"," +
                " \"value\": \"" + formatter.format(statsManager.earnedHonor()) + "\"," +
                " \"inline\": true" +
                "},";
        best += "{" +
                " \"name\": \"cargo\"," +
                " \"value\": \"" + main.statsManager.getCargo() + "/" + main.statsManager.getMaxCargo() + "\"," +
                " \"inline\": true" +
                "},";
        best += "{" +
                " \"name\": \"death\"," +
                " \"value\": \"" + main.guiManager.deaths + "\"," +
                " \"inline\": true" +
                "}";
        best += "]";
        best += "}";

        if (discordConfig.sendSessionStats) {
            best += ",{";
            best += "\"title\": \"In this session\",";
            best += "\"author\": {";
            best += "\"name\": \"Dm94 | DmPlugin\",";
            best += "\"url\": \"https://github.com/dm94/\",";
            best += "\"icon_url\": \"https://raw.githubusercontent.com/dm94/dm94/master/FENIX-dm94dani.png\"";
            best += "},";
            best += "\"color\": 4886754,";
            best += "\"fields\": [";
            best += "{" +
                    " \"name\": \"Credits\"," +
                    " \"value\": \"" + formatter.format(statsManager.getEarnedCredits()) + "\"," +
                    " \"inline\": true" +
                    "},";
            best += "{" +
                    " \"name\": \"Uridium\"," +
                    " \"value\": \"" + formatter.format(statsManager.getEarnedUridium()) + "\"," +
                    " \"inline\": true" +
                    "},";
            best += "{" +
                    " \"name\": \"Experiencie\"," +
                    " \"value\": \"" + formatter.format(statsManager.getEarnedExperience()) + "\"," +
                    " \"inline\": true" +
                    "},";
            best += "{" +
                    " \"name\": \"Honor\"," +
                    " \"value\": \"" + formatter.format(statsManager.getEarnedHonor()) + "\"," +
                    " \"inline\": true" +
                    "},";
            best += "{" +
                    " \"name\": \"Rank\"," +
                    " \"value\": \"±"
                    + formatter.format(
                            (statsManager.getEarnedHonor() / 100) + (statsManager.getTotalExperience() / 100000))
                    + "\"," +
                    " \"inline\": true" +
                    "}";
            best += "]";
            best += "}";
        }

        if (discordConfig.sendSidLink) {
            best += ",{";
            String sid = main.statsManager.sid;
            String instance = main.statsManager.instance;
            if (sid == null || sid.isEmpty() || instance == null || instance.isEmpty())
                return;
            String url = instance + "?dosid=" + sid;
            best += "\"description\": \"[SID Login](" + url + ")\"";
            best += "}";
        }
        if (lastUridium == statsManager.getTotalUridium() && lastCargo == main.statsManager.getCargo()) {
            best += ",{";
            best += "\"title\": \"Bot stopped\",";
            best += "\"description\": \"@here Bot stopped\"";
            best += "}";
        }
        if (main.config.GENERAL.SAFETY.MAX_DEATHS == main.guiManager.deaths) {
            best += ",{";
            best += "\"title\": \"Deaths limit reached\",";
            best += "\"description\": \"@here Deaths limit reached\"";
            best += "}";
        }

        best += "]";
        best += "}";

        lastCargo = main.statsManager.getCargo();
        lastUridium = statsManager.getTotalUridium();

        if (discordConfig.discordWebHook == null || discordConfig.discordWebHook.isEmpty()) {
            return;
        }
        Utils.sendMessage(best, discordConfig.discordWebHook);

    }

}
