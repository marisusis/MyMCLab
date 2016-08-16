package me.megamichiel.mymclab.perm;

import java.util.*;

public final class CustomPermission implements IPermission {

    private static final Map<String, CustomPermission> values = new HashMap<>();

    private final String name;
    private boolean isDefault = false;

    private CustomPermission(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CustomPermission && ((CustomPermission) obj).name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean getDefault() {
        return isDefault;
    }

    @Override
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Override
    public String toString() {
        return "CustomPermission[" + name + "]";
    }

    /**
     * Returns all known permissions. This includes:
     * <ul>
     *     <li>All DefaultPermission values</li>
     *     <li>All custom permissions called through {@link #resolvePermission(String)}</li>
     * </ul>
     */
    public static Set<IPermission> getKnownPermissions() {
        DefaultPermission[] def = DefaultPermission.values();
        Set<IPermission> out = new HashSet<>(values.size() + def.length);
        Collections.addAll(out, DefaultPermission.values());
        out.addAll(values.values());
        return out;
    }

    /**
     * Tries to find a permission by a specific name, in these steps:<br/>
     * <ol>
     *     <li>If a DefaultPermission with this name exists, it returns that one</li>
     *     <li>If a CustomPermission instance exists with the name, it returns that</li>
     *     <li>If none of the above have a result, it returns a new CustomPermission instance</li>
     * </ol>
     *
     * @param name the name of the permission
     */
    public static IPermission resolvePermission(String name) {
        if (name == null || name.isEmpty()) return null;
        for (DefaultPermission perm : DefaultPermission.values())
            if (perm.getName().equals(name))
                return perm;
        CustomPermission perm = values.get(name);
        if (perm == null) values.put(name, perm = new CustomPermission(name));
        return perm;
    }
}
