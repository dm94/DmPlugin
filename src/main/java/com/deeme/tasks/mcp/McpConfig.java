package com.deeme.tasks.mcp;

import eu.darkbot.api.config.annotations.Editor;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;

public class McpConfig {
    @Option("mcpconfig.port")
    @Number(min = 1024, max = 65535, step = 1)
    public int port = 9876;

    @Option("mcpconfig.host")
    public String host = "127.0.0.1";

    @Option("mcpconfig.control")
    @Editor(StatusPanelEditor.class)
    public String control = "";
}
