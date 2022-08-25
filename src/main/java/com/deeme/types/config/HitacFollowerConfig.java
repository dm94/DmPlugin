package com.deeme.types.config;

import com.github.manolo8.darkbot.config.types.Editor;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.config.types.Options;
import com.github.manolo8.darkbot.core.manager.StarManager.MapList;
import com.github.manolo8.darkbot.gui.tree.components.JListField;

public class HitacFollowerConfig {
    @Option(value = "Enable")
    public boolean enable = false;
    @Option(value = "Go to PVP Maps")
    public boolean goToPVP = true;
    @Option(value = "Return to waiting map")
    public boolean returnToWaitingMap = true;
    @Option("Waiting map")
    @Editor(JListField.class)
    @Options(MapList.class)
    public int WAIT_MAP = 8;
}
