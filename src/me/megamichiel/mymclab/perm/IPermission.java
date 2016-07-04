package me.megamichiel.mymclab.perm;

public interface IPermission {

    /**
     * Returns the name of this permission
     */
    String getName();

    /**
     * Returns whether clients have this permission by default
     */
    boolean getDefault();

    /**
     * Sets whether this permission is enabled by default
     */
    void setDefault(boolean isDefault);
}
