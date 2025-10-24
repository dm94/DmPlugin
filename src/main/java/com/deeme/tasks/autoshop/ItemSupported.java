package com.deeme.tasks.autoshop;

import eu.darkbot.api.config.annotations.Configuration;

@Configuration("supported_item")
public enum ItemSupported implements ShopItem {
    LCB_10("ammunition_laser_lcb-10", 10, 0),
    MCB_25("ammunition_laser_mcb-25", 0, 0.5),
    MCB_50("ammunition_laser_mcb-50", 0, 1),
    RB_214("ammunition_laser_rb-214", 0, 5),
    RSB_75("ammunition_laser_rsb-75", 0, 5),
    JOB_100("ammunition_laser_job-100", 0, 0.5),
    CBO_100("ammunition_laser_cbo-100", 0, 5),
    PIB_100("ammunition_laser_pib-100", 0, 9),
    SAB_50("ammunition_laser_sab-50", 0, 1),
    PLT_2026("ammunition_rocket_plt-2026", 500, 0),
    PLT_3030("ammunition_rocket_plt-3030", 0, 7),
    R_310("ammunition_rocket_r-310", 100, 0),
    PLT_2021("ammunition_rocket_plt-2021", 0, 5),
    DCR_250("ammunition_specialammo_dcr-250", 0, 500),
    PLD_8("ammunition_specialammo_pld-8", 0, 100),
    EMP_01("ammunition_specialammo_emp-01", 0, 500),
    SP_100X("ammunition_specialammo_sp-100x", 0, 250),
    SAR_02("ammunition_rocketlauncher_sar-02", 0, 20),
    SAR_01("ammunition_rocketlauncher_sar-01", 2000, 0),
    ECO_10("ammunition_rocketlauncher_eco-10", 1500, 0),
    UBR_100("ammunition_rocketlauncher_ubr-100", 0, 30),
    HSTRM_01("ammunition_rocketlauncher_hstrm-01", 0, 25),
    CBR("ammunition_rocketlauncher_cbr", 0, 40),
    IM_01("ammunition_mine_im-01", 0, 54),
    DD_M01("ammunition_mine_ddm-01", 0, 150),
    SAB_M01("ammunition_mine_sabm-01", 0, 150),
    PEM_M01("ammunition_mine_empm-01", 0, 150),
    ACM_1("ammunition_mine_acm-01", 0, 100),
    SL_M01("ammunition_mine_slm-01", 0, 50),
    ANTI_Z1("equipment_extra_cpu_anti-z1", "special", 0, 108),
    ANTI_Z1_XL("equipment_extra_cpu_anti-z1-xl", "special", 0, 972),
    CL04K_MOD("equipment_extra_cpu_cl04k-xs", 0, 500),
    CLO4K_CPU("equipment_extra_cpu_cl04k-m", 0, 5000),
    CLO4K_XL_CPU("equipment_extra_cpu_cl04k-xl", 0, 22500),
    CPU_JP01("equipment_extra_cpu_jp-01", 150000, 0),
    CPU_JP02("equipment_extra_cpu_jp-02", 0, 15000),
    BK_100("resource_booty-key", "special", 0, 1500),
    H_HFC("resource_high-frequency-cable", "special", 0, 5000),
    H_HP("resource_hybrid-processor", "special", 0, 40000),
    LOGFILE("resource_logfile", "special", 0, 300),
    H_MT("resource_micro-transistors", "special", 0, 40000),
    H_NCA("resource_nano-case", "special", 0, 5000),
    H_NCO("resource_nano-condenser", "special", 0, 20000),
    H_PS("resource_prismatic-socket", "special", 0, 20000),
    PET_FUEL("resource_pet-fuel", "petGear", 0, 0.25),
    EP_B01("1", "booster", 0, 10000),
    HON_B01("2", "booster", 0, 10000),
    DMG_B01("3", "booster", 0, 10000),
    SHD_B01("4", "booster", 0, 10000),
    REP_B01("5", "booster", 0, 10000),
    SREG_B01("6", "booster", 0, 10000),
    RES_B01("7", "booster", 0, 10000),
    HP_B01("8", "booster", 0, 10000),
    EP_B02("11", "booster", 0, 10000),
    HON_B02("12", "booster", 0, 10000),
    DMG_B02("13", "booster", 0, 10000),
    SHD_B02("14", "booster", 0, 10000),
    REP_B02("15", "booster", 0, 10000),
    SREG_B02("16", "booster", 0, 10000),
    RES_B02("17", "booster", 0, 10000),
    HP_B02("18", "booster", 0, 10000),
    CD_B01("21", "booster", 0, 10000),
    CD_B02("22", "booster", 0, 10000),
    PRC_B00("34", "booster", 0, 10000),
    DMG_H01("45", "booster", 0, 10000),
    DMG_PVP01("46", "booster", 0, 10000);

    private final String id;
    private final String category;
    private final double creditsPrice;
    private final double uridiumPrice;

    ItemSupported(String id, String category, double creditsPrice, double uridiumPrice) {
        this.id = id;
        this.creditsPrice = creditsPrice;
        this.uridiumPrice = uridiumPrice;
        this.category = category;
    }

    ItemSupported(String id, double creditsPrice, double uridiumPrice) {
        this.id = id;
        this.creditsPrice = creditsPrice;
        this.uridiumPrice = uridiumPrice;

        if (id.contains("ammunition_laser_")) {
            this.category = "battery";
        } else if (id.contains("equipment_extra_cpu_")) {
            this.category = "special";
        } else {
            this.category = "rocket";
        }
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public double getCreditsPrice() {
        return creditsPrice;
    }

    public double getUridiumPrice() {
        return uridiumPrice;
    }

    @Override
    public String getItemId() {
        return getId();
    }

    @Override
    public String getDisplayName() {
        return name();
    }
}