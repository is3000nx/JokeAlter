package is3000nx.mod.alter;

import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.dragon.phase.PhaseList;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.stats.AchievementList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 祭壇(笑)Mod
 */
@Mod(name = SacrificialAlter.modname,
	 modid = SacrificialAlter.modid,
	 version = SacrificialAlter.version)
public class SacrificialAlter
{
	public static final String modname = "Joke-SacrificialAlter";
    public static final String modid = "joke.sacrificial.alter";
    public static final String version = "mc1.11.2-r1";

	/**
	 * ベッドをクリックした位置と祭壇の中心のオフセット.
	 */
	private static final int[][] OFFSET = {
		{0, 0}, {-1, 0}, {+1, 0}, {0, -1}, {0, +1},
	};

	/**
	 * 祭壇の上部の形状.
	 */
	private static final boolean TOP[] = {
		false,false,false,false,false,
		false, true, true, true,false,
		false, true, true, true,false,
		false, true, true, true,false,
		false,false,false,false,false,
	};

	/**
	 * 祭壇の足の部分の形状.
	 */
	private static final boolean LOW[] = {
		false,false,false,false,false,
		false,false,false,false,false,
		false,false, true,false,false,
		false,false,false,false,false,
		false,false,false,false,false,
	};

	// trueが何かのブロックで、falseが空気
	// 5x5の範囲

	/**
	 * 初期化.
	 * イベントハンドラの登録
	 */
	@EventHandler
	public void preInit(FMLPreInitializationEvent evt)
	{
		MinecraftForge.EVENT_BUS.register(this);
	}

	/**
	 * ベッドで寝た時のハンドラ.
	 *
	 * 四隅にエンドクリスタルを置いた祭壇上のベッドで寝ると
	 * スポーンする
	 */
	@SubscribeEvent
	public void onPlayerSleepInBed(PlayerSleepInBedEvent event)
	{
		EntityPlayer player = event.getEntityPlayer();
		World world = player.world;
		if (world.isRemote)
			return;
		if (world.isDaytime())
			return;
		if (player.isPlayerSleeping() || !player.isEntityAlive())
			return;
		
		if (!event.getEntityPlayer().hasAchievement(AchievementList.THE_END2))
			return;
		// 「ドラゴンのリスポーン」なので、1回倒している事が前提条件。
		// 1回倒しているかどうかのフラグは存在するけど、
		// 面倒なので、実績で代用


		// ベッドの向きを調べれば、2箇所チェックするだけで
		// 祭壇の中心が確定するが、
		// 面倒だし、
		// クリック位置と、その前後左右の5箇所調べても
		// 処理量はそんなに変わらない(はず)
		for (int i = 0; i < 5; i++) {
			BlockPos posCenter = event.getPos().add(OFFSET[i][0], 0, OFFSET[i][1]);
			if (!isAlter(world, posCenter))
				continue;

			EntityDragon dragon = new EntityDragon(world);
			dragon.getPhaseManager().setPhase(PhaseList.STRAFE_PLAYER /*HOLDING_PATTERN*/);
			dragon.setLocationAndAngles(posCenter.getX(),
										128.0,
										posCenter.getZ(),
										world.rand.nextFloat() * 360.0f,
										0.0f);
			world.spawnEntity(dragon);
			// 参考：net.minecraft.world.end.DragonFightManager.spawnDragon();

			return;
		}
	}

	/**
	 * 指定位置に条件を満たした祭壇があるか判定
	 *
	 * @param world ワールド
	 * @param posCenter 祭壇の中心
	 * @return true=祭壇
	 */
	private static boolean isAlter(World world, BlockPos posCenter)
	{
		// 中心がベッド
		IBlockState state = world.getBlockState(posCenter);
		if (!Block.isEqualTo(state.getBlock(), Blocks.BED))
			return false;

		// 祭壇の土台の形状を判定
		// 最上部が 3x3 のブロック
		// その下に中心だけブロックの足が、最低2段
		// （更にその下に地面があるかどうかは省略）
		if (!detectAlterLayer(world, posCenter, -1, TOP) ||
			!detectAlterLayer(world, posCenter, -2, LOW) ||
			!detectAlterLayer(world, posCenter, -3, LOW)) {

			return false;
		}

		// 4隅にエンドクリスタルがあるかどうか
		if (!isCrystal(world, posCenter.add( 1, 0, 1)) ||
			!isCrystal(world, posCenter.add( 1, 0,-1)) ||
			!isCrystal(world, posCenter.add(-1, 0, 1)) ||
			!isCrystal(world, posCenter.add(-1, 0,-1))) {

			return false;
		}

		return true;
	}

	/**
	 * 祭壇の土台部分の形状判定
	 *
	 * @param world ワールド
	 * @param posCenter 祭壇の中心位置
	 * @param yOffset 形状判定を行う部分の、祭壇の中心位置からのオフセット
	 * @param MAP 土台の形状
	 * @return true=合っている
	 */
	private static boolean detectAlterLayer(World world, BlockPos posCenter,
											int yOffset, boolean[] MAP)
	{
		int i = 0;
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {

				// 土台の素材は何でも可。
				// なので、空気かどうかで判定
				if (MAP[i++] == world.isAirBlock(posCenter.add(x, yOffset, z)))
					return false;
			}
		}

		return true;
	}

	/**
	 * 指定位置にエンドクリスタルがあるかどうか
	 *
	 * @param world ワールド
	 * @param pos エンドクリスタルの有無を調べてたい座標
	 * @return true=エンドクリスタルあり
	 */
	private static boolean isCrystal(World world, BlockPos pos)
	{
		AxisAlignedBB bb = new AxisAlignedBB(pos.getX(),
											 pos.getY(),
											 pos.getZ(),
											 pos.getX() + 1.0,
											 pos.getY() + 1.0,
											 pos.getZ() + 1.0);
		List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(null, bb);
		for (Entity entity : list) {
			if (entity instanceof EntityEnderCrystal)
				return true;
		}

		return false;
	}
}
