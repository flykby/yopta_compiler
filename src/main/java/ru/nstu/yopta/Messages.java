package ru.nstu.yopta;

import java.util.ResourceBundle;

/**
 * Доступ к локализованным строкам по ключам из messages_*.properties.
 */
public final class Messages {

    private static ResourceBundle bundle;

    public static void setLocale(java.util.Locale locale) {
        bundle = ResourceBundle.getBundle("i18n.messages", locale);
    }

    public static String getString(String key) {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("i18n.messages", AppSettings.getInstance().getLocale());
        }
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return key;
        }
    }

    public static String getString(String key, Object... args) {
        String fmt = getString(key);
        return String.format(fmt, args);
    }
}
