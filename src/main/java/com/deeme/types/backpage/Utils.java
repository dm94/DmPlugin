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

import eu.darkbot.util.Popups;
import eu.darkbot.util.SystemUtils;

public class Utils {

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

    public static boolean checkDiscordApi(String id) {
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

        if (allData.contains("true")) {
            saveCheckDiscord();
            return true;
        }

        return false;
    }

    public static void saveCheckDiscord() {
        Preferences prefs = Preferences.userNodeForPackage(Utils.class);
        prefs.putBoolean("discord", true);
        prefs.putLong("nextDiscordCheck", System.currentTimeMillis() + 1296000000);
    }

    public static boolean checkDiscordCached(String id) {
        Preferences prefs = Preferences.userNodeForPackage(Utils.class);

        if (prefs.getLong("nextDiscordCheck", 0) > System.currentTimeMillis()) {
            if (prefs.getBoolean("discord", false)) {
                return true;
            }
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

    public static boolean discordCheck(String authID) {
        String discordID = parseDataToDiscordID(authID);

        if (checkDiscordCached(discordID)) {
            return true;
        }

        return false;
    }

    public static void showDiscordDialog() {
        JButton discordBtn = new JButton("Discord");
        JButton closeBtn = new JButton("Close");
        discordBtn.addActionListener(e -> {
            SystemUtils.openUrl("https://discord.gg/GPRTRRZJPw");
            SwingUtilities.getWindowAncestor(discordBtn).setVisible(false);
        });
        closeBtn.addActionListener(e -> {
            SwingUtilities.getWindowAncestor(closeBtn).setVisible(false);
        });

        Popups.showMessageSync("DmPlugin",
                new JOptionPane("To use this option you need to be on my discord", JOptionPane.INFORMATION_MESSAGE,
                        JOptionPane.DEFAULT_OPTION, null, new Object[] { discordBtn, closeBtn }));
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
            closeBtn.addActionListener(e -> {
                SwingUtilities.getWindowAncestor(closeBtn).setVisible(false);
            });

            Popups.showMessageSync("DmPlugin donate",
                    new JOptionPane(
                            "You can help improve the plugin by donating. \n You get nothing extra if you donate.",
                            JOptionPane.INFORMATION_MESSAGE,
                            JOptionPane.DEFAULT_OPTION, null, new Object[] { donateBtn, closeBtn }));
        }
    }
}