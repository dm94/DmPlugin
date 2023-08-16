package com.deeme.modules.astral;

import com.deeme.types.SharedFunctions;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deemetool.modules.astral.AstralPlus;
import com.github.manolo8.darkbot.config.NpcExtraFlag;
import com.github.manolo8.darkbot.core.itf.NpcExtraProvider;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.NpcFlag;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Module;
import eu.darkbot.api.extensions.InstructionProvider;
import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.entities.Portal;
import eu.darkbot.api.game.enums.EntityEffect;
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

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

@Feature(name = "Astral Gate", description = "For the astral gate and another GGs")
public class AstralGate implements Module, InstructionProvider, Configurable<AstralConfig>, NpcExtraProvider {
    protected final PluginAPI api;
    protected final HeroAPI heroapi;
    protected final BotAPI bot;
    protected final MovementAPI movement;
    protected final AttackAPI attacker;
    protected final PetAPI pet;
    protected final HeroItemsAPI items;
    protected final StarSystemAPI starSystem;
    protected final AuthAPI auth;
    protected ConfigSetting<Integer> maxCircleIterations;
    protected ConfigSetting<Boolean> runConfigInCircle;
    protected final ConfigSetting<Character> ammoKey;

    protected final AstralPlus astralPlus;

    protected Collection<? extends Portal> portals;
    protected Collection<? extends Npc> npcs;

    protected boolean backwards = false;
    protected long maximumWaitingTime = 0;
    protected long lastTimeAttack = 0;
    protected long clickDelay;
    protected long chooseClickDelay = 0;
    protected long nextCPUCheck = 0;
    protected long nextWaveCheck = 0;

    protected int waitTime = 10000;

    protected boolean repairShield = false;
    protected boolean waveHasBeenAwaited = false;

    private RocketSupplier rocketSupplier;
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
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        Utils.discordCheck(api.getAPI(ExtensionsAPI.class).getFeatureInfo(this.getClass()), auth.getAuthId());
        Utils.showDonateDialog();

        this.api = api;
        this.auth = auth;
        this.bot = api.getAPI(BotAPI.class);
        this.heroapi = api.getAPI(HeroAPI.class);
        this.movement = api.getAPI(MovementAPI.class);
        this.attacker = api.getAPI(AttackAPI.class);
        this.pet = api.getAPI(PetAPI.class);
        this.starSystem = api.getAPI(StarSystemAPI.class);
        this.items = api.getAPI(HeroItemsAPI.class);

        EntitiesAPI entities = api.getAPI(EntitiesAPI.class);
        this.portals = entities.getPortals();
        this.npcs = entities.getNpcs();

        ConfigAPI configApi = api.getAPI(ConfigAPI.class);

        this.maxCircleIterations = configApi.requireConfig("loot.max_circle_iterations");
        this.runConfigInCircle = configApi.requireConfig("loot.run_config_in_circle");
        this.ammoKey = configApi.requireConfig("loot.ammo_key");

        ConfigSetting<Boolean> devStuff = configApi.requireConfig("bot_settings.other.dev_stuff");

        this.rocketSupplier = new RocketSupplier(heroapi, items);
        this.ammoSupplier = new AmmoSupplier(items);
        this.astralPlus = new AstralPlus(api, devStuff.getValue());

        this.currentStatus = State.WAIT;
        this.showDialog = false;
        this.warningDisplayed = false;
    }

    @Override
    public void setConfig(ConfigSetting<AstralConfig> arg0) {
        this.astralConfig = arg0.getValue();
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

    private void waveLogic() {
        nextWaveCheck = System.currentTimeMillis() + waitTime;
        waveHasBeenAwaited = false;
        this.currentStatus = State.DO;

        attacker.tryLockAndAttack();
        npcMove();
        changeAmmo();
    }

    private void preparingWaveLogic() {
        if (nextWaveCheck > System.currentTimeMillis()) {
            return;
        }
        if (!waveHasBeenAwaited) {
            waveHasBeenAwaited = true;
            nextWaveCheck = System.currentTimeMillis() + waitTime;
        }

        goToTheMiddle();

        if (astralConfig.autoChoose && astralPlus.autoChoose()) {
            this.currentStatus = State.CHOOSING_BEST_OPTION;
        } else if (!portals.isEmpty() || astralPlus.hasOptionsToChoose()) {
            stopBot(State.WAITING_HUMAN);
        } else {
            this.currentStatus = State.WAITING_WAVE;
        }
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

    protected double getRadius(Lockable target) {
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

    protected void npcMove() {
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

        while (!movement.canMove(direction) && distance < 10000)
            direction.toAngle(targetLoc, angle += backwards ? -0.3 : 0.3, distance += 2);
        if (distance >= 10000)
            direction.toAngle(targetLoc, angle, 500);

        movement.moveTo(direction);
    }

    protected Location getBestDir(Locatable targetLoc, double angle, double angleDiff, double distance) {
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

    protected double score(Locatable loc) {
        return (movement.canMove(loc) ? 0 : -1000) - npcs.stream()
                .filter(n -> attacker.getTarget() != n)
                .mapToDouble(n -> Math.max(0, n.getInfo().getRadius() - n.distanceTo(loc)))
                .sum();
    }

    public void changeRocket(boolean bestRocket) {
        SelectableItem rocket = null;
        if (bestRocket) {
            rocket = rocketSupplier.get();
        } else {
            rocket = SharedFunctions.getItemById(astralConfig.defaultRocket);
            if (rocket == null
                    || items.getItem(rocket, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.POSITIVE_QUANTITY).isEmpty()) {
                rocket = rocketSupplier.getReverse();
            }
        }

        if (rocket != null && heroapi.getRocket() != null && !heroapi.getRocket().getId().equals(rocket.getId())) {
            useSelectableReadyWhenReady(rocket);
        }
    }

    private void changeLaser(SelectableItem laser) {
        try {
            if (laser != null && heroapi.getLaser() != null && !heroapi.getLaser().getId().equals(laser.getId())
                    && items.useItem(laser, ItemFlag.USABLE, ItemFlag.READY).isSuccessful()) {
                changeAmmoKey(laser);
            }
        } catch (Exception e) {
            // HeroApi getLaser Error
        }
    }

    private void changeAmmoKey(SelectableItem laser) {
        Character key = items.getKeyBind(laser);
        if (ammoKey.getValue() != key) {
            ammoKey.setValue(key);
        }
    }

    public void changeLaser(boolean bestLaser) {
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

    public boolean useSelectableReadyWhenReady(SelectableItem selectableItem) {
        if (System.currentTimeMillis() - clickDelay < 1000) {
            return false;
        }
        if (selectableItem == null) {
            return false;
        }

        if (items.useItem(selectableItem, ItemFlag.USABLE, ItemFlag.READY).isSuccessful()) {
            clickDelay = System.currentTimeMillis();
            return true;
        }
        return false;
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
                                    useSelectableReadyWhenReady(Cpu.ASTRAL_CPU);
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
        if (!movement.isMoving() && heroapi.distanceTo(loc) > 500) {
            movement.moveTo(loc);
        }
    }

    private void activeAutoRocketCPU() {
        if (nextCPUCheck < System.currentTimeMillis()) {
            nextCPUCheck = System.currentTimeMillis() + 300000;
            items.useItem(SelectableItem.Cpu.AROL_X, ItemFlag.NOT_SELECTED);
        }
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
        discordBtn.addActionListener(e -> {
            SystemUtils.openUrl(Utils.discordUrl);
        });

        Popups.of("Astral Gate",
                new JOptionPane(
                        "Manual action is needed. \n With the PLUS functions the bot will not need manual actions.",
                        JOptionPane.INFORMATION_MESSAGE,
                        JOptionPane.DEFAULT_OPTION, null, new Object[] { discordBtn, closeBtn }))
                .showAsync();
    }
}
