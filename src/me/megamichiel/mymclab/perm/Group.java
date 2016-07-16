package me.megamichiel.mymclab.perm;

import java.util.function.Predicate;

/**
 * A group where clients can be added to to give them certain permissions
 */
public class Group {

    private final String name;
    private final byte[] password;
    private final Predicate<IPermission> permissionChecker;

    public Group(String name, byte[] password, Predicate<IPermission> permissionChecker) {
        this.name = name;
        this.password = password;
        this.permissionChecker = permissionChecker;
    }

    /**
     * Returns this group's name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns this group's password. May be null, if so this group is a parent and cannot be logged in with
     */
    public byte[] getPassword() {
        return password;
    }

    /**
     * Returns this group's permission checker
     */
    public Predicate<IPermission> getPermissionChecker() {
        return permissionChecker;
    }

    /**
     * Returns whether this group has a certain permission
     *
     * @param permission the permission to check
     */
    public boolean hasPermission(IPermission permission) {
        return permissionChecker.test(permission);
    }
}
