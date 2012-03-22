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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Loads classes in a jar and searches a given list of jars for overidden 
 * 	versions.
 * 
 * Extends URLClassLoader only for the sake of ModLoader, searching for resources
 *   and classes is done here to enforce JAR search order.
 *
 */
public class MinecraftClassLoader extends URLClassLoader {
	private final File 											jarToOverride;

	private ProtectionDomain								jarProtectionDomain;
	private Set<File>												modLoaderJars;

	public MinecraftClassLoader(ClassLoader parent, File jarToOverride, 
			Comparator<File> modOrder) throws IOException {
		super(new URL[0], parent);
		this.jarToOverride = jarToOverride;
		this.modLoaderJars = new TreeSet<File>(modOrder);
		
		CodeSource source = new CodeSource(jarToOverride.toURI().toURL(), (CodeSigner[])null);

		jarProtectionDomain = new ProtectionDomain(
				source, 
				null,
				this,
				null);
	
		
	}
	
	// NOTE: VerifyException is due to multiple classes of the same type in
	// jars, need to override all classloader methods to fix...

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> clazz;
			
		for (File file : modLoaderJars) {
			clazz = findClassInjar(name, file);
			if (clazz != null) { return clazz; }
		}
		
		clazz = findClassInjar(name, jarToOverride);
		if (clazz != null) { return clazz; }
		
		return super.findClass(name);
	}
	
	private Class<?> findClassInjar(String name, File file) {
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			JarFile jar = new JarFile(file);
			try {
				JarEntry entry = jar.getJarEntry(name.replace(".", "/") + ".class");
				if (entry != null) {
					InputStream is = jar.getInputStream(entry);
					try {
						byte[] buf = new byte[256];
						
						int count = is.read(buf);
						while (-1 != count) {
							byteStream.write(buf, 0, count);
							count = is.read(buf);
						}
		
						// using jarToOverride's ProtectionDomain
						byte classByte[] = byteStream.toByteArray();
						Class<?> result = defineClass(name, classByte, 0, classByte.length, 
								jarProtectionDomain);
						return result;
					} finally {
						is.close();
					}
				} 
			}finally {
				jar.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public URL findResource(String name) {
		URL url;

		for (File file : modLoaderJars) {
			url = findResourceInjar(name, file);
			if (url != null) { return url; }
		}
		
		url = findResourceInjar(name, jarToOverride);
		if (url != null) { return url; }
		
		return super.findResource(name);
	}

	private URL findResourceInjar(String name, File file) {
		try {
			JarFile jar = new JarFile(file);
			try {
				JarEntry entry = jar.getJarEntry(name);
				if (entry == null) return null;
				
				return new URL("jar:"+file.toURI().toURL()+"!/"+entry.getName());
			} finally {
				jar.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * ModLoader expects the classloader to have an addURL method.
	 */
	public void addURL(URL url) {
		if (!url.getProtocol().startsWith("file")) {
			throw new IllegalArgumentException("Only file protocol supported with url :"+url.toString());
		}
		File f = new File(url.getPath());
		if (f.isFile()) {
			String name = f.getName().toLowerCase();
			if (name.endsWith(".java") || name.endsWith(".zip")) {
				modLoaderJars.add(f);
			}
		}
		
		super.addURL(url);
	}
}
