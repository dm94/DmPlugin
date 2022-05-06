package com.deeme.tasks;

import com.deeme.types.VerifierChecker;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.backpage.BackpageManager;
import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.core.itf.Configurable;
import com.github.manolo8.darkbot.core.itf.Task;
import com.github.manolo8.darkbot.extensions.features.Feature;
import com.github.manolo8.darkbot.utils.http.Method;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

@Feature(name = "Skylab", description = "Control the skylab")
public class Skylab implements Task,Configurable<Skylab.SkylabConfig> {
    private long deliveryTime = 0;
    private Main main;
    private BackpageManager backpageManager;
    private SkylabConfig config;
    private long nextCheck = 0;
    private boolean waitingTransport = true;

    @Override
    public void install(Main main) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) return;
        VerifierChecker.checkAuthenticity();
        this.main = main;
        this.backpageManager = main.backpage;
    }

    @Override
    public void tick() {
        try {
            sendResources();
        } catch (Exception e) {
            System.err.println("SKYLAB SENDER: Unable to send for values from BackPage (SID KO)");
            e.printStackTrace();
        }
    }


    @Override
    public void setConfig(SkylabConfig skylabConfig) {
        this.config = skylabConfig;
    }

    public static class SkylabConfig {
        @Option(value = "Seprom To Send", description = "Amount of seprom to send")
        @Num(max = 10000, step = 10)
        public int sepromToSend = 0;

        @Option(value = "Promerium To Send", description = "Amount of promerium to send")
        @Num(max = 10000, step = 10)
        public int promeriumToSend = 0;

        @Option(value = "Premium", description = "Check here if you are premium")
        public boolean premium = false;

        @Option(value = "Force check", description = "It will check every 10 minutes whether it can send cargo or not.")
        public boolean forceCheck = false;

    }

    private int sendResources() {
        String sid = this.main.statsManager.sid;
        String instance = this.main.statsManager.instance;
        if (sid == null || instance == null || sid.isEmpty() || instance.isEmpty())
            return 1;

        if (this.config.sepromToSend == 0 && this.config.promeriumToSend == 0)
            return 1;

        int cargo = this.main.statsManager.getMaxCargo() - this.main.statsManager.getCargo() + 50;
        int seprom = 0;
        int promerium = 0;

        if (this.config.sepromToSend + this.config.promeriumToSend > cargo) {
            if (this.config.sepromToSend < cargo) {
                seprom = this.config.sepromToSend;
                cargo -= this.config.sepromToSend;
            }
            if (this.config.promeriumToSend > 0)
                promerium = cargo;
        } else {
            seprom = this.config.sepromToSend;
            promerium = this.config.promeriumToSend;
        }
        if (seprom == 0 && promerium == 0) {
            return 1;
        }
        if (cargo > 50 && (this.deliveryTime <= System.currentTimeMillis() || this.config.forceCheck) && (this.nextCheck <= System.currentTimeMillis() || !this.waitingTransport)) {
            try {
                this.nextCheck = System.currentTimeMillis() + 600000L;
                if (this.waitingTransport) {
                    this.waitingTransport = this.backpageManager.getConnection("indexInternal.es?action=internalSkylab", Method.GET).consumeInputStream(this::checkTransport);
                    if (this.waitingTransport) {
                        System.out.println("SKYLAB SENDER: Waiting Transport");
                        return 1;
                    }
                }
                String token = this.backpageManager.getConnection("indexInternal.es", Method.GET)
                        .setRawParam("action", "internalSkylab")
                        .consumeInputStream(backpageManager::getReloadToken);

                if (token.isEmpty()) {
                    System.out.println("SKYLAB SENDER: No reload Token");
                    this.deliveryTime = System.currentTimeMillis() + 60000L; // wait 1 minute to check for reload token again
                } else {
                    backpageManager.getConnection("indexInternal.es", Method.POST)
                            .setRawParam("reloadToken", token)
                            .setRawParam("action", "internalSkylab")
                            .setRawParam("subaction", "startTransport")
                            .setRawParam("mode", "normal")
                            .setRawParam("construction", "TRANSPORT_MODULE")
                            .setRawParam("count_prometium", "0")
                            .setRawParam("count_endurium", "0")
                            .setRawParam("count_terbium", "0")
                            .setRawParam("count_prometid", "0")
                            .setRawParam("count_duranium", "0")
                            .setRawParam("count_xenomit", "0")
                            .setRawParam("count_promerium", String.valueOf(promerium))
                            .setRawParam("count_seprom", String.valueOf(seprom))
                            .getContent();

                    System.out.println("SKYLAB SENDER: Cargo Sent");
                    if (this.config.premium) {
                        this.deliveryTime = System.currentTimeMillis() + ((promerium + seprom) / 1000 * 3600000L);
                    } else {
                        this.deliveryTime = System.currentTimeMillis() + ((promerium + seprom) / 500 * 3600000L);
                    }
                    this.waitingTransport = true;
                }
                return cargo;
            } catch (Exception e) {
                System.out.println(e.getLocalizedMessage());
                return 1;
            }
        }
        return 1;
    }

    private boolean checkTransport(InputStream input) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(input))) {
            String currentLine;
            while ((currentLine = in.readLine()) != null) {
                if (currentLine.contains("progress_timer_transportModule")) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
        return false;
    }
}
