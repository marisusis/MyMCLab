package me.megamichiel.mymclab.server.util;

import java.util.List;
import java.util.Set;

public interface IConfig {

    int getInt(String path, int def);

    String getString(String path);

    String getString(String path, String def);

    boolean isString(String path);

    IConfig getSection(String path);

    Set<String> keys();

    boolean isBoolean(String path);

    boolean getBoolean(String path);

    List<String> getStringList(String path);

    List<? extends IConfig> getSectionList(String path);
}
