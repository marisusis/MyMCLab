package me.megamichiel.mymclab.perm;

public interface GroupManager {

    /**
     * Finds a group by name
     */
    Group getGroup(String name);

    /**
     * Returns the default group.
     */
    default Group getDefaultGroup() {
        return getGroup("default");
    }
}
