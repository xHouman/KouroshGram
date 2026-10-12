package com.kouroshgram.setup;

import android.content.Context;
import android.content.SharedPreferences;

/** Only this app's developer API credentials; never a phone login or bot token. */
public final class ApiConfig {
    private static final String STORE = "kourosh_api_config";
    private ApiConfig() {}
    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(STORE, Context.MODE_PRIVATE);
    }
    public static int id(Context c) { return prefs(c).getInt("api_id", 0); }
    public static String hash(Context c) { return prefs(c).getString("api_hash", ""); }
    public static boolean valid(String id, String hash) {
        try {
            return Integer.parseInt(id) > 0 && hash.matches("[a-fA-F0-9]{32}");
        } catch (NumberFormatException e) { return false; }
    }
    public static boolean ready(Context c) {
        return c != null && valid(Integer.toString(id(c)), hash(c));
    }
    public static boolean save(Context c, String id, String hash) {
        if (!valid(id, hash)) return false;
        return prefs(c).edit().putInt("api_id", Integer.parseInt(id))
            .putString("api_hash", hash.toLowerCase(java.util.Locale.ROOT)).commit();
    }
}
