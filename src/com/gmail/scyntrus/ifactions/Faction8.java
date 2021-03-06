package com.gmail.scyntrus.ifactions;

import java.lang.reflect.Method;

import com.gmail.scyntrus.fmob.FactionMobs;

public class Faction8 extends Faction {

	public static Method getRelationTo;
	public static Method getFlag;
	
	public com.massivecraft.factions.Faction faction;

	public Faction8 (com.massivecraft.factions.Faction faction) {
		this.faction = faction;
	}
	
	public Faction8 (Object faction) {
		this.faction = (com.massivecraft.factions.Faction) faction;
	}

	@Override
	public int getRelationTo(Faction other) {
		if (faction == null || isNone()) return 0;
		try {
			Object rel = getRelationTo.invoke(faction, ((Faction8)other).faction);
			if (rel.equals(com.massivecraft.factions.struct.Rel.ENEMY)) {
				return -1;
			} else if (rel.equals(com.massivecraft.factions.struct.Rel.NEUTRAL)) {
				return 0;
			} else if (rel.equals(com.massivecraft.factions.struct.Rel.ALLY) || rel.equals(com.massivecraft.factions.struct.Rel.MEMBER)) {
				return 1;
			}
		} catch (Exception e) {
    	    if (!FactionMobs.silentErrors) e.printStackTrace();
		}
		return 0;
	}

	@Override
	public boolean isNone() {
		if (faction == null || faction.detached()) return true;
		return faction.isNone();
	}

	@Override
	public String getName() {
		if (faction == null) return "";
		return faction.getTag();
	}

	@Override
	public double getPower() {
		if (faction == null) return 0;
		return faction.getPower();
	}

	@Override
	public boolean monstersNotAllowed() {
		if (faction == null) return false;
		try {
			return !((Boolean) getFlag.invoke(faction, com.massivecraft.factions.struct.FFlag.MONSTERS));
		} catch (Exception e) {
    	    if (!FactionMobs.silentErrors) e.printStackTrace();
		}
		return false;
	}
}
