package com.capito.bonty.commands;

import com.capito.bonty.Bonty;
import com.capito.bonty.utils.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BontyCommand implements CommandExecutor {
    private final Bonty plugin;
    
    public BontyCommand(Bonty plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ColorUtil.color("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ColorUtil.color("&6&lBonty &7- The ALL-IN-ONE KoTH Plugin"));
        sender.sendMessage(ColorUtil.color("&7Version: &e1.0.0"));
        sender.sendMessage(ColorUtil.color("&7Author: &eCapitoMC"));
        sender.sendMessage("");
        sender.sendMessage(ColorUtil.color("&7Use &e/koth help &7for commands"));
        sender.sendMessage(ColorUtil.color("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        return true;
    }
}