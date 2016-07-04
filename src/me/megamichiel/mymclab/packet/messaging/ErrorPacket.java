package me.megamichiel.mymclab.packet.messaging;

import me.megamichiel.mymclab.io.ProtocolInput;
import me.megamichiel.mymclab.io.ProtocolOutput;
import me.megamichiel.mymclab.packet.Packet;
import me.megamichiel.mymclab.perm.DefaultPermission;

import java.io.IOException;
import java.util.Date;

public class ErrorPacket extends Packet {

    private static final byte ID = getId(ErrorPacket.class);

    private final Error[] values;

    public ErrorPacket(Error[] values) {
        super(ID);
        this.values = values;
    }

    public ErrorPacket(ProtocolInput data) throws IOException {
        super(ID);
        values = new Error[data.readUnsignedByte()];
        for (int i = 0; i < values.length; i++)
            values[i] = new Error(data);
    }

    public Error[] getValues() {
        return values;
    }

    @Override
    public void encode(ProtocolOutput data) throws IOException {
        data.writeByte(values.length);
        for (Error value : values) value.write(data);
    }

    @Override
    public DefaultPermission getPermission() {
        return DefaultPermission.VIEW_ERRORS;
    }

    public static class Error {

        private final long at;
        private final String name;
        private final String message;
        private final Error cause;
        private final StackElement[] stackTrace;

        public Error(long at, Throwable source) {
            this.at = at;
            name = source.getClass().getName();
            message = source.getMessage();
            Throwable cause = source.getCause();
            this.cause = cause == null ? null : new Error(at, cause);
            StackTraceElement[] elements = source.getStackTrace();
            stackTrace = new StackElement[elements.length];
            for (int i = 0; i < elements.length; i++)
                stackTrace[i] = new StackElement(elements[i]);
        }

        public Error(ProtocolInput data) throws IOException {
            at = data.readVarLong();
            name = data.readString();
            message = data.readString();
            cause = data.readBoolean() ? new Error(data) : null;
            stackTrace = new StackElement[data.readVarInt()];
            for (int i = 0; i < stackTrace.length; i++)
                stackTrace[i] = new StackElement(data);
        }

        public long getAt() {
            return at;
        }

        public String timeToString() {
            return MessagePacket.TIME_FORMAT.format(new Date(at));
        }

        public String getName() {
            return name;
        }

        public String getMessage() {
            return message;
        }

        public Error getCause() {
            return cause;
        }

        public boolean hasCause() {
            return cause != null;
        }

        public StackElement[] getStackTrace() {
            return stackTrace;
        }

        public String generateStackTrace() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < stackTrace.length; i++) {
                if (i > 0) sb.append('\n');
                sb.append(stackTrace[i]);
            }
            return sb.toString();
        }

        public void write(ProtocolOutput data) throws IOException {
            data.writeVarLong(at);
            data.writeString(name);
            data.writeString(message);
            data.writeBoolean(cause != null);
            if (cause != null) cause.write(data);
            data.writeVarInt(stackTrace.length);
            for (StackElement stackElement : stackTrace)
                stackElement.write(data);
        }
    }

    public static class StackElement {

        private final String declaringClass, methodName, fileName;
        private final int lineNumber;

        public StackElement(StackTraceElement parent) {
            declaringClass = parent.getClassName();
            methodName = parent.getMethodName();
            fileName = parent.getFileName();
            lineNumber = parent.getLineNumber();
        }

        public StackElement(ProtocolInput data) throws IOException {
            declaringClass = data.readString();
            methodName = data.readString();
            fileName = data.readString();
            lineNumber = data.readVarInt();
        }

        public String getDeclaringClass() {
            return declaringClass;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getFileName() {
            return fileName;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public void write(ProtocolOutput data) throws IOException {
            data.writeString(declaringClass);
            data.writeString(methodName);
            data.writeString(fileName);
            data.writeVarInt(lineNumber);
        }

        public String toString() {
            return declaringClass + "." + methodName +
                    (lineNumber == -2 ? "(Native Method)" :
                            (fileName != null && lineNumber >= 0 ?
                                    "(" + fileName + ":" + lineNumber + ")" :
                                    (fileName != null ?  "("+fileName+")" : "(Unknown Source)")));
        }
    }
}
