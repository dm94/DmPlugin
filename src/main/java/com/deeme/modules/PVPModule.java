package com.deeme.modules;

import com.deeme.types.ShipAttacker;
import com.deeme.types.VerifierChecker;
import com.deeme.types.config.PVPConfig;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.types.ShipMode;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Module;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.game.entities.Ship;
import eu.darkbot.api.game.items.SelectableItem.Special;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.ConfigAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.utils.Inject;
import eu.darkbot.shared.modules.CollectorModule;
import eu.darkbot.shared.utils.SafetyFinder;

import java.util.Arrays;
@Feature(name = "PVP Module", description = "It is limited so as not to spoil the game")
public class PVPModule implements Module, Configurable<PVPConfig> {
    private PVPConfig pvpConfig;
    public Ship target;
    private ShipAttacker shipAttacker;

    protected final PluginAPI api;
    protected final HeroAPI heroapi;
    protected final ConfigSetting<ShipMode> configOffensive;
    protected final ConfigSetting<ShipMode> configRun;

    private boolean attackConfigLost = false;
    protected boolean firstAttack;
    protected long isAttacking;
    protected int fixedTimes;
    protected Character lastShot;
    protected long laserTime;
    protected long fixTimes;
    protected long clickDelay;

    private SafetyFinder safety;
    private double lastDistanceTarget = 1000;
    protected CollectorModule collectorModule;

    public PVPModule(PluginAPI api) {
        this(api, api.requireAPI(HeroAPI.class),
                api.requireAPI(AuthAPI.class),
                api.requireAPI(ConfigAPI.class),
                api.requireInstance(SafetyFinder.class));
    }

    @Inject
    public PVPModule(PluginAPI api, HeroAPI hero, AuthAPI auth, ConfigAPI configApi, SafetyFinder safety) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) throw new SecurityException();
        VerifierChecker.checkAuthenticity(auth);

        this.api = api;
        this.heroapi = hero;
        this.safety = safety;
        this.configOffensive = configApi.requireConfig("general.offensive");
        this.configRun = configApi.requireConfig("general.run");
        this.collectorModule = new CollectorModule(api);
        setup();
    }

    @Override
    public String getStatus() {
        return safety.state() != SafetyFinder.Escaping.NONE ? safety.status() : target != null ? "Attacking an enemy" : collectorModule.getStatus() ;
    }
    
    @Override
    public void setConfig(ConfigSetting<PVPConfig> arg0) {
        this.pvpConfig = arg0.getValue();
        setup();
    }

    @Override
    public boolean canRefresh() {
        if (pvpConfig.move && target == null && collectorModule.canRefresh()) {
            return safety.tick();
        }
        return false;
    }

    private void setup() {
        if (api == null || pvpConfig == null) return;

        this.shipAttacker = new ShipAttacker(api, pvpConfig.SAB, pvpConfig.useRSB);
    }

    @Override
    public void onTickModule() {
        if (!pvpConfig.move || safety.tick()) {
            getTarget();
            if (target == null || !target.isValid()) {
                attackConfigLost = false;
                target = null;
                shipAttacker.resetDefenseData();
                if (pvpConfig.move) {
                    if (pvpConfig.changeConfig) {
                        heroapi.setRoamMode();
                    }
                    collectorModule.onTickModule();
                }
                return;
            }
            shipAttacker.setTarget(target);

            if (pvpConfig.changeConfig) {
                setConfigToUse();
            }

            shipAttacker.doKillTargetTick();

            if (pvpConfig.useBestRocket) {
                shipAttacker.changeRocket();
            }

            if (heroapi.getLocationInfo().distanceTo(target) < 575) {
                shipAttacker.useKeyWithConditions(pvpConfig.ability, null);
            }

            shipAttacker.useKeyWithConditions(pvpConfig.ISH, Special.ISH_01);
            shipAttacker.useKeyWithConditions(pvpConfig.SMB, Special.SMB_01);
            shipAttacker.useKeyWithConditions(pvpConfig.PEM, Special.EMP_01);
            shipAttacker.useKeyWithConditions(pvpConfig.otherKey, null);

            shipAttacker.tryAttackOrFix();

            if (pvpConfig.move) {
                shipAttacker.vsMove();
            }
        }
    }

    private boolean getTarget() {
        if (target != null && target.isValid()) return true;

        target = shipAttacker.getEnemy(pvpConfig.rangeForEnemies);

        return target != null;
    }

    private void setConfigToUse() {
        if (attackConfigLost || heroapi.getHealth().shieldPercent() < 0.1 && heroapi.getHealth().hpPercent() < 0.3) {
            attackConfigLost = true;
            shipAttacker.setMode(configRun.getValue(), pvpConfig.useBestFormation);
        } else if (pvpConfig.useRunConfig && target != null) {
            double distance = heroapi.getLocationInfo().distanceTo(target);
            if (distance > 400 && distance > lastDistanceTarget && target.getSpeed() > heroapi.getSpeed()) {
                shipAttacker.setMode(configRun.getValue(), pvpConfig.useBestFormation);
                lastDistanceTarget = distance;
            } else {
                shipAttacker.setMode(configOffensive.getValue(), pvpConfig.useBestFormation);
            }
        } else {
            shipAttacker.setMode(configOffensive.getValue(), pvpConfig.useBestFormation);
        }
    }
}
