package com.deeme.modules;

import com.deeme.types.AmmoSupplier;
import com.deeme.types.RocketSupplier;
import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.NpcFlag;
import eu.darkbot.api.config.types.PercentRange;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Module;
import eu.darkbot.api.extensions.InstructionProvider;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.entities.Portal;
import eu.darkbot.api.game.enums.EntityEffect;
import eu.darkbot.api.game.items.ItemFlag;
import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.other.Locatable;
import eu.darkbot.api.game.other.Location;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.game.other.Movable;
import eu.darkbot.api.managers.AttackAPI;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import eu.darkbot.api.managers.MovementAPI;
import eu.darkbot.api.managers.PetAPI;
import eu.darkbot.api.managers.StarSystemAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.utils.SafetyFinder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

@Feature(name = "Astral Gate", description = "For the astral gate and another GGs")
public class AstralGate implements Module, InstructionProvider {
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

    protected Collection<? extends Portal> portals;
    protected Collection<? extends Npc> npcs;

    protected boolean backwards = false;
    protected long maximumWaitingTime = 0;
    protected long lastTimeAttack = 0;
    protected long rocketTime;
    protected long laserTime;
    protected long clickDelay;

    protected boolean repairShield = false;
    protected boolean waitingSign = false;

    private RocketSupplier rocketSupplier;
    private AmmoSupplier ammoSupplier;

    public AstralGate(PluginAPI api) throws UnsupportedOperationException, Exception {
        this(api, api.requireAPI(AuthAPI.class),
                api.requireInstance(SafetyFinder.class));
    }

    @Inject
    public AstralGate(PluginAPI api, AuthAPI auth, SafetyFinder safety) throws Exception {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners()))
            throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        if (!Utils.discordCheck(auth.getAuthId())) {
            Utils.showDiscordDialog();
            throw new Exception("To use this option you need to be on my discord");
        }

        this.api = api;
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

        ConfigSetting<PercentRange> repairHpRange = configApi.requireConfig("general.safety.repair_hp_range");

        this.rocketSupplier = new RocketSupplier(heroapi, items, repairHpRange.getValue().getMin());
        this.ammoSupplier = new AmmoSupplier(items);
    }

    @Override
    public boolean canRefresh() {
        return waitingSign && npcs.size() < 1;
    }

    @Override
    public String getStatus() {
        return (waitingSign ? "Choose an option to continue" : "") + " | " + npcs.size() + " | " + attacker.getStatus();
    }

    @Override
    public String instructions() {
        return "You need to manually enter the gate and select the ship. \n" +
                "Place the quick bar with everything you want to be used. \n" +
                "The bot will wait until you choose an item and portal. He does not jump through the gates. \n" +
                "Add the 'Ignore Boxes' tag to the NPC to make the bot choose the best ammo automatically.";
    }

    @Override
    public void onTickModule() {
        if (heroapi.getMap().getId() == 466 || heroapi.getMap().isGG()) {
            pet.setEnabled(false);
            repairShield = repairShield && heroapi.getHealth().shieldPercent() < 0.9
                    || heroapi.getHealth().shieldPercent() < 0.2;
            if (findTarget()) {
                waitingSign = false;
                attacker.tryLockAndAttack();
                npcMove();
                boolean bestAmmo = attacker.hasExtraFlag(NpcFlag.IGNORE_BOXES);
                if (bestAmmo) {
                    changeRocket();
                    changeLaser();
                }
            } else {
                if (npcs.size() < 1) {
                    waitingSign = true;
                }
            }
        }
    }

    private boolean findTarget() {
        if (!npcs.isEmpty()) {
            if (!allLowLifeOrISH(true)) {
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
            return 1200;
        }
        if (!(target instanceof Npc)) {
            return 570;
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

        if (radius < 560) {
            radius = 580;
        }

        if (radius > 750) {
            noCircle = false;
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
            double maxRadFix = radius / 2,
                    radiusFix = (int) Math.max(Math.min(radius - distance, maxRadFix), -maxRadFix);
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
        int maxCircleIterationsValue = this.maxCircleIterations.getValue(), iteration = 1;
        double forwardScore = 0, backScore = 0;
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

    public void changeRocket() {
        if (System.currentTimeMillis() < rocketTime) {
            return;
        }
        SelectableItem rocket = getBestRocket();

        if (rocket != null && !heroapi.getRocket().getId().equals(rocket.getId())
                && useSelectableReadyWhenReady(rocket)) {
            rocketTime = System.currentTimeMillis() + 2000;
        }
    }

    public void changeLaser() {
        if (System.currentTimeMillis() < laserTime) {
            return;
        }
        SelectableItem laser = ammoSupplier.get();

        if (laser != null && heroapi.getLaser() != null && !heroapi.getLaser().getId().equals(laser.getId())
                && useSelectableReadyWhenReady(laser)) {
            laserTime = System.currentTimeMillis() + 2000;
        }

    }

    private SelectableItem getBestRocket() {
        return rocketSupplier.get();
    }

    public boolean useSelectableReadyWhenReady(SelectableItem selectableItem) {
        if (System.currentTimeMillis() - clickDelay < 1000)
            return false;
        if (selectableItem == null)
            return false;

        boolean isReady = items.getItem(selectableItem, ItemFlag.USABLE, ItemFlag.READY).isPresent();

        if (isReady && items.useItem(selectableItem).isSuccessful()) {
            clickDelay = System.currentTimeMillis();
            return true;
        }

        return false;
    }
}
