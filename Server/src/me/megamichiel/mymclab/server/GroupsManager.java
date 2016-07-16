package me.megamichiel.mymclab.server;

import me.megamichiel.mymclab.packet.Packet;
import me.megamichiel.mymclab.perm.*;
import me.megamichiel.mymclab.server.util.MapConfig;
import me.megamichiel.mymclab.util.Reporter;

import java.util.*;
import java.util.function.Predicate;

public class GroupsManager implements GroupManager {

    private final Reporter reporter;
    private final List<Group> groups = new ArrayList<>();
    private final Set<IPermission> disabledFeatures = new HashSet<>();

    public GroupsManager(Reporter reporter) {
        this.reporter = reporter;
    }

    public void disableFeatures(DefaultPermission... permissions) {
        Collections.addAll(disabledFeatures, permissions);
    }

    public boolean enable(MapConfig config) {
        groups.clear();

        MapConfig groups = config.getSection("groups");
        if (groups != null) {
            List<String> names = new ArrayList<>(groups.keys());
            while (!names.isEmpty()) loadGroup(config, names, names.get(0), groups);
            if (this.groups.isEmpty()) {
                reporter.warning("No groups specified/loaded!");
                return false;
            }
            if (getGroup("default") == null) {
                reporter.warning("No 'default' group specified/loaded!");
                return false;
            }
        } else {
            String password = config.getString("password");
            if (password != null) {
                this.groups.add(new Group("default",
                        password.getBytes(Packet.UTF_8), perm ->
                        !(perm instanceof DefaultPermission)
                                || !disabledFeatures.contains(perm)));
            } else {
                reporter.warning("No password or groups specified in config!");
                return false;
            }
        }
        MapConfig permDef = config.getSection("permission-defaults");
        if (permDef != null)
            permDef.keys().stream().filter(permDef::isBoolean)
                    .forEach(key -> CustomPermission.resolvePermission(key)
                            .setDefault(permDef.getBoolean(key)));
        return true;
    }

    private Group loadGroup(MapConfig config, List<String> names, String name, MapConfig groups) {
        names.remove(name);
        MapConfig section = groups.getSection(name);
        if (section == null) {
            reporter.warning("Group " + name + " is not a section!");
            return null;
        }
        String password = config.getString("password");
        PermissionChecker permissions = new PermissionChecker(section.getStringList("permissions"));
        String parentsString = section.getString("parents");
        if (parentsString != null) {
            String[] split = parentsString.split(",");
            for (String parentName : split) {
                parentName = parentName.trim();
                Group group = getGroup(parentName);
                if (group == null && names.contains(parentName))
                    group = loadGroup(config, names, parentName, groups);
                if (group != null) ((PermissionChecker) group.getPermissionChecker()).copyTo(permissions);
                else {
                    reporter.warning("Unknown parent '" + parentName + "' for group '" + name + "'!");
                }
            }
        }
        Group group = new Group(name,
                password == null ? null : password.getBytes(Packet.UTF_8), permissions);
        this.groups.add(group);
        return group;
    }

    @Override
    public Group getGroup(String name) {
        for (Group group : groups)
            if (group.getName().equals(name))
                return group;
        return null;
    }

    public Group getGroupOrDefault(String name) {
        if (groups.size() == 1) return groups.get(0); // In case of default group
        for (Group group : groups)
            if (group.getName().equals(name))
                return group;
        return getDefaultGroup();
    }

    private class PermissionChecker implements Predicate<IPermission> {

        private final Set<IPermission> enabledPermissions = new HashSet<>(),
                                        disabledPermissions = new HashSet<>();

        private PermissionChecker(List<String> values) {
            for (String perm : values) {
                if (perm.isEmpty()) continue;
                if (perm.charAt(0) == '-')
                    disabledPermissions.add(CustomPermission.resolvePermission(perm.substring(1)));
                else enabledPermissions.add(CustomPermission.resolvePermission(perm));
            }
        }

        void copyTo(PermissionChecker other) { // For group inheriting
            other.enabledPermissions.addAll(enabledPermissions);
            other.disabledPermissions.addAll(disabledPermissions);
        }

        @Override
        public boolean test(IPermission perm) {
            return !(perm instanceof DefaultPermission
                    && disabledFeatures.contains(perm))
                    && (perm == null || perm.getName() == null
                    || enabledPermissions.contains(perm)
                    || (!disabledPermissions.contains(perm) && perm.getDefault()));
        }
    }
}
