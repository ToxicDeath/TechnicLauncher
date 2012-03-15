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

		///
		File spoutcraftJar = new File(mcBinFolder, "modpack.jar");
		File minecraftJar = new File(mcBinFolder, "minecraft.jar");
		File jinputJar = new File(mcBinFolder, "jinput.jar");
		File lwglJar = new File(mcBinFolder, "lwjgl.jar");
		File lwjgl_utilJar = new File(mcBinFolder, "lwjgl_util.jar");

		///
		File[] foundJars = mcBinFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".mod.jar");
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
		
		if (spoutcraftJar.canRead()) {
			orderedModJars.add(0, spoutcraftJar);
		}
		
		///
		ModpackBuild build = ModpackBuild.getSpoutcraftBuild();
		Map<String, Object> libraries = build.getLibraries();
		
		int librarycount = 4;
		if (libraries != null) {
			librarycount += libraries.size();
		}
		File[] files = new File[librarycount];

		int index = 0;
		if (libraries != null) {
			Iterator<Entry<String, Object>> i = libraries.entrySet().iterator();
			while (i.hasNext()) {
				Entry<String, Object> lib = i.next();
				File libraryFile = new File(mcBinFolder, "lib" + File.separator + lib.getKey() + ".jar");
				files[index] = libraryFile;
				index++;
			}
		}

		URL urls[] = new URL[5];

		try {
			//FIXME: aren't the urls redundant?
			urls[0] = minecraftJar.toURI().toURL();
			files[index + 0] = minecraftJar;
			urls[1] = jinputJar.toURI().toURL();
			files[index + 1] = jinputJar;
			urls[2] = lwglJar.toURI().toURL();
			files[index + 2] = lwglJar;
			urls[3] = lwjgl_utilJar.toURI().toURL();
			files[index + 3] = lwjgl_utilJar;
			urls[4] = spoutcraftJar.toURI().toURL();
			
			ClassLoader classLoader = new MinecraftClassLoader(urls, 
					ClassLoader.getSystemClassLoader(), 
					orderedModJars.toArray(new File[]{}), files);
			
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
}
