package com.deeme.tasks.externalchat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
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
import eu.darkbot.api.extensions.FeatureInfo;
import eu.darkbot.api.extensions.Task;
import eu.darkbot.api.game.other.Gui;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.GameScreenAPI;
import eu.darkbot.api.managers.ChatAPI.MessageSentEvent;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.util.Popups;

import com.github.manolo8.darkbot.utils.Time;
import static com.github.manolo8.darkbot.Main.API;

@Feature(name = "ExternalChat", description = "Allows you to use the chat")
public class ExternalChat implements Task, Listener, ExtraMenus {

    private final ExtensionsAPI extensionsAPI;

    private JPanel mainPanel;
    private JTextArea globalChatTextArea;
    private JTextArea otherChatTextArea;
    private JTextField input;
    private ArrayList<String> globalChat = new ArrayList<>();
    private ArrayList<String> otherChats = new ArrayList<>();

    private ChatProcessor globalChatProcessor;
    private ChatProcessor otherChatProcesssor;

    private int lastGlobalSize = 0;
    private int lastOtherSize = 0;

    private Gui chatGui;
    private long nextClick = 0;
    private int clickDelay = 2000;
    private Random rnd;

    private LinkedList<String> pendingMessages = new LinkedList<>();

    private static final int INPUT_WIDTH_OFFSET = 20;
    private static final int INPUT_BOTTOM_OFFSET = 15;
    private static final int INPUT_HEIGHT = 15;

    public ExternalChat(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class));
    }

    @Inject
    public ExternalChat(PluginAPI api, AuthAPI auth) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.requireAuthenticity(auth);

        this.extensionsAPI = api.requireAPI(ExtensionsAPI.class);
        FeatureInfo feature = extensionsAPI.getFeatureInfo(this.getClass());
        Utils.discordCheck(feature, auth.getAuthId());
        Utils.showDonateDialog(feature, auth.getAuthId());

        GameScreenAPI gameScreenAPI = api.requireAPI(GameScreenAPI.class);
        this.chatGui = gameScreenAPI.getGui("chat");
        this.rnd = new Random();

        initGui();
    }

    private void initGui() {
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
        JButton clearBtn = new JButton("Clear Chat");
        clearBtn.addActionListener(e -> {
            this.globalChat.clear();
            this.otherChats.clear();
        });

        input = new JTextField("");

        JButton sendButton = new JButton("Send a message to global chat");
        sendButton.addActionListener(e -> addMessageToPendingList());

        tabbedPane.add(globalChatPanel, "Global");
        tabbedPane.add(otherChatPanel, "Others");
        this.mainPanel.add(tabbedPane, "span");
        this.mainPanel.add(clearBtn, "span");
        this.mainPanel.add(input, "span, grow");
        this.mainPanel.add(sendButton, "grow");
    }

    private void addMessageToPendingList() {
        if (input == null || input.getText().length() <= 0) {
            return;
        }

        this.pendingMessages.add(input.getText());
        input.setText("");
    }

    @Override
    public void onTickTask() {

        if (!pendingMessages.isEmpty()) {
            if (openGui()) {
                return;
            }
            sendMessage(pendingMessages.pollFirst());
        }

        closeGui();

        try {
            if (globalChatProcessor != null && lastGlobalSize != globalChat.size()) {
                lastGlobalSize = globalChat.size();
                globalChatProcessor.doInBackground();
            }
            if (otherChatProcesssor != null && lastOtherSize != otherChats.size()) {
                lastOtherSize = otherChats.size();
                otherChatProcesssor.doInBackground();
            }
        } catch (Exception e) {
            extensionsAPI.getFeatureInfo(this.getClass()).addWarning("External Chat", e.getLocalizedMessage());
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
        lastGlobalSize = globalChat.size();
        lastGlobalSize = otherChats.size();

        globalChatProcessor.execute();
        otherChatProcesssor.execute();
        Popups.of("Chat", this.mainPanel).showAsync();
    }

    private boolean waitToClick() {
        return System.currentTimeMillis() < nextClick;
    }

    private void closeGui() {
        if (waitToClick() || chatGui == null) {
            return;
        }
        if (chatGui.isVisible()) {
            nextClick = System.currentTimeMillis() + this.clickDelay;
            chatGui.setVisible(false);
        }
    }

    private boolean openGui() {
        if (waitToClick() || chatGui == null) {
            return true;
        }
        if (!chatGui.isVisible()) {
            nextClick = System.currentTimeMillis() + this.clickDelay;
            chatGui.setVisible(true);
            return true;
        }

        return false;
    }

    private void sendMessage(String message) {
        int inputWidth = (int) chatGui.getWidth() - (INPUT_WIDTH_OFFSET * 2);
        int xPoint = INPUT_WIDTH_OFFSET + rnd.nextInt(inputWidth);
        int yPoint = (int) chatGui.getHeight() - INPUT_BOTTOM_OFFSET - (rnd.nextInt(INPUT_HEIGHT));
        chatGui.click(xPoint, yPoint);

        Time.sleep(100);
        API.sendText(message);
        API.keyClick(Character.LINE_SEPARATOR);
    }

}