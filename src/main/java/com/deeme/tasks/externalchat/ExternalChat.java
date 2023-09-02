package com.deeme.tasks.externalchat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;
import net.miginfocom.swing.MigLayout;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;

import java.awt.Toolkit;
import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.events.EventHandler;
import eu.darkbot.api.events.Listener;
import eu.darkbot.api.extensions.ExtraMenus;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Task;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.ChatAPI.MessageSentEvent;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.util.Popups;

@Feature(name = "ExternalChat", description = "See the chat")
public class ExternalChat implements Task, Listener, ExtraMenus {

    protected final PluginAPI api;
    protected final ExtensionsAPI extensionsAPI;
    private JPanel mainPanel;
    private JTextArea globalChatTextArea;
    private JTextArea otherChatTextArea;
    private ArrayList<String> globalChat = new ArrayList<>();
    private ArrayList<String> otherChats = new ArrayList<>();

    private ChatProcessor globalChatProcessor;
    private ChatProcessor otherChatProcesssor;

    private int lastGlobalSize = 0;
    private int lastOtherSize = 0;

    public ExternalChat(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class));
    }

    @Inject
    public ExternalChat(PluginAPI api, AuthAPI auth) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        this.extensionsAPI = api.getAPI(ExtensionsAPI.class);
        Utils.discordCheck(extensionsAPI.getFeatureInfo(this.getClass()), auth.getAuthId());
        Utils.showDonateDialog();

        this.api = api;

        this.mainPanel = new JPanel(new MigLayout(""));
        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel globalChatPanel = new JPanel(new MigLayout(""));
        JPanel otherChatPanel = new JPanel(new MigLayout(""));
        this.globalChatTextArea = new JTextArea();
        this.otherChatTextArea = new JTextArea();
        globalChatTextArea.setEditable(false);
        otherChatTextArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(this.globalChatTextArea);
        scroll.getVerticalScrollBar().setUnitIncrement(15);
        globalChatPanel.add(scroll,
                "height :" + (Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2.0D) + ":"
                        + (Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 1.3D) + ", width :"
                        + (Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 3.0D) + ":"
                        + (Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 1.4D));
        JScrollPane scrollOthers = new JScrollPane(this.otherChatTextArea);
        scrollOthers.getVerticalScrollBar().setUnitIncrement(15);
        otherChatPanel.add(scrollOthers,
                "height :" + (Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2.0D) + ":"
                        + (Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 1.3D) + ", width :"
                        + (Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 3.0D) + ":"
                        + (Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 1.4D));
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> {
            this.globalChat.clear();
            this.otherChats.clear();
            SwingUtilities.getWindowAncestor(clearBtn).setVisible(false);
        });
        tabbedPane.add(globalChatPanel, "Global");
        tabbedPane.add(otherChatPanel, "Others");
        this.mainPanel.add(tabbedPane, "span");
        this.mainPanel.add(clearBtn, "span");
    }

    @Override
    public void onTickTask() {
        try {
            if (globalChatProcessor != null && lastGlobalSize != globalChat.size()) {
                lastGlobalSize = globalChat.size();
                globalChatProcessor.doInBackground();
            }
            if (otherChatProcesssor != null && lastOtherSize != otherChats.size()) {
                lastGlobalSize = otherChats.size();
                otherChatProcesssor.doInBackground();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onChatMessage(MessageSentEvent event) {
        if (extensionsAPI.getFeatureInfo(this.getClass()).isEnabled()) {
            String message = String.format("%s %s : %s",
                    (!event.getMessage().getClanTag().equals("ERROR") ? "[" + event.getMessage().getClanTag() + "]"
                            : ""),
                    event.getMessage().getUsername(),
                    event.getMessage().getMessage());

            if (event.getRoom().toLowerCase().contains("global")) {
                globalChat.add(message);
            } else {
                otherChats.add(event.getRoom() + " | " + message);
            }
        }
    }

    @Override
    public Collection<JComponent> getExtraMenuItems(PluginAPI pluginAPI) {
        return Arrays.asList(
                createSeparator("ExternalChat"),
                create("Show chat", e -> showChat()));
    }

    private void showChat() {
        DefaultCaret caretGlobal = (DefaultCaret) this.globalChatTextArea.getCaret();
        caretGlobal.setUpdatePolicy(1);
        DefaultCaret caretOthers = (DefaultCaret) this.otherChatTextArea.getCaret();
        caretOthers.setUpdatePolicy(1);
        globalChatProcessor = new ChatProcessor(this.globalChatTextArea, this.globalChat);
        otherChatProcesssor = new ChatProcessor(this.otherChatTextArea, this.otherChats);

        globalChatProcessor.execute();
        otherChatProcesssor.execute();
        Popups.of("Chat", this.mainPanel).showAsync();
    }
}