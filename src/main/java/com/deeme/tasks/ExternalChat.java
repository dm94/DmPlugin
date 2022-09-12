package com.deeme.tasks;

import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deeme.types.gui.ChatProcessor;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.itf.ExtraMenuProvider;
import java.awt.Toolkit;
import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.events.EventHandler;
import eu.darkbot.api.events.Listener;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Task;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ChatAPI.MessageSentEvent;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.util.Popups;
import net.miginfocom.swing.MigLayout;

@Feature(name = "ExternalChat", description = "See the chat")
public class ExternalChat implements Task, Listener, ExtraMenuProvider {

    protected final PluginAPI api;
    private JTabbedPane tabbedPane;
    private JTextArea globalChatTextArea;
    private JTextArea otherChatTextArea;
    private ArrayList<String> globalChat = new ArrayList<>();
    private ArrayList<String> otherChats = new ArrayList<>();

    public ExternalChat(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class));
    }

    @Inject
    public ExternalChat(PluginAPI api, AuthAPI auth) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        if (!Utils.discordCheck(auth.getAuthId())) {
            Utils.showDiscordDialog();
            throw new UnsupportedOperationException("To use this option you need to be on my discord");
        }

        this.api = api;

        this.tabbedPane = new JTabbedPane();
        JPanel panel = new JPanel((LayoutManager) new MigLayout(""));
        JPanel globalChatPanel = new JPanel((LayoutManager) new MigLayout(""));
        JPanel otherChatPanel = new JPanel((LayoutManager) new MigLayout(""));
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
        this.tabbedPane.add(globalChatPanel, "Global");
        this.tabbedPane.add(otherChatPanel, "Others");
        panel.add(this.tabbedPane, "span");
    }

    @Override
    public void onTickTask() {
    }

    @EventHandler
    public void onChatMessage(MessageSentEvent event) {
        String message = event.getMessage().getUsername() + " | " + event.getMessage().getMessage();
        if (event.getRoom().toLowerCase().contains("global")) {
            globalChat.add(message);
        } else {
            otherChats.add(event.getRoom() + " | " + message);
        }
    }

    @Override
    public Collection<JComponent> getExtraMenuItems(Main main) {
        return Arrays.asList(
                create("Show chat", e -> {
                    showChat();
                }));
    }

    private void showChat() {
        DefaultCaret caretGlobal = (DefaultCaret) this.globalChatTextArea.getCaret();
        caretGlobal.setUpdatePolicy(1);
        DefaultCaret caretOthers = (DefaultCaret) this.otherChatTextArea.getCaret();
        caretOthers.setUpdatePolicy(1);
        new ChatProcessor(this.globalChatTextArea, this.globalChat).execute();
        new ChatProcessor(this.otherChatTextArea, this.otherChats).execute();
        Popups.showMessageAsync("Chat", new Object[] { this.tabbedPane }, -1);
    }
}