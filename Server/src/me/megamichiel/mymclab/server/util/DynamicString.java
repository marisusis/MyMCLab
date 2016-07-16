package me.megamichiel.mymclab.server.util;

import java.util.Map;
import java.util.regex.Pattern;

public interface DynamicString {

    String toString(Object player, Object context);

    void colorAmpersands();

    void replacePrompts(Pattern pattern, Map<String, String> promptValues);
}
