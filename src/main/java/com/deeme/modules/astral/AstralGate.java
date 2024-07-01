package com.deeme.modules.astral;

import com.deeme.types.ConditionsManagement;
import com.deeme.types.SharedFunctions;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deeme.types.suppliers.AmmoSupplier;
import com.deeme.types.suppliers.BestRocketSupplier;
import com.deemeplus.modules.astral.AstralPlus;
import com.deemeplus.modules.astral.PortalInfo;
import com.github.manolo8.darkbot.config.NpcExtraFlag;
import com.github.manolo8.darkbot.core.itf.NpcExtraProvider;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.NpcFlag;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.FeatureInfo;
import eu.darkbot.api.extensions.Module;
import eu.darkbot.api.extensions.InstructionProvider;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.entities.Portal;
import eu.darkbot.api.game.enums.EntityEffect;
import eu.darkbot.api.game.enums.PortalType;
import eu.darkbot.api.game.items.Item;
import eu.darkbot.api.game.items.ItemCategory;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.items.SelectableItem.Cpu;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.game.other.Locatable;
import eu.darkbot.api.game.other.Location;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.game.other.Movable;
import eu.darkbot.api.managers.AttackAPI;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.managers.PetAPI;
import eu.darkbot.api.managers.StarSystemAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.modules.MapModule;
import eu.darkbot.shared.utils.SafetyFinder;
import eu.darkbot.util.Popups;
import eu.darkbot.util.SystemUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

@Feature(name = "Astral Gate", description = "For the astral gate and another GGs")
public class AstralGate implements Module, InstructionProvider, Configurable<AstralConfig>, NpcExtraProvider {
    private final PluginAPI api;
    private final HeroAPI heroapi;
    private final BotAPI bot;
    private final MovementAPI movement;
    private final AttackAPI attacker;
    private final PetAPI pet;
    private final HeroItemsAPI items;
    private final StarSystemAPI starSystem;
    private ConfigSetting<Integer> maxCircleIterations;
    private final ConfigSetting<Character> ammoKey;
    private final ConditionsManagement conditionsManagement;

    private AstralPlus astralPlus;

    private Collection<? extends Portal> portals;
    private Collection<? extends Npc> npcs;

    private boolean backwards = false;
    private long nextCPUCheck = 0;
    private long nextWaveCheck = 0;

    private static final int MIN_TIME_FOR_WAVE_CHECK = 20000;

    private boolean repairShield = false;
    private boolean waveHasBeenAwaited = false;

    private BestRocketSupplier rocketSupplier;
    private AmmoSupplier ammoSupplier;
    private AstralConfig astralConfig;

    private State currentStatus;
    private boolean showDialog = false;
    private boolean warningDisplayed = false;

    private double lastRadius = 0;

    private enum State {
        WAIT("Waiting"),
        DO("Attacking"),
        WAITING_SIGN("Waiting for the selector"),
        WAITING_WAVE("Waiting for the wave"),
        WAITING_HUMAN("Choose an option to continue"),
        WAITING_SHIP("Choose a ship"),
        CHOOSING_BEST_OPTION("Choosing the best option");

        private final String message;

        State(String message) {
            this.message = message;
        }
    }

    public AstralGate(PluginAPI api) {
        this(api, api.requireAPI(AuthAPI.class),
                api.requireInstance(SafetyFinder.class));
    }

    @Inject
    public AstralGate(PluginAPI api, AuthAPI auth, SafetyFinder safety) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }

        VerifierChecker.requireAuthenticity(auth);

        ExtensionsAPI extensionsAPi = api.requireAPI(ExtensionsAPI.class);
        FeatureInfo<?> featureInfo = extensionsAPi.getFeatureInfo(this.getClass());

        Utils.discordCheck(featureInfo, auth.getAuthId());
        Utils.showDonateDialog(featureInfo, auth.getAuthId());

        this.api = api;
        this.bot = api.requireAPI(BotAPI.class);
        this.heroapi = api.requireAPI(HeroAPI.class);
        this.movement = api.requireAPI(MovementAPI.class);
        this.attacker = api.requireAPI(AttackAPI.class);
        this.pet = api.requireAPI(PetAPI.class);
        this.starSystem = api.requireAPI(StarSystemAPI.class);
        this.items = api.requireAPI(HeroItemsAPI.class);
        this.conditionsManagement = new ConditionsManagement(api, items);

        EntitiesAPI entities = api.requireAPI(EntitiesAPI.class);
        this.portals = entities.getPortals();
        this.npcs = entities.getNpcs();

        ConfigAPI configApi = api.requireAPI(ConfigAPI.class);

        this.maxCircleIterations = configApi.requireConfig("loot.max_circle_iterations");
        this.ammoKey = configApi.requireConfig("loot.ammo_key");

        this.ammoSupplier = new AmmoSupplier(items);

        this.currentStatus = State.WAIT;
        this.showDialog = false;
        this.warningDisplayed = false;
        initAstral();
    }

    @Override
    public void setConfig(ConfigSetting<AstralConfig> arg0) {
        this.astralConfig = arg0.getValue();
        initAstral();
    }

    @Override
    public NpcExtraFlag[] values() {
        return ExtraNpcFlags.values();
    }

    @Override
    public boolean canRefresh() {
        return isHomeMap();
    }

    @Override
    public String getStatus() {
        return "Astral | " + currentStatus.message + " | " + npcs.size() + " | " + attacker.getStatus() + " | Radius: "
                + lastRadius + " | " + astralPlus.getStatus();
    }

    @Override
    public String getStoppedStatus() {
        return getStatus();
    }

    @Override
    public String instructions() {
        return "Instructions for those who do not have access to PLUS functions: \n" +
                "- You need to manually select the ship. \n" +
                "- Place the quick bar with everything you want to be used. \n" +
                "- The bot will wait until you choose an item and portal. He does not jump through the gates!!";
    }

    @Override
    public void onTickModule() {
        pet.setEnabled(false);
        showWarningDialog();
        if (isAstral() || heroapi.getMap().isGG()) {
            if (astralPlus.isValidShip()) {
                activeAutoRocketCPU();
                repairShield = repairShield && heroapi.getHealth().shieldPercent() < 0.9
                        || heroapi.getHealth().shieldPercent() < 0.2;
                if (findTarget()) {
                    waveLogic();
                } else if (npcs.isEmpty() || !portals.isEmpty()) {
                    preparingWaveLogic();
                }
            } else if (!astralPlus.autoChooseShip(astralConfig.shipType.getId())) {
                stopBot(State.WAITING_SHIP);
            }
        } else {
            goToAstral();
        }
    }

    @Override
    public void onTickStopped() {
        if (isAstral()) {
            showWarningDialog();
        }
    }

    private void initAstral() {
        if (astralPlus != null || astralConfig == null) {
            return;
        }

        this.rocketSupplier = new BestRocketSupplier(api, this.astralConfig.defaultRocket);
        this.astralPlus = new AstralPlus(api, this.astralConfig.customItemPriority, this.astralConfig.astralPlusConfig);
        this.astralPlus.updateConfig(this.astralConfig.customItemPriority, this.astralConfig.astralPlusConfig);
        fillPortalInfo();
    }

    private void fillPortalInfo() {
        if (this.astralConfig.portalInfos.size() > 0) {
            return;
        }

        Map<String, PortalInfo> defaultPortals = new HashMap<>();
        defaultPortals.put(PortalType.ROUGE_LITE_HP_RECOVER.name(), new PortalInfo(0));
        defaultPortals.put(PortalType.ROUGE_LITE_AMMUNITION.name(), new PortalInfo(1));
        defaultPortals.put(PortalType.ROUGE_LITE_MODULE.name(), new PortalInfo(2));
        defaultPortals.put(PortalType.ROUGE_LITE_GENERATOR.name(), new PortalInfo(3));
        defaultPortals.put(PortalType.ROUGE_LITE_WEAPON.name(), new PortalInfo(4));
        defaultPortals.put(PortalType.ROUGE_LITE_RESOURCE.name(), new PortalInfo(5));
        defaultPortals.put(PortalType.ROUGE_LITE_AMMUNITION_BRUTAL.name(), new PortalInfo(6));
        defaultPortals.put(PortalType.ROUGE_LITE_MODULE_BRUTAL.name(), new PortalInfo(7));
        defaultPortals.put(PortalType.ROUGE_LITE_GENERATOR_BRUTAL.name(), new PortalInfo(8));
        defaultPortals.put(PortalType.ROUGE_LITE_WEAPON_BRUTAL.name(), new PortalInfo(9));
        defaultPortals.put(PortalType.ROUGE_LITE_RESOURCE_BRUTAL.name(), new PortalInfo(10));

        this.astralConfig.portalInfos.putAll(defaultPortals);
    }

    private void waveLogic() {
        nextWaveCheck = System.currentTimeMillis() + MIN_TIME_FOR_WAVE_CHECK;
        waveHasBeenAwaited = false;
        this.currentStatus = State.DO;

        attacker.tryLockAndAttack();
        npcMove();
        changeAmmo();
    }

    private void preparingWaveLogic() {
        if (astralPlus.allowedToEquip()) {
            nextWaveCheck = 0;
        }

        if (nextWaveCheck > System.currentTimeMillis()) {
            return;
        }

        if (!waveHasBeenAwaited) {
            waveHasBeenAwaited = true;
            nextWaveCheck = System.currentTimeMillis() + MIN_TIME_FOR_WAVE_CHECK;
        }

        if (astralConfig.autoChoose
                && astralPlus.autoChoose(astralConfig.portalInfos, astralConfig.customItemPriority)) {
            this.currentStatus = State.CHOOSING_BEST_OPTION;
        } else if (!portals.isEmpty() || astralPlus.hasOptions()) {
            stopBot(State.WAITING_HUMAN);
        } else {
            this.currentStatus = State.WAITING_WAVE;
        }

        goToTheMiddle();
    }

    private void stopBot(State stateToSet) {
        this.showDialog = true;
        this.currentStatus = stateToSet;
        this.bot.setRunning(false);
    }

    private boolean isAstral() {
        return heroapi.getMap() != null && heroapi.getMap().getShortName() != null
                && heroapi.getMap().getShortName().equals("GG Astral");
    }

    private boolean changeAmmo() {
        if (astralConfig.useBestAmmoLogic == BestAmmoConfig.ALWAYS || attacker.hasExtraFlag(ExtraNpcFlags.BEST_AMMO)
                || useSpecialLogic()) {
            changeLaser(true);
            changeRocket(true);
            return true;
        } else if (astralConfig.useBestAmmoLogic == BestAmmoConfig.SPECIAL_LOGIC) {
            changeRocket(false);
        }

        Item currentLaser = items.getItems(ItemCategory.LASERS).stream().filter(Item::isSelected).findFirst()
                .orElse(null);
        if (currentLaser == null || currentLaser.getQuantity() <= 100) {
            changeLaser(false);
            return true;
        }

        Item currentRocket = items.getItems(ItemCategory.ROCKETS).stream().filter(Item::isSelected).findFirst()
                .orElse(null);
        if (currentRocket == null || currentRocket.getQuantity() <= 2) {
            changeRocket(false);
        }

        Item defaultLaser = items.getItems(ItemCategory.LASERS).stream()
                .filter(i -> i.getId().equals(astralConfig.defaultLaser)).findFirst()
                .orElse(null);
        if (defaultLaser != null && defaultLaser.getQuantity() > 100) {
            changeLaser(defaultLaser);
        }

        return false;
    }

    private boolean findTarget() {
        if (!npcs.isEmpty()) {
            if (!astralConfig.alwaysTheClosestNPC && !allLowLifeOrISH(true)) {
                Npc target = bestNpc();
                if (target != null) {
                    attacker.setTarget(target);
                }
            } else {
                Npc target = closestNpc();
                if (target != null) {
                    attacker.setTarget(target);
                }
            }
        } else {
            resetTarget();
        }

        return attacker.getTarget() != null;
    }

    private void resetTarget() {
        attacker.setTarget(null);
    }

    private boolean allLowLifeOrISH(boolean countISH) {
        int npcsLowLife = 0;

        for (Npc n : npcs) {
            if (countISH && (n.hasEffect(EntityEffect.ISH) || n.hasEffect(EntityEffect.NPC_ISH))) {
                return true;
            }
            if (isLowHealh(n)) {
                npcsLowLife++;
            }
        }

        return npcsLowLife >= npcs.size();
    }

    private boolean isLowHealh(Npc npc) {
        return npc.getHealth().hpPercent() < 0.25;
    }

    private Npc bestNpc() {
        return this.npcs.stream()
                .filter(n -> (!n.hasEffect(EntityEffect.ISH) && n.getHealth().hpPercent() > 0.25 &&
                        !n.hasEffect(EntityEffect.NPC_ISH)))
                .min(Comparator.<Npc>comparingDouble(n -> (n.getInfo().getPriority()))
                        .thenComparing(n -> (n.getLocationInfo().getCurrent().distanceTo(heroapi))))
                .orElse(null);
    }

    private Npc closestNpc() {
        return this.npcs.stream()
                .filter(n -> (!n.hasEffect(EntityEffect.ISH) &&
                        !n.hasEffect(EntityEffect.NPC_ISH)))
                .min(Comparator.<Npc>comparingDouble(n -> n.getLocationInfo().getCurrent().distanceTo(heroapi))
                        .thenComparing(n -> n.getInfo().getPriority())
                        .thenComparing(n -> n.getHealth().hpPercent()))
                .orElse(null);
    }

    private double getRadius(Lockable target) {
        if (repairShield) {
            return 1500;
        }
        if (!(target instanceof Npc)) {
            return astralConfig.radioMin;
        }

        double npcRadius = ((Npc) target).getInfo().getRadius();

        if (npcRadius < astralConfig.radioMin) {
            npcRadius = astralConfig.radioMin;
        }

        return attacker.modifyRadius(npcRadius);
    }

    private void npcMove() {
        if (!attacker.hasTarget()) {
            return;
        }
        Lockable target = attacker.getTarget();

        Location direction = movement.getDestination();
        Location targetLoc = target.getLocationInfo().destinationInTime(400);

        double distance = heroapi.distanceTo(attacker.getTarget());
        double angle = targetLoc.angleTo(heroapi);
        double radius = getRadius(target);
        double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;
        boolean noCircle = attacker.hasExtraFlag(NpcFlag.NO_CIRCLE);

        lastRadius = radius;

        double angleDiff;
        if (noCircle) {
            double dist = targetLoc.distanceTo(direction);
            double minRad = Math.max(0, Math.min(radius - 200, radius * 0.5));
            if (dist <= radius && dist >= minRad) {
                return;
            }
            distance = minRad + Math.random() * (radius - minRad - 10);
            angleDiff = (Math.random() * 0.1) - 0.05;
        } else {
            double maxRadFix = radius / 2;
            double radiusFix = (int) Math.max(Math.min(radius - distance, maxRadFix), -maxRadFix);
            distance = (radius += radiusFix);
            angleDiff = Math.max((heroapi.getSpeed() * 0.625) + (Math.max(200, speed) * 0.625)
                    - heroapi.distanceTo(Location.of(targetLoc, angle, radius)), 0) / radius;
        }
        direction = getBestDir(targetLoc, angle, angleDiff, distance);

        while (!canMoveFix(direction) && distance < 10000) {
            direction.toAngle(targetLoc, angle += backwards ? -0.3 : 0.3, distance += 2);
        }
        if (distance >= 10000) {
            direction.toAngle(targetLoc, angle, 500);
        }

        if (canMoveFix(direction)) {
            movement.moveTo(direction);
        }
    }

    private Location getBestDir(Locatable targetLoc, double angle, double angleDiff, double distance) {
        int maxCircleIterationsValue = this.maxCircleIterations.getValue();
        int iteration = 1;
        double forwardScore = 0;
        double backScore = 0;
        do {
            forwardScore += score(Locatable.of(targetLoc, angle + (angleDiff * iteration), distance));
            backScore += score(Locatable.of(targetLoc, angle - (angleDiff * iteration), distance));
            if (forwardScore < 0 != backScore < 0 || Math.abs(forwardScore - backScore) > 300)
                break;
        } while (iteration++ < maxCircleIterationsValue);

        if (iteration <= maxCircleIterationsValue)
            backwards = backScore > forwardScore;
        return Location.of(targetLoc, angle + angleDiff * (backwards ? -1 : 1), distance);
    }

    private double score(Locatable loc) {
        return (canMoveFix(loc) ? 0 : -1000) - npcs.stream()
                .filter(n -> attacker.getTarget() != n)
                .mapToDouble(n -> Math.max(0, n.getInfo().getRadius() - n.distanceTo(loc)))
                .sum();
    }

    private void changeRocket(boolean bestRocket) {
        SelectableItem rocket = null;
        if (this.rocketSupplier == null) {
            return;
        }

        if (bestRocket) {
            rocket = rocketSupplier.getBestRocket(heroapi.getLocalTarget(), false);
        } else {
            rocket = SharedFunctions.getItemById(astralConfig.defaultRocket);
            if (rocket == null
                    || items.getItem(rocket, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.POSITIVE_QUANTITY).isEmpty()) {
                rocket = rocketSupplier.getWorstRocket(false);
            }
        }

        if (rocket != null && heroapi.getRocket() != null && !heroapi.getRocket().getId().equals(rocket.getId())) {
            conditionsManagement.useSelectableReadyWhenReady(rocket);
        }
    }

    private void changeLaser(SelectableItem laser) {
        SelectableItem configuredLaserNpc = getLaserConfiguredFromNPC();
        if (configuredLaserNpc == null) {
            configuredLaserNpc = laser;
        }

        if (conditionsManagement.useSelectableReadyWhenReady(configuredLaserNpc)) {
            changeAmmoKey(configuredLaserNpc);
        }
    }

    private void changeAmmoKey(SelectableItem laser) {
        Character key = items.getKeyBind(laser);
        ammoKey.setValue(key);
    }

    private void changeLaser(boolean bestLaser) {
        SelectableItem laser = null;
        if (bestLaser) {
            laser = ammoSupplier.get();
        } else {
            laser = ammoSupplier.getReverse();
        }

        if (laser == null) {
            laser = SharedFunctions.getItemById(astralConfig.defaultLaser);
        }

        changeLaser(laser);
    }

    private SelectableItem getLaserConfiguredFromNPC() {
        Lockable target = heroapi.getLocalTarget();
        if (target != null && target.isValid() && target instanceof Npc) {
            Npc npc = (Npc) target;
            return npc.getInfo().getAmmo().orElse(null);
        }
        return null;
    }

    private void goToAstral() {
        try {
            if (portals.isEmpty()) {
                return;
            }

            GameMap map = starSystem.findMap("GG Astral").orElse(null);
            if (map == null || map == starSystem.getCurrentMap()) {
                return;
            }

            if (isHomeMap()) {
                if (portals.stream().anyMatch(p -> p.getTargetMap().isPresent() && p.getTargetMap().get() == map)) {
                    this.bot.setModule(api.requireInstance(MapModule.class)).setTarget(map);
                } else {
                    items.getItem(Cpu.ASTRAL_CPU, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.POSITIVE_QUANTITY)
                            .ifPresent(i -> {
                                if (i.getQuantity() > astralConfig.minCPUs) {
                                    conditionsManagement.useSelectableReadyWhenReady(Cpu.ASTRAL_CPU);
                                }
                            });
                }
            } else {
                this.bot.setModule(api.requireInstance(MapModule.class)).setTarget(map);
            }

        } catch (Exception e) {
            /* Nothing Here */
        }
    }

    private boolean isHomeMap() {
        return StarSystemAPI.HOME_MAPS.contains(starSystem.getCurrentMap().getShortName());
    }

    private boolean useSpecialLogic() {
        if (astralConfig.useBestAmmoLogic != BestAmmoConfig.SPECIAL_LOGIC) {
            return false;
        }

        if (astralPlus.getRift() >= 15) {
            return true;
        }

        if (heroapi.getHealth().hpPercent() <= 0.20) {
            return true;
        }

        Entity target = heroapi.getLocalTarget();

        if (target == null || !target.isValid()) {
            return false;
        }

        double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;
        return speed >= heroapi.getSpeed() && heroapi.getHealth() != null
                && heroapi.getHealth().shieldPercent() <= 0.95;

    }

    private void goToTheMiddle() {
        Locatable loc = Locatable.of(starSystem.getCurrentMapBounds().getWidth() / 2,
                starSystem.getCurrentMapBounds().getHeight() / 2);
        if (heroapi.distanceTo(loc) > 500 && canMoveFix(loc)
                && (!movement.isMoving() || !movement.getDestination().isSameAs(loc))) {
            movement.moveTo(loc);
        }
    }

    private void activeAutoRocketCPU() {
        if (nextCPUCheck < System.currentTimeMillis()) {
            nextCPUCheck = System.currentTimeMillis() + 300000;
            conditionsManagement.useSelectableReadyWhenReady(SelectableItem.Cpu.AROL_X);
        }
    }

    private boolean canMoveFix(Locatable loc) {
        if (!movement.canMove(loc)) {
            return false;
        }

        int radiationOffSet = 50;

        return movement.canMove(loc.getX() + radiationOffSet, loc.getY())
                && movement.canMove(loc.getX(), loc.getY() + radiationOffSet)
                && movement.canMove(loc.getX() - radiationOffSet, loc.getY())
                && movement.canMove(loc.getX(), loc.getY() - radiationOffSet);
    }

    private void showWarningDialog() {
        if (!showDialog || warningDisplayed) {
            return;
        }

        this.warningDisplayed = true;

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> {
            SwingUtilities.getWindowAncestor(closeBtn).setVisible(false);
            warningDisplayed = false;
            showDialog = false;
        });

        JButton discordBtn = new JButton("+Info");
        discordBtn.addActionListener(e -> SystemUtils.openUrl(Utils.DISCORD_URL));

        Popups.of("Astral Gate",
                new JOptionPane(
                        "Manual action is needed. \n With the PLUS functions the bot will not need manual actions. \n If you have access to the PLUS functions, activate in the configuration the option 'Auto choose the best option'.",
                        JOptionPane.INFORMATION_MESSAGE,
                        JOptionPane.DEFAULT_OPTION, null, new Object[] { discordBtn, closeBtn }))
                .showAsync();
    }
}
