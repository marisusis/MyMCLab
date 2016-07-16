package me.megamichiel.mymclab.server.util;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

public class MapConfig implements Serializable {

    private final Map<String, Object> parent = new HashMap<>();

    public MapConfig(Map<?, ?> map) {
        for (Map.Entry entry : map.entrySet())
            parent.put(entry.getKey().toString().toLowerCase(Locale.US), entry.getValue());
    }

    public MapConfig() {}

    public Object get(String key, Object def) {
        Object o = parent.get(key);
        return o == null ? def : o;
    }

    public <T> T get(String key, Function<Object, T> func, T def) {
        Optional<Object> opt = Optional.ofNullable(parent.get(key));
        return opt.isPresent() ? opt.map(func).orElse(def) : def;
    }

    public String getString(String key, String def) {
        return get(key, Object::toString, def);
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public boolean isString(String key) {
        Object o = parent.get(key);
        return o instanceof String || isPrimitiveWrapper(o);
    }

    public int getInt(String key, int def) {
        Object o = get(key, def);
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    public long getLong(String key, long def) {
        Object o = get(key, def);
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    public double getDouble(String key, double def) {
        Object o = get(key, def);
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    public boolean getBoolean(String key, boolean def) {
        Object o = get(key, def);
        if (o instanceof Boolean) return (Boolean) o;
        return o.toString().equalsIgnoreCase("true");
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean isBoolean(String key) {
        Object o = get(key, null);
        if (o instanceof Boolean) return true;
        if (o != null) {
            String str = o.toString();
            return str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false");
        }
        return false;
    }

    public MapConfig getSection(String key) {
        Object o = get(key, null);
        return o instanceof Map ? new MapConfig((Map) o) : null;
    }

    public List getList(String key) {
        return get(key, List.class::cast, null);
    }

    public <T> List<T> getList(String key, Function<Object, T> func) {
        List<?> list = getList(key);
        List<T> res = new ArrayList<>();
        if (list != null)
            list.stream().map(func::apply).filter(e -> e != null).forEach(res::add);
        return res;
    }

    public List<String> getStringList(String key) {
        return getList(key, o -> o instanceof String || isPrimitiveWrapper(o) ? o.toString() : null);
    }

    public List<MapConfig> getSectionList(String key) {
        return getList(key, o -> o instanceof Map ? new MapConfig((Map) o) : null);
    }

    public Set<String> keys() {
        return parent.keySet();
    }

    public Map<String, Object> values() {
        return new HashMap<>(parent);
    }

    @Override
    public String toString() {
        return parent.toString();
    }

    public static boolean isPrimitiveWrapper(Object o) {
        return o instanceof Number || o instanceof Boolean;
    }
}
