package com.deeme.modules.astral;

public class AstralShip {

    private Integer maxModules = 3;
    private Integer maxWeapons = 15;
    private Integer maxGenerators = 15;

    private Integer modules = 0;
    private Integer weapons = 12;
    private Integer generators = 11;

    private ShipType shipType = null;

    public enum ShipType {
        SENTINEL,
        DIMINISHER,
        ZEPHIR,
        PUSAT;
    }

    public AstralShip(String shipType) {
        if (shipType.equals("ship_sentinel")) {
            this.shipType = ShipType.SENTINEL;
            this.maxWeapons = 15;
            this.maxGenerators = 15;
        } else if (shipType.equals("ship_diminisher")) {
            this.shipType = ShipType.DIMINISHER;
            this.maxWeapons = 15;
            this.maxGenerators = 15;
        } else if (shipType.equals("ship_zephyr")) {
            this.shipType = ShipType.ZEPHIR;
            this.maxWeapons = 12;
            this.maxGenerators = 16;
        } else if (shipType.equals("ship_pusat")) {
            this.shipType = ShipType.PUSAT;
            this.maxWeapons = 12;
            this.maxGenerators = 11;
        }
    }

    public String getStatus() {
        return getShipType() + " | M:" + getModules() + "/" + getMaxModules() + " | W:" + getWeapons() + "/"
                + getMaxWeapons() + " | G: " + getGenerators() + "/" + getMaxGenerators();
    }

    public boolean isValid(String ship) {
        return this.shipType != null
                && (ship.equals("ship_sentinel") || ship.equals("ship_diminisher") || ship.equals("ship_zephyr")
                        || ship.equals("ship_pusat"));
    }

    public ShipType getShipType() {
        return shipType;
    }

    public Integer getGenerators() {
        return generators;
    }

    public Integer getMaxGenerators() {
        return maxGenerators;
    }

    public Integer getModules() {
        return modules;
    }

    public Integer getMaxModules() {
        return maxModules;
    }

    public Integer getWeapons() {
        return weapons;
    }

    public Integer getMaxWeapons() {
        return maxWeapons;
    }

    public void setGenerators(Integer generators) {
        this.generators = generators;
    }

    public void setModules(Integer modules) {
        this.modules = modules;
    }

    public void setWeapons(Integer weapons) {
        this.weapons = weapons;
    }
}
