package com.deeme.tasks;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.backpage.BackpageManager;
import com.github.manolo8.darkbot.utils.http.Method;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.InstructionProvider;
import eu.darkbot.api.extensions.Task;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BackpageAPI;
import eu.darkbot.api.managers.StatsAPI;
import eu.darkbot.api.utils.Inject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.JLabel;

@Feature(name = "Skylab", description = "Control the skylab")
public class SkylabSender implements Task, Configurable<SkylabSender.SkylabConfig>, InstructionProvider {
    protected final Main main;
    protected final BackpageManager backpageManager;

    protected final StatsAPI stats;
    protected final BackpageAPI backpage;

    private SkylabConfig config;
    private long nextCheck = 0;
    private boolean waitingTransport = true;

    private JLabel label = new JLabel("Loading");

    public SkylabSender(Main main, PluginAPI api) {
        this(main, api, api.requireAPI(AuthAPI.class));
    }

    @Inject
    public SkylabSender(Main main, PluginAPI api, AuthAPI auth) {
        if (!Arrays.equals(VerifierChecker.class.getSigners(), getClass().getSigners())) {
            throw new SecurityException();
        }
        VerifierChecker.checkAuthenticity(auth);

        Utils.showDonateDialog();

        this.main = main;
        this.backpageManager = main.backpage;
        this.stats = api.getAPI(StatsAPI.class);
        this.backpage = api.getAPI(BackpageAPI.class);
        this.nextCheck = 0;
    }

    @Override
    public void setConfig(ConfigSetting<SkylabConfig> arg0) {
        this.config = arg0.getValue();
    }

    @Override
    public void onTickTask() {
        if (this.nextCheck <= System.currentTimeMillis()) {
            this.nextCheck = System.currentTimeMillis() + (this.config.timeToCheck * 60000);
            tryToSend();
        }
    }

    @Override
    public JComponent beforeConfig() {
        return this.label;
    }

    @Configuration("skylab")
    public static class SkylabConfig {
        @Option("skylab.seprom_to_send")
        @Number(max = 100000, step = 10)
        public int sepromToSend = 0;

        @Option("skylab.promerium_to_send")
        @Number(max = 100000, step = 10)
        public int promeriumToSend = 0;

        @Option("general.next_check_time_minutes")
        @Number(max = 1440, step = 1)
        public int timeToCheck = 20;

    }

    private void changeStatus(String text) {
        this.label.setText("Status: " + text);
    }

    private boolean tryToSend() {
        if (!this.backpage.isInstanceValid() || !this.backpage.getSidStatus().contains("OK")) {
            changeStatus("Unable to send for values from BackPage (SID KO)");
            return false;
        }

        if (this.config.sepromToSend == 0 && this.config.promeriumToSend == 0) {
            changeStatus("Error nothing to send");
            return false;
        }

        try {
            if (this.waitingTransport) {
                this.waitingTransport = this.backpageManager
                        .getConnection("indexInternal.es?action=internalSkylab", Method.GET)
                        .consumeInputStream(this::checkTransport);
                if (this.waitingTransport) {
                    changeStatus("Waiting Transport");
                    return false;
                }
            }

            String token = getToken();
            if (!token.isEmpty()) {
                sendSkylabResources(token);
            }
            return true;
        } catch (Exception e) {
            changeStatus("Error when sending the transport");
        }

        return false;
    }

    private int getFreeCargo() {
        return this.stats.getMaxCargo() - this.stats.getCargo() + 50;
    }

    private String getToken() throws IOException, RuntimeException {
        return this.backpageManager.getConnection("indexInternal.es", Method.GET)
                .setRawParam("action", "internalSkylab")
                .consumeInputStream(backpageManager::getReloadToken);
    }

    private void sendSkylabResources(String token) throws IOException {
        int freeCargo = getFreeCargo();
        int seprom = 0;
        int promerium = 0;

        if (this.config.sepromToSend + this.config.promeriumToSend > freeCargo) {
            if (this.config.sepromToSend < freeCargo) {
                seprom = this.config.sepromToSend;
                freeCargo -= this.config.sepromToSend;
            }
            if (this.config.promeriumToSend > 0) {
                promerium = freeCargo;
            }
        } else {
            seprom = this.config.sepromToSend;
            promerium = this.config.promeriumToSend;
        }
        if (seprom == 0 && promerium == 0) {
            changeStatus("Error no space to send anything");
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
            changeStatus("Cargo Sent");
            this.waitingTransport = true;
        }
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
            changeStatus("Error when checking transport");
        }
        return false;
    }
}
