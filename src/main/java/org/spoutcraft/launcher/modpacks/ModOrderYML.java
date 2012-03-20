package org.spoutcraft.launcher.modpacks;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.spoutcraft.launcher.CachedYmlFile;
import org.spoutcraft.launcher.GameUpdater;
import org.spoutcraft.launcher.Util;

/**
 * Class to handle modorder.yml.
 * 
 * Ex. YML format:
 * 
 * #mods will be loaded in the order listed
 * # versions in the file name and here are separated with an underscore('_')
 * modorder:
 * 	- ModLoader
 *  - ModLoaderMP
 *  - Forge
 *  - examplemod_v1.0      #version specific names can be used and will override
 *  											# generic names
 *  - yetanothermod
 *  - examplemod
 *  - examplemod_v1.2
 */
public class ModOrderYML extends CachedYmlFile {
	public static final String			MODORDER_YML	= "modorder.yml";
	public static final File				MODORDER_FILE	= new File(GameUpdater.launcherDir, MODORDER_YML);

	private static ModOrderYML modOrder = null;
	
	/**
	 * return a singleton of ModOrderYML, this intended to be temporary as YML 
	 * 	files probably should be accessed purely as class instances in the future
	 * 
	 * @throws FileNotFoundException if the file isn't available locally 
	 * 		and download from mirror fails.
	 */
	public static ModOrderYML getModOrderYML() throws FileNotFoundException {
		if (modOrder != null) {
			return modOrder;
		}
		
		modOrder = new ModOrderYML();
		return modOrder;
	}
	
	public ModOrderYML() throws FileNotFoundException {
		super(MODORDER_YML, null, MODORDER_FILE);
	}
	
	/**
	 * Return the jars in the ideal order according modorder.yml(in reverse order 
	 *  they would be added to minecraft.jar),
	 * 
	 *  If a jar isn't in the modorder.yml, be put in the earliest positions
	 *  	(the last one to be added to minecraft.jar)
	 */
	public List<File> reorderMods(List<File> modJars) throws FileNotFoundException {
		
		List<String> order = getConfig().getStringList("modorder", new ArrayList<String>());
		
		Map<Integer, File> sorted = new TreeMap<Integer, File>(Collections.reverseOrder());
			
		int lastOrder = order.size();
		String name;
		int priority; 
		for(File modJar : modJars) {
			name = modJar.getName().replaceFirst("\\.jmod$", "");
			
			priority = order.indexOf(name);
			if (priority == -1) {
				priority = order.indexOf(name.substring(0, name.indexOf('_')));
				if (priority == -1) {
					Util.log("Warning: The jar mod '%s' is not in modorder.yml, add to ensure correct order.", name);
					priority = lastOrder;
					lastOrder++;
				}
			}
			
			sorted.put(priority, modJar);
		}
		
		assert sorted.size() == modJars.size();
		
		return new ArrayList<File>(sorted.values());
	}
}
