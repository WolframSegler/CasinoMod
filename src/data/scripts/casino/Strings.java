package data.scripts.casino;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Strings {
    private static final Logger log = Global.getLogger(Strings.class);
    private static final String STRINGS_PATH = "data/config/strings.json";
    private static JSONObject strings = null;

    public static void load() {
        try {
            strings = Global.getSettings().loadJSON(STRINGS_PATH, CasinoConfig.MOD_ID);
            log.info("Casino strings loaded successfully");
        } catch (IOException | JSONException e) {
            log.error("Error loading casino strings: " + e.getMessage());
            strings = new JSONObject();
        }
    }

    public static String get(String key) {
        if (strings == null) {
            log.warn("Strings not loaded, returning key: " + key);
            return key;
        }
        
        String[] parts = key.split("\\.");
        JSONObject current = strings;
        
        try {
            for (int i = 0; i < parts.length - 1; i++) {
                if (!current.has(parts[i])) {
                    return key;
                }
                current = current.getJSONObject(parts[i]);
            }
            
            String lastPart = parts[parts.length - 1];
            if (!current.has(lastPart)) {
                return key;
            }
            
            return current.getString(lastPart);
        } catch (JSONException e) {
            log.warn("Error getting string for key: " + key + " - " + e.getMessage());
            return key;
        }
    }

    public static String format(String key, Object... args) {
        String template = get(key);
        try {
            return String.format(template, args);
        } catch (Exception e) {
            log.warn("Error formatting string for key: " + key + " - " + e.getMessage());
            return template;
        }
    }

    public static boolean has(String key) {
        if (strings == null) return false;
        
        String[] parts = key.split("\\.");
        JSONObject current = strings;
        
        try {
            for (int i = 0; i < parts.length - 1; i++) {
                if (!current.has(parts[i])) return false;
                current = current.getJSONObject(parts[i]);
            }
            return current.has(parts[parts.length - 1]);
        } catch (JSONException e) {
            return false;
        }
    }

    public static List<String> getList(String key) {
        if (strings == null) {
            log.warn("Strings not loaded, returning empty list for key: " + key);
            return Collections.emptyList();
        }
        
        String[] parts = key.split("\\.");
        JSONObject current = strings;
        
        try {
            for (int i = 0; i < parts.length - 1; i++) {
                if (!current.has(parts[i])) {
                    return Collections.emptyList();
                }
                current = current.getJSONObject(parts[i]);
            }
            
            String lastPart = parts[parts.length - 1];
            if (!current.has(lastPart)) {
                return Collections.emptyList();
            }
            
            JSONArray array = current.getJSONArray(lastPart);
            List<String> result = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                result.add(array.getString(i));
            }
            return result;
        } catch (JSONException e) {
            log.warn("Error getting string list for key: " + key + " - " + e.getMessage());
            return Collections.emptyList();
        }
    }
}