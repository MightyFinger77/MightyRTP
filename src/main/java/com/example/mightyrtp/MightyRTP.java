package com.example.mightyrtp;

import com.example.mightyrtp.commands.RTPCommand;
import com.example.mightyrtp.commands.RTPTabCompleter;
import com.example.mightyrtp.commands.ReloadCommand;
import com.example.mightyrtp.managers.ConfigManager;
import com.example.mightyrtp.managers.MessageManager;
import com.example.mightyrtp.managers.CooldownManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MightyRTP extends JavaPlugin {
    
    private static MightyRTP instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private CooldownManager cooldownManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        cooldownManager = new CooldownManager(this);
        
        // Load configurations
        configManager.loadConfig();
        messageManager.loadMessages();
        
        // Register commands
        getCommand("rtp").setExecutor(new RTPCommand(this));
        getCommand("rtp").setTabCompleter(new RTPTabCompleter());
        getCommand("rtp-reload").setExecutor(new ReloadCommand(this));
        
        getLogger().info("MightyRTP 1.0.4 has been enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("MightyRTP 1.0.4 has been disabled!");
    }
    
    public static MightyRTP getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
}
