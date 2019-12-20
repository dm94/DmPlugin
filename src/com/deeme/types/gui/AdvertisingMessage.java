package com.deeme.types.gui;

import com.deeme.types.VersionJson;
import com.deeme.types.backpage.Utils;
import com.github.manolo8.darkbot.extensions.features.FeatureDefinition;
import com.github.manolo8.darkbot.gui.utils.Popups;
import com.github.manolo8.darkbot.utils.SystemUtils;

import javax.swing.*;

public class AdvertisingMessage {
    public static volatile boolean hasAccepted = false;
    public static volatile boolean singOpen = false;
    public static volatile VersionJson latestVersion = null;

    public static synchronized void newUpdateMessage(FeatureDefinition featureDefinition) {
        if (latestVersion != null) return;

        latestVersion = Utils.updateLastVersion();

        if (Utils.newVersionAvailable(latestVersion, featureDefinition)) {
            JButton yesButton = new JButton("Download");
            yesButton.addActionListener(e -> {
                SystemUtils.openUrl("https://gist.github.com/dm94/d8fa849330513e1b402cc89eba5e4739/raw/DmPlugin.jar");
                hasAccepted = true;
                SwingUtilities.getWindowAncestor(yesButton).setVisible(false);
            });

            JButton noButton = new JButton("Ignore");
            noButton.addActionListener(e -> {
                hasAccepted = false;
                SwingUtilities.getWindowAncestor(noButton).setVisible(false);
            });

            Popups.showMessageSync("DmPlugin", new JOptionPane("New version available " + latestVersion.getVersionNumber(), JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[] { yesButton, noButton }));
        }
    }

    public static synchronized void showAdverMessage() {
        if (!hasAccepted) {
            singOpen = true;
            JButton yesButton = new JButton("Open Advertising Link");
            yesButton.addActionListener(e -> {
                SystemUtils.openUrl("https://comunidadgzone.es/colabora/");
                hasAccepted = true;
                singOpen = false;
                SwingUtilities.getWindowAncestor(yesButton).setVisible(false);
            });

            JButton noButton = new JButton("No open link");
            noButton.addActionListener(e -> {
                hasAccepted = false;
                singOpen = false;
                SwingUtilities.getWindowAncestor(noButton).setVisible(false);
            });

            Popups.showMessageSync("DmPlugin", new JOptionPane("To use this plugin you have to open the following link", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[] { yesButton, noButton }));
        }
    }


}
