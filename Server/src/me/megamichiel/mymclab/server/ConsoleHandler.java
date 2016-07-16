package me.megamichiel.mymclab.server;

import me.megamichiel.mymclab.packet.messaging.ErrorPacket;
import me.megamichiel.mymclab.packet.messaging.MessagePacket;
import me.megamichiel.mymclab.util.ColoredText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class ConsoleHandler {

    private final Method getMillis;
    private final Object value;
    private java.util.logging.Logger logger;

    public ConsoleHandler(Consumer<MessagePacket.Message> messages, Consumer<ErrorPacket.Error> errors) {
        Method getMillis = null;
        Object value;
        try {
            Class.forName("org.apache.logging.log4j.Logger");
            try {
                getMillis = LogEvent.class.getDeclaredMethod("getMillis");
            } catch (Exception ex) {
                getMillis = LogEvent.class.getDeclaredMethod("getTimeMillis");
            }
            value = new AbstractAppender("MyMCLab", null, null, false) {
                private MessagePacket.LogLevel toLogLevel(org.apache.logging.log4j.Level level) {
                    if (level == org.apache.logging.log4j.Level.FATAL) return MessagePacket.LogLevel.SEVERE;
                    if (level == org.apache.logging.log4j.Level.WARN) return MessagePacket.LogLevel.WARNING;
                    try {
                        return MessagePacket.LogLevel.valueOf(level.name());
                    } catch (IllegalArgumentException ex) {
                        return MessagePacket.LogLevel.ALL;
                    }
                }

                @Override
                public void append(LogEvent log) {
                    long at;
                    try {
                        at = (long) ConsoleHandler.this.getMillis.invoke(log);
                    } catch (Exception ex) {
                        return;
                    }
                    if (log.getMessage() != null)
                        messages.accept(new MessagePacket.Message(
                                at, toLogLevel(log.getLevel()),
                                ColoredText.parse(log.getMessage().getFormattedMessage(), true)
                        ));
                    Throwable thrown = log.getThrown();
                    if (thrown != null) errors.accept(new ErrorPacket.Error(at, thrown));
                }
            };
        } catch (Exception ex) {
            value = new Handler() {
                @Override
                public void publish(LogRecord log) {
                    if (log.getMessage() != null)
                        messages.accept(new MessagePacket.Message(
                                log.getMillis(), toLogLevel(log.getLevel()),
                                ColoredText.parse(log.getMessage(), true)
                        ));
                    Throwable thrown = log.getThrown();
                    if (thrown != null)
                        errors.accept(new ErrorPacket.Error(log.getMillis(), thrown));
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
            logger = java.util.logging.Logger.getLogger("");
        }
        this.value = value;
        this.getMillis = getMillis;
    }

    public ConsoleHandler(Consumer<MessagePacket.Message> messages,
                          Consumer<ErrorPacket.Error> errors, java.util.logging.Logger logger) {
        value = new Handler() {
            @Override
            public void publish(LogRecord log) {
                if (log.getMessage() != null)
                    messages.accept(new MessagePacket.Message(
                            log.getMillis(), toLogLevel(log.getLevel()),
                            ColoredText.parse(log.getMessage(), true)
                    ));
                Throwable thrown = log.getThrown();
                if (thrown != null)
                    errors.accept(new ErrorPacket.Error(log.getMillis(), thrown));
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
        this.logger = logger;
        getMillis = null;
    }

    public void enable() {
        if (logger == null) {
            Appender appender = (Appender) value;
            appender.start();
            ((Logger) LogManager.getRootLogger()).addAppender(appender);
        } else logger.addHandler((Handler) value);
    }

    public void disable() {
        if (getMillis != null) ((Logger) LogManager.getRootLogger()).removeAppender((Appender) value);
        else logger.removeHandler((Handler) value);
    }
}
