package me.megamichiel.mymclab.bukkit;

import me.megamichiel.mymclab.api.Client;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Locale;

import static org.bukkit.ChatColor.*;

class MyMCLabCommand implements CommandExecutor {

    private final MyMCLabPlugin plugin;

    MyMCLabCommand(MyMCLabPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String str = execute(sender, args);
        if (str != null) sender.sendMessage(str.replace("<command>", label));
        return true;
    }

    private String execute(CommandSender sender, String[] args) {
        if (args.length > 0) {
            switch (args[0].toLowerCase(Locale.US)) {
                case "reload":
                    if (sender.hasPermission("mymclab.reload")) {
                        plugin.reload();
                        return GREEN + "[MyMCLab] reloaded!";
                    } else return RED + "You don't have permission for that!";
                case "clientlist":
                    if (sender.hasPermission("mymclab.clientlist")) {
                        for (Client client : plugin.getClients()) {
                            sender.sendMessage(GREEN + client.getRemoteAddress().toString() + ':');
                            long time = System.currentTimeMillis() - client.getJoinTime(),
                                 seconds = time / 1000,
                                 minutes = seconds / 60,
                                 hours = minutes / 60;
                            seconds %= 60;
                            minutes %= 60;

                            sender.sendMessage(YELLOW + "    Time since join: " + hours + ':' + minutes + ':' + seconds);
                            sender.sendMessage(AQUA + "    Bytes sent: " + client.getSentBytes());
                            sender.sendMessage(LIGHT_PURPLE + "    Bytes received: " + client.getReceivedBytes());
                        }
                    } else return RED + "You don't have permission for that!";
                    return null;
                /*case "spasm":
                    meme(20);
                    return null;*/
            }
        }
        return RED + "/<command> reload";
    }

    /*private void meme(int i) {
        if (i == 0) throw new NullPointerException();
        meme(i - 1);
    }*/
}
