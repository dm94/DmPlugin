package com.deeme.modules.astral;

public class AstralShip {

    private Integer maxModules = 3;
    private Integer maxWeapons = 15;
    private Integer maxGenerators = 15;

    private Integer modules = 0;
    private Integer weapons = 12;
    private Integer generators = 11;

    private String shipType = "";
    private boolean valid = false;

    public AstralShip(String shipType) {
        if (shipType.equals("ship_sentinel")) {
            this.shipType = "Sentinel";
            this.maxWeapons = 15;
            this.maxGenerators = 15;
            this.valid = true;
        } else if (shipType.equals("ship_diminisher")) {
            this.shipType = "Diminisher";
            this.maxWeapons = 15;
            this.maxGenerators = 15;
            this.valid = true;
        } else if (shipType.equals("ship_zephyr")) {
            this.shipType = "Zephir";
            this.maxWeapons = 12;
            this.maxGenerators = 16;
            this.valid = true;
        } else if (shipType.equals("ship_pusat")) {
            this.shipType = "Pusat";
            this.maxWeapons = 12;
            this.maxGenerators = 11;
            this.valid = true;
        } else {
            this.shipType = shipType;
        }
    }

    public String getStatus() {
        return getShipType() + " | M:" + getModules() + "/" + getMaxModules() + " | W:" + getWeapons() + "/"
                + getMaxWeapons() + " | G: " + getGenerators() + "/" + getMaxGenerators();
    }

    public boolean isValid() {
        return valid;
    }

    public String getShipType() {
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
