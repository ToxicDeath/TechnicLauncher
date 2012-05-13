package org.spoutcraft.launcher.modpacks;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.util.config.Configuration;
import org.spoutcraft.launcher.GameUpdater;
import org.spoutcraft.launcher.YmlUtils;

public class ModLibraryYML {

	public static final String			MODLIBRARY_YML	= "modlibrary.yml";
	public static final File				modLibraryYML		= new File(GameUpdater.launcherDir, MODLIBRARY_YML);

	private static volatile boolean	updated					= false;
	private static final Object			key							= new Object();

	private static Configuration 		config	 				= null;

	public static void updateModLibraryYML() {
		if (updated) { return; }
		synchronized (key) {
			YmlUtils.downloadRelativeYmlFile(ModLibraryYML.MODLIBRARY_YML);
			updated = true;
		}
	}

	private static void parseFile() {
		config = new Configuration(modLibraryYML);
		
		
		
	}

	
	public static Configuration getModLibraryYML() {
		updateModLibraryYML();
		Configuration config = new Configuration(modLibraryYML);
		config.load();
		return config;
	}

	private static class Mod {
		public String name;
		public String version;
		
		public Mod(String name, String version) {
			this.name = name;
			this.version = version;
		}

		@Override
		public int hashCode() {
			return 31 * name.hashCode() + version.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return (obj instanceof Mod) && 
					this.name.equals(((Mod)obj).name) &&
					this.version.equals(((Mod)obj).version);
		}
		
	}
	
	private static class DependInfo {
		private Mod mod;
		private Set<Mod> depends = new HashSet<Mod>();
		private Set<Mod> revDepends = new HashSet<Mod>();
		
		public DependInfo(Mod mod) {
			this.mod = mod;
		}

		public void addDepend(Mod depend) {
			depends.add(depend);
		}
		
		public void addRevDepend(Mod revDepend) {
			revDepends.add(revDepend);
		}

		public boolean isDepend(Mod mod) {
			return depends.contains(mod);
		}

		public boolean isRevDepend(Mod mod) {
			return revDepends.contains(mod);
		}
	}
	
	private static class DependMap implements Comparator<Mod> {
		Map<Mod, Set<Mod>> depends = new HashMap<Mod, Set<Mod>>();
		
		public void addDepend(Mod mod, Mod dependency) {
			Set<Mod> deps; 
			if (depends.containsKey(mod)) {
				deps = depends.get(mod);
			} else {
				deps = new HashSet<Mod>();
				depends.put(mod, deps);
			}
			
			deps.add(dependency);
		}
		

		private boolean has(Map<Mod, Set<Mod>> map, Mod lookupMod, Mod hasMod) {
			if (depends.containsKey(lookupMod)) {
				return depends.get(lookupMod).contains(hasMod);
			}
			return false;
		}
		
		@Override
		public int compare(Mod o1, Mod o2) {
			if (has(depends, o1, o2)) return 1;
			if (has(depends, o2, o1)) return -1;
			return 0;
		}
	}
}
