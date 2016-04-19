package minefantasy.mf2.entity.mob;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.api.armour.IArmourPenetrationMob;
import minefantasy.mf2.api.armour.IArmouredEntity;
import minefantasy.mf2.api.helpers.ArmourCalculator;
import minefantasy.mf2.api.helpers.TacticalManager;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.config.ConfigMobs;
import minefantasy.mf2.entity.mob.ai.AI_MinotaurFindTarget;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.item.list.CustomToolListMF;
import minefantasy.mf2.item.list.ToolListMF;
import minefantasy.mf2.util.MFLogUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveThroughVillage;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

public class EntityMinotaur extends EntityMob implements IArmouredEntity, IArmourPenetrationMob
{

	public int swing;
	private int grabCooldown;
	private int hitCooldownTime;
	
	public EntityMinotaur(World world)
	{
		super(world);
		this.getNavigator().setBreakDoors(true);
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(2, new EntityAIAttackOnCollide(this, EntityPlayer.class, 1.0D, false));
        this.tasks.addTask(4, new EntityAIAttackOnCollide(this, EntityLivingBase.class, 1.0D, true));
        this.tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 1.0D));
        this.tasks.addTask(7, new EntityAIWander(this, 0.5D));
        this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(8, new EntityAILookIdle(this));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
        this.targetTasks.addTask(2, new AI_MinotaurFindTarget(this, EntityPlayer.class, 0, true));
        this.targetTasks.addTask(2, new AI_MinotaurFindTarget(this, EntityLiving.class, 0, false));
        //this.targetTasks.addTask(2, new AI_MinotaurFindTarget(this, EntityLivingBase.class, 0, false).limitToBeserk());
        this.setSize(1.5F, 3F);
        this.stepHeight = 1.0F;
        this.experienceValue = 40;
        
        for (int i = 0; i < this.equipmentDropChances.length; ++i)
        {
            this.equipmentDropChances[i] = 1F;
        }
	}
	@Override
    public IEntityLivingData onSpawnWithEgg(IEntityLivingData data)
    {
		int species = MinotaurBreed.getEnvironment(this);
    	setSpecies((byte)species);
    	setBreed(MinotaurBreed.getRandomMinotaur(species));
    	return super.onSpawnWithEgg(data);
    }
	
	@Override
	public String getCommandSenderName()
    {
        return StatCollector.translateToLocal("entity." + getMinotaur().name + ".name");
    }
	@Override
	protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(40.0D);
        this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.35F);
        this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(ConfigMobs.minotaurMD);
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(ConfigMobs.minotaurHP);
    }
	@Override
	protected boolean isAIEnabled()
    {
        return true;
    }
	private static final int dataID = 12;
    protected void entityInit()
    {
        super.entityInit();
        this.dataWatcher.addObject(dataID, Byte.valueOf((byte)0));
        this.dataWatcher.addObject(dataID+1, Byte.valueOf((byte)1));
        this.dataWatcher.addObject(dataID+2, Integer.valueOf(2));
        this.dataWatcher.addObject(dataID+3, Integer.valueOf(0));
    }
    @Override
    public float getBlockPathWeight(int x, int y, int z)
    {
        return super.getBlockPathWeight(x, y, z);
    }
    @Override
    public void onUpdate()
    {
    	super.onUpdate();
    	
    	if(!worldObj.isRemote)
    	{
    		if(getRageLevel() == 0 && getAttackTarget() == null)
    		{
    			if(ticksExisted % 40 == 0)
    			{
    				forage();
    			}
    		}
    		if(ticksExisted % 20 == 0)
    		{
				if(!isBloodied())
	    		{
					if(getRageVariable() > 0)
					{
		    			addRage(-1);
		    			if(getRageVariable() > 50)
		    			{
		    				setRage(50);
		    			}
					}
	    		}
				else
				{
					heal(1);
				}
    		}
    		if(hitCooldownTime > 0)--hitCooldownTime;
    		if(grabCooldown > 0)
    		{
    			--grabCooldown;
    		}
    		if(specialAttackTime > 0)
    		{
    			--specialAttackTime;
    		}
    		if((getAttack() == (byte)1 || getAttack() == (byte)3) && specialAttackTime == 0)
    		{
    			initBasicAttack();
    		}
    		if(this.getAttackTarget() != null)
    		{
    			if(getRageLevel() > 20 && getAttack() != (byte)2 && getHeldItem() == null && rand.nextInt(100) == 0)
    			{
    				this.initHeadbutt();
    			}
    			EntityLivingBase target = getAttackTarget();
    			if(getIntLvl() > 0)//Smarter means more likely
    			{
    				if(getAttack() !=2 && rand.nextInt(10) == 0 && onGround && target instanceof EntityPlayer && ((EntityPlayer)target).isBlocking() && this.getDistanceToEntity(target) < 4F)
        			{
        				this.jump();
        				this.initPowerAttack();
        			}
    			}
    			else
    			{
	    			if(getRageLevel() > 40 && getAttack() !=2 && rand.nextInt(50) == 0 && onGround && target instanceof EntityPlayer && ((EntityPlayer)target).isBlocking() && this.getDistanceToEntity(target) < 4F)
	    			{
	    				this.jump();
	    				this.initPowerAttack();
	    			}
    			}
    			
    			if(this.ticksExisted % 20 == 0 && (getRageLevel() <= 0 || !canEntityBeSeen(getAttackTarget())))
    			{
    				int chance = 10;
    				int intel = getIntLvl();
    				if(intel > 0)
    				{
    					chance += (intel * 90);
    				}
    				if(rand.nextInt(chance) == 0)
    				{
    					this.setAttackTarget(null);
    				}
    			}
    		}
    		
    		boolean inBeserk = getRageLevel() >= 100 && isBloodied();
    		if(getAttack() == (byte)2)
    		{
    			if(!inBeserk)
    			{
    				initBasicAttack();
    			}
    			else
    			{
    				this.swingItem();
    			}
    		}
    		if(getAttack() != (byte)2 && inBeserk)
    		{
				initBeserk();
    		}
    	}
    }

    private void forage() 
    {
    	if(rand.nextInt(100) == 0)
    	{
    		int radius = 8;
    		for(int x = -radius; x <= radius; x++)
    		{
    			for(int y = -2; y <= 2; y++)
        		{
    				for(int z = -radius; z <= radius; z++)
    	    		{
    	    			int xPos = (int)posX + x;
    	    			int yPos = (int)posY + y;
    	    			int zPos = (int)posZ + z;
    	    			if(rand.nextInt(10) == 0)
    	    			{
	    	    			if(worldObj.getBlock(xPos, yPos, zPos) == BlockListMF.berryBush && worldObj.getBlockMetadata(xPos, yPos, zPos) == 0)
	    	    			{
	    	    				getNavigator().tryMoveToXYZ(xPos+0.5, yPos+1, zPos+0.5, 0.6F);
	    	    				return;
	    	    			}
	    	    			if(worldObj.getBlock(xPos, yPos, zPos) instanceof BlockCrops && worldObj.getBlockMetadata(xPos, yPos, zPos) >= 4)
	    	    			{
	    	    				getNavigator().tryMoveToXYZ(xPos+0.5, yPos+1, zPos+0.5, 0.6F);
	    	    				return;
	    	    			}
    	    			}
    	    		}	
        		}	
    		}
    		return;
    	}
    	if(worldObj.getBlock((int)posX, (int)posY, (int)posZ) == BlockListMF.berryBush && worldObj.getBlockMetadata((int)posX, (int)posY, (int)posZ) == 0)
    	{
    		worldObj.playSoundEffect(posX, posY+getEyeHeight(), posZ, "random.eat", 1.0F, 0.75F);
    		swingItem();
    		heal(10);
    		worldObj.setBlockMetadataWithNotify((int)posX, (int)posY, (int)posZ, 1, 2);
    	}
    	else if(worldObj.getBlock((int)posX, (int)posY, (int)posZ) instanceof BlockCrops && worldObj.getBlockMetadata((int)posX, (int)posY, (int)posZ) > 0)
    	{
    		worldObj.playSoundEffect(posX, posY+getEyeHeight(), posZ, "random.eat", 1.0F, 0.75F);
    		worldObj.playSoundEffect(posX, posY+getEyeHeight(), posZ, "dig.grass", 1.0F, 1.00F);
    		swingItem();
    		heal(5);
    		worldObj.setBlockToAir((int)posX, (int)posY, (int)posZ);
    	}
	}

	@Override
    protected void dropFewItems(boolean playerKill, int looting)
    {
    	int count = rand.nextInt(looting+getLootCount()+1);
    	for(int a = 0; a < count; a++)
    	{
    		Item drop = getLoot();
    		this.dropItem(drop, 1);
    	}
    	count = rand.nextInt(looting+4)+1;
    	for(int a = 0; a < count; a++)
    	{
    		this.dropItem(ComponentListMF.rawhideLarge, 1);
    	}
    	count = rand.nextInt(looting+2)+1;
    	for(int a = 0; a < count; a++)
    	{
    		this.dropItem(isBurning() ? Items.cooked_beef : Items.beef, 1);
    	}
    }

    private Item getLoot() 
    {
    	if(getSpecies() == 1)//NETHER
    	{
    		return ToolListMF.loot_sack_uc;
    	}
		return rand.nextInt(8) == 0 ? ToolListMF.loot_sack_uc : ToolListMF.loot_sack;
	}
    private int getLootCount() 
    {
		return 2;
	}
    @Override
    public boolean canAttackClass(Class enemy)
    {
    	return super.canAttackClass(enemy) && (getIntLvl() <= 0 || enemy != EntityMinotaur.class);
    }
    
	@Override
	public boolean attackEntityFrom(DamageSource source, float damage)
    {
        if (!super.attackEntityFrom(source, damage))
        {
            return false;
        }
        else
        {
        	if(source.getEntity() != null)
    		{
    			addRage(this.getIntLvl() >= 1 ? (int)(damage/2) : (int)damage);
    		}
        	
            EntityLivingBase entitylivingbase = this.getAttackTarget();

            if (entitylivingbase == null && this.getEntityToAttack() instanceof EntityLivingBase)
            {
                entitylivingbase = (EntityLivingBase)this.getEntityToAttack();
            }

            if (entitylivingbase == null && source.getEntity() instanceof EntityLivingBase)
            {
                entitylivingbase = (EntityLivingBase)source.getEntity();
            }
        }
        return true;
    }
	
	public void initBeserk()
	{
		TacticalManager.tryDisarm(this);
		//this.playSound("minefantasy2:mob.minotaur.beserk", 1.0F, 1.0F);
		setSprinting(true);
		setAttack((byte)2);
	}
	public void initPowerAttack()
	{
		setSprinting(true);
		specialAttackTime = 30;
		hitCooldownTime = 0;
		setAttack((byte)3);
	}
	public void initHeadbutt()
	{
		setSprinting(true);
		specialAttackTime = 20;
		this.hitCooldownTime = 0;
		setAttack((byte)1);
	}
	public void initBasicAttack()
	{
		setSprinting(false);
		specialAttackTime = 0;
		setAttack((byte)0);
	}
	@Override
	protected String getLivingSound()
    {
        return "minefantasy2:mob.minotaur.say";
    }

    /**
     * Returns the sound this mob makes when it is hurt.
     */
	@Override
    protected String getHurtSound()
    {
        return "minefantasy2:mob.minotaur.hurt";
    }

    /**
     * Returns the sound this mob makes on death.
     */
    @Override
    protected String getDeathSound()
    {
        return "minefantasy2:mob.minotaur.death";
    }
    @Override
    public float getSoundPitch()
    {
    	return 1.0F;
    }

    protected void func_145780_a(int p_145780_1_, int p_145780_2_, int p_145780_3_, Block p_145780_4_)
    {
        this.playSound("mob.cow.step", 0.75F, 0.9F);
    }

    protected Item getDropItem()
    {
        return ComponentListMF.hideLarge;
    }
	public double getHeadChargeAngle() 
	{
		return getAttack() == (byte)1 ? 80F : 0F;
	}
	@Override
	public float getThreshold(DamageSource src) 
	{
		float[] armour = getValueResistences();
		return ArmourCalculator.adjustACForDamage(src, getDT(), armour[0], armour[1], armour[2]);
	}
	
	@Override
	public void writeEntityToNBT(NBTTagCompound nbt)
    {
        super.writeEntityToNBT(nbt);
        
        nbt.setInteger("Attack", (int)getAttack());
        nbt.setInteger("Species", (int)getSpecies());
        nbt.setInteger("Breed", (int)getBreed());
        nbt.setInteger("Rage", (int)getRageVariable());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt)
    {
        super.readEntityFromNBT(nbt);
        
        setAttack((byte)nbt.getInteger("Attack"));
        setSpecies((byte)nbt.getInteger("Species"));
        setBreed(nbt.getInteger("Breed"));
        setRage(nbt.getInteger("Rage"));
    }
	
	private float getDT()
	{
		byte att = getAttack();
		if(att == (byte)2)
		{
			return 5F;
		}
		return 2F;
	}
	/**
	 * Cutting, Blunt, Piercing
	 */
	private float[] getValueResistences()
	{
		return new float[]{0.75F, 1F, 0.5F};
	}
	
	/**
	 * 0=Normal, 1=Headbutt, 2=Beserk, 3=Power
	 */
	public byte getAttack()
	{
		return dataWatcher.getWatchableObjectByte(dataID);
	}
    
	/**
	 * 0=Normal, 1=Headbutt, 2=Beserk, 3=Power
	 */
    public void setAttack(byte type)
    {
    	dataWatcher.updateObject(dataID, type);
    }
    
    /**
	 * 0=Normal, 1=Nether
	 */
    public byte getSpecies()
	{
		return dataWatcher.getWatchableObjectByte(dataID+1);
	}
	/**
	 * 0=Normal, 1=Nether
	 */
    public void setSpecies(byte type)
    {
    	this.isImmuneToFire = type == 1;
    	dataWatcher.updateObject(dataID+1, type);
    	
    	if(type == 1)//nether
    	{
    		this.setCurrentItemOrArmor(0, CustomToolListMF.standard_halbeard.construct("iron", "ScrapWood"));
    	}
    }
    public int getBreed()
	{
		return dataWatcher.getWatchableObjectInt(dataID+2);
	}
    public void setBreed(int type)
    {
    	dataWatcher.updateObject(dataID+2, type);
    }
    
    private int specialAttackTime;

    private float[]punch = new float[]{0F, 1F, 0F};
    private float[]headbutt = new float[]{0F, 1F, 4F};
	@Override
	public float[] getHitTraits() 
	{
		byte att = getAttack();
		if(att == (byte)1)
		{
			return headbutt;
		}
		return punch;
	}
	
	@Override
	public boolean attackEntityAsMob(Entity target)
    {
		//if(hitCooldownTime > 0)return false;
		
		if(!canEntityBeSeen(target))
		{
			return false;
		}
		if(getAttack() == 3 && motionY > 0 && target.posY < posY)
		{
			return false;
		}
		
		
		float dam = getHitDamage();
		
		if(getRageLevel() > 50)
		{
			addRage((int) (dam*1.5));
		}
		if(getAttack() == 2 && this.riddenByEntity != null && this.riddenByEntity == target)
		{
			if(target instanceof EntityLivingBase && rand.nextInt(4) == 0)
			{
				ArmourCalculator.damageArmour((EntityLivingBase) target, (int)(dam*0.25F));
			}
		}
		if(this.riddenByEntity == null)
		{
			if(grabCooldown <= 0 && rand.nextInt(100) < getGrabChance() && canPickUp(target))
			{
				target.mountEntity(this);
			}
		}
		else if(rand.nextInt(100) < ConfigMobs.minotaurTC && riddenByEntity == target)
		{
			riddenByEntity.mountEntity(null);
			grabCooldown = 60;
			TacticalManager.knockbackEntity(target, this, 4F, 1F);
		}
		if(getAttack() == 3 && fallDistance > 0 && target instanceof EntityPlayer && ((EntityPlayer)target).isBlocking() && rand.nextInt(100) < getDisarmChance())
		{
			TacticalManager.tryDisarm(this, (EntityLivingBase)target, true);
		}
		
        int i = getKnockbackBoost();

        if (target instanceof EntityLivingBase)
        {
            dam += EnchantmentHelper.getEnchantmentModifierLiving(this, (EntityLivingBase)target);
            i += EnchantmentHelper.getKnockbackModifier(this, (EntityLivingBase)target);
        }

        boolean flag = target.attackEntityFrom(new DamageSourceMobMF(getAttackType(), this), dam);

        if (flag)
        {
        	this.hitCooldownTime = getAttackSpeed();
        	this.swingItem();
            if (i > 0)
            {
                target.addVelocity((double)(-MathHelper.sin(this.rotationYaw * (float)Math.PI / 180.0F) * (float)i * 0.5F), 0.1D, (double)(MathHelper.cos(this.rotationYaw * (float)Math.PI / 180.0F) * (float)i * 0.5F));
                this.motionX *= 0.6D;
                this.motionZ *= 0.6D;
            }

            int j = EnchantmentHelper.getFireAspectModifier(this);

            if (j > 0)
            {
                target.setFire(j * 4);
            }

            if (target instanceof EntityLivingBase)
            {
                EnchantmentHelper.func_151384_a((EntityLivingBase)target, this);
            }

            EnchantmentHelper.func_151385_b(this, target);
        }

        return flag;
    }
	private boolean canPickUp(Entity target) 
	{
		float mysize = getVolume(this);
		float theirsize = getVolume(target);
		return mysize > theirsize;
	}

	private float getVolume(Entity entity)
	{
		return entity.width*entity.width*entity.height;
	}

	private int getAttackSpeed()
	{
		return 0;
		/*
		if(rage >= 100)
		{
			return 10;
		}
		if(rage > 80)
		{
			return 15;
		}
		if(rage > 50)
		{
			return 20;
		}
		if(rage > 25)
		{
			return 30;
		}
		return 35;
		*/
	}

	@Override
	public void onKillEntity(EntityLivingBase victim) 
	{
		super.onKillEntity(victim);
		if(!isBloodied())
		{
			addRage(-20);
		}
		if(getHealth() < getMaxHealth()*0.75F)
		{
			this.heal(5);
		}
	}
	
	public MinotaurBreed getMinotaur()
	{
		return MinotaurBreed.getBreed(getSpecies(), getBreed());
	}
	
	private int getDisarmChance() 
	{
		return getMinotaur().disarmChance;
	}
	private int getGrabChance() 
	{
		return (getAttack() == 2 ? getMinotaur().grabChanceBeserk : getMinotaur().grabChance);
	}
	private int getThrowChance() 
	{
		return getMinotaur().throwChance;
	}
	private String getAttackType() 
	{
		byte att = getAttack();
		if(att == (byte)1)
		{
			return "gore";
		}
		if(att == (byte)2)
		{
			return "beserk";
		}
		return "pound";
	}
	private int getKnockbackBoost() 
	{
		byte att = getAttack();
		if(att == (byte)1)
		{
			return getSpecies() == 1 ? 4 : 2;
		}
		return 0;
	}
	private float getHitDamage() 
	{
		MinotaurBreed breed = getMinotaur();
		if(getHeldItem() != null)
		{
			return Math.max(2F, breed.poundDamage-2F);//Weapon Dam
		}
		byte att = getAttack();
		if(att == (byte)1)
		{
			return breed.goreDamage;
		}
		if(att == (byte)2)
		{
			return breed.beserkDamage;
		}
		return breed.poundDamage;
	}
	
	public boolean isBloodied()
	{
		float t = this.getMinotaur().beserkThreshold/100F;
		return getHealth() < (getMaxHealth()*t);
	}
	@SideOnly(Side.CLIENT)
	public String getTexture()
	{
		String tex = "minotaur" + getMinotaur().tex;
		if(isBloodied())
		{
			tex += "_bloodied";
		}
		return tex;
	}

	public int getRageVariable()
	{
		return dataWatcher.getWatchableObjectInt(dataID+3);
	}
	public void addRage(int level)
	{
		setRage(getRageVariable() + level);
	}
	public void setRage(int level)
	{
		if(!worldObj.isRemote)
		dataWatcher.updateObject(dataID+3, Math.max(0, level));
	}
	
	
	public int getRageLevel() 
	{
		return isBloodied() ? 100 : getRageVariable();
	}
	
	private int getIntLvl()
	{
		return this.getMinotaur().intelligenceLvl;
	}
}
