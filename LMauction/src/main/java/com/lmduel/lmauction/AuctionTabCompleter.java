package com.lmduel.lmauction;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AuctionTabCompleter implements TabCompleter {

    private final ItemTranslator translator;

    public AuctionTabCompleter(ItemTranslator translator) {
        this.translator = translator;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("menu", "search", "searsh", "sell", "sellinv", "claim", "view", "help"));
            if (sender.hasPermission("lmauction.admin")) {
                subs.add("reload");
                subs.add("fillout");
                subs.add("clearfake");
            }
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length >= 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("search") || sub.equals("searsh") || sub.equals("sell") || sub.equals("sellinv")) {
                String query = args[args.length - 1].toLowerCase();
                if (translator != null) {
                    List<String> list = translator.getTranslations();
                    if (list != null) {
                        return list.stream()
                                .filter(name -> name.toLowerCase().startsWith(query))
                                .limit(20)
                                .collect(Collectors.toList());
                    }
                }
            }
        }

        return new ArrayList<>();
    }
}