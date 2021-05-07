package com.deeme.types.gui;

import com.github.manolo8.darkbot.config.types.suppliers.OptionList;
import com.github.manolo8.darkbot.core.manager.HeroManager;

import java.util.List;

public class ConfigSupplier extends OptionList<String> {

    @Override
    public String getValue(String s) {
        return s;
    }

    @Override
    public String getText(String s) {
        return s;
    }

    @Override
    public List<String> getOptions() {
        return HeroManager.instance.main.configManager.getAvailableConfigs();
    }
}
