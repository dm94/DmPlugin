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
import eu.darkbot.api.game.items.SelectableItem.Rocket;
import eu.darkbot.api.managers.HeroItemsAPI;

public class RocketSupplier implements PrioritizedSupplier<SelectableItem> {
    private Main main;
    private final HeroItemsAPI items;

    private boolean stopEnemy = false;
    private boolean usePLD = false;

    List<String> damageOrder = Arrays.asList(Rocket.PLT_3030.getId(), Rocket.PLT_2021.getId(), Rocket.PLT_2026.getId(), Rocket.R_310.getId());

    public RocketSupplier(Main main, HeroItemsAPI items) {
        this.main = main;
        this.items = items;
    }

    public SelectableItem get() {
        boolean isAvailable = false;
        if (stopEnemy) {
            isAvailable = items.getItem(Rocket.R_IC3, ItemFlag.USABLE, ItemFlag.READY).isPresent();
            if (isAvailable) {
                return Rocket.R_IC3;
            } else {
                isAvailable = items.getItem(Rocket.DCR_250, ItemFlag.USABLE, ItemFlag.READY).isPresent();
                if (isAvailable) {
                    return Rocket.DCR_250;
                }
            }
        }

        if (usePLD) {
            isAvailable = items.getItem(Rocket.PLD_8, ItemFlag.USABLE, ItemFlag.READY).isPresent();
            if (isAvailable) {
                return Rocket.PLD_8;
            }
        }

        return items.getItems(ItemCategory.ROCKETS).stream()
            .filter(item -> item.isUsable() && item.isAvailable()).sorted(Comparator.comparing(i -> damageOrder.indexOf(i.getId()))).findFirst().orElse(null);
    }

    private boolean shoulFocusSpeed(Ship target) {
        double distance = main.hero.getLocationInfo().now.distance(target.getLocationInfo());
        return (distance > 400 && target.getSpeed() > main.hero.getSpeed() || main.hero.getHealth().hpPercent() < main.config.GENERAL.SAFETY.REPAIR_HP_RANGE.min);
    }

    private boolean shoulUsePLD(Ship target) {
        return target.isAiming(main.hero);
    }
    
    @Override
    public Priority getPriority() {
        Ship target = this.main.hero.target;
        if (target != null) {
            stopEnemy = shoulFocusSpeed(target);
            usePLD = shoulUsePLD(target);
        }
        return stopEnemy ? Priority.MODERATE : usePLD ? Priority.LOW : Priority.LOWEST;
    }
}