package net.kjnine;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Wool;
import org.bukkit.plugin.java.JavaPlugin;
import org.inventivetalent.mapmanager.MapManagerPlugin;
import org.inventivetalent.mapmanager.controller.MapController;
import org.inventivetalent.mapmanager.controller.MultiMapController;
import org.inventivetalent.mapmanager.manager.MapManager;
import org.inventivetalent.mapmanager.wrapper.MapWrapper;
import org.inventivetalent.playerversion.IPlayerVersion;
import org.inventivetalent.playerversion.PlayerVersion;

public class MenuPlugin extends JavaPlugin {

	private static final String RESOURCEID = "";
	
	private World menuWorld;
	private Location menuLocation;
	
	public IPlayerVersion playerVersion;
	
	public Map<Block, ItemFrame> frames = new HashMap<>();
	public ItemFrame[][] frameMatrix;

    public MapManager mapManager;
    
    private int width;
    private int height;
	
	@Override
	public void onEnable() {
		saveDefaultConfig();
		
		width = Math.max(1, getConfig().getInt("width"));
		height = Math.max(2, getConfig().getInt("height"));
		frameMatrix = new ItemFrame[width][height];
		
		playerVersion = PlayerVersion.getInstance();
		mapManager = ((MapManagerPlugin) Bukkit.getPluginManager().getPlugin("MapManager")).getMapManager();
		
		getServer().getPluginManager().registerEvents(new MenuListener(this), this);
		
		// Needs to be run sync (delaying startup) because players shouldn't join before the menu is loaded.
		setupWorld();
		setupImages();
		
		String curVer = getDescription().getVersion();
		if (getConfig().getBoolean("check-for-updates")) {
			getServer().getScheduler().runTaskAsynchronously(this, () -> {
				getLogger().info(getDescription().getName() + " v" + curVer + " checking for updates...");
				try {
					final HttpsURLConnection connection = (HttpsURLConnection) new URL("https://api.spigotmc.org/legacy/update.php?resource=" + RESOURCEID).openConnection();
	                connection.setRequestMethod("GET");
	                String updatedVersion = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
					
					if (updatedVersion.equals(curVer)) {
						getLogger().info(getDescription().getName() + " v" + curVer + " is up to date!");
					} else {
						getLogger().info(getDescription().getName() + " v" + curVer + " has a new version available, " + getDescription().getName() + " v" + updatedVersion);
						getLogger().info("Download it from https://www.spigotmc.org/resources/" + RESOURCEID + "/updates");
					}
				} catch (IOException e) {
					getLogger().warning("Failed to check for updates on " + getDescription().getName());
				}
			});
		}

		getLogger().info(getDescription().getName() + " v" + curVer + " Enabled!");
	}
	
	
	
	@SuppressWarnings("deprecation")
	private void setupWorld() {
		String wn = getConfig().getString("world-name");
		menuWorld = getServer().createWorld(WorldCreator.name(wn).environment(Environment.NORMAL).generator(new MenuWorldGenerator(width)).type(WorldType.FLAT));

		menuLocation = new Location(menuWorld, (width % 2 == 0) ? 0 : 0.5, 1.2, 0.5);
		
		int minX, minY, maxX, maxY, bminZ, bmaxZ;
		int widthEven = width - (width % 2);
		minX = (widthEven / -2);
		maxX = (widthEven / 2) + (width % 2);
		minY = 1;
		maxY = height;
		bminZ = -1;bmaxZ = 4;
		
		for(int x = minX-1; x < maxX+1; x++) {
			for(int z = bminZ; z < bmaxZ; z++) {
				for(int y = minY-1; y < maxY+1; y++) {
					if(y == minY-1 || y == maxY+1 || x == minX-1 || x == maxX+1 || z == bminZ || z == bmaxZ) {
						Block b = menuWorld.getBlockAt(x, y, z);
						b.setType(Material.WOOL);
						Wool w = new Wool(DyeColor.BLACK);
						w.setColor(DyeColor.BLACK);
						b.getState().setData(w);
						b.setData((byte) 15);
					}
				}
			}
		}
		for(int x = minX; x < maxX; x++) {
			for(int y = minY; y < maxY; y++) {
				Block b = menuWorld.getBlockAt(x, y, 3);
				ItemFrame frame = (ItemFrame) menuWorld.spawnEntity(b.getLocation(), EntityType.ITEM_FRAME);
				frame.setFacingDirection(BlockFace.NORTH);
				ItemStack map = new ItemStack(Material.MAP);
				map.setDurability((short)2);
				frame.setItem(map);
				frames.put(b, frame);
				frameMatrix[x-minX][y-minY] = frame;
			}
		}
		
	}
	
	private MapController blackMap;
	
	private MultiMapController[] defMaps;
	
	private void setupImages() {
		BufferedImage bi = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = bi.createGraphics();
		g.setBackground(Color.black);
		g.clearRect(0, 0, 128, 128);
		MapWrapper blackMapWrapper = mapManager.wrapImage(bi);
		blackMap = blackMapWrapper.getController();
		
		BufferedImage[] defImages = generateDefaultImages();
		
		for(int i = 0; i < defImages.length; i++) {
			BufferedImage arrImg = defImages[i];
			MapWrapper wrap = mapManager.wrapMultiImage(arrImg, height, width);
			defMaps[i] = (MultiMapController) wrap.getController();
		}
	}
	
	private BufferedImage[] generateDefaultImages() {
		int w = width*128, h = height*128;
		int f = Math.min(w, h) / 8;
		BufferedImage dynDefault = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = dynDefault.createGraphics();
		g.setBackground(Color.black);
		g.clearRect(0, 0, w, h);
		g.setColor(new Color(250, 80, 255));
		g.setFont(new Font("SansSerif", Font.PLAIN, f));
		FontMetrics fm = g.getFontMetrics();
		int defw = fm.stringWidth("Default");
		int menw = fm.stringWidth("Menu");
		System.out.printf("%s : %s", defw, menw);
		System.out.printf("%s : %s", defw, menw);

		g.drawString("Default", (w-defw)/2, (h-f)/2);
		g.drawString("Menu", (w-menw)/2, (h+f)/2);
		
		ColorModel cm = dynDefault.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = dynDefault.copyData(null);
		BufferedImage dynHover = new BufferedImage(cm, raster, isAlphaPremultiplied, null);
		Graphics2D gh = dynHover.createGraphics();
		
		
		g.setColor(Color.darkGray);
		g.fillRect((w-100)/2, (h+f+36)/2, 100, 36);
		gh.setColor(Color.gray);
		gh.fillRect((w-100)/2, (h+f+36)/2, 100, 36);

		g.setColor(Color.white);
		g.setFont(new Font("SansSerif", Font.BOLD, 24));
		FontMetrics fm2 = g.getFontMetrics();
		int butw = fm2.stringWidth("Button");
		g.drawString("Button", (w-butw)/2, ((h+f+36)/2)+26);
		gh.setColor(Color.white);
		gh.setFont(new Font("SansSerif", Font.BOLD, 24));
		gh.drawString("Button", (w-butw)/2, ((h+f+36)/2)+26);
		
		return new BufferedImage[] {dynDefault, dynHover};
	}
	
	public void displayMenu(Player p) {
		defMaps[0].addViewer(p);
		defMaps[0].sendContent(p);
		defMaps[0].showInFrames(p, frameMatrix);
		
		blackMap.addViewer(p);
		blackMap.sendContent(p);
		blackMap.showInHand(p, true);
	}
	
	@Override
	public void onDisable() {
		frames.values().forEach(e->e.remove());
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(command.getName().equals("hubmenu")) {
			if(!(sender instanceof Player)) {
				sender.sendMessage(p("Plugin Reloaded"));
				return true;
			}
			Player p = (Player) sender;
			if(args.length == 0) {
				p.teleport(menuLocation);
				p.sendMessage(p("Refreshing Menu Display..."));
				
				
				return true;
			}
		}
		
		return true;
	}
	
	@Override
	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
		return new MenuWorldGenerator(width);
	}
	
	public String p(String s) {
		return m("&e[Hub2D] &b" + s);
	}
	
	public String m(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}

	public World getMenuWorld() {
		return menuWorld;
	}

	public Location getMenuLocation() {
		return menuLocation;
	}
	
}
