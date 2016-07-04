package me.megamichiel.mymclab.bukkit;

import me.megamichiel.animationlib.YamlConfig;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.PlaceholderContext;
import me.megamichiel.animationlib.placeholder.StringBundle;
import me.megamichiel.mymclab.api.Client;
import me.megamichiel.mymclab.bukkit.util.PlayerMap;
import me.megamichiel.mymclab.io.ProtocolOutput;
import me.megamichiel.mymclab.packet.player.PromptRequestPacket;
import me.megamichiel.mymclab.packet.player.PromptResponsePacket;
import me.megamichiel.mymclab.packet.player.StatisticPacket;
import me.megamichiel.mymclab.perm.CustomPermission;
import me.megamichiel.mymclab.perm.DefaultPermission;
import me.megamichiel.mymclab.perm.IPermission;
import me.megamichiel.mymclab.util.ColoredText;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.megamichiel.mymclab.packet.player.StatisticPacket.StatisticField.*;
import static me.megamichiel.mymclab.packet.player.StatisticPacket.StatisticItem;
import static me.megamichiel.mymclab.packet.player.StatisticPacket.StatisticItemAction;

public class StatisticManager {

    private static final Pattern PROMPT_PATTERN = Pattern.compile("\\{prompt_([^}]+)\\}");

    private final MyMCLabPlugin plugin;
    private final StatisticTemplate[] playerValues, serverValues;

    StatisticManager(MyMCLabPlugin plugin, ConfigurationSection section) {
        this.plugin = plugin;
        playerValues = loadConfig(plugin, "Player", YamlConfig.getConfigurationList(section, "player-info"));
        serverValues = loadConfig(plugin, "Server", YamlConfig.getConfigurationList(section, "server-info"));
    }

    public void handlePrompt(Client client, PromptResponsePacket packet) {
        String playerName = packet.getPlayer();
        Player player;
        if (playerName != null) player = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getName().equals(playerName)).findAny().orElse(null);
        else player = null;
        if (player != null || playerName == null) {
            StatisticTemplate template = (playerName == null ? serverValues : playerValues)[packet.getItemIndex()];
            PlaceholderContext context = PlaceholderContext.create(plugin);
            Map<String, String> map = template.promptValues.get();
            for (PromptResponsePacket.PromptResponse response : packet.getResponses()) {
                DefaultPromptRequest request = template.sentPrompts[response.getIndex()];

                String value = response.getValue();
                if (request instanceof CheckBoxPromptRequest) {
                    CheckBoxPromptRequest box = (CheckBoxPromptRequest) request;
                    value = ("checked".equals(value) ? box.checked : box.unchecked).toString(player, context);
                }
                map.put(request.id, value);
            }
            for (ChildPromptRequest child : template.childPrompts) {
                String value = map.get(child.parent);
                if (value != null) {
                    String result = child.get(player, value.toLowerCase(Locale.US), context);
                    if (result != null) map.put(child.id, result);
                }
            }
            for (StringBundle command : template.commands)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.toString(player, context));
            if (template.toast != null) client.makeToast(template.toast.toString(player, context));
            map.clear();
        }
    }

    public void handleClick(Client client, String name, int index) {
        Player player = null;
        if (name != null) player = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.getName().equals(name)).findAny().orElse(null);
        if (player != null || name == null) {
            StatisticTemplate template =
                    (name == null ? serverValues : playerValues)[index];
            if ((template.permission != null && !client.hasPermission(template.permission)) ||
                    (template.clickPermission != null && !client.hasPermission(template.clickPermission)))
                return;
            PlaceholderContext context = PlaceholderContext.create(plugin);
            if (template.sentPrompts != null) {
                client.sendPacket(new PromptRequestPacket(name,
                                ChatColor.stripColor(template.text.toString(player, context)),
                                index, template.sentPrompts));
                return;
            }
            for (StringBundle command : template.commands)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.toString(player, context));
            if (template.toast != null) client.makeToast(template.toast.toString(player, context));
        }
    }

    public StatisticPacket createPlayerInfoPacket(Client client,
                                                  StatisticItemAction action) {
        List<StatisticPacket.StatisticInfo> info = new ArrayList<>();

        PlaceholderContext context = PlaceholderContext.create(plugin);

        boolean add = action == StatisticItemAction.ADD;

        if (client.hasPermission(DefaultPermission.VIEW_SERVER_INFO))
            info.add(createPlayerInfo(client, null, context, add));

        if (client.hasPermission(DefaultPermission.VIEW_PLAYERS))
            Bukkit.getOnlinePlayers().forEach(player -> info.add(createPlayerInfo(client, player, context, add)));

        return new StatisticPacket(action, info);
    }

    StatisticPacket.StatisticInfo createPlayerInfo(Client client, Player player, PlaceholderContext context, boolean add) {
        StatisticTemplate[] templates = player != null ? playerValues : serverValues;
        List<StatisticItem> items = new ArrayList<>(templates.length);
        for (StatisticTemplate template : templates) {
            if (template.permission == null || client.hasPermission(template.permission))
                items.add(template.createItem(player, context, add));
        }
        return new StatisticPacket.StatisticInfo(player == null ? null : player.getName(), items);
    }

    class StatisticTemplate {

        final StatisticPacket.StatisticType type;
        final StringBundle text;
        final StringBundle[] commands;
        final StringBundle toast;
        final IPermission permission, clickPermission;

        final DefaultPromptRequest[] prompts, sentPrompts;
        final ChildPromptRequest[] childPrompts;
        final ThreadLocal<Map<String, String>> promptValues = ThreadLocal.withInitial(HashMap::new);

        final Map<Player, StatisticItem> values = new PlayerMap<>(plugin);

        StatisticTemplate(StatisticPacket.StatisticType type, StringBundle text,
                          DefaultPromptRequest[] prompts,
                          String[] commands, StringBundle toast, IPermission permission,
                          IPermission clickPermission) {
            this.type = type;
            this.text = text;
            this.prompts = prompts;
            if (prompts != null) {
                List<DefaultPromptRequest> sentPrompts = new ArrayList<>(prompts.length);
                List<ChildPromptRequest> childPrompts = new ArrayList<>(prompts.length);
                for (DefaultPromptRequest req : prompts) {
                    if (req instanceof ChildPromptRequest)
                        childPrompts.add((ChildPromptRequest) req);
                    else sentPrompts.add(req);
                }
                this.sentPrompts = sentPrompts.toArray(new DefaultPromptRequest[sentPrompts.size()]);
                this.childPrompts = childPrompts.toArray(new ChildPromptRequest[childPrompts.size()]);
            } else this.sentPrompts = this.childPrompts = null;
            this.commands = StringBundle.parse(plugin, commands);
            this.toast = toast;
            this.permission = permission;
            this.clickPermission = clickPermission;
            if (prompts != null) {
                Function<Matcher, IPlaceholder<?>> func = matcher -> {
                    final String name = matcher.group(1);
                    return (IPlaceholder<String>) (nagger, player) -> {
                        StringBundle bundle = StringBundle.parse(nagger, promptValues.get().get(name));
                        return bundle == null ? null : bundle.toString(player);
                    };
                };
                for (StringBundle command : this.commands) command.replace(PROMPT_PATTERN, func::apply);
                if (toast != null) toast.replace(PROMPT_PATTERN, func::apply);
            }
        }

        StatisticItem createItem(Player player, PlaceholderContext context, boolean add) {
            ColoredText text = ColoredText.parse(this.text.toString(player, context), false);
            StatisticItem current = values.get(player);
            if (!add && current != null) {
                int bits = current.getModified();
                if (TYPE.test(bits)) current.setModified(bits &= ~TYPE.get());
                if (!text.equals(current.getText())) {
                    current.setText(text);
                    current.setModified(bits | TEXT.get());
                } else if (TEXT.test(bits)) current.setModified(bits & ~TEXT.get());
                return current;
            } else {
                current = new StatisticItem(
                        TYPE.get() | TEXT.get(), type, text, 0, 0, 0, 0);
                values.put(player, current);
                return current;
            }
        }
    }

    private class ProgressStatisticTemplate extends StatisticTemplate {

        private final StringBundle value, max;
        private final int progressColor, emptyColor;

        ProgressStatisticTemplate(StatisticPacket.StatisticType type, StringBundle text,
                                  DefaultPromptRequest[] prompts,
                                  String[] commands, StringBundle toast,
                                  IPermission permission, IPermission clickPermission,
                                  StringBundle value, StringBundle max,
                                  int progressColor, int emptyColor) {
            super(type, text, prompts, commands, toast, permission, clickPermission);
            this.value = value;
            this.max = max;
            this.progressColor = progressColor;
            this.emptyColor = emptyColor;
        }

        @Override
        StatisticItem createItem(Player player, PlaceholderContext context, boolean add) {
            ColoredText text = ColoredText.parse(this.text.toString(player, context), false);
            double value, max;
            try {
                value = Double.parseDouble(this.value.toString(player, context));
            } catch (NumberFormatException ex) {
                value = 0;
            }
            try {
                max = Double.parseDouble(this.max.toString(player, context));
            } catch (NumberFormatException ex) {
                max = 0;
            }
            StatisticItem current = values.get(player);
            if (!add && current != null) {
                int bits = current.getModified();
                if (TYPE.test(bits)) current.setModified(bits &= ~TYPE.get());
                if (!text.equals(current.getText())) {
                    current.setText(text);
                    current.setModified(bits |= TEXT.get());
                } else if (TEXT.test(bits)) current.setModified(bits &= ~TEXT.get());
                if (value != current.getValue()) {
                    current.setValue(value);
                    current.setModified(bits |= VALUE.get());
                } else if (VALUE.test(bits)) current.setModified(bits &= ~VALUE.get());
                if (max != current.getMax()) {
                    current.setMax(max);
                    current.setModified(bits | MAX.get());
                } else if (MAX.test(bits)) current.setModified(bits & ~MAX.get());
            } else current = new StatisticItem(255, type, text, value, max, progressColor, emptyColor);
            return current;
        }
    }

    private DefaultPromptRequest[] parsePrompts(ConfigurationSection sec) {
        if (sec == null) return null;
        List<DefaultPromptRequest> values = new ArrayList<>();
        for (String id : sec.getKeys(false)) {
            ConfigurationSection section = sec.getConfigurationSection(id);
            if (section == null) continue; // Not a section
            String typeString = section.getString("type", "text");
            if ("child".equalsIgnoreCase(typeString)) {
                String parent = section.getString("parent");
                if (parent == null) continue;
                ConfigurationSection childValues = section.getConfigurationSection("values");
                if (childValues == null) continue;
                Map<StringBundle, StringBundle> map = new HashMap<>();
                for (String key : childValues.getKeys(false)) {
                    String value = childValues.getString(key);
                    if (value != null)
                        map.put(StringBundle.parse(plugin, key), StringBundle.parse(plugin, value));
                }
                values.add(new ChildPromptRequest(id, parent.toLowerCase(Locale.US), map));
                continue;
            }
            PromptRequestPacket.PromptType type;
            try {
                type = PromptRequestPacket.PromptType.valueOf(typeString.toUpperCase(Locale.US).replace('-', '_'));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Illegal prompt type: " + typeString);
                continue;
            }
            String name = section.getString("name", id);
            DefaultPromptRequest request;
            switch (type) {
                case CHECKBOX:
                    request = new CheckBoxPromptRequest(id, name, type,
                            section.getString("checked"), section.getString("unchecked"));
                    break;
                case SELECTION:
                    List<String> val = section.getStringList("values");
                    request = new SelectionPromptRequest(id, name, type,
                            val.toArray(new String[val.size()]));
                    break;
                default:
                    request = new DefaultPromptRequest(id, name, type);
                    break;
            }
            values.add(request);
        }
        return values.isEmpty() ? null : values.toArray(new DefaultPromptRequest[values.size()]);
    }

    private class DefaultPromptRequest extends PromptRequestPacket.PromptRequest {

        final String id;

        DefaultPromptRequest(String id, String name, PromptRequestPacket.PromptType type) {
            super(name, type);
            this.id = id;
        }
    }

    private class CheckBoxPromptRequest extends DefaultPromptRequest {

        private final StringBundle checked, unchecked;

        CheckBoxPromptRequest(String id, String name, PromptRequestPacket.PromptType type,
                                     String checked, String unchecked) {
            super(id, name, type);
            this.checked = StringBundle.parse(plugin, checked == null ? "yes" : checked);
            this.unchecked = StringBundle.parse(plugin, unchecked == null ? "no" : unchecked);
        }
    }

    private class SelectionPromptRequest extends DefaultPromptRequest {

        private final String[] values;

        SelectionPromptRequest(String id, String name, PromptRequestPacket.PromptType type, String[] values) {
            super(id, name, type);
            this.values = values;
        }

        @Override
        public void write(ProtocolOutput data) throws IOException {
            super.write(data);
            data.writeByte(values.length);
            for (String str : values) data.writeString(str);
        }
    }

    private class ChildPromptRequest extends DefaultPromptRequest {

        private final String parent;
        private final Map<StringBundle, StringBundle> values;

        ChildPromptRequest(String id, String parent, Map<StringBundle, StringBundle> values) {
            super(id, id, null);
            this.parent = parent;
            this.values = values;
        }

        String get(Player player, String key, PlaceholderContext context) {
            for (Map.Entry<StringBundle, StringBundle> entry : values.entrySet())
                if (entry.getKey().toString(player, context).equals(key))
                    return entry.getValue().toString(player, context);
            return null;
        }
    }

    private StatisticTemplate[] loadConfig(MyMCLabPlugin plugin, String id,
                                           List<ConfigurationSection> list) {
        List<StatisticTemplate> values = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            ConfigurationSection section = list.get(i);

            StatisticPacket.StatisticType type;
            try {
                type = StatisticPacket.StatisticType.valueOf(
                        section.getString("type", "TEXT")
                                .toUpperCase(Locale.US).replace('-', '_'));
            } catch (IllegalArgumentException ex) {
                plugin.nag(id + " info " + (i + 1) + " has an invalid type: " + section.getString("type"));
                continue;
            }
            StringBundle text = StringBundle.parse(plugin, section.getString("text"));
            if (text == null) {
                plugin.nag(id + " info " + (i + 1) + " doesn't contain 'text'!");
                continue;
            }
            text.colorAmpersands();

            DefaultPromptRequest[] prompts = parsePrompts(section.getConfigurationSection("prompts"));
            List<String> commandsList = section.getStringList("click-commands");
            if (commandsList.isEmpty() && section.isString("click-commands"))
                commandsList.add(section.getString("click-commands"));
            String[] commands = commandsList.toArray(new String[commandsList.size()]);
            StringBundle toast = StringBundle.parse(plugin, section.getString("make-toast"));
            IPermission permission = CustomPermission.resolvePermission(section.getString("permission")),
                        clickPermission = CustomPermission.resolvePermission(section.getString("click-permission"));
            StatisticTemplate template;
            if (type != StatisticPacket.StatisticType.TEXT) {
                final StringBundle value = StringBundle.parse(plugin, section.getString("value")),
                        max = StringBundle.parse(plugin, section.getString("max"));
                if (value == null) {
                    plugin.nag(id + " info " + (i + 1) + " doesn't contain 'value'");
                    continue;
                }
                if (max == null) {
                    plugin.nag(id + " info " + (i + 1) + " doesn't contain 'max'!");
                    continue;
                }
                int progressColor, emptyColor;
                try {
                    String str = section.getString("progress-color");
                    progressColor = Integer.parseInt(str, 16); // str == null -> NFE
                    switch (str.length()) {
                        case 1:
                            progressColor = 0xFF000000 | (progressColor * 0x111111);
                            break;
                        case 2:
                            progressColor = 0xFF000000 | (progressColor * 0x010101);
                            break;
                        case 3:
                            progressColor = 0xFF000000 | (progressColor * 0x001001);
                            break;
                        case 4:
                            int a = (progressColor & 0xF000),
                                r = (progressColor & 0x0F00),
                                g = (progressColor & 0x00F0),
                                b = progressColor & 0x000F;
                            progressColor = (a << 16) | (a << 12) | (r << 12) | (r << 8) |
                                            (g << 8) | (g << 4) | (b << 4) | b;
                            break;
                        case 6:
                            progressColor |= 0xFF000000;
                            break;
                        case 8:
                            break;
                        default:
                            throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    progressColor = 0xFF0000FF;
                }
                try {
                    String str = section.getString("empty-color");
                    emptyColor = Integer.parseInt(str, 16); // str == null -> NFE
                    switch (str.length()) {
                        case 1:
                            emptyColor = 0xFF000000 | (emptyColor * 0x111111);
                            break;
                        case 2:
                            emptyColor = 0xFF000000 | (emptyColor * 0x010101);
                            break;
                        case 3:
                            emptyColor = 0xFF000000 | (emptyColor * 0x001001);
                            break;
                        case 4:
                            emptyColor = (0x11000 * (emptyColor & 0xF000))
                                    |    (0x1001 * (emptyColor & 0xFFF));
                            int a = (emptyColor & 0xF000),
                                    r = (emptyColor & 0x0F00),
                                    g = (emptyColor & 0x00F0),
                                    b = emptyColor & 0x000F;
                            emptyColor = (a << 16) | (a << 12) | (r << 12) | (r << 8) |
                                         (g <<  8) | (g <<  4) | (b <<  4) | b;
                            break;
                        case 6:
                            emptyColor |= 0xFF000000;
                            break;
                        case 8:
                            break;
                        default:
                            throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    emptyColor = 0xFF00FFFF;
                }
                template = new ProgressStatisticTemplate(type, text, prompts, commands, toast,
                        permission, clickPermission, value, max, progressColor, emptyColor);
            } else template = new StatisticTemplate(type, text, prompts, commands, toast, permission, clickPermission);
            values.add(template);
        }

        return values.toArray(new StatisticTemplate[values.size()]);
    }
}
