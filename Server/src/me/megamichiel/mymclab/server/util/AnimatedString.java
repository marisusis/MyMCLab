package me.megamichiel.mymclab.server.util;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

public class AnimatedString<E> {
    
    private static final long serialVersionUID = 5235518796395129933L;
    private static final Random random = new Random();

    private final Function<String, E> parser;
    
    private final List<E> values = new ArrayList<>();
    private final E defaultValue;

    private int frame = 0;
    private boolean isRandom;
    
    public AnimatedString(Function<String, E> parser,
                          E defaultValue) {
        this.parser = parser;
        this.defaultValue = defaultValue;
    }

    public E next() {
        int size = values.size();
        if (size == 0) return defaultValue;
        E current = values.get(frame);
        switch (size) {
            case 1: return current;
            case 2:
                frame = 1 - frame; // Ezpz 2 frames
                return current;
            default:
                if (isRandom) {
                    // No frames twice in a row ;3
                    for (int prev = frame; frame == prev;)
                        frame = random.nextInt(size);
                } else if (++frame == size) frame = 0;
                return current;
        }
    }

    public boolean load(Logger logger, IConfig section, String key) {
        values.clear();
        IConfig sec = section.getSection(key);
        if (sec != null) {
            Map<Integer, String> values = new HashMap<>();
            int highest = 1;
            for (String id : sec.keys()) {
                if ("random".equals(id)) {
                    isRandom = sec.getBoolean(id);
                    continue;
                }
                String value = sec.getString(id);
                if (value == null) continue;
                for (String item : id.split(",")) {
                    item = item.trim();
                    try {
                        int num = Integer.parseInt(item);
                        if (num > 0) {
                            if (num > highest) highest = num;
                            values.put(num, value);
                        }
                    } catch (NumberFormatException ex) {
                        int index = item.indexOf('-');
                        if (index > 0 && index < item.length() - 1) {
                            try {
                                int min = Integer.parseInt(item.substring(0, index)),
                                        max = Integer.parseInt(item.substring(index + 1));
                                if (max < min) {
                                    logger.warning("Max < Min at " + item + "!");
                                    continue;
                                }
                                if (max > highest) highest = max;
                                for (int i = min; i <= max; i++) values.put(i, value);
                            } catch (NumberFormatException ex2) {
                                logger.warning("Invalid number: " + item + '!');
                            }
                        }
                    }
                }
            }
            String last = null;
            for (int i = 1; i <= highest; i++) {
                String s = values.get(i);
                if (s != null) last = s;
                if (last == null)
                    logger.warning("No frame specified at " + i + " in " + key + "!");
                else this.values.add(parser.apply(last));
            }
            return true;
        }
        String value = section.getString(key);
        if (value != null) {
            values.add(parser.apply(value));
            return true;
        }
        return false;
    }
}
