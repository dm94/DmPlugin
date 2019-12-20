package com.deeme.tasks;

import com.deeme.types.VerifierChecker;
import com.deeme.types.backpage.Utils;
import com.deeme.types.gui.AdvertisingMessage;
import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.config.types.Option;
import com.github.manolo8.darkbot.core.itf.Configurable;
import com.github.manolo8.darkbot.core.itf.Task;
import com.github.manolo8.darkbot.extensions.features.Feature;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.LinkedHashMap;

@Feature(name = "Skylab", description = "Control the skylab")
public class Skylab implements Task,Configurable<Skylab.SkylabConfig> {

    private long deliveryTime = 0;
    private Main main;
    private SkylabConfig skylabConfig;
    private long nextCheck = 0;
    private boolean waitingTransport = true;

    @Override
    public void install(Main main) {
        this.main = main;
        if (!AdvertisingMessage.hasAccepted) {
            AdvertisingMessage.hasAccepted = VerifierChecker.getAuthApi().isDonor();
            AdvertisingMessage.showAdverMessage();
        }
        if (!main.hero.map.gg) {
            AdvertisingMessage.newUpdateMessage(main.featureRegistry.getFeatureDefinition(this));
        }
    }

    @Override
    public void tick() {
        if (!AdvertisingMessage.hasAccepted) {
            main.featureRegistry.getFeatureDefinition(this).setStatus(false);
            main.pluginHandler.updatePluginsSync();
            return;
        }
        sendResources();
    }


    @Override
    public void setConfig(SkylabConfig skylabConfig) {
        this.skylabConfig = skylabConfig;
    }

    public static class SkylabConfig {
        @Option(value = "Seprom To Send", description = "Amount of seprom to send")
        @Num(max = 10000, step = 100)
        public int sepromToSend = 0;

        @Option(value = "Promerium To Send", description = "Amount of promerium to send")
        @Num(max = 10000, step = 100)
        public int promeriumToSend = 0;

        @Option(value = "Premium", description = "Check here if you are premium")
        public boolean premium = false;

    }

    private int sendResources() {
        String sid = main.statsManager.sid;
        String instance = main.statsManager.instance;
        if (sid == null || instance == null || sid.isEmpty() || instance.isEmpty()) {
            return 1;
        }

        int cargo = main.statsManager.depositTotal - (main.statsManager.deposit+100);
        if (cargo > 2000) { cargo = 2000; }

        int seprom = 0;
        int promerium = 0;

        if (skylabConfig.sepromToSend + skylabConfig.promeriumToSend > cargo) {
            if (skylabConfig.sepromToSend < cargo) {
                seprom = skylabConfig.sepromToSend;
                cargo = cargo - skylabConfig.sepromToSend;
            }
            if (skylabConfig.promeriumToSend > 0) {
                promerium = cargo;
            }
        } else {
            seprom = skylabConfig.sepromToSend;
            promerium = skylabConfig.promeriumToSend;
        }

        if (seprom == 0 && promerium == 0) {
            return 1;
        }

        if (cargo > 100 && this.deliveryTime <= System.currentTimeMillis() && (this.nextCheck <= System.currentTimeMillis() || !waitingTransport)) {
            try {
                nextCheck = System.currentTimeMillis() + 300000;
                if (waitingTransport) {
                    waitingTransport = checkTransport(main.backpage.getConnection("indexInternal.es?action=internalSkylab").getInputStream());
                    if (waitingTransport) {
                        System.out.println("Waiting Transport");
                    }
                    return 1;
                } else {
                    String token = main.backpage.getReloadToken(main.backpage.getConnection("indexInternal.es?action=internalSkylab").getInputStream());
                    if (token.isEmpty()) {
                        System.out.println("No reload Token");
                        this.deliveryTime = System.currentTimeMillis() + 60000;
                    } else {
                        HttpURLConnection conn = main.backpage.getConnection("indexInternal.es?action=internalSkylab",30000);
                        LinkedHashMap<String,Object> params = new LinkedHashMap<>();
                        params.put("reloadToken", token);
                        params.put("action", "internalSkylab");
                        params.put("subaction", "startTransport");
                        params.put("mode", "normal");
                        params.put("construction", "TRANSPORT_MODULE");
                        params.put("count_prometium", "0");
                        params.put("count_endurium", "0");
                        params.put("count_terbium", "0");
                        params.put("count_prometid", "0");
                        params.put("count_duranium", "0");
                        params.put("count_xenomit", "0");
                        params.put("count_promerium", String.valueOf(promerium));
                        params.put("count_seprom", String.valueOf(seprom));
                        StringBuilder postData = new StringBuilder();
                        for (java.util.Map.Entry<String,Object> param : params.entrySet()) {
                            if (postData.length() != 0) postData.append('&');
                            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                            postData.append('=');
                            postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
                        }
                        byte[] postDataBytes = postData.toString().getBytes();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                        conn.setDoOutput(true);
                        conn.getOutputStream().write(postDataBytes);
                        conn.getResponseCode();
                        conn.disconnect();

                        System.out.println("Cargo sended");
                        if (skylabConfig.premium) {
                            deliveryTime = System.currentTimeMillis() + ((cargo/1000) * 3600000);
                        } else {
                            deliveryTime = System.currentTimeMillis() + ((cargo/500) * 3600000);
                        }

                        waitingTransport = true;
                    }

                    return cargo;
                }
            } catch (Exception e){
                System.out.println(e.getLocalizedMessage());
                return 1;
            }
        }
        return 1;
    }

    private boolean checkTransport(InputStream input) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            String currentLine;

            while ((currentLine = in.readLine()) != null){
                if (currentLine.contains("progress_timer_transportModule")) {
                    in.close();
                    return true;
                }
            }
            in.close();
        } catch (Exception e){
            System.out.println(e.getLocalizedMessage());
        }

        return false;
    }

}
