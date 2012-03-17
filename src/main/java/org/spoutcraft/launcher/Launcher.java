/*
 * This file is part of Spoutcraft Launcher (http://wiki.getspout.org/).
 * 
 * Spoutcraft Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Spoutcraft Launcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.spoutcraft.launcher;

import java.applet.Applet;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.spoutcraft.launcher.exception.CorruptedMinecraftJarException;
import org.spoutcraft.launcher.exception.MinecraftVerifyException;
import org.spoutcraft.launcher.exception.UnknownMinecraftException;
import org.spoutcraft.launcher.modpacks.ModOrderYML;

public class Launcher {

	@SuppressWarnings("rawtypes")
	public static Applet getMinecraftApplet() 
			throws CorruptedMinecraftJarException, MinecraftVerifyException {

		File mcBinFolder = GameUpdater.binDir;
		
		List<URL> classPath = new ArrayList<URL>();
		
		// core minecraft files
		File jinputJar = new File(mcBinFolder, "jinput.jar");
		File lwglJar = new File(mcBinFolder, "lwjgl.jar");
		File lwjgl_utilJar = new File(mcBinFolder, "lwjgl_util.jar");
		
		classPath.add(fileToURL(jinputJar));
		classPath.add(fileToURL(lwglJar));
		classPath.add(fileToURL(lwjgl_utilJar));
		
		// libraries that mods depend on
		ModpackBuild build = ModpackBuild.getSpoutcraftBuild();
		Map<String, Object> libraries = build.getLibraries();

		if (libraries != null) {
			Iterator<Entry<String, Object>> i = libraries.entrySet().iterator();
			while (i.hasNext()) {
				Entry<String, Object> lib = i.next();
				File libraryFile = new File(mcBinFolder, "lib" + File.separator + lib.getKey() + ".jar");
				classPath.add(fileToURL(libraryFile));
			}
		}

		// minecraft.jar overrides
		File[] foundJars = mcBinFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".jmod");
			}
		});
		
		List<File> orderedModJars;
		
		if (foundJars != null) {
			try {
				orderedModJars = ModOrderYML.getModOrderYML().reorderMods(Arrays.asList(foundJars));
			} catch (FileNotFoundException e) {
				orderedModJars = new ArrayList<File>(Arrays.asList(foundJars));
				Collections.reverse(orderedModJars);
			}
		} else {
			orderedModJars = new ArrayList<File>();
		}

		File minecraftJar = new File(mcBinFolder, "minecraft.jar");

		// backward compatibility with the basemods zip
		File spoutcraftJar = new File(mcBinFolder, "modpack.jar");
		
		if (spoutcraftJar.canRead()) {
			orderedModJars.add(0, spoutcraftJar);
		}

		try {
			ClassLoader classLoader;
			if (orderedModJars.size() > 0) {			
				classLoader = new MinecraftClassLoader(
						new URLClassLoader(classPath.toArray(new URL[0])), 
						minecraftJar,
						orderedModJars.toArray(new File[0]));
			} else {
				// vanilla minecraft, no need for a special classloader
				classPath.add(0, minecraftJar.toURI().toURL());
				classLoader = new URLClassLoader(classPath.toArray(new URL[0]));
			}
			
			String nativesPath = new File(mcBinFolder, "natives").getAbsolutePath();
			System.setProperty("org.lwjgl.librarypath", nativesPath);
			System.setProperty("net.java.games.input.librarypath", nativesPath);

			Class minecraftClass = classLoader.loadClass("net.minecraft.client.MinecraftApplet");
			return (Applet) minecraftClass.newInstance();
		} catch (MalformedURLException ex) {
			ex.printStackTrace();
			return null;
		} catch (ClassNotFoundException ex) {
			throw new CorruptedMinecraftJarException(ex);
		} catch (IllegalAccessException ex) {
			throw new CorruptedMinecraftJarException(ex);
		} catch (InstantiationException ex) {
			throw new CorruptedMinecraftJarException(ex);
		} catch (VerifyError ex) {
			throw new MinecraftVerifyException(ex);
		} catch (Throwable t) {
			throw new UnknownMinecraftException(t);
		}
	}
	
	private static URL fileToURL(File f) {
		try {
			return f.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new AssertionError("Never supposed to happen since converting from files.");
		}
	}
	

}
