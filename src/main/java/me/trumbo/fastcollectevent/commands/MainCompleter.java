package me.trumbo.fastcollectevent.commands;

import me.trumbo.fastcollectevent.FastCollectEvent;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

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
            completions.addAll(Arrays.asList("delay", "top", "collect", "help"));
            if (sender.hasPermission("fce.admin")) {
                completions.addAll(Arrays.asList("reload", "score", "start", "stop"));
            }
        } else if (args[0].equalsIgnoreCase("start") && sender.hasPermission("fce.admin")) {
            if (args.length == 2) {
                List<Material> eventItems = main.getConfigManager().getEventItems();
                for (Material material : eventItems) {
                    completions.add(material.name().toLowerCase());
                }
            } else if (args.length == 3) {
                completions.addAll(Arrays.asList("10", "50", "100", "200", "500", "1000"));
            }
        } else if (args[0].equalsIgnoreCase("score") && sender.hasPermission("fce.admin")) {
            if (args.length == 2) {
                completions.addAll(Arrays.asList("plus", "minus"));
            } else if (args.length == 3) {
                main.getServer().getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            } else if (args.length == 4) {
                completions.addAll(Arrays.asList("1", "5", "10", "20", "50"));
            }
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
