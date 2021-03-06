package powercrystals.minefactoryreloaded.block;

import java.util.ArrayList;
import java.util.Random;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Icon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;

import powercrystals.core.position.IRotateableTile;
import powercrystals.core.util.Util;
import powercrystals.minefactoryreloaded.MineFactoryReloadedCore;
import powercrystals.minefactoryreloaded.core.MFRUtil;
import powercrystals.minefactoryreloaded.gui.MFRCreativeTab;
import powercrystals.minefactoryreloaded.tile.TileEntityConveyor;
import powercrystals.minefactoryreloaded.tile.machine.TileEntityItemRouter;

public class BlockConveyor extends BlockContainer
{
	private String[] _names = new String []
			{ "white", "orange", "magenta", "lightblue", "yellow", "lime", "pink", "gray", "lightgray", "cyan", "purple", "blue", "brown", "green", "red", "black", "default" };
	private Icon[] _iconsActive = new Icon[_names.length];
	private Icon[] _iconsStopped = new Icon[_names.length];
	
	public BlockConveyor(int id)
	{
		super(id, Material.circuits);
		setHardness(0.5F);
		setUnlocalizedName("mfr.conveyor");
		setBlockBounds(0.0F, 0.0F, 0.0F, 0.1F, 0.1F, 0.1F);
		setCreativeTab(MFRCreativeTab.tab);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IconRegister ir)
	{
		for(int i = 0; i < _names.length; i++)
		{
			_iconsActive[i] = ir.registerIcon("powercrystals/minefactoryreloaded/" + getUnlocalizedName() + ".active." + _names[i]);
			_iconsStopped[i] = ir.registerIcon("powercrystals/minefactoryreloaded/" + getUnlocalizedName() + ".stopped." + _names[i]);
		}
	}
	
	@Override
	public Icon getBlockTexture(IBlockAccess iblockaccess, int x, int y, int z, int side)
	{
		TileEntity te = iblockaccess.getBlockTileEntity(x, y, z);
		if(te != null && te instanceof TileEntityConveyor)
		{
			int dyeColor = ((TileEntityConveyor)te).getDyeColor();
			if(dyeColor == -1) dyeColor = 16;
			if(Util.isRedstonePowered(te))
			{
				return _iconsStopped[dyeColor];
			}
			else
			{
				return _iconsActive[dyeColor];
			}
		}
		else
		{
			return _iconsStopped[_iconsStopped.length - 1];
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public Icon getIcon(int side, int meta)
	{
		return _iconsStopped[meta];
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving entity, ItemStack stack)
	{
		if(entity == null)
		{
			return;
		}
		int facing = MathHelper.floor_double((double)((entity.rotationYaw * 4F) / 360F) + 0.5D) & 3;
		if(facing == 0)
		{
			world.setBlockMetadataWithNotify(x, y, z, 1, 2);
		}
		if(facing == 1)
		{
			world.setBlockMetadataWithNotify(x, y, z, 2, 2);
		}
		if(facing == 2)
		{
			world.setBlockMetadataWithNotify(x, y, z, 3, 2);
		}
		if(facing == 3)
		{
			world.setBlockMetadataWithNotify(x, y, z, 0, 2);
		}
		
		TileEntity te = world.getBlockTileEntity(x, y, z);
		if(te != null && te instanceof TileEntityConveyor)
		{
			((TileEntityConveyor)te).setDyeColor(stack.getItemDamage() == 16 ? -1 : stack.getItemDamage());
		}
	}

	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity)
	{
		if(!(entity instanceof EntityItem || entity instanceof EntityXPOrb || (entity instanceof EntityLiving && MineFactoryReloadedCore.conveyorCaptureNonItems.getBoolean(true))))
		{
			return;
		}
		if(Util.isRedstonePowered(world.getBlockTileEntity(x, y, z)))
		{
			return;
		}
		
		TileEntity te = world.getBlockTileEntity(x, y - 1, z);
		if(!world.isRemote && entity instanceof EntityItem && te != null && te instanceof TileEntityItemRouter)
		{
			if(((TileEntityItemRouter)te).hasRouteForItem(((EntityItem)entity).getEntityItem()))
			{
				ItemStack s = ((TileEntityItemRouter)te).routeItem(((EntityItem)entity).getEntityItem()); 
				if(s == null)
				{
					entity.setDead();
					return;
				}
				else
				{
					((EntityItem)entity).setEntityItemStack(s);
				}
			}
		}
		
		double xVelocity = 0;
		double yVelocity = 0;
		double zVelocity = 0;
		
		int md = world.getBlockMetadata(x, y, z);
		
		int horizDirection = md & 0x03;
		boolean isUphill = (md & 0x04) != 0;
		boolean isDownhill = (md & 0x08) != 0;
		
		if(isUphill)
		{
			yVelocity = 0.25D;
		}
		
		if(isUphill || isDownhill)
		{
			entity.onGround = false;
		}
		
		
		if(horizDirection == 0)
		{
			xVelocity = 0.1D;
		}
		else if(horizDirection == 1)
		{
			zVelocity = 0.1D;
		}
		else if(horizDirection == 2)
		{
			xVelocity = -0.1D;
		}
		else if(horizDirection == 3)
		{
			zVelocity = -0.1D;
		}
		
		if(horizDirection == 0 || horizDirection == 2)
		{
			if(entity.posZ > z + 0.55D) zVelocity = -0.1D;
			else if(entity.posZ < z + 0.45D) zVelocity = 0.1D;
		}
		else if(horizDirection == 1 || horizDirection == 3)
		{
			if(entity.posX > x + 0.55D) xVelocity = -0.1D;
			else if(entity.posX < x + 0.45D) xVelocity = 0.1D;
		}
		
		setEntityVelocity(entity, xVelocity, yVelocity, zVelocity);
		
		if(entity instanceof EntityLiving)
		{
			((EntityLiving)entity).fallDistance = 0;
		}
		else if(entity instanceof EntityItem)
		{
			((EntityItem)entity).delayBeforeCanPickup = 40;
		}
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z)
	{
		int md = world.getBlockMetadata(x, y, z);

		if((md & 0x0C) == 0)
		{
			return AxisAlignedBB.getAABBPool().getAABB(x + 0.05F, y, z + 0.05F, (x + 1) - 0.05F, y + 0.1F, z + 1 - 0.05F);
		}
		else
		{
			return AxisAlignedBB.getAABBPool().getAABB(x + 0.2F, y, z + 0.2F, (x + 1) - 0.2F, y + 0.1F, z + 1 - 0.2F);
		}
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z)
	{
		return getCollisionBoundingBoxFromPool(world, x, y, z);
	}

	@Override
	public boolean isOpaqueCube()
	{
		return false;
	}

	@Override
	public MovingObjectPosition collisionRayTrace(World world, int i, int j, int k, Vec3 vec3d, Vec3 vec3d1)
	{
		setBlockBoundsBasedOnState(world, i, j, k);
		return super.collisionRayTrace(world, i, j, k, vec3d, vec3d1);
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess iblockaccess, int i, int j, int k)
	{
		int l = iblockaccess.getBlockMetadata(i, j, k);
		if(l >= 4 && l <= 11)
		{
			setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
		}
		else
		{
			setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
		}
	}

	@Override
	public boolean renderAsNormalBlock()
	{
		return false;
	}

	@Override
	public int getRenderType()
	{
		return MineFactoryReloadedCore.renderIdConveyor;
	}

	@Override
	public int quantityDropped(Random random)
	{
		return 1;
	}

	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z)
	{
		return canBlockStay(world, x, y, z);
	}
	
	@Override
	public boolean canBlockStay(World world, int x, int y, int z)
	{
		return world.isBlockSolidOnSide(x, y - 1, z, ForgeDirection.UP);
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer entityplayer, int side, float xOffset, float yOffset, float zOffset)
	{
		if(MFRUtil.isHoldingWrench(entityplayer))
		{
			TileEntity te = world.getBlockTileEntity(x, y, z);
			if(te != null && te instanceof IRotateableTile)
			{
				((IRotateableTile)te).rotate();
			}
		}
		return true;
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, int neighborId)
	{
		if(!canBlockStay(world, x, y, z))
		{
			world.setBlockToAir(x, y, z);
		}
	}
	
	private void setEntityVelocity(Entity e, double x, double y, double z)
	{
		e.motionX = x;
		e.motionY = y;
		e.motionZ = z;
	}

	@Override
	public boolean canProvidePower()
	{
		return true;
	}

	@Override
	public TileEntity createNewTileEntity(World world)
	{
		return new TileEntityConveyor();
	}
	
	@Override
	public ArrayList<ItemStack> getBlockDropped(World world, int x, int y, int z, int metadata, int fortune)
	{
		return new ArrayList<ItemStack>();
	}
	
	@Override
	public void breakBlock(World world, int x, int y, int z, int blockId, int meta)
	{
		TileEntity te = world.getBlockTileEntity(x, y, z);
		int dyeColor = 16;
		if(te != null && te instanceof TileEntityConveyor)
		{
			dyeColor = ((TileEntityConveyor)te).getDyeColor();
			if(dyeColor == -1) dyeColor = 16;
		}
		
		dropBlockAsItem_do(world, x, y, z, new ItemStack(blockID, 1, dyeColor));
		super.breakBlock(world, x, y, z, blockId, meta);
	}
}
