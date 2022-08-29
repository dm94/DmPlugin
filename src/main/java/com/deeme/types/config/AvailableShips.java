package com.deeme.types.config;

import java.util.Arrays;
import java.util.List;

import com.github.manolo8.darkbot.config.types.suppliers.OptionList;

public class AvailableShips extends OptionList<Integer> {
    private final List<String> AVAILABLE_SHIPS = Arrays
            .asList(new String[] { "Aegis", "Hammerclaw", "Solace" });

    public Integer getValue(String text) {
        return AVAILABLE_SHIPS.indexOf(text);
    }

    public String getText(Integer value) {
        return AVAILABLE_SHIPS.get(value.intValue());
    }

    public List<String> getOptions() {
        return AVAILABLE_SHIPS;
    }

}