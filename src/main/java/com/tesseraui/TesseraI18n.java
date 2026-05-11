package com.tesseraui;

import java.util.function.UnaryOperator;

/**
 * Thin wrapper around Minecraft's {@code I18n.get(key)} that allows
 * unit tests to inject a mock translator without a live Minecraft runtime.
 */
public final class TesseraI18n {

    /**
     * The active translator.  Defaults to {@code net.minecraft.client.resources.language.I18n::get}.
     * Tests can replace this field with a lambda to avoid requiring Minecraft.
     */
    public static UnaryOperator<String> TRANSLATOR =
            net.minecraft.client.resources.language.I18n::get;

    private TesseraI18n() {}

    /**
     * Translates {@code key} using the current {@link #TRANSLATOR}.
     *
     * @param key  the translation key (e.g. {@code "ui.mymod.confirm"})
     * @return the localised string, or {@code key} itself when no mapping exists
     */
    public static String translate(String key) {
        return TRANSLATOR.apply(key);
    }

    /**
     * Returns {@code true} when {@code key} has a translation.
     *
     * <p>Minecraft's {@code I18n.get} returns the key unchanged when the mapping
     * is absent, so a result different from the key means the key is translated.</p>
     *
     * @param key  the translation key to test
     * @return {@code true} if the key resolved to something other than itself
     */
    public static boolean isTranslated(String key) {
        return !translate(key).equals(key);
    }
}
