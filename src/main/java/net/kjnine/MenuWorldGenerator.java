package net.kjnine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

public class MenuWorldGenerator extends ChunkGenerator {

	private int width;
	
	public MenuWorldGenerator(int width) {
		super();
		this.width = width;
	}

	@Override
	public Location getFixedSpawnLocation(World world, Random random) {
		return new Location(world, (width % 2 == 0) ? 0 : 0.5, 1.2, 0.5);
	}
	
	@Override
	public List<BlockPopulator> getDefaultPopulators(World world) {
		return new ArrayList<BlockPopulator>();
	}
	
	@Override
	public boolean canSpawn(World world, int x, int z) {
		return true;
	}
	
	@Override
	public byte[][] generateBlockSections(World world, Random random, int x, int z, BiomeGrid biomes) {
		return new byte[world.getMaxHeight() / 16][];
	}
	
}
