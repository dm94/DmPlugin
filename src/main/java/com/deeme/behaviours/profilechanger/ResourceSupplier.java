package com.deeme.behaviours.profilechanger;

import eu.darkbot.api.config.annotations.Dropdown;

import java.util.ArrayList;
import java.util.List;

public class ResourceSupplier implements Dropdown.Options<String> {

    private static List<String> boxInfos = new ArrayList<String>();

    public static void updateBoxes(List<String> allboxes) {
        if (allboxes != null && !allboxes.isEmpty()) {
            boxInfos = allboxes;
        }
    }

    @Override
    public String getText(String id) {
        return id;
    }

    @Override
    public List<String> options() {
        return boxInfos;
    }
}
