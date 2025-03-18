package me.trumbo.fastcollectevent.commands;

import me.trumbo.fastcollectevent.FastCollectEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MainCompleter implements TabCompleter {

    private final FastCollectEvent main;

    public MainCompleter(FastCollectEvent main) {
        this.main = main;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("delay");
            completions.add("top");
            completions.add("collect");
            completions.add("help");

            if (sender.hasPermission("fce.admin")) {
                completions.add("reload");
                completions.add("score");
            }

            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("score") && sender.hasPermission("fce.admin")) {
            if (args.length == 2) {

                completions.add("plus");
                completions.add("minus");
                return completions.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args.length == 3) {
                for (Player player : main.getServer().getOnlinePlayers()) {
                    completions.add(player.getName());
                }
                return completions.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args.length == 4) {
                completions.addAll(Arrays.asList("1", "5", "10", "20", "50"));
                return completions.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }
}
