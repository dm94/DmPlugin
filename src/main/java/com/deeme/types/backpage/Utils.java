package com.deeme.types.backpage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import eu.darkbot.api.extensions.FeatureInfo;
import eu.darkbot.util.Popups;
import eu.darkbot.util.SystemUtils;

public class Utils {
    private static boolean discordChecked = false;

    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    private static void setDiscordCheck() {
        discordChecked = true;
    }

    private static boolean isDiscordChecked() {
        return discordChecked;
    }

    public static void sendMessage(String message, String url) {
        if (message == null || message.isEmpty() || url == null || url.isEmpty()) {
            return;
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
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

    private static boolean checkDiscordApi(String id) {
        String baseURL = "https://checkdiscord.stiletto.live/users/";
        String allData = "";
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(baseURL + id).openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                allData += inputLine;
            }
            in.close();
            conn.disconnect();
        } catch (Exception ex) {
            return false;
        }

        setDiscordCheck();

        if (allData.contains("true")) {
            saveCheckDiscord();
            return true;
        }

        return false;
    }

    private static void saveCheckDiscord() {
        Preferences prefs = Preferences.userNodeForPackage(Utils.class);
        prefs.putBoolean("discord", true);
        prefs.putLong("nextDiscordCheck", System.currentTimeMillis() + 1296000000);
    }

    private static boolean checkDiscordCached(String id) {
        Preferences prefs = Preferences.userNodeForPackage(Utils.class);

        if (prefs.getLong("nextDiscordCheck", 0) > System.currentTimeMillis() && prefs.getBoolean("discord", false)) {
            return true;
        }

        if (isDiscordChecked()) {
            return false;
        }

        return checkDiscordApi(id);
    }

    public static String parseDataToDiscordID(String data) {
        if (data != null && data.contains("-")) {
            String[] strArray = data.split("-");
            if (strArray[1] != null) {
                return strArray[1];
            }

        }

        return data;
    }

    public static synchronized void discordCheck(FeatureInfo featureInfo, String authID) {
        if (authID == null || !isInDiscord(authID)) {
            showDiscordDialog();
            featureInfo
                    .addFailure("To use this option you need to be on my discord", "Log in to my discord and reload");
        }
    }

    public static synchronized boolean isInDiscord(String authID) {
        String discordID = parseDataToDiscordID(authID);
        return checkDiscordCached(discordID);
    }

    public static void showDiscordDialog() {
        JButton discordBtn = new JButton("Discord");
        JButton closeBtn = new JButton("Close");
        discordBtn.addActionListener(e -> {
            SystemUtils.openUrl("https://discord.gg/GPRTRRZJPw");
            SwingUtilities.getWindowAncestor(discordBtn).setVisible(false);
        });
        closeBtn.addActionListener(e -> SwingUtilities.getWindowAncestor(closeBtn).setVisible(false));

        Popups.of("DmPlugin",
                new JOptionPane("To use this option you need to be on my discord", JOptionPane.INFORMATION_MESSAGE,
                        JOptionPane.DEFAULT_OPTION, null, new Object[] { discordBtn, closeBtn }))
                .showAsync();
    }

    public static void showDonateDialog() {
        Preferences prefs = Preferences.userNodeForPackage(Utils.class);

        if (prefs.getLong("donateDialog", 0) <= System.currentTimeMillis()) {
            prefs.putLong("donateDialog", System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000));
            JButton donateBtn = new JButton("Donate");
            JButton closeBtn = new JButton("Close");
            donateBtn.addActionListener(e -> {
                SystemUtils.openUrl(
                        "https://www.paypal.com/donate/?business=JR2XWPSKLWN76&amount=5&no_recurring=0&currency_code=EUR");
                SwingUtilities.getWindowAncestor(donateBtn).setVisible(false);
            });
            closeBtn.addActionListener(e -> SwingUtilities.getWindowAncestor(closeBtn).setVisible(false));

            Popups.of("DmPlugin donate",
                    new JOptionPane(
                            "You can help improve the plugin by donating. \n You get nothing extra if you donate.",
                            JOptionPane.INFORMATION_MESSAGE,
                            JOptionPane.DEFAULT_OPTION, null, new Object[] { donateBtn, closeBtn }))
                    .showAsync();
        }
    }
}