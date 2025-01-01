package com.deeme.modules.genericgate;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.github.manolo8.darkbot.config.Config.ShipConfig;
import com.github.manolo8.darkbot.config.NpcExtraFlag;
import com.github.manolo8.darkbot.core.itf.NpcExtraProvider;

import eu.darkbot.api.config.types.PercentRange;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.FeatureInfo;
import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.NpcFlag;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.entities.Portal;
import eu.darkbot.api.game.enums.EntityEffect;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.game.other.Locatable;
import eu.darkbot.api.game.other.Location;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.game.other.Movable;
import eu.darkbot.api.managers.AttackAPI;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.shared.modules.CollectorModule;
import eu.darkbot.shared.modules.MapModule;

@Feature(name = "Generic Gate", description = "For any map, event")
public class GenericGateDummy extends CollectorModule implements Configurable<Config>, NpcExtraProvider {
    private final HeroAPI heroapi;
    private final AttackAPI attacker;

    private Collection<? extends Npc> npcs;

    private ConfigSetting<Integer> maxCircleIterations;
    private final ConfigSetting<PercentRange> repairHpRange;
    private final ConfigSetting<ShipConfig> repairConfig;

    private boolean repair = false;
    private boolean backwards = false;
    private long nextWaveCheck = 0;

    private Config gateConfig;

    private State currentStatus = State.WAIT;

    private enum State {
        WAIT("Waiting"),
        ATTACKING("Attacking"),
        WAITING_WAVE("Waiting for the wave");

        private final String message;

        State(String message) {
            this.message = message;
        }
    }

    public GenericGateDummy(PluginAPI api) {
        super(api);

        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }

        this.heroapi = api.requireAPI(HeroAPI.class);
        this.attacker = api.requireAPI(AttackAPI.class);

        EntitiesAPI entities = api.requireAPI(EntitiesAPI.class);
        this.npcs = entities.getNpcs();

        ConfigAPI configApi = api.requireAPI(ConfigAPI.class);

        this.maxCircleIterations = configApi.requireConfig("loot.max_circle_iterations");
        this.repairHpRange = configApi.requireConfig("general.safety.repair_hp_range");
        this.repairConfig = configApi.requireConfig("general.safety.repair");
        this.currentStatus = State.WAIT;

        setup();
    }

    @Override
    public NpcExtraFlag[] values() {
        return ExtraNpcFlagsEnum.values();
    }

    @Override
    public void setConfig(ConfigSetting<Config> arg0) {
        this.gateConfig = arg0.getValue();

        setup();
    }

    @Override
    public String getStatus() {
        return "Generic Gate | " + currentStatus.message + " | " + npcs.size() + " | " + attacker.getStatus();
    }

    @Override
    public boolean canRefresh() {
        return !attacker.isAttacking() && (!gateConfig.collectorActive || super.canRefresh());
    }

    @Override
    public void onTickModule() {
        pet.setEnabled(true);
        repair = needsRepairing();

        if (gateConfig.collectorActive && !super.canRefresh()) {
            return;
        }

        if (!isTheCorrectMap()) {
            goToTheWorkingMap();
            return;
        }

        if (findTarget()) {
            nextWaveCheck = System.currentTimeMillis() + 30000;
            this.currentStatus = State.ATTACKING;
            attacker.tryLockAndAttack();
            npcMove();
        } else if (npcs.isEmpty() && nextWaveCheck < System.currentTimeMillis()) {
            heroapi.setRoamMode();
            if (isCollecting()) {
                return;
            }

            if (gateConfig.roaming && (!movement.isMoving() || !canMove(movement.getDestination()))) {
                movement.moveRandom();
            }

            jumpToTheBestPortal();
        }
    }

    private void setup() {
        if (this.api == null || this.gateConfig == null) {
            return;
        }

        AuthAPI auth = this.api.requireAPI(AuthAPI.class);
        VerifierChecker.requireAuthenticity(auth);

        ExtensionsAPI extensionsAPI = api.requireAPI(ExtensionsAPI.class);
        FeatureInfo<?> feature = extensionsAPI.getFeatureInfo(this.getClass());
        Utils.discordCheck(feature, auth.getAuthId());
        Utils.showDonateDialog(feature, auth.getAuthId());
    }

    private boolean isTheCorrectMap() {
        if (!gateConfig.travelMap.active) {
            return true;
        }

        return portals.isEmpty() || heroapi.getMap() != null && heroapi.getMap().getId() == gateConfig.travelMap.map;
    }

    private void goToTheWorkingMap() {
        try {
            GameMap map = starSystem.getOrCreateMap(gateConfig.travelMap.map);
            if (map != null && !portals.isEmpty() && map != starSystem.getCurrentMap()) {
                this.bot.setModule(api.requireInstance(MapModule.class)).setTarget(map);
            }
        } catch (Exception e) {
            System.out.println("Map not found" + e.getMessage());
        }
    }

    private void jumpToTheBestPortal() {
        if (!gateConfig.travelToNextMap || portals.isEmpty()) {
            return;
        }

        Portal portal = portals.stream().filter(p -> p.getTypeId() != 1).findFirst().orElse(null);

        if (portal == null) {
            portal = portals.stream().filter(p -> p.getTypeId() == 1).findFirst().orElse(null);
        }

        if (portal != null) {
            if (heroapi.distanceTo(portal) < 200) {
                movement.jumpPortal(portal);
            } else {
                movement.moveTo(portal);
            }
        }
    }

    private boolean isCollecting() {
        if (gateConfig.collectorActive) {
            super.findBox();
            if (super.currentBox != null) {
                super.tryCollectNearestBox();
                return true;
            }
        }
        return false;
    }

    private boolean needsRepairing() {
        return heroapi.getHealth().hpPercent() < repairHpRange.getValue().getMin();
    }

    private boolean findTarget() {
        if (!npcs.isEmpty()) {
            if (!gateConfig.alwaysTheClosestNPC && !allLowLifeOrISH(true)) {
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

    private boolean isLowHealh(Npc n) {
        return n.getHealth().hpPercent() < 0.25;
    }

    private boolean shouldKill(Npc n) {
        return (gateConfig.attackAllNpcs || n.getInfo().getShouldKill()) && !n.isBlacklisted()
                && !n.hasEffect(EntityEffect.ISH) &&
                !n.hasEffect(EntityEffect.NPC_ISH);
    }

    private Npc bestNpc() {
        return this.npcs.stream()
                .filter(n -> shouldKill(n) && n.getHealth().hpPercent() > 0.25)
                .min(Comparator.<Npc>comparingDouble(n -> (n.getInfo().getPriority()))
                        .thenComparing(n -> (n.getLocationInfo().getCurrent().distanceTo(heroapi))))
                .orElse(null);
    }

    private Npc closestNpc() {
        return this.npcs.stream()
                .filter(this::shouldKill)
                .min(Comparator.<Npc>comparingDouble(n -> n.getLocationInfo().getCurrent().distanceTo(heroapi))
                        .thenComparing(n -> n.getInfo().getPriority())
                        .thenComparing(n -> n.getHealth().hpPercent()))
                .orElse(null);
    }

    private double getRadius(Lockable target) {
        if (repair && gateConfig.repairLogic) {
            return 1500;
        }

        if (!(target instanceof Npc)) {
            return gateConfig.radioMin;
        }

        double npcRadius = ((Npc) target).getInfo().getRadius();

        if (npcRadius < gateConfig.radioMin) {
            npcRadius = gateConfig.radioMin;
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

        if (repair && gateConfig.useRepairConfigWhenNeedRepair) {
            heroapi.setMode(this.repairConfig.getValue());
        } else {
            heroapi.setAttackMode((Npc) target);
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

        while (!canMove(direction) && distance < 10000) {
            direction.toAngle(targetLoc, angle += backwards ? -0.3 : 0.3, distance += 2);
        }

        if (distance >= 10000) {
            direction.toAngle(targetLoc, angle, 500);
        }

        movement.moveTo(direction);
    }

    private boolean canMove(Location direction) {
        Npc npc = this.npcs.stream()
                .filter(n -> n.getInfo().hasExtraFlag(ExtraNpcFlagsEnum.MOVE_AWAY))
                .min(Comparator.<Npc>comparingDouble(n -> n.getLocationInfo().getCurrent().distanceTo(direction)))
                .orElse(null);

        if (npc != null && npc.getLocationInfo().getCurrent().distanceTo(direction) < gateConfig.awayDistance) {
            return false;
        }

        return movement.canMove(direction);
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
        return (movement.canMove(loc) ? 0 : -1000) - npcs.stream()
                .filter(n -> attacker.getTarget() != n)
                .mapToDouble(n -> Math.max(0, n.getInfo().getRadius() - n.distanceTo(loc)))
                .sum();
    }
}
