package net.minecraft.hopper;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.hopper.PublishRequest;
import net.minecraft.hopper.PublishResponse;
import net.minecraft.hopper.Report;
import net.minecraft.hopper.Response;
import net.minecraft.hopper.SubmitRequest;
import net.minecraft.hopper.SubmitResponse;
import net.minecraft.hopper.Util;

public final class HopperService {
    private static final String BASE_URL = "http://hopper.minecraft.net/crashes/";
    private static final URL ROUTE_SUBMIT = Util.constantURL("http://hopper.minecraft.net/crashes/submit_report/");
    private static final URL ROUTE_PUBLISH = Util.constantURL("http://hopper.minecraft.net/crashes/publish_report/");
    private static final String[] INTERESTING_SYSTEM_PROPERTY_KEYS = new String[]{"os.version", "os.name", "os.arch", "java.version", "java.vendor", "sun.arch.data.model"};
    private static final Gson GSON = new Gson();

    public static SubmitResponse submitReport(Proxy proxy, String report, String product, String version) throws IOException {
        return HopperService.submitReport(proxy, report, product, version, null);
    }

    public static SubmitResponse submitReport(Proxy proxy, String report, String product, String version, Map<String, String> env) throws IOException {
        HashMap<String, String> environment = new HashMap<String, String>();
        if (env != null) {
            environment.putAll(env);
        }
        for (String key : INTERESTING_SYSTEM_PROPERTY_KEYS) {
            String value = System.getProperty(key);
            if (value == null) continue;
            environment.put(key, value);
        }
        SubmitRequest request = new SubmitRequest(report, product, version, environment);
        return HopperService.makeRequest(proxy, ROUTE_SUBMIT, request, SubmitResponse.class);
    }

    public static PublishResponse publishReport(Proxy proxy, Report report) throws IOException {
        PublishRequest request = new PublishRequest(report);
        return HopperService.makeRequest(proxy, ROUTE_PUBLISH, request, PublishResponse.class);
    }

    private static <T extends Response> T makeRequest(Proxy proxy, URL url, Object input, Class<T> classOfT) throws IOException {
        String jsonResult = Util.performPost(url, GSON.toJson(input), proxy, "application/json", true);
        Response result = (Response)GSON.fromJson(jsonResult, classOfT);
        if (result == null) {
            return null;
        }
        if (result.getError() != null) {
            throw new IOException(result.getError());
        }
        return (T)result;
    }
}

