package com.gmail.scyntrus.fmob.mobs;

import net.minecraft.server.v1_8_R1.AttributeInstance;
import net.minecraft.server.v1_8_R1.Block;
import net.minecraft.server.v1_8_R1.BlockPosition;
import net.minecraft.server.v1_8_R1.DamageSource;
import net.minecraft.server.v1_8_R1.EntityCreature;
import net.minecraft.server.v1_8_R1.EntityHuman;
import net.minecraft.server.v1_8_R1.EntityLiving;
import net.minecraft.server.v1_8_R1.EntityPlayer;
import net.minecraft.server.v1_8_R1.EntityPotion;
import net.minecraft.server.v1_8_R1.EntityProjectile;
import net.minecraft.server.v1_8_R1.EntityWitch;
import net.minecraft.server.v1_8_R1.EnumMonsterType;
import net.minecraft.server.v1_8_R1.GenericAttributes;
import net.minecraft.server.v1_8_R1.Item;
import net.minecraft.server.v1_8_R1.ItemStack;
import net.minecraft.server.v1_8_R1.MathHelper;
import net.minecraft.server.v1_8_R1.MobEffectList;
import net.minecraft.server.v1_8_R1.NBTTagCompound;
import net.minecraft.server.v1_8_R1.PathfinderGoal;
import net.minecraft.server.v1_8_R1.PathfinderGoalArrowAttack;
import net.minecraft.server.v1_8_R1.PathfinderGoalFloat;
import net.minecraft.server.v1_8_R1.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_8_R1.PathfinderGoalRandomLookaround;
import net.minecraft.server.v1_8_R1.PathfinderGoalRandomStroll;
import net.minecraft.server.v1_8_R1.World;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R1.util.UnsafeList;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.metadata.FixedMetadataValue;

import com.gmail.scyntrus.fmob.FactionMob;
import com.gmail.scyntrus.fmob.FactionMobs;
import com.gmail.scyntrus.fmob.ReflectionManager;
import com.gmail.scyntrus.fmob.Utils;
import com.gmail.scyntrus.ifactions.Faction;
import com.gmail.scyntrus.ifactions.Factions;

public class Mage extends EntityWitch implements FactionMob {
	
	public Location spawnLoc = null;
	public Faction faction = null;
	public String factionName = "";
    public EntityLiving attackedBy = null;
    public EntityLiving target = null;
	public static String typeName = "Mage";
	public static float maxHp = 20;
	public static Boolean enabled = true;
	public static double powerCost = 0;
	public static double moneyCost = 0;
	public static double range = 16;
	public static int drops = 0;
	private int retargetTime = 0;
	private double moveSpeed;
	
	public double poiX=0, poiY=0, poiZ=0;
	public String order = "poi";
	
	public Mage(World world) {
		super(world);
		this.forceDie();
	}
	
	public Mage(Location spawnLoc, Faction faction) {
		super(((CraftWorld) spawnLoc.getWorld()).getHandle());
		this.setSpawn(spawnLoc);
		this.setFaction(faction);
		Utils.giveColorArmor(this);
	    this.persistent = true;
	    this.fireProof = false;
	    this.canPickUpLoot = false;
	    this.moveSpeed = FactionMobs.mobSpeed;
	    getAttributeInstance(GenericAttributes.d).setValue(this.moveSpeed);
	    getAttributeInstance(GenericAttributes.maxHealth).setValue(maxHp);
	    this.setHealth(maxHp);
	    this.S = 1.5F;
		this.setEquipment(0, new ItemStack((Item)Item.d("potion"), 1, 8204));
	    
	    if (ReflectionManager.good_Navigation_Distance) {
		    try {
				AttributeInstance e = (AttributeInstance) ReflectionManager.navigation_Distance.get(this.getNavigation());
				e.setValue(FactionMobs.mobNavRange);
			} catch (Exception e) {
	    	    if (!FactionMobs.silentErrors) e.printStackTrace();
			}
	    }
	    if (ReflectionManager.good_PathfinderGoalSelector_GoalList) {
		    try {
		    	ReflectionManager.pathfinderGoalSelector_GoalList.set(this.goalSelector, new UnsafeList<PathfinderGoal>());
		    	ReflectionManager.pathfinderGoalSelector_GoalList.set(this.targetSelector, new UnsafeList<PathfinderGoal>());
			} catch (Exception e) {
	    	    if (!FactionMobs.silentErrors) e.printStackTrace();
			}
	    }
	    
	    this.goalSelector.a(1, new PathfinderGoalFloat(this));
	    this.goalSelector.a(2, new PathfinderGoalArrowAttack(this, 1.0, 60, 10.0F));
	    this.goalSelector.a(2, new PathfinderGoalRandomStroll(this, 1.0));
	    this.goalSelector.a(3, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
	    this.goalSelector.a(3, new PathfinderGoalRandomLookaround(this));
	    this.getBukkitEntity().setMetadata("CustomEntity", new FixedMetadataValue(FactionMobs.instance, true));
	    this.getBukkitEntity().setMetadata("FactionMob", new FixedMetadataValue(FactionMobs.instance, true));
	}

	@Override
	public void m() {
		super.m();
		if (--retargetTime < 0) {
			retargetTime = 20;
			if (this.getGoalTarget() == null || !this.getGoalTarget().isAlive()) {
				this.findTarget();
			} else {
				double dist = Utils.dist3D(this.locX, this.getGoalTarget().locX, this.locY, this.getGoalTarget().locY, this.locZ, this.getGoalTarget().locZ);
				if (dist > range) {
					this.findTarget();
				} else if (dist > 1.5) {
					this.findCloserTarget();
				}
			}
			if (this.getGoalTarget() == null) {
				if (this.order.equals("home") || this.order == null || this.order.equals("")) {
					this.getNavigation().a(this.spawnLoc.getX(), this.spawnLoc.getY(), this.spawnLoc.getZ(), 1.0);
					this.order = "home";
					return;
				} else if (this.order.equals("poi")) {
					this.getNavigation().a(this.poiX, this.poiY, this.poiZ, 1.0);
					return;
				} else if (this.order.equals("wander")) {
					return;
				} else if (this.order.equals("phome")) {
					this.getNavigation().a(this.spawnLoc.getX(), this.spawnLoc.getY(), this.spawnLoc.getZ(), FactionMobs.mobPatrolSpeed);
					if (Utils.dist3D(this.locX,this.spawnLoc.getX(),this.locY,this.spawnLoc.getY(),this.locZ,this.spawnLoc.getZ()) < 1) {
						this.order = "ppoi";
					}
					return;
				} else if (this.order.equals("ppoi")) {
					this.getNavigation().a(poiX, poiY, poiZ, FactionMobs.mobPatrolSpeed);
					if (Utils.dist3D(this.locX,this.poiX,this.locY,this.poiY,this.locZ,this.poiZ) < 1) {
						this.order = "phome";
					}
					return;
				} else if (this.order.equals("path")) {
					this.getNavigation().a(poiX, poiY, poiZ, 1.0);
					if (Utils.dist3D(this.locX,this.poiX,this.locY,this.poiY,this.locZ,this.poiZ) < 1) {
						this.order = "home";
					}
					return;
				}
			}
		}
		return;
	}
	
	private void setSpawn(Location loc) {
		spawnLoc = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());
		this.setPosition(loc.getX(), loc.getY(), loc.getZ());
		this.setPoi(loc.getX(),loc.getY(),loc.getZ());
		this.order = "home";
	}

    @Override
    public EntityLiving findCloserTarget() {
        if (this.attackedBy != null) {
            if (this.attackedBy.isAlive() 
                    && this.attackedBy.world.getWorldData().getName().equals(this.world.getWorldData().getName())
                    && Utils.FactionCheck(this.attackedBy, this.faction) < 1) {
                double dist = Utils.dist3D(this.locX, this.attackedBy.locX, this.locY, this.attackedBy.locY, this.locZ, this.attackedBy.locZ);
                if (dist < 16) {
                    this.setTarget(this.attackedBy);
                    return this.attackedBy;
                } else if (dist > 32) {
                    this.attackedBy = null;
                }
            } else {
                this.attackedBy = null;
            }
        }
        Location thisLoc;
        double thisDist;
        for (org.bukkit.entity.Entity e : this.getBukkitEntity().getNearbyEntities(1.5, 1.5, 1.5)) {
            if (e.isDead() || !(((CraftEntity) e).getHandle() instanceof EntityLiving))
                continue;
            EntityLiving entity = (EntityLiving) ((CraftEntity) e).getHandle();
            if (Utils.FactionCheck(entity, faction) == -1) {
                thisLoc = e.getLocation();
                thisDist = Math.sqrt(Math.pow(this.locX-thisLoc.getX(),2) + Math.pow(this.locY-thisLoc.getY(),2) + Math.pow(this.locZ-thisLoc.getZ(),2));
                if (thisDist < 1.5) {
                    if (((CraftLivingEntity) this.getBukkitEntity()).hasLineOfSight(e)) {
                        this.setTarget(entity);
                        return entity;
                    }
                }
            }
        }
        return null;
    }
    
    public void findTarget() {
        EntityLiving found = this.findCloserTarget();
        if (found != null) {
            return;
        }
        double dist = range;
        Location thisLoc;
        double thisDist;
        for (org.bukkit.entity.Entity e : this.getBukkitEntity().getNearbyEntities(range, range, range)) {
            if (e.isDead() || !(((CraftEntity) e).getHandle() instanceof EntityLiving))
                continue;
            EntityLiving entity = (EntityLiving) ((CraftEntity) e).getHandle();
            if (Utils.FactionCheck(entity, faction) == -1) {
                thisLoc = e.getLocation();
                thisDist = Math.sqrt(Math.pow(this.locX-thisLoc.getX(),2) + Math.pow(this.locY-thisLoc.getY(),2) + Math.pow(this.locZ-thisLoc.getZ(),2));
                if (thisDist < dist) {
                    if (((CraftLivingEntity) this.getBukkitEntity()).hasLineOfSight(e)) {
                        found = entity;
                        dist = thisDist;
                    }
                }
            }
        }
        this.setTarget(found);
        return;
    }
    
    @Override
    public boolean damageEntity(DamageSource damagesource, float i) {
        boolean out = super.damageEntity(damagesource, i);
        if (!out)
            return out;
        EntityLiving damager;
        if (damagesource.getEntity() instanceof EntityLiving) {
            damager = (EntityLiving) damagesource.getEntity();
        } else if (damagesource.getEntity() instanceof EntityProjectile) {
            damager = ((EntityProjectile) damagesource.getEntity()).getShooter();
        } else {
            return out;
        }
        switch (Utils.FactionCheck(damager, this.faction)) {
        case 1:
            this.findTarget();
            if (damager instanceof EntityPlayer) {
                this.lastDamageByPlayerTime = 0;
            }
            break;
        case 0:
        case -1:
            if (damager instanceof EntityLiving) {
                this.attackedBy = damager;
                this.setTarget(damager);
            } else if (damagesource.getEntity() instanceof EntityProjectile) {
                EntityProjectile p = (EntityProjectile) damagesource.getEntity();
                this.attackedBy = p.getShooter();
                this.setTarget(p.getShooter());
            } else {
                this.findTarget();
            }
            break;
        }
        return out;
    }
	
	@Override
	public boolean canSpawn() {
		return true;
	}

	@Override
	public Faction getFaction() {
		if (this.faction == null) {
			this.setFaction(Factions.getFactionByName(this.world.getWorldData().getName(),factionName));
		}
		if (this.faction == null) {
			this.forceDie();
			System.out.println("[Error] Found and removed factionless faction mob");
		}
		return this.faction;
	}

	public void setFaction(Faction faction) {
		if (faction == null) return;
		this.faction = faction;
		if (faction.isNone()) this.forceDie();
		this.factionName = faction.getName();
		if (FactionMobs.displayMobFaction) {
			this.setCustomName(ChatColor.YELLOW + this.factionName + " " + typeName);
			this.setCustomNameVisible(true);
		}
	}

    @Override
    public void setTarget(EntityLiving entity) {
        this.target = entity;
        if (entity instanceof EntityLiving) {
            this.setGoalTarget((EntityLiving) entity);
        } else if (entity == null) {
            this.setGoalTarget(null);
        }
        if (this.getGoalTarget() != null && !this.getGoalTarget().isAlive()) {
            this.setGoalTarget(null);
        }
    }
    
    @Override
    public void setGoalTarget(EntityLiving entityliving, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        if (this.target instanceof EntityLiving && this.target.isAlive()) {
            super.setGoalTarget(this.target, EntityTargetEvent.TargetReason.CUSTOM, false);
        } else {
            super.setGoalTarget(null, EntityTargetEvent.TargetReason.CUSTOM, false);
        }
    }
	
	@Override
	public void updateMob() {
		this.setFaction(Factions.getFactionByName(this.world.getWorldData().getName(),factionName));
		if (this.faction == null || this.faction.isNone()) {
			this.forceDie();
			return;
		}
		if (this.target instanceof EntityLiving && this.target.isAlive()) {
			this.setGoalTarget((EntityLiving) this.target);
		} else {
			this.findTarget();
		}
		Utils.giveColorArmor(this);
	}

	@Override
	public String getTypeName() {
		return typeName;
	}

	@Override
	public Location getSpawn() {
		return this.spawnLoc;
	}

	@Override
	public double getlocX() {
		return this.locX;
	}

	@Override
	public double getlocY() {
		return this.locY;
	}

	@Override
	public double getlocZ() {
		return this.locZ;
	}

    @Override
    protected String z() { //TODO: Update name on version change
        return FactionMobs.sndBreath;
    }

    @Override
    protected String bn() { //TODO: Update name on version change
        return FactionMobs.sndHurt;
    }

    @Override
    protected String bo() { //TODO: Update name on version change
        return FactionMobs.sndDeath;
    }

    @Override
    protected void a(BlockPosition blockposition, Block block) { //TODO: Update name on version change
        makeSound(FactionMobs.sndStep, 0.15F, 1.0F);
    }

	@Override
	public Boolean getEnabled() {
		return enabled;
	}

	@Override
	public double getPowerCost() {
		return powerCost;
	}

	@Override
	public double getMoneyCost() {
		return moneyCost;
	}
	
	@Override
	public boolean isTypeNotPersistent() {
		return false;
	}
	
	@Override
	public double getPoiX() {
		return this.poiX;
	}
	
	@Override
	public double getPoiY() {
		return this.poiY;
	}
	
	@Override
	public double getPoiZ() {
		return this.poiZ;
	}
	
	@Override
	public void setOrder(String order) {
		this.order = order;
	}
	
	@Override
	public void setPoi(double x, double y, double z) {
		this.poiX = x;
		this.poiY = y;
		this.poiZ = z;
	}
	
	@Override
	public String getOrder() {
		return this.order;
	}
	
	@Override
	public EntityCreature getEntity() {
		return this;
	}
	
	@Override
	public String getFactionName() {
		if (this.factionName == null) this.factionName = "";
		return this.factionName;
	}
	
	@Override
	public void die() {
		if (this.getHealth() <= 0) {
			super.die();
			this.setHealth(0);
			this.setEquipment(0, null);
			this.setEquipment(1, null);
			this.setEquipment(2, null);
			this.setEquipment(3, null);
			this.setEquipment(4, null);
			if (FactionMobs.mobList.contains(this)) {
				FactionMobs.mobList.remove(this);
			}
		}
	}

	@Override
	public void forceDie() {
		this.setHealth(0);
		this.die();
	}

	@Override
	public boolean c(NBTTagCompound nbttagcompound) {
		return false;
	}

	@Override
	public boolean d(NBTTagCompound nbttagcompound) {
		return false;
	}
	
	@Override
	public void f(NBTTagCompound nbttagcompound) {
		this.die();
	}

	@Override
	public void clearAttackedBy() {
		if (this.target == this.attackedBy) {
			this.setTarget(null);
		}
		this.attackedBy = null;
	}
	
	@Override
	public EnumMonsterType getMonsterType() {
		return EnumMonsterType.UNDEFINED;
	}

	@Override
	public int getDrops() {
		return drops;
	}
	
	@Override
    public boolean softAgro(EntityLiving entity) {
		if (this.attackedBy == null 
				&& entity instanceof EntityLiving
				&& entity.isAlive()) {
			this.attackedBy = entity;
			this.setTarget(entity);
			return true;
		}
		return false;
	}
	
	@Override
	public void setHealth(float f) {
		this.datawatcher.watch(6, Float.valueOf(MathHelper.a(f, 0.0F, maxHp)));
	}
	
	@Override
	public void s_() {
		if (this.getHealth() > 0) {
			this.dead = false;
		}
		this.ak = false;
		super.s_();
	}

	public void a(EntityLiving paramEntityLiving, float paramFloat) {  //TODO: Update name on version change
		if (n()) //TODO: Update name on version change
			return;

		EntityPotion localEntityPotion = new EntityPotion(this.world, this, 32732);
		localEntityPotion.pitch -= -20.0F;
		double d1 = paramEntityLiving.locX + paramEntityLiving.motX - this.locX;
		double d2 = paramEntityLiving.locY + paramEntityLiving.getHeadHeight() - 1.100000023841858D - this.locY;
		double d3 = paramEntityLiving.locZ + paramEntityLiving.motZ - this.locZ;
		float f = MathHelper.sqrt(d1 * d1 + d3 * d3);

		if ((f >= 8.0F) && (!paramEntityLiving.hasEffect(MobEffectList.SLOWER_MOVEMENT)))
			localEntityPotion.setPotionValue(32698);
		else if ((paramEntityLiving.getHealth() >= 8.0F) && (!paramEntityLiving.hasEffect(MobEffectList.POISON)) 
				&& (paramEntityLiving.getMonsterType() != EnumMonsterType.UNDEAD)
				&& (paramEntityLiving.getMonsterType() != EnumMonsterType.ARTHROPOD))
			localEntityPotion.setPotionValue(32660);
		else if ((f <= 3.0F) && (!paramEntityLiving.hasEffect(MobEffectList.WEAKNESS)) && (this.random.nextFloat() < 0.25F)) {
			localEntityPotion.setPotionValue(32696);
		}
		else if (paramEntityLiving.getMonsterType() == EnumMonsterType.UNDEAD) {
			localEntityPotion.setPotionValue(32696);
		}

		localEntityPotion.shoot(d1, d2 + f * 0.2F, d3, 0.75F, 8.0F);

		this.world.addEntity(localEntityPotion);
	}
}
