package com.mojang.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Http {
    private static final Logger LOGGER = LogManager.getLogger();

    private Http() {
    }

    public static String buildQuery(Map<String, Object> query) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> entry : query.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            try {
                builder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            }
            catch (UnsupportedEncodingException e) {
                LOGGER.error("Unexpected exception building query", (Throwable)e);
            }
            if (entry.getValue() == null) continue;
            builder.append('=');
            try {
                builder.append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
            }
            catch (UnsupportedEncodingException e) {
                LOGGER.error("Unexpected exception building query", (Throwable)e);
            }
        }
        return builder.toString();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static String performGet(URL url, Proxy proxy) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)url.openConnection(proxy);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setRequestMethod("GET");
        InputStream inputStream = connection.getInputStream();
        try {
            String string = IOUtils.toString(inputStream);
            return string;
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
}

