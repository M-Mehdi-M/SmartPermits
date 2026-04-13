package project.smartpermits;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREFS = "smart_permits_prefs";
    private static final String KEY = "app_language";

    public static Context applyLocale(Context context) {
        String lang = getLanguage(context);
        if (lang.isEmpty()) return context;
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

    public static void setLanguage(Context context, String lang) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY, lang).apply();
    }

    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY, "");
    }

    public static void updateResources(Context context, String lang) {
        Locale locale = lang.isEmpty() ? Resources.getSystem().getConfiguration().getLocales().get(0) : new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    public static String[] getSupportedLanguageCodes() {
        return new String[]{"", "en", "ro", "es", "fr", "it", "de", "pt", "pl", "tr", "uk"};
    }

    public static String[] getSupportedLanguageNames() {
        return new String[]{
                "System Default",
                "English",
                "Romana",
                "Espanol",
                "Francais",
                "Italiano",
                "Deutsch",
                "Portugues",
                "Polski",
                "Turkce",
                "Ukrainska"
        };
    }
}

