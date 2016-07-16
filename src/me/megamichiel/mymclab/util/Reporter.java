package me.megamichiel.mymclab.util;

public interface Reporter {

    void info(String msg);

    void warning(String msg);

    void error(String msg);
}
