package me.megamichiel.mymclab.util;

import me.megamichiel.mymclab.io.ProtocolInput;
import me.megamichiel.mymclab.io.ProtocolOutput;
import me.megamichiel.mymclab.packet.Packet;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColoredText {

    private final List<Object> values = new ArrayList<>();

    public ColoredText() {}

    public ColoredText(ProtocolInput data) throws IOException {
        int read;
        while ((read = data.readVarInt()) != 0) {
            switch (read & 0x3) {
                case 1:
                    byte[] b = new byte[read >>> 2];
                    data.readFully(b);
                    values.add(new String(b, Packet.UTF_8));
                    break;
                case 2:
                    values.add(EnumFormat.values()[read >>> 2]);
                    break;
                case 3:
                    values.add(EnumColor.values()[read >>> 2]);
                    break;
            }
        }
    }

    public void write(ProtocolOutput data) throws IOException {
        for (Object o : values) {
            if (o instanceof String) {
                byte[] b = ((String) o).getBytes(Packet.UTF_8);
                data.writeVarInt(0x1 | (b.length << 2));
                data.write(b);
            } else if (o instanceof EnumFormat)
                data.writeVarInt(0x2 | (((EnumFormat) o).ordinal() << 2));
            else if (o instanceof EnumColor)
                data.writeVarInt(0x3 | (((EnumColor) o).ordinal() << 2));
        }
        data.writeByte(0);
    }

    public ColoredText text(String text) {
        values.add(text);
        return this;
    }

    public ColoredText format(EnumFormat format) {
        values.add(format);
        return this;
    }

    public ColoredText color(EnumColor color) {
        values.add(color);
        return this;
    }

    public String toHtml() {
        StringBuilder sb = new StringBuilder();
        Deque<EnumFormat> formats = new ArrayDeque<>(4);
        EnumColor color = null;
        for (Object value : values) {
            if (value instanceof String) sb.append((String) value);
            else if (value instanceof EnumFormat) {
                if (!formats.contains(value)) {
                    formats.add((EnumFormat) value);
                    sb.append('<').append(((EnumFormat) value).tag).append('>');
                }
            } else if (value instanceof EnumColor) {
                while (!formats.isEmpty())
                    sb.append("</").append(formats.pop()).append('>');
                if (color != null) sb.append("</font>");
                color = (EnumColor) value;
                sb.append("<font color=\"").append(color.hex).append("\">");
            }
        }
        while (!formats.isEmpty()) sb.append("</").append(formats.pop().tag).append('>');
        if (color != null) sb.append("</font>");

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ColoredText && ((ColoredText) obj).values.equals(values);
    }

    public enum EnumColor {
        BLACK           ("#000000"),
        DARK_BLUE       ("#000080"),
        DARK_GREEN      ("#008000"),
        DARK_AQUA       ("#008080"),
        DARK_RED        ("#800000"),
        DARK_PURPLE     ("#800080"),
        GOLD            ("#C08000"),
        LIGHT_GRAY      ("#808080"),
        DARK_GRAY       ("#3B3B3B"),
        BLUE            ("#3B3BC0"),
        GREEN           ("#3BC03B"),
        AQUA            ("#3BC0C0"),
        RED             ("#C03B3B"),
        LIGHT_PURPLE    ("#C03BC0"),
        YELLOW          ("#C0C03B"),
        WHITE           ("#C0C0C0"),
        RESET           ("#000000");

        private final String hex;

        EnumColor(String hex) {
            this.hex = hex;
        }
    }

    public enum EnumFormat {

        BOLD("strong"), STRIKE_THROUGH("s"), UNDERLINE("u"), ITALIC("i");

        private final String tag;

        EnumFormat(String tag) {
            this.tag = tag;
        }
    }

    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");
    private static final Map<String, Character> replacements = new HashMap<>();

    private static final char COLOR_CHAR = '\u00A7';
    private static final String COLOR_CHARS = "0123456789ABCDEFRabcdefrKLMNOklmno";
    private static final String[] FORMAT_TAGS = { "strong", "u", "s", "i" };

    static {
        replacements.put("\u001B[0;30;22m", '0');
        replacements.put("\u001B[0;34;22m", '1');
        replacements.put("\u001B[0;32;22m", '2');
        replacements.put("\u001B[0;36;22m", '3');
        replacements.put("\u001B[0;31;22m", '4');
        replacements.put("\u001B[0;35;22m", '5');
        replacements.put("\u001B[0;33;22m", '6');
        replacements.put("\u001B[0;37;22m", '7');
        replacements.put("\u001B[0;30;1m", '8');
        replacements.put("\u001B[0;34;1m", '9');
        replacements.put("\u001B[0;32;1m", 'a');
        replacements.put("\u001B[0;36;1m", 'b');
        replacements.put("\u001B[0;31;1m", 'c');
        replacements.put("\u001B[0;35;1m", 'd');
        replacements.put("\u001B[0;33;1m", 'e');
        replacements.put("\u001B[0;37;1m", 'f');
        replacements.put("\u001B[5m", 'k');
        replacements.put("\u001B[21m", 'l');
        replacements.put("\u001B[9m", 'm');
        replacements.put("\u001B[4m", 'n');
        replacements.put("\u001B[3m", 'o');
        replacements.put("\u001B[m", 'r');
    }

    public static ColoredText parse(String text, boolean ansi) {
        if (ansi) {
            Matcher matcher = ANSI_PATTERN.matcher(text);
            if (matcher.find()) {
                StringBuilder sb = new StringBuilder();
                int lastEnd = 0;
                do {
                    if (matcher.start() > 0) sb.append(text.substring(lastEnd, matcher.start()));
                    Character c = replacements.get(matcher.group());
                    if (c != null)
                        sb.append(COLOR_CHAR).append(c.charValue());
                    lastEnd = matcher.end();
                } while (matcher.find());
                if (lastEnd < text.length())
                    sb.append(text.substring(lastEnd));
                text = sb.toString();
            }
        }

        text = text.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
        int index = text.indexOf(COLOR_CHAR);
        ColoredText result = new ColoredText();
        if (index == -1) return result.text(text);

        int lastIndex = 0, color;
        boolean[] formats = new boolean[FORMAT_TAGS.length];

        do {
            if (index + 1 < text.length() && (color = COLOR_CHARS.indexOf(text.charAt(index + 1))) > -1) {
                result.text(text.substring(lastIndex, index));
                lastIndex = index + 2;
                if (color < 24) {
                    result.color(EnumColor.values()[(color > 16 ? color - 7 : color)]);
                    Arrays.fill(formats, false);
                } else { // A Chat Format
                    int format = (format = color - 24) >= 5 ? format - 5 : format;
                    if (format > 0 && !formats[format - 1]) { // No obfuscated stoof ;p
                        result.format(EnumFormat.values()[format]);
                        formats[format - 1] = true;
                    }
                }
            } else result.text(text.substring(lastIndex, lastIndex = index + 1));
        } while ((index = text.indexOf(COLOR_CHAR, index + 1)) > -1);

        result.text(text.substring(lastIndex, text.length()));

        return result;
    }

    private static Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + COLOR_CHAR + "[0-9A-FK-OR]");

    public static String stripChatColors(String text) {
        return text == null ? null : STRIP_COLOR_PATTERN.matcher(text).replaceAll("");
    }
}
