package com.deeme.modules;

import com.deeme.types.VerifierChecker;
import com.deeme.types.gui.AdvertisingMessage;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.config.types.Editor;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.config.types.Options;
import com.github.manolo8.darkbot.config.types.suppliers.OptionList;
import com.github.manolo8.darkbot.core.entities.Npc;
import com.github.manolo8.darkbot.core.itf.Configurable;
import com.github.manolo8.darkbot.core.itf.InstructionProvider;
import com.github.manolo8.darkbot.core.itf.Module;
import com.github.manolo8.darkbot.core.manager.HeroManager;
import com.github.manolo8.darkbot.core.manager.StarManager;
import com.github.manolo8.darkbot.core.utils.Drive;
import com.github.manolo8.darkbot.core.utils.Location;
import com.github.manolo8.darkbot.extensions.features.Feature;
import com.github.manolo8.darkbot.gui.tree.components.JListField;
import com.github.manolo8.darkbot.gui.tree.components.JShipConfigField;
import com.github.manolo8.darkbot.modules.CollectorModule;
import com.github.manolo8.darkbot.modules.MapModule;
import com.github.manolo8.darkbot.modules.utils.NpcAttacker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.lang.Double.max;
import static java.lang.Double.min;

@Feature(name = "GGModule", description = "Repairs, makes the vast majority of GGs, npcs sends them to the corner, collect materials")
public class GGModule extends CollectorModule implements Module, Configurable<GGModule.GGConfig>, InstructionProvider {

    private Main main;
    private Config config;
    private List<Npc> npcs;
    private HeroManager hero;
    private Drive drive;
    private int radiusFix;
    private GGConfig ggConfig;
    private boolean repairing;
    private int rangeNPCFix = 0;
    private long lastCheck = System.currentTimeMillis();
    private int lasNpcHealth = 0;
    private int lasPlayerHealth = 0;
    private NpcAttacker attack;

    @Override
    public void setConfig(GGConfig ggConfig) {
        this.ggConfig = ggConfig;
    }

    @Override
    public String instructions() {
        return  "GGModule: \n"+
                "Dynamic Range: The ship is approaching and moving away depending on the NPC. If you know which ranges to use it is better not to activate it.  \n" +
                "To make trilogy (Alpha, Beta, Gamma) put that makes Gamma. \n" +
                "It is recommended to put a % of repair of 70% or more \n " +
                "If you don't want to get an advertising link, become a donor and you won't have to open it.";
    }

    public static class GGConfig {
        @Option(value = "Honor config", description = "Used on finish wave")
        @Editor(JShipConfigField.class)
        public Config.ShipConfig Honor = new Config.ShipConfig();

        @Option("GG Gate - Chosse GG Gamma to make ABG")
        @Editor(JListField.class)
        @Options(GGList.class)
        public int idGate = 51;

        @Option("Take materials")
        public boolean takeBoxes = true;

        @Option("Send NPCs to corner")
        public boolean sendNPCsCorner = true;

        @Option(value = "Dynamic Range", description = "Automatically changes the range")
        public boolean useDynamicRange = false;
    }

    @Override
    public void install(Main main) {
        super.install(main);
        this.main = main;
        this.config = main.config;
        this.attack = new NpcAttacker(main);
        this.hero = main.hero;
        this.drive = main.hero.drive;
        this.npcs = main.mapManager.entities.npcs;

        if (!AdvertisingMessage.hasAccepted) {
            if (!main.hero.map.gg) {
                AdvertisingMessage.showAdverMessage();
            }
        }
        if (!main.hero.map.gg) {
            AdvertisingMessage.newUpdateMessage(main.featureRegistry.getFeatureDefinition(this));
        }
    }

    public static class GGList extends OptionList<Integer> {
        private static final StarManager starManager = new StarManager();

        @Override
        public Integer getValue(String text) {
            return starManager.byName(text).id;
        }

        @Override
        public String getText(Integer value) {
            return starManager.byId(value).name;
        }

        @Override
        public List<String> getOptions() {
            return new ArrayList<>(starManager.getGGMaps());
        }
    }

    @Override
    public boolean canRefresh() {
        return !main.hero.map.gg;
    }

    @Override
    public String status() {
        return !AdvertisingMessage.hasAccepted ? "You haven't opened the link" : ((repairing ? "Repairing" :
                attack.hasTarget() ? attack.status() : "Roaming") + " | NPCs: "+this.npcs.size());
    }

    @Override
    public void tick() {
        if (!AdvertisingMessage.hasAccepted && !main.hero.map.gg) {
            return;
        }

        if (super.isNotWaiting() && main.hero.map.gg) {
            main.guiManager.pet.setEnabled(true);
            if (findTarget()) {
                hero.attackMode();
                attack.doKillTargetTick();
                removeIncorrectTarget();
                moveLogic();
            } else {
                hero.setMode(ggConfig.Honor);
                if (ggConfig.takeBoxes && super.isNotWaiting()) {
                    findBox();
                    tryCollectNearestBox();
                }
                if (!drive.isMoving() || drive.isOutOfMap()){
                    if (hero.health.hpPercent() >= config.GENERAL.SAFETY.REPAIR_HP_RANGE.min) {
                        repairing = false;
                        if (!main.mapManager.entities.portals.isEmpty()) {
                            this.main.setModule(new MapModule()).setTarget(main.starManager.byId(main.mapManager.entities.portals.get(0).id));
                        }
                        return;
                    } else {
                        repairing = true;
                    }
                    drive.move(main.mapManager.boundMaxX/2,main.mapManager.boundMaxY/2);
                }
            }
        } else if ( main.hero.map.id == 1 || main.hero.map.id == 5 || main.hero.map.id == 9) {
            if (ggConfig.idGate == 73 || ggConfig.idGate == 72){ ggConfig.idGate = 71; }
            hero.roamMode();
            for (int i=0; i < main.mapManager.entities.portals.size();i++){
                if (main.mapManager.entities.portals.get(i).target.id == ggConfig.idGate ||
                        (ggConfig.idGate == 53 && (main.mapManager.entities.portals.get(i).target.id == 52 ||
                                main.mapManager.entities.portals.get(i).target.id == 51))) {
                    this.main.setModule(new MapModule()).setTarget(main.mapManager.entities.portals.get(i).target);
                }
            }
        } else {
            hero.roamMode();
            this.main.setModule(new MapModule()).setTarget(this.main.starManager.byId(ggConfig.idGate));
        }
    }

    private boolean findTarget() {
        if (attack.target == null || attack.target.removed) {
            if (!npcs.isEmpty()) {
                if (ggConfig.sendNPCsCorner && !allLowLifeOrISH(true)) {
                    attack.target = bestNpc(hero.locationInfo.now);
                } else {
                    attack.target = closestNpc(hero.locationInfo.now);
                }
            } else {
                resetTarget();
            }
        } else if (attack.target.health.hpPercent() < 0.25 && !allLowLifeOrISH(true)) {
            resetTarget();
        }
        return attack.target != null;
    }

    private void removeIncorrectTarget() {
        if (attack.target.ish){
            resetTarget();
        } else if (ggConfig.sendNPCsCorner && main.mapManager.isTarget(attack.target) &&
                isLowHealh(attack.target) && !allLowLifeOrISH(true)) {
            resetTarget();
        }
    }

    private void resetTarget() {
        attack.target = null;
        lasNpcHealth = 0;
    }

    private boolean isLowHealh(Npc npc){
        return npc.health.hpPercent() < 0.25;
    }

    private boolean allLowLifeOrISH(boolean countISH){
        int npcsLowLife = 0;

        for(Npc n:npcs){
            if (countISH && n.ish) { return true; }
            if (isLowHealh(n)) { npcsLowLife++; }
        }

        return npcsLowLife >= npcs.size();
    }

    private void moveLogic() {
        Npc target = attack.target;

        if (target == null || target.locationInfo == null) return;

        Location heroLoc = hero.locationInfo.now;
        Location targetLoc = target.locationInfo.destinationInTime(400);

        double angle = targetLoc.angle(heroLoc), distance = heroLoc.distance(targetLoc), radius = target.npcInfo.radius;

        if (ggConfig.useDynamicRange) {
            dynamicNPCRange(distance);
        } else {
            rangeNPCFix = 0;
        }

        if (hero.health.hpPercent() <= config.GENERAL.SAFETY.REPAIR_HP_RANGE.min){
            rangeNPCFix = 1000;
            repairing = true;

            if (allLowLifeOrISH(false)) {
                drive.move(main.mapManager.boundMaxX/2,main.mapManager.boundMaxY/2);
                return;
            }
        } else if  (hero.health.hpPercent() >= config.GENERAL.SAFETY.REPAIR_HP_RANGE.max){
            repairing = false;
        }

        if (allLowLifeOrISH(false) && lasPlayerHealth >= hero.health.hp && attack.target.health.hp < lasNpcHealth) {
            lasPlayerHealth = hero.health.hp;
            lasNpcHealth = attack.target.health.hp;
            return;
        }

        radius += rangeNPCFix;

        if (distance > radius) {
            if (ggConfig.takeBoxes && super.isNotWaiting()) {
                findBox();
                if (tryCollectNearestBox()) {
                    return;
                }
            }
            radiusFix -= (distance - radius) / 2;
            radiusFix = (int) max(radiusFix, -target.npcInfo.radius / 2);
        } else {
            radiusFix += (radius - distance) / 6;
            radiusFix = (int) min(radiusFix, target.npcInfo.radius / 2);
        }

        distance = (radius += radiusFix);
        angle += Math.max((hero.shipInfo.speed * 0.625) + (min(200, target.locationInfo.speed) * 0.625)
                - heroLoc.distance(Location.of(targetLoc, angle, radius)), 0) / radius;

        Location direction = Location.of(targetLoc, angle, distance);
        while (!drive.canMove(direction) && distance < 10000)
            direction.toAngle(targetLoc, angle += 0.3, distance += 2);
        if (distance >= 10000) direction.toAngle(targetLoc, angle, 500);

        drive.move(direction);
    }

    private void dynamicNPCRange(double distance){
        if (lastCheck <= System.currentTimeMillis()-30000 && distance <= 1000) {
            if (lasPlayerHealth > hero.health.hp && rangeNPCFix < 500) {
                rangeNPCFix += 50;
            } else if (lasNpcHealth == attack.target.health.hp) {
                rangeNPCFix -= 50;
            }
            lasPlayerHealth =  hero.health.hp;
            lasNpcHealth = attack.target.health.hp;
            lastCheck = System.currentTimeMillis();
        }
    }

    private Npc closestNpc(Location location) {
        return this.npcs.stream()
                .filter(n -> (!n.ish))
                .min(Comparator.<Npc>comparingDouble(n -> n.locationInfo.now.distance(location))
                        .thenComparing(n -> n.npcInfo.priority)
                        .thenComparing(n -> n.health.hpPercent())).orElse(null);
    }

    private Npc bestNpc(Location location) {
        return this.npcs.stream()
                .filter(n -> (!n.ish && n.health.hpPercent() > 0.25))
                .min(Comparator.<Npc>comparingDouble(n -> (n.npcInfo.priority))
                        .thenComparing(n -> (n.locationInfo.now.distance(location)))).orElse(null);
    }

}

