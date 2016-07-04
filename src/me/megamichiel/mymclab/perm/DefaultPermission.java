package me.megamichiel.mymclab.perm;

/**
 * An enum which contains all default permissions
 */
public enum DefaultPermission implements IPermission {

    VIEW_CONSOLE        ("view.console"),
    VIEW_CHAT           ("view.chat"),
    VIEW_ERRORS         ("view.errors"),
    VIEW_PLAYERS        ("view.players"),
    VIEW_SERVER_INFO    ("view.serverinfo"),

    INPUT_CHAT          ("input.chat"),
    INPUT_COMMANDS      ("input.commands"),

    CLICK_PLAYERS       ("click.players"),
    CLICK_SERVER_INFO   ("click.serverinfo");

    private final String name;
    private boolean isDefault = true;

    DefaultPermission(String name) {
        this.name = "default." + name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean getDefault() {
        return isDefault;
    }

    @Override
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
