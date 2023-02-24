package org.envirocar.core.utils;

public class TextViewUtils {
    public String getColoredSpanned(String text, Integer color) {
        return "<font color=" + color + ">" + text + "</font>";
    }
}