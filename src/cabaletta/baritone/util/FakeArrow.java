package baritone.util;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class FakeArrow extends Entity implements IProjectile {
    private int xTile = -1;
    private int yTile = -1;
    private int zTile = -1;
    private Block inTile;
    private int inData;
    private boolean inGround;
    /**
     * 1 if the player can pick up the arrow
     */
    public int canBePickedUp;
    /**
     * Seems to be some sort of timer for animating an arrow.
     */
    public int arrowShake;
    /**
     * The owner of this arrow.
     */
    public Entity shootingEntity;
    public boolean didIHitAnEntity = false;
    private int ticksInGround;
    private int ticksInAir;
    private double damage = 2.0D;
    /**
     * The amount of knockback an arrow applies when it hits a mob.
     */
    private int knockbackStrength;
    public FakeArrow(World worldIn, EntityLivingBase shooter, float velocity, float partialTucks) {
        super(worldIn);
        double d0 = shooter.lastTickPosX + (shooter.posX - shooter.lastTickPosX) * (double) partialTucks;
        double d1 = shooter.lastTickPosY + (shooter.posY - shooter.lastTickPosY) * (double) partialTucks;
        double d2 = shooter.lastTickPosZ + (shooter.posZ - shooter.lastTickPosZ) * (double) partialTucks;
        this.renderDistanceWeight = 10.0D;
        this.shootingEntity = shooter;
        if (shooter instanceof EntityPlayer) {
            this.canBePickedUp = 1;
        }
        this.setSize(0.5F, 0.5F);
        this.setLocationAndAngles(d0, d1 + (double) shooter.getEyeHeight(), d2, shooter.rotationYaw, shooter.rotationPitch);
        this.posX -= (double) (MathHelper.cos(this.rotationYaw / 180.0F * (float) Math.PI) * 0.16F);
        this.posY -= 0.10000000149011612D;
        this.posZ -= (double) (MathHelper.sin(this.rotationYaw / 180.0F * (float) Math.PI) * 0.16F);
        this.setPosition(this.posX, this.posY, this.posZ);
        this.motionX = (double) (-MathHelper.sin(this.rotationYaw / 180.0F * (float) Math.PI) * MathHelper.cos(this.rotationPitch / 180.0F * (float) Math.PI));
        this.motionZ = (double) (MathHelper.cos(this.rotationYaw / 180.0F * (float) Math.PI) * MathHelper.cos(this.rotationPitch / 180.0F * (float) Math.PI));
        this.motionY = (double) (-MathHelper.sin(this.rotationPitch / 180.0F * (float) Math.PI));
        this.setThrowableHeading(this.motionX, this.motionY, this.motionZ, velocity * 1.5F, 1.0F);
    }
    protected void entityInit() {
        this.dataWatcher.addObject(16, Byte.valueOf((byte) 0));
    }
    /**
     * Similar to setArrowHeading, it's point the throwable entity to a x, y, z
     * direction.
     */
    public void setThrowableHeading(double x, double y, double z, float velocity, float inaccuracy) {
        float f = MathHelper.sqrt_double(x * x + y * y + z * z);
        x = x / (double) f;
        y = y / (double) f;
        z = z / (double) f;
        //x = x + this.rand.nextGaussian() * (double) (this.rand.nextBoolean() ? -1 : 1) * 0.007499999832361937D * (double) inaccuracy;
        //y = y + this.rand.nextGaussian() * (double) (this.rand.nextBoolean() ? -1 : 1) * 0.007499999832361937D * (double) inaccuracy;
        //z = z + this.rand.nextGaussian() * (double) (this.rand.nextBoolean() ? -1 : 1) * 0.007499999832361937D * (double) inaccuracy;
        x = x * (double) velocity;
        y = y * (double) velocity;
        z = z * (double) velocity;
        this.motionX = x;
        this.motionY = y;
        this.motionZ = z;
        float f1 = MathHelper.sqrt_double(x * x + z * z);
        this.prevRotationYaw = this.rotationYaw = (float) (MathHelper.func_181159_b(x, z) * 180.0D / Math.PI);
        this.prevRotationPitch = this.rotationPitch = (float) (MathHelper.func_181159_b(y, (double) f1) * 180.0D / Math.PI);
        this.ticksInGround = 0;
    }
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean p_180426_10_) {
        this.setPosition(x, y, z);
        this.setRotation(yaw, pitch);
    }
    /**
     * Sets the velocity to the args. Args: x, y, z
     */
    public void setVelocity(double x, double y, double z) {
        this.motionX = x;
        this.motionY = y;
        this.motionZ = z;
        if (this.prevRotationPitch == 0.0F && this.prevRotationYaw == 0.0F) {
            float f = MathHelper.sqrt_double(x * x + z * z);
            this.prevRotationYaw = this.rotationYaw = (float) (MathHelper.func_181159_b(x, z) * 180.0D / Math.PI);
            this.prevRotationPitch = this.rotationPitch = (float) (MathHelper.func_181159_b(y, (double) f) * 180.0D / Math.PI);
            this.prevRotationPitch = this.rotationPitch;
            this.prevRotationYaw = this.rotationYaw;
            this.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
            this.ticksInGround = 0;
        }
    }
    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate() {
        super.onUpdate();
        if (this.prevRotationPitch == 0.0F && this.prevRotationYaw == 0.0F) {
            float f = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
            this.prevRotationYaw = this.rotationYaw = (float) (MathHelper.func_181159_b(this.motionX, this.motionZ) * 180.0D / Math.PI);
            this.prevRotationPitch = this.rotationPitch = (float) (MathHelper.func_181159_b(this.motionY, (double) f) * 180.0D / Math.PI);
        }
        BlockPos blockpos = new BlockPos(this.xTile, this.yTile, this.zTile);
        IBlockState iblockstate = this.worldObj.getBlockState(blockpos);
        Block block = iblockstate.getBlock();
        if (block.getMaterial() != Material.air) {
            block.setBlockBoundsBasedOnState(this.worldObj, blockpos);
            AxisAlignedBB axisalignedbb = block.getCollisionBoundingBox(this.worldObj, blockpos, iblockstate);
            if (axisalignedbb != null && axisalignedbb.isVecInside(new Vec3(this.posX, this.posY, this.posZ))) {
                this.inGround = true;
            }
        }
        if (this.arrowShake > 0) {
            --this.arrowShake;
        }
        if (this.inGround) {
            int j = block.getMetaFromState(iblockstate);
            if (block == this.inTile && j == this.inData) {
                ++this.ticksInGround;
                if (this.ticksInGround >= 1200) {
                    this.setDead();
                }
            } else {
                this.inGround = false;
                this.motionX *= (double) (this.rand.nextFloat() * 0.2F);
                this.motionY *= (double) (this.rand.nextFloat() * 0.2F);
                this.motionZ *= (double) (this.rand.nextFloat() * 0.2F);
                this.ticksInGround = 0;
                this.ticksInAir = 0;
            }
        } else {
            ++this.ticksInAir;
            Vec3 vec31 = new Vec3(this.posX, this.posY, this.posZ);
            Vec3 vec3 = new Vec3(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
            MovingObjectPosition movingobjectposition = this.worldObj.rayTraceBlocks(vec31, vec3, false, true, false);
            vec31 = new Vec3(this.posX, this.posY, this.posZ);
            vec3 = new Vec3(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
            if (movingobjectposition != null) {
                vec3 = new Vec3(movingobjectposition.hitVec.xCoord, movingobjectposition.hitVec.yCoord, movingobjectposition.hitVec.zCoord);
            }
            Entity entity = null;
            List<Entity> list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox().addCoord(this.motionX, this.motionY, this.motionZ).expand(1.0D, 1.0D, 1.0D));
            double d0 = 0.0D;
            for (int i = 0; i < list.size(); ++i) {
                Entity entity1 = (Entity) list.get(i);
                if (entity1.canBeCollidedWith() && (entity1 != this.shootingEntity || this.ticksInAir >= 5)) {
                    float f1 = 0.3F;
                    AxisAlignedBB axisalignedbb1 = entity1.getEntityBoundingBox().expand((double) f1, (double) f1, (double) f1);
                    MovingObjectPosition movingobjectposition1 = axisalignedbb1.calculateIntercept(vec31, vec3);
                    if (movingobjectposition1 != null) {
                        double d1 = vec31.squareDistanceTo(movingobjectposition1.hitVec);
                        if (d1 < d0 || d0 == 0.0D) {
                            entity = entity1;
                            d0 = d1;
                        }
                    }
                }
            }
            if (entity != null) {
                movingobjectposition = new MovingObjectPosition(entity);
            }
            if (movingobjectposition != null && movingobjectposition.entityHit != null && movingobjectposition.entityHit instanceof EntityPlayer) {
                EntityPlayer entityplayer = (EntityPlayer) movingobjectposition.entityHit;
                if (entityplayer.capabilities.disableDamage || this.shootingEntity instanceof EntityPlayer && !((EntityPlayer) this.shootingEntity).canAttackPlayer(entityplayer)) {
                    movingobjectposition = null;
                }
            }
            if (movingobjectposition != null) {
                if (movingobjectposition.entityHit != null) {
                    float f2 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
                    int l = MathHelper.ceiling_double_int((double) f2 * this.damage);
                    didIHitAnEntity = true;
                    /*if (this.getIsCritical()) {
                     l += this.rand.nextInt(l / 2 + 2);
                     }
                     mageSource damagesource;
                     if (this.shootingEntity == null) {
                     damagesource = DamageSource.causeArrowDamage(this, this);
                     } else {
                     damagesource = DamageSource.causeArrowDamage(this, this.shootingEntity);
                     }
                     if (this.isBurning() && !(movingobjectposition.entityHit instanceof EntityEnderman)) {
                     movingobjectposition.entityHit.setFire(5);
                     }
                     if (movingobjectposition.entityHit.attackEntityFrom(damagesource, (float) l)) {
                     if (movingobjectposition.entityHit instanceof EntityLivingBase) {
                     EntityLivingBase entitylivingbase = (EntityLivingBase) movingobjectposition.entityHit;
                     if (!this.worldObj.isRemote) {
                     entitylivingbase.setArrowCountInEntity(entitylivingbase.getArrowCountInEntity() + 1);
                     }
                     if (this.knockbackStrength > 0) {
                     float f7 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
                     if (f7 > 0.0F) {
                     movingobjectposition.entityHit.addVelocity(this.motionX * (double) this.knockbackStrength * 0.6000000238418579D / (double) f7, 0.1D, this.motionZ * (double) this.knockbackStrength * 0.6000000238418579D / (double) f7);
                     }
                     }
                     if (this.shootingEntity instanceof EntityLivingBase) {
                     EnchantmentHelper.applyThornEnchantments(entitylivingbase, this.shootingEntity);
                     EnchantmentHelper.applyArthropodEnchantments((EntityLivingBase) this.shootingEntity, entitylivingbase);
                     }
                     if (this.shootingEntity != null && movingobjectposition.entityHit != this.shootingEntity && movingobjectposition.entityHit instanceof EntityPlayer && this.shootingEntity instanceof EntityPlayerMP) {
                     ((EntityPlayerMP) this.shootingEntity).playerNetServerHandler.sendPacket(new S2BPacketChangeGameState(6, 0.0F));
                     }
                     }
                     this.playSound("random.bowhit", 1.0F, 1.2F / (this.rand.nextFloat() * 0.2F + 0.9F));
                     if (!(movingobjectposition.entityHit instanceof EntityEnderman)) {
                     this.setDead();
                     }
                     } else {
                     this.motionX *= -0.10000000149011612D;
                     this.motionY *= -0.10000000149011612D;
                     this.motionZ *= -0.10000000149011612D;
                     this.rotationYaw += 180.0F;
                     this.prevRotationYaw += 180.0F;
                     this.ticksInAir = 0;
                     }*/
                } else {
                    BlockPos blockpos1 = movingobjectposition.getBlockPos();
                    this.xTile = blockpos1.getX();
                    this.yTile = blockpos1.getY();
                    this.zTile = blockpos1.getZ();
                    IBlockState iblockstate1 = this.worldObj.getBlockState(blockpos1);
                    this.inTile = iblockstate1.getBlock();
                    this.inData = this.inTile.getMetaFromState(iblockstate1);
                    this.motionX = (double) ((float) (movingobjectposition.hitVec.xCoord - this.posX));
                    this.motionY = (double) ((float) (movingobjectposition.hitVec.yCoord - this.posY));
                    this.motionZ = (double) ((float) (movingobjectposition.hitVec.zCoord - this.posZ));
                    float f5 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
                    this.posX -= this.motionX / (double) f5 * 0.05000000074505806D;
                    this.posY -= this.motionY / (double) f5 * 0.05000000074505806D;
                    this.posZ -= this.motionZ / (double) f5 * 0.05000000074505806D;
                    //this.playSound("random.bowhit", 1.0F, 1.2F / (this.rand.nextFloat() * 0.2F + 0.9F));
                    this.inGround = true;
                    this.arrowShake = 7;
                    if (this.inTile.getMaterial() != Material.air) {
                        this.inTile.onEntityCollidedWithBlock(this.worldObj, blockpos1, iblockstate1, this);
                    }
                }
            }
            this.posX += this.motionX;
            this.posY += this.motionY;
            this.posZ += this.motionZ;
            float f3 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
            this.rotationYaw = (float) (MathHelper.func_181159_b(this.motionX, this.motionZ) * 180.0D / Math.PI);
            for (this.rotationPitch = (float) (MathHelper.func_181159_b(this.motionY, (double) f3) * 180.0D / Math.PI); this.rotationPitch - this.prevRotationPitch < -180.0F; this.prevRotationPitch -= 360.0F) {
                ;
            }
            while (this.rotationPitch - this.prevRotationPitch >= 180.0F) {
                this.prevRotationPitch += 360.0F;
            }
            while (this.rotationYaw - this.prevRotationYaw < -180.0F) {
                this.prevRotationYaw -= 360.0F;
            }
            while (this.rotationYaw - this.prevRotationYaw >= 180.0F) {
                this.prevRotationYaw += 360.0F;
            }
            this.rotationPitch = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * 0.2F;
            this.rotationYaw = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * 0.2F;
            float f4 = 0.99F;
            float f6 = 0.05F;
            if (this.isInWater()) {
                for (int i1 = 0; i1 < 4; ++i1) {
                    float f8 = 0.25F;
                    this.worldObj.spawnParticle(EnumParticleTypes.WATER_BUBBLE, this.posX - this.motionX * (double) f8, this.posY - this.motionY * (double) f8, this.posZ - this.motionZ * (double) f8, this.motionX, this.motionY, this.motionZ, new int[0]);
                }
                f4 = 0.6F;
            }
            if (this.isWet()) {
                this.extinguish();
            }
            this.motionX *= (double) f4;
            this.motionY *= (double) f4;
            this.motionZ *= (double) f4;
            this.motionY -= (double) f6;
            this.setPosition(this.posX, this.posY, this.posZ);
            this.doBlockCollisions();
        }
    }
    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    public void writeEntityToNBT(NBTTagCompound tagCompound) {
        tagCompound.setShort("xTile", (short) this.xTile);
        tagCompound.setShort("yTile", (short) this.yTile);
        tagCompound.setShort("zTile", (short) this.zTile);
        tagCompound.setShort("life", (short) this.ticksInGround);
        ResourceLocation resourcelocation = (ResourceLocation) Block.blockRegistry.getNameForObject(this.inTile);
        tagCompound.setString("inTile", resourcelocation == null ? "" : resourcelocation.toString());
        tagCompound.setByte("inData", (byte) this.inData);
        tagCompound.setByte("shake", (byte) this.arrowShake);
        tagCompound.setByte("inGround", (byte) (this.inGround ? 1 : 0));
        tagCompound.setByte("pickup", (byte) this.canBePickedUp);
        tagCompound.setDouble("damage", this.damage);
    }
    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readEntityFromNBT(NBTTagCompound tagCompund) {
        this.xTile = tagCompund.getShort("xTile");
        this.yTile = tagCompund.getShort("yTile");
        this.zTile = tagCompund.getShort("zTile");
        this.ticksInGround = tagCompund.getShort("life");
        if (tagCompund.hasKey("inTile", 8)) {
            this.inTile = Block.getBlockFromName(tagCompund.getString("inTile"));
        } else {
            this.inTile = Block.getBlockById(tagCompund.getByte("inTile") & 255);
        }
        this.inData = tagCompund.getByte("inData") & 255;
        this.arrowShake = tagCompund.getByte("shake") & 255;
        this.inGround = tagCompund.getByte("inGround") == 1;
        if (tagCompund.hasKey("damage", 99)) {
            this.damage = tagCompund.getDouble("damage");
        }
        if (tagCompund.hasKey("pickup", 99)) {
            this.canBePickedUp = tagCompund.getByte("pickup");
        } else if (tagCompund.hasKey("player", 99)) {
            this.canBePickedUp = tagCompund.getBoolean("player") ? 1 : 0;
        }
    }
}
