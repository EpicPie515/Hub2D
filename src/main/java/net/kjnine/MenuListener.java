package net.kjnine;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class MenuListener implements Listener {
	
	private MenuPlugin plugin;
	
	public MenuListener(MenuPlugin pl) {
		this.plugin = pl;
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			p.teleport(plugin.getMenuLocation());
			plugin.displayMenu(p);
		}, 1L);
	}

}
