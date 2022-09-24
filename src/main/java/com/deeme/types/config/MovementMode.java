package com.deeme.types.config;

import com.github.manolo8.darkbot.config.types.suppliers.OptionList;

import java.util.Arrays;
import java.util.List;

public class MovementMode extends OptionList<Integer> {
    private static final List<String> MOVEMENT_MODE = Arrays
            .asList(new String[] { "Safety Logic", "VS Move", "Random Move", "VS + Safety", "Group + VS + Safety" });

    public Integer getValue(String text) {
        return MOVEMENT_MODE.indexOf(text);
    }

    public String getText(Integer value) {
        return MOVEMENT_MODE.get(value.intValue());
    }

    public List<String> getOptions() {
        return MOVEMENT_MODE;
    }

}
