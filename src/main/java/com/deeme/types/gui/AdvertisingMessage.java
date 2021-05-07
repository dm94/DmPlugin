package com.deeme.types.gui;

import com.deeme.types.VerifierChecker;
import com.deeme.types.VersionJson;
import com.deeme.types.backpage.Utils;
import com.github.manolo8.darkbot.extensions.features.FeatureDefinition;
import com.github.manolo8.darkbot.extensions.util.Version;
import com.github.manolo8.darkbot.gui.utils.Popups;
import com.github.manolo8.darkbot.utils.SystemUtils;

import javax.swing.*;

public class AdvertisingMessage {
    public static volatile boolean hasAccepted = false;
    public static volatile boolean singOpen = false;
    public static volatile VersionJson latestVersion = null;

    public static synchronized void newUpdateMessage(FeatureDefinition featureDefinition, Version botVersion) {
        if (latestVersion != null) return;

        latestVersion = Utils.getLastValidVersion(Utils.getVersions(), botVersion);

        if (latestVersion != null && Utils.newVersionAvailable(latestVersion, featureDefinition)) {
            JButton download = new JButton("Download");
            download.addActionListener(e -> {
                SystemUtils.openUrl(latestVersion.getDownloadLink());
                SwingUtilities.getWindowAncestor(download).setVisible(false);
            });

            JButton changelog = new JButton("Changelog");
            changelog.addActionListener(e -> {
                SystemUtils.openUrl(latestVersion.getChangelog());
            });

            JButton ignore = new JButton("Ignore");
            ignore.addActionListener(e -> {
                SwingUtilities.getWindowAncestor(ignore).setVisible(false);
            });

            Popups.showMessageSync("DmPlugin", new JOptionPane("New version available " + latestVersion.getVersionNumber(), JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[] { download,changelog, ignore }));
        }
    }

    public static synchronized void showAdverMessage() {
        VerifierChecker.getAuthApi().isAuthenticated();
        
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

            Popups.showMessageSync("DmPlugin", new JOptionPane("To use this plugin you have to open the following link, If you are a donor it is not necessary", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[] { yesButton, noButton }));
        }
        if (!hasAccepted) {
            hasAccepted = VerifierChecker.getAuthApi().isDonor();
        }
    }


}
