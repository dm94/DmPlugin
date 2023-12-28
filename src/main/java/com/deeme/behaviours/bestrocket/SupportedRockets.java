package com.deeme.behaviours.bestrocket;

import java.util.Locale;

import eu.darkbot.api.game.items.ItemCategory;
import eu.darkbot.api.game.items.SelectableItem;

/**
 * Based on
 * https://github.com/darkbot-reloaded/DarkBotAPI/blob/master/api/src/main/java/eu/darkbot/api/game/items/SelectableItem.java
 */

public enum SupportedRockets implements SelectableItem {
    R_310,
    PLT_2026,
    PLT_2021,
    PLT_3030,
    PLD_8(true),
    DCR_250(true),
    BDR_1211,
    R_IC3(true),
    SR_5(true),
    K_300M(true),
    SP_100X(true),
    AGT_500,
    RC_100(true);

    private static final String PREFIX = "ammunition_rocket_";
    private static final String PREFIX_SPECIAL = "ammunition_specialammo_";
    private final String id;

    SupportedRockets() {
        this(false);
    }

    SupportedRockets(boolean isSpecial) {
        this.id = (isSpecial ? PREFIX_SPECIAL : PREFIX) + name().toLowerCase(Locale.ROOT).replaceAll("_", "-");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.ROCKETS;
    }

}
