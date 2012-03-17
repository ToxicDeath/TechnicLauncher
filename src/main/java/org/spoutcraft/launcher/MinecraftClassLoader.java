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
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Loads classes in a jar and searches a given list of jars for overidden 
 * 	versions.
 * 
 *
 */
public class MinecraftClassLoader extends ClassLoader {
	private final File 											jarToOverride;
	private final File[]										overrideJars;

	private ProtectionDomain								jarProtectionDomain;
	private Set<String>											overrideClasses = new HashSet<String>();

	public MinecraftClassLoader(ClassLoader parent, File jarToOverride, File[] overrideJars) throws IOException {
		super(parent);
		this.jarToOverride = jarToOverride;
		this.overrideJars = overrideJars;
		
		CodeSource source = new CodeSource(jarToOverride.toURI().toURL(), (CodeSigner[])null);
		jarProtectionDomain = new ProtectionDomain(
				source, 
				Policy.getPolicy().getPermissions(source),
				this,
				null);
		
		JarFile jar = new JarFile(jarToOverride);
		try {
			String name;
			for(Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
				name = entries.nextElement().getName();
				if (name.endsWith(".class")) {
					overrideClasses.add(name.substring(0, name.length()-6).replace('/', '.'));
				}
			}
		} finally {
			jar.close();
		}
	}
	
	private Class <?> resolveOnExit(Class<?> clazz, boolean resolve) {
		if (resolve) {
			this.resolveClass(clazz);
		}
		return clazz;
	}

	// NOTE: VerifyException is due to multiple classes of the same type in
	// jars, need to override all classloader methods to fix...

	@Override
	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class<?> clazz = null;

		clazz = findLoadedClass(name);
		if(clazz != null) { return clazz; }

		if (overrideClasses.contains(name)) {
			for (File file : overrideJars) {
				clazz = findClassInjar(name, file);
				if (clazz != null) { return resolveOnExit(clazz, resolve); }
			}
			clazz = findClassInjar(name, jarToOverride);
			assert clazz != null;
			return resolveOnExit(clazz, resolve);
		}

		try {
			return getParent().loadClass(name);
		} catch (ClassNotFoundException e) {
			return resolveOnExit(findClass(name), resolve);
		}
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
}
