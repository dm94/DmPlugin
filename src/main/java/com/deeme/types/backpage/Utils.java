package com.deeme.types.backpage;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Utils {

    public static void sendMessage(String message, String url) {
        if (message == null || message.isEmpty() || url == null || url.isEmpty()) {
            return;
        }

        try {
            HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");

            OutputStream os = conn.getOutputStream();
            os.write(message.getBytes(StandardCharsets.UTF_8));
            os.close();
            conn.getInputStream();
            conn.disconnect();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
