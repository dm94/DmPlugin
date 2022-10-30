package com.deeme.types.gui;

import com.github.manolo8.darkbot.core.manager.HeroManager;
import eu.darkbot.api.config.annotations.Dropdown;
import java.util.List;

public class ConfigSupplier implements Dropdown.Options<String> {

    @Override
    public List<String> options() {
        return HeroManager.instance.main.configManager.getAvailableConfigs();
    }

    @Override
    public String getText(String s) {
        return s;
    }
}
