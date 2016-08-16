package me.megamichiel.mymclab.server.util;

import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public interface DynamicString {

    String toString(Object player, Object context);

    void colorAmpersands();

    void replacePrompts(Pattern pattern, Supplier<Map<String, String>> promptValues);

    boolean isDynamic();
}
