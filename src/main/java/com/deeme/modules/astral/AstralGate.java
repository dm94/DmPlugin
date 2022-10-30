package com.deeme.modules.astral;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.github.manolo8.darkbot.config.NpcExtraFlag;
import com.github.manolo8.darkbot.core.api.DarkBoatAdapter;
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
import eu.darkbot.api.managers.StarSystemAPI.MapNotFoundException;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.modules.MapModule;
import eu.darkbot.shared.utils.SafetyFinder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Random;

import static com.github.manolo8.darkbot.Main.API;

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

    private Gui astralGuiSelection;
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

    private enum State {
        WAIT("Waiting"),
        DO("Attacking"),
        WAITING_SIGN("Waiting for the selector"),
        WAITING_WAVE("Waiting for the wave"),
        WAITING_HUMAN("Choose an option to continue"),
        WAITING_SHIP("Choose a ship");

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

        if (!Utils.discordCheck(auth.getAuthId())) {
            Utils.showDiscordDialog();
            ExtensionsAPI extensionsAPI = api.getAPI(ExtensionsAPI.class);
            extensionsAPI.getFeatureInfo(this.getClass())
                    .addFailure("To use this option you need to be on my discord", "Log in to my discord and reload");
        }

        this.api = api;
        this.bot = api.getAPI(BotAPI.class);
        this.heroapi = api.getAPI(HeroAPI.class);
        this.movement = api.getAPI(MovementAPI.class);
        this.attacker = api.getAPI(AttackAPI.class);
        this.pet = api.getAPI(PetAPI.class);
        this.starSystem = api.getAPI(StarSystemAPI.class);
        this.items = api.getAPI(HeroItemsAPI.class);

        GameScreenAPI gameScreenAPI = api.getAPI(GameScreenAPI.class);
        this.astralGuiSelection = gameScreenAPI.getGui("rogue_lite_selection");
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
        return "Astral | " + currentStatus.message + " | " + npcs.size() + " | " + attacker.getStatus();
    }

    @Override
    public String getStoppedStatus() {
        return (astralShip != null ? astralShip.getStatus()
                : "")
                + " | Ammo: " + astralPortalSupplier.getAmmoCount() + "\n" + "GUI: "
                + (astralGui != null && astralGui.isVisible());
    }

    @Override
    public String instructions() {
        return "You need to manually enter the gate and select the ship. \n" +
                "Place the quick bar with everything you want to be used. \n" +
                "The bot will wait until you choose an item and portal. He does not jump through the gates.";
    }

    @Override
    public void onTickModule() {
        if (heroapi.getMap().getId() == 466 || heroapi.getMap().getId() == 467 || heroapi.getMap().getId() == 468
                || heroapi.getMap().isGG()) {
            if (astralShip == null) {
                astralShip = new AstralShip(heroapi.getShipType());
            }
            if (astralShip.isValid()) {
                activeAutoRocketCPU();
                pet.setEnabled(false);
                repairShield = repairShield && heroapi.getHealth().shieldPercent() < 0.9
                        || heroapi.getHealth().shieldPercent() < 0.2;
                if (findTarget()) {
                    this.currentStatus = State.DO;
                    if (astralGuiSelection != null && astralGuiSelection.isVisible()) {
                        astralGuiSelection.setVisible(false);
                    }
                    if (astralGui != null && astralGui.isVisible()) {
                        astralGui.setVisible(false);
                    }
                    waitingSign = false;
                    attacker.tryLockAndAttack();
                    npcMove();
                    changeAmmo();
                } else {
                    if (npcs.isEmpty()) {
                        if (waitingSign) {
                            goToTheMiddle();
                        }
                        waitingSign = true;
                        if (astralGui != null && (astralConfig.autoChoosePortal || astralConfig.autoChooseItem)) {
                            if (!astralGui.isVisible()) {
                                chooseClickDelay = System.currentTimeMillis() + 10000;
                            }
                            autoChooseLogic();
                        } else if (!portals.isEmpty()) {
                            if (astralGui != null && astralGui.isVisible()) {
                                this.currentStatus = State.WAITING_HUMAN;
                                this.bot.setRunning(false);
                            }
                        } else {
                            this.currentStatus = State.WAITING_WAVE;
                        }
                    }
                }
            } else {
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
        if (heroapi.getMap().getId() == 466 || heroapi.getMap().getId() == 467 || heroapi.getMap().getId() == 468) {
            countItems();
        }
    }

    private void autoChooseLogic() {
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
        } else {
            if (astralConfig.autoChoosePortal) {
                jumpToTheBestPortal();
            }
        }
    }

    private void jumpToTheBestPortal() {
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
        if (astralGui.isVisible() && chooseClickDelay < System.currentTimeMillis()) {
            chooseClickDelay = System.currentTimeMillis() + 30000;
            if (astralShip == null) {
                astralShip = new AstralShip(heroapi.getShipType());
            }
            if (lastPortal == 87 || lastPortal == 88) {
                astralShip.setWeapons(astralShip.getWeapons() + 1);
                randomChoose();
            } else if (lastPortal == 89 || lastPortal == 90) {
                astralShip.setGenerators(astralShip.getGenerators() + 1);
                randomChoose();
            } else if (lastPortal == 95 || lastPortal == 96) {
                astralShip.setModules(astralShip.getModules() + 1);
                randomChoose();
            }
            lastPortal = 0;
            astralGui.setVisible(false);
        }
    }

    private void randomChoose() {
        if (astralConfig.autoChooseItem) {
            Integer xPoint = rand.nextInt((int) astralGui.getWidth() - guiOffset) + guiOffset + (int) astralGui.getX();
            Integer yPoint = (int) ((astralGui.getHeight() / 2) + astralGui.getY());

            System.out.println("GUI || X: " + astralGui.getX() + " | Y: " + astralGui.getY());
            System.out.println("GUI MAX || X: " + astralGui.getX2() + " | Y: " + astralGui.getY2());
            System.out.println("X: " + xPoint + " | Y: " + yPoint);

            astralGui.click(xPoint, yPoint);
        }
    }

    private boolean changeAmmo() {
        if (astralConfig.useBestAmmoLogic == BestAmmoConfig.ALWAYS || attacker.hasExtraFlag(ExtraNpcFlags.BEST_AMMO)
                || (astralConfig.useBestAmmoLogic == BestAmmoConfig.SPECIAL_LOGIC && isSlowerThanTarget())) {
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
        } else if (!ammoKey.getValue().equals(astralConfig.ammoKey)) {
            ammoKey.setValue(astralConfig.ammoKey);
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

        return attacker.modifyRadius(((Npc) target).getInfo().getRadius());
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

        if (radius < astralConfig.radioMin) {
            radius = astralConfig.radioMin;
        }

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
            rocket = rocketSupplier.getReverse();
        }

        if (rocket != null && heroapi.getRocket() != null && !heroapi.getRocket().getId().equals(rocket.getId())
                && useSelectableReadyWhenReady(rocket)) {
            rocketTime = System.currentTimeMillis() + 2000;
        }
    }

    public void changeLaser(boolean bestLaser) {
        if (System.currentTimeMillis() < laserTime) {
            return;
        }
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
                if (key != null) {
                    ammoKey.setValue(key);
                }
                laserTime = System.currentTimeMillis() + 2000;
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
                    } else if (astralConfig.astralCPUKey != null) {
                        Item item = items.getItem(astralConfig.astralCPUKey);
                        if (item != null) {
                            useSelectableReadyWhenReady(item);
                        }
                    }
                } else {
                    this.bot.setModule(api.requireInstance(MapModule.class)).setTarget(map);
                }
            }
        } catch (MapNotFoundException e) {
            System.out.println("Map not found" + e.getMessage());
        }
    }

    private boolean isHomeMap() {
        return StarSystemAPI.HOME_MAPS.contains(starSystem.getCurrentMap().getShortName());
    }

    private boolean isSlowerThanTarget() {
        Entity target = heroapi.getLocalTarget();
        if (target != null && target.isValid()) {
            double speed = target instanceof Movable ? ((Movable) target).getSpeed() : 0;
            return speed >= heroapi.getSpeed();
        }

        return false;
    }

    private void goToTheMiddle() {
        if (!movement.isMoving() && astralGui != null && !astralGui.isVisible()) {
            if (API instanceof DarkBoatAdapter) {
                movement.moveRandom();
            } else {
                movement.moveTo(starSystem.getCurrentMapBounds().getWidth() / 2,
                        starSystem.getCurrentMapBounds().getHeight() / 2);
            }
        }
    }

    private void activeAutoRocketCPU() {
        if (nextCPUCheck < System.currentTimeMillis()
                && items.useItem(SelectableItem.Cpu.AROL_X, ItemFlag.NOT_SELECTED).isSuccessful()) {
            nextCPUCheck = System.currentTimeMillis() + 300000;
        }
    }

    private void countItems() {
        if (portals.isEmpty()) {
            if (lastPortal != 0) {
                if (lastPortal == 87 || lastPortal == 88) {
                    astralShip.setWeapons(astralShip.getWeapons() + 1);
                } else if (lastPortal == 89 || lastPortal == 90) {
                    astralShip.setGenerators(astralShip.getGenerators() + 1);
                } else if (lastPortal == 95 || lastPortal == 96) {
                    astralShip.setModules(astralShip.getModules() + 1);
                }
                lastPortal = 0;
            }
        } else {
            Portal portal = portals.stream().filter(p -> p.isJumping()).findFirst().orElse(null);
            if (portal != null) {
                lastPortal = portal.getTypeId();
            }
        }
    }
}
