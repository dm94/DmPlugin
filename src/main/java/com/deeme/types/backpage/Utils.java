package com.deeme.types.backpage;

import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.deemeplus.utils.Backpage;

import eu.darkbot.api.extensions.FeatureInfo;
import eu.darkbot.util.Popups;
import eu.darkbot.util.SystemUtils;

public class Utils {
    public static final String DISCORD_URL = "https://discord.gg/GPRTRRZJPw";

    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    public static synchronized void discordCheck(FeatureInfo featureInfo, String authID) {
        if (!Backpage.isInDiscord(authID)) {
            String discordTag = Backpage.getDiscordTagExternal(authID);
            showNeedDiscordDialog(discordTag);
            featureInfo
                    .addFailure("To use this option you need to be on my discord",
                            "ID: " + discordTag);
        }
    }

    public static synchronized void discordDonorCheck(FeatureInfo featureInfo, String authID) {
        if (!Backpage.isDonor(authID)) {
            String discordTag = Backpage.getDiscordTagExternal(authID);
            showDiscordDonorDialog(discordTag);
            featureInfo
                    .addFailure("[PLUS] Only some people can use this feature.",
                            "ID: " + discordTag);
        }
    }

    public static void showNeedDiscordDialog(String discordTag) {
        showDiscordDialog("To use this option you need to be on my discord. ID: " + discordTag);
    }

    public static void showDiscordDonorDialog(String discordTag) {
        showDiscordDialog("[PLUS] Only some people can use this feature. ID: " + discordTag);
    }

    public static void showDiscordDialog(String text) {
        JButton discordBtn = new JButton("Discord");
        JButton closeBtn = new JButton("Close");
        discordBtn.addActionListener(e -> {
            SystemUtils.openUrl(DISCORD_URL);
            SwingUtilities.getWindowAncestor(discordBtn).setVisible(false);
        });
        closeBtn.addActionListener(e -> SwingUtilities.getWindowAncestor(closeBtn).setVisible(false));

        Popups.of("DmPlugin",
                new JOptionPane(text, JOptionPane.INFORMATION_MESSAGE,
                        JOptionPane.DEFAULT_OPTION, null, new Object[] { discordBtn, closeBtn }))
                .showAsync();
    }

    public static void showDonateDialog(String authId) {
        if (Backpage.isDonor(authId)) {
            return;
        }

        Preferences prefs = Preferences.userNodeForPackage(Backpage.class);

        if (prefs.getLong("donateDialog", 0) <= System.currentTimeMillis()) {
            prefs.putLong("donateDialog", System.currentTimeMillis() + (60L * 24 * 60 * 60 * 1000));
            JButton donateBtn = new JButton("Donate");
            JButton closeBtn = new JButton("Close");
            donateBtn.addActionListener(e -> {
                SystemUtils.openUrl(
                        "https://ko-fi.com/deeme");
                SwingUtilities.getWindowAncestor(donateBtn).setVisible(false);
            });
            closeBtn.addActionListener(e -> SwingUtilities.getWindowAncestor(closeBtn).setVisible(false));

            Popups.of("DmPlugin donate",
                    new JOptionPane(
                            "You can help improve the plugin by donating.",
                            JOptionPane.INFORMATION_MESSAGE,
                            JOptionPane.DEFAULT_OPTION, null, new Object[] { donateBtn, closeBtn }))
                    .showAsync();
        }
    }
}