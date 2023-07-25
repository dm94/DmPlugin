package com.deeme.modules.astral;

import com.deeme.types.SharedFunctions;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
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
import eu.darkbot.api.game.items.SelectableItem.Laser;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.game.other.Gui;
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
import eu.darkbot.api.managers.GameScreenAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.managers.PetAPI;
import eu.darkbot.api.managers.StarSystemAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.modules.MapModule;
import eu.darkbot.shared.utils.SafetyFinder;
import eu.darkbot.util.Popups;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Random;

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
    protected ConfigSetting<Integer> maxCircleIterations;
    protected ConfigSetting<Boolean> runConfigInCircle;
    protected final ConfigSetting<Character> ammoKey;

    private Gui astralGui;

    protected Collection<? extends Portal> portals;
    protected Collection<? extends Npc> npcs;

    protected boolean backwards = false;
    protected long maximumWaitingTime = 0;
    protected long lastTimeAttack = 0;
    protected long rocketTime;
    protected long laserTime;
    protected long clickDelay;
    protected long chooseClickDelay = 0;
    protected long nextCPUCheck = 0;
    protected long nextWaveCheck = 0;

    protected boolean repairShield = false;
    protected boolean waitingSign = false;

    private RocketSupplier rocketSupplier;
    private AmmoSupplier ammoSupplier;
    private AstralConfig astralConfig;
    private AstralPortalSupplier astralPortalSupplier;

    private AstralShip astralShip = null;

    private final Random rand = new Random();
    private int lastPortal = 0;
    private State currentStatus;
    private int guiOffset = 100;
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
        CHOOSING_PORTAL("Choosing the best portal"),
        CHOOSING_ITEM("Choosing a random item");

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
        this.bot = api.getAPI(BotAPI.class);
        this.heroapi = api.getAPI(HeroAPI.class);
        this.movement = api.getAPI(MovementAPI.class);
        this.attacker = api.getAPI(AttackAPI.class);
        this.pet = api.getAPI(PetAPI.class);
        this.starSystem = api.getAPI(StarSystemAPI.class);
        this.items = api.getAPI(HeroItemsAPI.class);

        GameScreenAPI gameScreenAPI = api.getAPI(GameScreenAPI.class);
        this.astralGui = gameScreenAPI.getGui("rogue_lite");

        EntitiesAPI entities = api.getAPI(EntitiesAPI.class);
        this.portals = entities.getPortals();
        this.npcs = entities.getNpcs();

        ConfigAPI configApi = api.getAPI(ConfigAPI.class);

        this.maxCircleIterations = configApi.requireConfig("loot.max_circle_iterations");
        this.runConfigInCircle = configApi.requireConfig("loot.run_config_in_circle");
        this.ammoKey = configApi.requireConfig("loot.ammo_key");

        this.rocketSupplier = new RocketSupplier(heroapi, items);
        this.ammoSupplier = new AmmoSupplier(items);
        this.astralPortalSupplier = new AstralPortalSupplier(api, astralShip);

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
        return "Astral | " + currentStatus.message + " | " + npcs.size() + " | " + attacker.getStatus() + "\nRadius: "
                + lastRadius;
    }

    @Override
    public String getStoppedStatus() {
        return (astralShip != null ? astralShip.getStatus()
                : "")
                + " | Ammo: " + astralPortalSupplier.getAmmoCount()
                + "\nAstral GUI: " + (astralGui != null && astralGui.isVisible());
    }

    @Override
    public String instructions() {
        return "You need to manually enter the gate and select the ship. \n" +
                "Place the quick bar with everything you want to be used. \n" +
                "The bot will wait until you choose an item and portal. He does not jump through the gates.";
    }

    @Override
    public void onTickModule() {
        pet.setEnabled(false);
        showWarningDialog();
        if (isAstral() || heroapi.getMap().isGG()) {
            if (astralShip == null) {
                astralShip = new AstralShip(heroapi.getShipType());
            }
            if (astralShip.isValid(heroapi.getShipType())) {
                activeAutoRocketCPU();
                repairShield = repairShield && heroapi.getHealth().shieldPercent() < 0.9
                        || heroapi.getHealth().shieldPercent() < 0.2;
                if (findTarget()) {
                    nextWaveCheck = System.currentTimeMillis() + 30000;
                    waitingSign = false;
                    this.currentStatus = State.DO;
                    if (astralGui != null && astralGui.isVisible()) {
                        astralGui.setVisible(false);
                    }
                    attacker.tryLockAndAttack();
                    npcMove();
                    changeAmmo();
                } else if (npcs.isEmpty() && nextWaveCheck < System.currentTimeMillis()) {
                    if (waitingSign) {
                        nextWaveCheck = System.currentTimeMillis() + 30000;
                        goToTheMiddle();

                        if (astralConfig.autoChoosePortal || astralConfig.autoChooseItem) {
                            autoChooseLogic();
                        } else if (!portals.isEmpty() || (astralGui != null && astralGui.isVisible())) {
                            this.currentStatus = State.WAITING_HUMAN;
                            this.showDialog = true;
                            this.bot.setRunning(false);
                        } else {
                            this.currentStatus = State.WAITING_WAVE;
                        }
                    }
                    waitingSign = true;
                }
            } else {
                this.showDialog = true;
                this.currentStatus = State.WAITING_SHIP;
                this.astralShip = null;
                this.bot.setRunning(false);
            }
        } else {
            goToAstral();
        }
    }

    @Override
    public void onTickStopped() {
        if (isAstral()) {
            showWarningDialog();
            countItems();
        }
    }

    private void autoChooseLogic() {
        if (!astralGui.isVisible()) {
            chooseClickDelay = System.currentTimeMillis() + 10000;
        }
        if (portals.isEmpty()) {
            if (astralGui.isVisible()) {
                chooseItem();
            } else {
                if (this.currentStatus == State.WAITING_SIGN && maximumWaitingTime < System.currentTimeMillis()) {
                    astralGui.setVisible(true);
                } else {
                    this.currentStatus = State.WAITING_SIGN;
                    maximumWaitingTime = System.currentTimeMillis() + 20000;
                }
            }
        } else if (astralConfig.autoChoosePortal) {
            jumpToTheBestPortal();
        }
    }

    private void jumpToTheBestPortal() {
        this.currentStatus = State.CHOOSING_PORTAL;
        if (astralGui != null && astralGui.isVisible()) {
            astralGui.setVisible(false);
        }

        astralPortalSupplier.setAstralShip(astralShip);
        Portal portal = astralPortalSupplier.get();

        if (portal == null) {
            portal = portals.stream().filter(p -> p.getTypeId() == 1).findFirst().orElse(null);
        }

        if (portal != null) {
            lastPortal = portal.getTypeId();
            if (heroapi.distanceTo(portal) < 200) {
                movement.jumpPortal(portal);
            } else {
                movement.moveTo(portal);
            }
        }
    }

    private void chooseItem() {
        if (astralGui.isVisible() && chooseClickDelay < System.currentTimeMillis() && astralShip != null) {
            chooseClickDelay = System.currentTimeMillis() + 20000;
            if (lastPortal == 87 || lastPortal == 88) {
                astralShip.setWeapons(astralShip.getWeapons() + 1);
            } else if (lastPortal == 89 || lastPortal == 90) {
                astralShip.setGenerators(astralShip.getGenerators() + 1);
            } else if (lastPortal == 95 || lastPortal == 96) {
                astralShip.setModules(astralShip.getModules() + 1);
            }
            randomChoose();
            lastPortal = 0;
            astralGui.setVisible(false);
        }
    }

    private void randomChoose() {
        if (astralConfig.autoChooseItem) {
            this.currentStatus = State.CHOOSING_ITEM;
            Integer xPoint = rand.nextInt((int) astralGui.getWidth() - guiOffset) + guiOffset + (int) astralGui.getX();
            Integer yPoint = (int) ((astralGui.getHeight() / 2) + astralGui.getY());
            astralGui.click(xPoint, yPoint);
            System.out.println("Click  X: " + xPoint + " | Y:" + yPoint);
        }
    }

    private boolean isAstral() {
        return heroapi.getMap() != null && heroapi.getMap().getShortName() != null
                && heroapi.getMap().getShortName().equals("GG Astral");
    }

    private boolean changeAmmo() {
        if (System.currentTimeMillis() < laserTime) {
            return false;
        }

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

        if (astralConfig.ammoKey == null) {
            astralConfig.ammoKey = ammoKey.getValue();
        }
        if (!ammoKey.getValue().equals(astralConfig.ammoKey)) {
            ammoKey.setValue(astralConfig.ammoKey);
        }
        Item defaultLaser = items.getItem(ammoKey.getValue());
        if (defaultLaser == null || defaultLaser.getQuantity() > 100) {
            changeLaser(false);
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
        if (System.currentTimeMillis() < rocketTime) {
            return;
        }
        SelectableItem rocket = null;
        if (bestRocket) {
            rocket = rocketSupplier.get();
        } else {
            rocket = SharedFunctions.getItemById(astralConfig.defaultRocket);
            if (rocket == null || items.getItem(rocket, ItemFlag.USABLE, ItemFlag.READY).isEmpty()) {
                rocket = rocketSupplier.getReverse();
            }
        }

        if (rocket != null && heroapi.getRocket() != null && !heroapi.getRocket().getId().equals(rocket.getId())
                && useSelectableReadyWhenReady(rocket)) {
            rocketTime = System.currentTimeMillis() + 2000;
        }
    }

    public void changeLaser(boolean bestLaser) {
        SelectableItem laser = null;
        if (bestLaser) {
            laser = ammoSupplier.get();
        } else {
            laser = ammoSupplier.getReverse();
        }
        if (laser != null) {
            Laser currentLaser = heroapi.getLaser();
            if (currentLaser != null && !currentLaser.getId().equals(laser.getId())
                    && useSelectableReadyWhenReady(laser)) {
                Character key = items.getKeyBind(laser);
                if (key != null && !ammoKey.getValue().equals(key)) {
                    ammoKey.setValue(key);
                }
                laserTime = System.currentTimeMillis() + 500;
            }
        }
    }

    public boolean useSelectableReadyWhenReady(SelectableItem selectableItem) {
        if (System.currentTimeMillis() - clickDelay < 1000)
            return false;
        if (selectableItem == null)
            return false;

        if (items.useItem(selectableItem, ItemFlag.USABLE, ItemFlag.READY).isSuccessful()) {
            clickDelay = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    private void goToAstral() {
        try {
            GameMap map = starSystem.getByName("GG Astral");
            if (map != null && !portals.isEmpty() && map != starSystem.getCurrentMap()) {
                if (isHomeMap()) {
                    if (portals.stream().anyMatch(p -> p.getTargetMap().isPresent() && p.getTargetMap().get() == map)) {
                        this.bot.setModule(api.requireInstance(MapModule.class)).setTarget(map);
                    } else {
                        items.getItem(CPUPLUS.ASTRAL_CPU, ItemFlag.USABLE, ItemFlag.READY, ItemFlag.POSITIVE_QUANTITY)
                                .ifPresent(i -> {
                                    if (i.getQuantity() > astralConfig.minCPUs) {
                                        useSelectableReadyWhenReady(CPUPLUS.ASTRAL_CPU);
                                    }
                                });
                    }
                } else {
                    this.bot.setModule(api.requireInstance(MapModule.class)).setTarget(map);
                }
            }
        } catch (Exception e) {
            System.out.println("Map not found" + e.getMessage());
        }
    }

    private boolean isHomeMap() {
        return StarSystemAPI.HOME_MAPS.contains(starSystem.getCurrentMap().getShortName());
    }

    private boolean useSpecialLogic() {
        if (astralConfig.useBestAmmoLogic == BestAmmoConfig.SPECIAL_LOGIC) {
            if (heroapi.getHealth().hpPercent() <= 0.20) {
                return true;
            }

            Entity target = heroapi.getLocalTarget();
            if (target != null && target.isValid()) {
                double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;
                return speed >= heroapi.getSpeed() && heroapi.getHealth() != null
                        && heroapi.getHealth().shieldPercent() <= 0.95;
            }
        }

        return false;
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

    private void countItems() {
        if (portals != null) {
            if (portals.isEmpty()) {
                if (lastPortal != 0 && astralShip != null) {
                    switch (lastPortal) {
                        case 87:
                        case 88:
                            astralShip.setWeapons(astralShip.getWeapons() + 1);
                            break;
                        case 89:
                        case 90:
                            astralShip.setGenerators(astralShip.getGenerators() + 1);
                            break;
                        case 95:
                        case 96:
                            astralShip.setModules(astralShip.getModules() + 1);
                            break;
                        default:
                            lastPortal = 0;
                    }
                }
            } else {
                Portal portal = portals.stream().filter(Portal::isJumping).findFirst().orElse(null);
                if (portal != null) {
                    lastPortal = portal.getTypeId();
                }
            }
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
        Popups.of("Astral Gate",
                new JOptionPane(
                        "Manual action is needed",
                        JOptionPane.INFORMATION_MESSAGE,
                        JOptionPane.DEFAULT_OPTION, null, new Object[] { closeBtn }))
                .showAsync();
    }

    enum CPUPLUS implements SelectableItem {
        ASTRAL_CPU;

        @Override
        public String getId() {
            return "ammunition_ggportal_astral-cpu";
        }

        @Override
        public ItemCategory getCategory() {
            return ItemCategory.CPUS;
        }

    }
}
