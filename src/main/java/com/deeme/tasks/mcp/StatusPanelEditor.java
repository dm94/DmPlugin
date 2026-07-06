package com.deeme.tasks.mcp;

import com.deeme.tasks.mcp.server.McpHttpServer;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.util.OptionEditor;

import javax.swing.*;
import java.awt.*;

public class StatusPanelEditor implements OptionEditor<String> {

    private JPanel panel;
    private JLabel statusLabel;
    private JLabel connectionsLabel;
    private JButton restartBtn;

    @Override
    public JComponent getEditorComponent(ConfigSetting<String> setting) {
        if (panel == null) {
            panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            statusLabel = new JLabel(" ");
            connectionsLabel = new JLabel(" ");
            restartBtn = new JButton("Restart Server");
            restartBtn.setMaximumSize(new Dimension(160, 26));
            restartBtn.addActionListener(e -> {
                McpHttpServer s = McpBridge.liveServer;
                if (s != null) {
                    s.stop();
                    s.start();
                }
            });

            panel.add(statusLabel);
            panel.add(Box.createVerticalStrut(2));
            panel.add(connectionsLabel);
            panel.add(Box.createVerticalStrut(4));
            panel.add(restartBtn);
        }

        refresh();
        return panel;
    }

    private void refresh() {
        McpHttpServer s = McpBridge.liveServer;
        if (s != null && s.isRunning()) {
            statusLabel.setText("Running on http://" + s.getHost() + ":" + s.getPort() + "/mcp");
            connectionsLabel.setText("Active connections: " + s.getConnectionCount());
            restartBtn.setEnabled(true);
        } else {
            statusLabel.setText("Server stopped");
            connectionsLabel.setText("");
            restartBtn.setEnabled(true);
        }
    }

    @Override
    public String getEditorValue() {
        return "";
    }
}
