package com.gmail.scyntrus.fmob;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.gmail.scyntrus.ifactions.Factions;

public class FactionListener2 implements Listener {
	
	FactionMobs plugin;
	
	public FactionListener2(FactionMobs plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onFactionRename(com.massivecraft.factions.event.EventFactionsNameChange e) {
		String oldName = e.getFaction().getName();
		String newName = e.getNewName();
		FactionMobs.factionColors.put(newName, 
				FactionMobs.factionColors.containsKey(oldName) ? 
						FactionMobs.factionColors.remove(oldName) : 
							10511680);
		plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
			
			String oldName;
			String newName;
			
			public Runnable init(String oldName, String newName) {
				this.oldName = oldName;
				this.newName = newName;
				return this;
			}
			
			public void run() {
				for (FactionMob fmob : FactionMobs.mobList) {
					if (fmob.getFactionName().equals(oldName)) {
						fmob.setFaction(Factions.getFactionByName(fmob.getSpawn().getWorld().getName(), newName));
					}
				}
			}
		}.init(oldName,  newName), 0);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onFactionDisband(com.massivecraft.factions.event.EventFactionsDisband e) {
		String factionName = e.getFaction().getName();
		for (int i = FactionMobs.mobList.size()-1; i >= 0; i--) {
			if (FactionMobs.mobList.get(i).getFactionName().equals(factionName)) {
				FactionMobs.mobList.get(i).forceDie();
			}
		}
	}
}
