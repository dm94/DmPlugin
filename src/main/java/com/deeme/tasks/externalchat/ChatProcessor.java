package com.deeme.tasks.externalchat;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextArea;
import javax.swing.SwingWorker;

public class ChatProcessor extends SwingWorker<List<String>, String> {

    private JTextArea globalChatTextArea;
    private List<String> globalChat = new ArrayList<>();

    public ChatProcessor(JTextArea globalChatTextArea, List<String> globalChat) {
        this.globalChatTextArea = globalChatTextArea;
        this.globalChat = globalChat;
    }

    @Override
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
        publish(buffer.toString());
        return this.globalChat;
    }

}
