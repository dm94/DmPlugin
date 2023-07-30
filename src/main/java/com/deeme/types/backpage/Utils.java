package com.deeme.types.backpage;

import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.deemetool.utils.Backpage;

import eu.darkbot.api.extensions.FeatureInfo;
import eu.darkbot.util.Popups;
import eu.darkbot.util.SystemUtils;

public class Utils {

    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    public static synchronized void discordCheck(FeatureInfo featureInfo, String authID) {
        if (!Backpage.isInDiscord(authID)) {
            showDiscordDialog();
            featureInfo
                    .addFailure("To use this option you need to be on my discord", "Log in to my discord and reload");
        }
    }

    public static synchronized void discordDonorCheck(FeatureInfo featureInfo, String authID) {
        if (!Backpage.isDonor(authID)) {
            showDiscordDonorDialog();
            featureInfo
                    .addFailure("Only some people can use this feature, wait your turn.",
                            "Enter my discord to know more");
        }
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
        Preferences prefs = Preferences.userNodeForPackage(Backpage.class);

        if (prefs.getLong("donateDialog", 0) <= System.currentTimeMillis()) {
            prefs.putLong("donateDialog", System.currentTimeMillis() + (60L * 24 * 60 * 60 * 1000));
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

    public static void showDiscordDonorDialog() {
        JButton discordBtn = new JButton("Discord");
        JButton closeBtn = new JButton("Close");
        discordBtn.addActionListener(e -> {
            SystemUtils.openUrl("https://discord.gg/GPRTRRZJPw");
            SwingUtilities.getWindowAncestor(discordBtn).setVisible(false);
        });
        closeBtn.addActionListener(e -> SwingUtilities.getWindowAncestor(closeBtn).setVisible(false));

        Popups.of("DmPlugin",
                new JOptionPane("Special features: Enter my discord to know more", JOptionPane.INFORMATION_MESSAGE,
                        JOptionPane.DEFAULT_OPTION, null, new Object[] { discordBtn, closeBtn }))
                .showAsync();
    }
}