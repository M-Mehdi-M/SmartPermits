package project.smartpermits.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;

import java.util.Locale;

public class ApiConfig {

    public static final int SERVER_PORT = 5000;

    public static final String DEFAULT_IP = "192.168.137.1";

    private static final String PREFS = "smart_permits_prefs";
    private static final String KEY_MANUAL_IP = "server_ip_override";

    public static String resolveIp(Context context) {
        Context app = context.getApplicationContext();

        String manual = getManualIp(app);
        if (manual != null && !manual.trim().isEmpty()) {
            return manual.trim();
        }

        String gateway = getGatewayIp(app);
        if (gateway != null && !gateway.isEmpty()) {
            return gateway;
        }

        return DEFAULT_IP;
    }

    /** Full REST base URL, e.g. http://192.168.137.1:5000/api/ */
    public static String getApiUrl(Context context) {
        return "http://" + resolveIp(context) + ":" + SERVER_PORT + "/api/";
    }

    /** Socket.IO base URL, e.g. http://192.168.137.1:5000 */
    public static String getSocketUrl(Context context) {
        return "http://" + resolveIp(context) + ":" + SERVER_PORT;
    }

    private static String getGatewayIp(Context app) {
        try {
            WifiManager wifi =
                    (WifiManager) app.getSystemService(Context.WIFI_SERVICE);
            if (wifi == null) {
                return null;
            }
            int gateway = wifi.getDhcpInfo().gateway;
            if (gateway == 0) {
                return null;
            }

            return String.format(Locale.US, "%d.%d.%d.%d",
                    (gateway & 0xff),
                    (gateway >> 8 & 0xff),
                    (gateway >> 16 & 0xff),
                    (gateway >> 24 & 0xff));
        } catch (Exception e) {
            return null;
        }
    }

    public static void setManualIp(Context context, String ip) {
        prefs(context).edit()
                .putString(KEY_MANUAL_IP, ip == null ? "" : ip.trim())
                .apply();
    }

    public static String getManualIp(Context context) {
        return prefs(context).getString(KEY_MANUAL_IP, "");
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
