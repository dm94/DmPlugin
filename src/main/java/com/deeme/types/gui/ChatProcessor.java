package com.deeme.types.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextArea;
import javax.swing.SwingWorker;

public class ChatProcessor extends SwingWorker<List<String>, String> {

    private JTextArea globalChatTextArea;
    private ArrayList<String> globalChat = new ArrayList<>();

    public ChatProcessor(JTextArea globalChatTextArea, ArrayList<String> globalChat) {
        this.globalChatTextArea = globalChatTextArea;
        this.globalChat = globalChat;
    }

    protected void process(List<String> chunks) {
        for (String text : chunks) {
            this.globalChatTextArea.append(text);
        }
    }

    @Override
    protected List<String> doInBackground() throws Exception {
        this.globalChatTextArea.setText("");
        StringBuilder buffer = new StringBuilder();
        for (String s : globalChat) {
            buffer.append(s).append("\n");
        }
        publish(new String[] { buffer.toString() });
        return this.globalChat;
    }

}
