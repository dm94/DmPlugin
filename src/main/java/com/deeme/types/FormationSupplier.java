package com.deeme.types;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.entities.Ship;
import eu.darkbot.api.extensions.selectors.PrioritizedSupplier;
import eu.darkbot.api.game.items.ItemCategory;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Formation;
import eu.darkbot.api.managers.HeroItemsAPI;

public class FormationSupplier implements PrioritizedSupplier<SelectableItem> {
    private Main main;
    private final HeroItemsAPI items;

    private boolean useDiamond = false;
    private boolean focusDamage = false;
    private boolean focusSpeed = false;
    private boolean focusPenetration = false;

    List<String> damageOrder = Arrays.asList(Formation.DRILL.getId(), Formation.PINCER.getId(), Formation.STAR.getId(), Formation.DOUBLE_ARROW.getId());

    public FormationSupplier(Main main, HeroItemsAPI items) {
        this.main = main;
        this.items = items;
    }

    public SelectableItem get() {
        boolean isAvailable = false;
        if (focusSpeed) {
            isAvailable = items.getItem(Formation.WHEEL, ItemFlag.USABLE, ItemFlag.READY).isPresent();
            if (isAvailable) {
                return Formation.WHEEL;
            }
        }
        if (focusPenetration) {
            isAvailable = items.getItem(Formation.MOTH, ItemFlag.USABLE, ItemFlag.READY).isPresent();
            if (isAvailable) {
                return Formation.MOTH;
            } else {
                isAvailable = items.getItem(Formation.DOUBLE_ARROW, ItemFlag.USABLE, ItemFlag.READY).isPresent();
                if (isAvailable) {
                    return Formation.DOUBLE_ARROW;
                } 
            }
        }

        if (useDiamond) {
            isAvailable = items.getItem(Formation.DIAMOND, ItemFlag.USABLE, ItemFlag.READY).isPresent();
            if (isAvailable) {
                return Formation.DIAMOND;
            } 
        }

        if (focusDamage) {
            return items.getItems(ItemCategory.DRONE_FORMATIONS).stream()
            .filter(item -> item.isUsable() && item.isAvailable()).sorted(Comparator.comparing(i -> damageOrder.indexOf(i.getId()))).findFirst().orElse(null);
        }
        
        return Formation.MOTH;
    }

    private boolean shoulFocusDamage(Ship target) {
        return target.getHealth().shieldPercent() < 0.3;
    }

    private boolean shoulFocusPenetration(Ship target) {
        return target.getHealth().shieldPercent() > 0.5 && main.hero.health.shieldPercent() < 0.2;
    }

    private boolean shoulFocusSpeed(Ship target) {
        double distance = main.hero.getLocationInfo().now.distance(target.getLocationInfo());
        return distance > 800 && target.getSpeed() > main.hero.getSpeed();
    }

    private boolean shoulUseDiamond() {
        return main.hero.getHealth().hpPercent() < 0.7 && main.hero.getHealth().shieldPercent() < 0.1;
    }
    
    @Override
    public Priority getPriority() {
        Ship target = this.main.hero.target;
        if (target != null) {
            focusDamage = shoulFocusDamage(target);
            focusPenetration = shoulFocusPenetration(target);
            focusSpeed = shoulFocusSpeed(target);
            useDiamond = shoulUseDiamond();
        }
        return focusSpeed ? Priority.HIGHEST : focusPenetration ? Priority.HIGH : focusDamage ? Priority.MODERATE : useDiamond ? Priority.LOW : Priority.LOWEST;
    }
}