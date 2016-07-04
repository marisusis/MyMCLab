package me.megamichiel.mymclab.bukkit;

import me.megamichiel.mymclab.packet.messaging.ErrorPacket;
import me.megamichiel.mymclab.packet.messaging.MessagePacket;
import me.megamichiel.mymclab.util.ColoredText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

class ConsoleHandler {

    private final boolean log4j;
    private final Object value;

    ConsoleHandler(final List<MessagePacket.Message> messages, final List<ErrorPacket.Error> errors) {
        boolean log4j = false;
        Object value;
        try {
            Class.forName("org.apache.logging.log4j.Logger");
            log4j = true;
            value = new AbstractAppender("MyMCLab", null, null, false) {
                private MessagePacket.LogLevel toLogLevel(org.apache.logging.log4j.Level level) {
                    switch (level) {
                        case OFF:
                            return MessagePacket.LogLevel.OFF;
                        case FATAL:
                            return MessagePacket.LogLevel.SEVERE;
                        case ERROR:
                            return MessagePacket.LogLevel.ERROR;
                        case WARN:
                            return MessagePacket.LogLevel.WARNING;
                        case INFO:
                            return MessagePacket.LogLevel.INFO;
                        case DEBUG:
                            return MessagePacket.LogLevel.DEBUG;
                        case TRACE:
                            return MessagePacket.LogLevel.TRACE;
                        default:
                            return MessagePacket.LogLevel.ALL;
                    }
                }

                @Override
                public void append(LogEvent log) {
                    if (log.getMessage() != null)
                        messages.add(new MessagePacket.Message(
                                log.getMillis(), toLogLevel(log.getLevel()),
                                ColoredText.parse(log.getMessage().getFormattedMessage(), true)
                        ));
                    Throwable thrown = log.getThrown();
                    if (thrown != null)
                        errors.add(new ErrorPacket.Error(log.getMillis(), thrown));
                }
            };
        } catch (Exception ex) {
            value = new Handler() {
                @Override
                public void publish(LogRecord log) {
                    if (log.getMessage() != null)
                        messages.add(new MessagePacket.Message(
                                log.getMillis(), toLogLevel(log.getLevel()),
                                ColoredText.parse(log.getMessage(), true)
                        ));
                    Throwable thrown = log.getThrown();
                    if (thrown != null)
                        errors.add(new ErrorPacket.Error(log.getMillis(), thrown));
                }

                private MessagePacket.LogLevel toLogLevel(Level level) {
                    if (level == Level.ALL) return MessagePacket.LogLevel.ALL;
                    if (level == Level.CONFIG || level == Level.FINE
                            || level == Level.FINER) return MessagePacket.LogLevel.DEBUG;
                    if (level == Level.FINEST) return MessagePacket.LogLevel.TRACE;
                    if (level == Level.WARNING) return MessagePacket.LogLevel.WARNING;
                    if (level == Level.SEVERE) return MessagePacket.LogLevel.ERROR;
                    if (level == Level.OFF) return MessagePacket.LogLevel.OFF;
                    return MessagePacket.LogLevel.INFO;
                }

                @Override
                public void flush() {}

                @Override
                public void close() throws SecurityException {}
            };
        }
        this.value = value;
        this.log4j = log4j;
    }

    void onEnable() {
        if (log4j) {
            Appender appender = (Appender) value;
            appender.start();
            ((Logger) LogManager.getRootLogger()).addAppender(appender);
        } else java.util.logging.Logger.getLogger("").addHandler((Handler) value);
    }

    void onDisable() {
        if (log4j) ((Logger) LogManager.getRootLogger()).removeAppender((Appender) value);
        else java.util.logging.Logger.getLogger("").removeHandler((Handler) value);
    }
}
