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
import java.io.FilePermission;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.PropertyPermission;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.spoutcraft.launcher.exception.CorruptedMinecraftJarException;
import org.spoutcraft.launcher.exception.MinecraftVerifyException;
import org.spoutcraft.launcher.exception.UnknownMinecraftException;
import org.spoutcraft.launcher.sandbox.Sandbox;

public class Launcher {
	private static final String 		BIN_PATH = "bin";
	
	public static Class<?>	mcClass	= null, appletClass = null;
	public static Field			mcField	= null;

	@SuppressWarnings("rawtypes")
	public static Applet getMinecraftApplet(File launcherPath, File[] Extralibs, 
			Comparator<File> modOrder) throws CorruptedMinecraftJarException, MinecraftVerifyException {

		File mcBinFolder = new File(launcherPath, BIN_PATH);
		List<URL> classPath = new ArrayList<URL>();
		
		// core minecraft files
		final File jinputJar = new File(mcBinFolder, "jinput.jar");
		File lwglJar = new File(mcBinFolder, "lwjgl.jar");
		File lwjgl_utilJar = new File(mcBinFolder, "lwjgl_util.jar");
		
		classPath.add(fileToURL(jinputJar));
		classPath.add(fileToURL(lwglJar));
		classPath.add(fileToURL(lwjgl_utilJar));
		
		// libraries that mods depend on

		if (Extralibs != null) {
			for (File lib : Extralibs) {
				classPath.add(fileToURL(lib));
			}
		}

		File minecraftJar = new File(mcBinFolder, "minecraft.jar");

		
		try {
			MinecraftClassLoader classLoader;
			classLoader = new MinecraftClassLoader(
					new URLClassLoader(classPath.toArray(new URL[0])), 
					minecraftJar,
					modOrder);

			
			if (Sandbox.isStarted()) {
				Sandbox sandbox = Sandbox.getSandbox();
					sandbox.addSpecial(
							Launcher.class.getProtectionDomain().getCodeSource(),
							
							// needed for launch 
							new PropertyPermission("org.lwjgl.librarypath", "write"),
							new PropertyPermission("net.java.games.input.librarypath", "write"),
							new PropertyPermission("user.dir", "write")
							);
					
				// special permissions for libs
				CodeSource source = codeSourceOf(classLoader, jinputJar);
				if (source != null) {
					sandbox.addSpecial(source, 
							new FilePermission("<<ALL FILES>>", "read,execute"));
				}
			}
			
			// backward compatibility with the basemods zip
			File spoutcraftJar = new File(mcBinFolder, "modpack.jar");
			if (spoutcraftJar.canRead()) {
				classLoader.addURL(spoutcraftJar.toURI().toURL());
			}

			String nativesPath = new File(mcBinFolder, "natives").getAbsolutePath();
			System.setProperty("org.lwjgl.librarypath", nativesPath);
			System.setProperty("net.java.games.input.librarypath", nativesPath);

			setMinecraftDirectory(classLoader, launcherPath.getAbsoluteFile());
			int a = 1;

			// some mods try to use the current directory
			System.setProperty("user.dir", launcherPath.getAbsolutePath());
			
			appletClass = classLoader.loadClass("net.minecraft.client.MinecraftApplet");
			mcClass = classLoader.loadClass("net.minecraft.client.Minecraft");
			mcField = appletClass.getDeclaredFields()[1];

			return (Applet) appletClass.newInstance();
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
		} catch (IOException e) {
			throw new UnknownMinecraftException(e);
		} 
	}
	
	private static URL fileToURL(File f) {
		try {
			return f.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new AssertionError("Never supposed to happen since converting from files.");
		}
	}

	/**
	 * Return a CodeSource for a given jar by attempting to load a class from it.
	 * 
	 * @return the CodeSource, or null on error
	 */
	private static CodeSource codeSourceOf(ClassLoader loader, File jarFile)  {
		try {
			JarFile jar = new JarFile(jarFile);
			try {
				for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements() ;) {
					JarEntry entry = e.nextElement();
					
					String name = entry.getName(); 
					if (name.endsWith(".class")) {
						Class<?> clazz = loader.loadClass(name.substring(0, name.length()-7).replace("/", "."));
						ProtectionDomain domain = clazz.getProtectionDomain();
						if (domain == null) {
							return null;
						}
						return domain.getCodeSource();
					}
				}
				throw new IOException("file specified doesn't contain classes");
			} finally {
				jar.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * This method works based on the assumption that there is only one field in
	 * Minecraft.class that is a private static File, this may change in the
	 * future and so should be tested with new minecraft versions.
	 */
	private static void setMinecraftDirectory(ClassLoader loader, File directory) throws MinecraftVerifyException {
		try {
			Class<?> clazz = loader.loadClass("net.minecraft.client.Minecraft");
			Field[] fields = clazz.getDeclaredFields();

			int fieldCount = 0;
			Field mineDirField = null;
			for (Field field : fields) {
				if (field.getType() == File.class) {
					int mods = field.getModifiers();
					if (Modifier.isStatic(mods) && Modifier.isPrivate(mods)) {
						mineDirField = field;
						fieldCount++;
					}
				}
			}
			if (fieldCount != 1) { 
				throw new MinecraftVerifyException("Cannot find directory field in minecraft"); 
			}

			mineDirField.setAccessible(true);
			mineDirField.set(null, directory);

		} catch (Exception e) {
			throw new MinecraftVerifyException(e, "Cannot set directory in Minecraft class");
		}

	}
}
