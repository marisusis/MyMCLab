package me.megamichiel.mymclab.bukkit;

import me.megamichiel.mymclab.packet.Packet;
import me.megamichiel.mymclab.perm.CustomPermission;
import me.megamichiel.mymclab.perm.Group;
import me.megamichiel.mymclab.perm.GroupManager;
import me.megamichiel.mymclab.perm.IPermission;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupsManager implements GroupManager {

    private final MyMCLabPlugin plugin;
    private final List<Group> groups = new ArrayList<>();

    GroupsManager(MyMCLabPlugin plugin) {
        this.plugin = plugin;
    }

    boolean onEnable() {
        groups.clear();
        ConfigurationSection groups = plugin.getConfig().getConfigurationSection("groups");
        if (groups != null) {
            List<String> names = new ArrayList<>(groups.getKeys(false));
            while (!names.isEmpty()) loadGroup(names, names.get(0), groups);
            if (this.groups.isEmpty()) {
                plugin.nag("No groups specified/loaded!");
                return false;
            }
            if (getGroup("default") == null) {
                plugin.nag("No 'default' group specified/loaded!");
                return false;
            }
        } else {
            String password = plugin.getConfig().getString("password");
            if (password != null) {
                this.groups.add(new Group("default",
                        password.getBytes(Packet.UTF_8),
                        iPermission -> true));
            } else {
                plugin.nag("No password or groups specified in config!");
                return false;
            }
        }
        ConfigurationSection permDef = plugin.getConfig().getConfigurationSection("permission-defaults");
        if (permDef != null)
            permDef.getKeys(false).stream().filter(permDef::isBoolean).forEach(key -> CustomPermission.resolvePermission(key).setDefault(permDef.getBoolean(key)));
        return true;
    }

    private Group loadGroup(List<String> names, String name, ConfigurationSection groups) {
        names.remove(name);
        ConfigurationSection section = groups.getConfigurationSection(name);
        if (section == null) {
            plugin.nag("Group " + name + " is not a section!");
            return null;
        }
        String password = plugin.getConfig().getString("password");
        PermissionChecker permissions = new PermissionChecker(section.getStringList("permissions"));
        String parentsString = section.getString("parents");
        if (parentsString != null) {
            String[] split = parentsString.split(",");
            for (String parentName : split) {
                parentName = parentName.trim();
                Group group = getGroup(parentName);
                if (group == null && names.contains(parentName))
                    group = loadGroup(names, parentName, groups);
                if (group != null) ((PermissionChecker) group.getPermissionChecker()).copyTo(permissions);
                else {
                    plugin.nag("Unknown parent '" + parentName + "' for group '" + name + "'!");
                }
            }
        }
        Group group = new Group(name, password == null ? null : password.getBytes(Packet.UTF_8), permissions);
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

    private class PermissionChecker implements Group.PermissionPredicate {

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

        void copyTo(PermissionChecker other) {
            other.enabledPermissions.addAll(enabledPermissions);
            other.disabledPermissions.addAll(disabledPermissions);
        }

        @Override
        public boolean apply(@Nullable IPermission perm) {
            return perm == null || perm.getName() == null || enabledPermissions.contains(perm)
                    || (!disabledPermissions.contains(perm) && perm.getDefault());
        }
    }
}
