package org.spoutcraft.launcher.sandbox;

import java.io.File;
import java.io.FilePermission;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.BasicPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.logging.LoggingPermission;

/** 
 * A java sandbox.
 * 
 * After this class is instantiated, the program runs in a scurity based sandbox.
 * All file access is denied outside of the basePath except for special 
 * permissions intended for lirbaries.
 * 
 * All the magic is done through an extension of java.security.Policy class.
 * 
 * @author _sir_maniac
 *
 */
public class Sandbox {
	private static final Permission								SPECIAL_PERM = new SandboxPermission("addSpecial");
	private static final Permission								PREFIX_PERM = new SandboxPermission("addPrefix");
	
	private static Sandbox 												instance = null; 
	
	private Policy																parent;
	private PermissionCollection									corePerms;
	private File																	basePath;
	private PermissionCollection									basePerms;

	private Map<File, PermissionCollection>				prefixes = new HashMap<File, PermissionCollection>();
	private Map<CodeSource, PermissionCollection>	specialPerms = new HashMap<CodeSource, PermissionCollection>();
	
	
	/**
	 * return instance of the sandbox.
	 * @return
	 */
	public static Sandbox getSandbox() {
			return instance; 
	}

	public static boolean isStarted() {
		return instance != null;
	}

	/**
	 * Start the VM-wide sandbox, note that this method can only be called once.
	 * 
	 * @param basePath the sandbox path, no files can be accessed outside of this.
	 * @param basePerms default permissions for core path
	 * 			(Overridden in any prefixes). permissions here can have read 
	 *       access to specified locations, this is to allow for access to 
	 *       libraries and resources for the executed app.  Be sure to add 
	 *       additional prefixes to prevent other parts access.
	 * @return the newly created Sandbox instance
	 */
	public static Sandbox startSandbox(File basePath, Permission... basePerms) {
		// be strict for security reasons
		if (instance != null) {
			throw new SecurityException("Only one call to startSandbox allowed");
		}
		
		instance = new Sandbox(basePath, basePerms);
		return instance;
	}
	
	protected Sandbox(File basePath, Permission... basePerms) {
		if (basePath == null) throw new NullPointerException("arg basePath");

		parent = Policy.getPolicy();
		this.basePath = basePath;
		
		// give this codesource access to itself, and access to SandboxPolicy
		ProtectionDomain thisDomain;
		thisDomain = SandboxPolicy.class.getProtectionDomain();
		if (thisDomain != null) {
			addPriviledged(thisDomain.getCodeSource());
		}

		// the caller of this library is also priviledged
		ProtectionDomain callerDomain = Thread.currentThread()
				.getStackTrace()[1].getClass().getProtectionDomain();
		if (callerDomain != null && thisDomain != null  
				&& !thisDomain.equals(callerDomain)) {
			addPriviledged(callerDomain.getCodeSource());
		}
		
		File javaHome = new File(System.getProperty("java.home"));
		
		corePerms = new Permissions();
		//WARNING: this expects the java default permissions to be included 
		addToCollection(basePath, corePerms, parent.getPermissions(thisDomain));
		
		// always read the core java libraries
		corePerms.add(new FilePermission(javaHome.getAbsolutePath()+File.separator+"-", "read,execute,readlink"));

		corePerms.add(new PropertyPermission("*", "read"));
		corePerms.add(new LoggingPermission("control", ""));
		
		corePerms.add(new RuntimePermission("exitVM"));
		corePerms.add(new RuntimePermission("shutdownHooks"));
		corePerms.add(new RuntimePermission("setIO"));
		corePerms.add(new RuntimePermission("modifyThread"));
		corePerms.add(new RuntimePermission("stopThread"));
		corePerms.add(new RuntimePermission("modifyThreadGroup"));
		
		//corePerms.add(new SecurityPermission("*"));

		this.basePerms = new Permissions();
		this.basePerms.add(new FilePermission(basePath.getAbsolutePath(), "read"));
		this.basePerms.add(new FilePermission(basePath.getAbsolutePath()+File.separator+"-", "*"));

		// base permission allows 
		for (Permission perm : basePerms) {
			if (perm instanceof FilePermission) {
				String actions = perm.getActions();
				if (actions.contains("write") || 
						actions.contains("delete")) {
					continue;
				}
			}
			this.basePerms.add(perm);
		}
		
		Policy.setPolicy(new SandboxPolicy());
		System.setSecurityManager(new SecurityManager());
	}
	
	private void addPriviledged(CodeSource source) {
		
		File location;
		try {
			location = new File(source.getLocation().toURI());
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(
					String.format("codeSource location needs to be a file : %s",
							source.getLocation()), e);
		}
		
		// give it read access to it's codeSource
		List<Permission> perms = new ArrayList<Permission>();
		if (location.isFile()) {
			perms.add(new FilePermission(location.getAbsolutePath(), "read"));
		} else {
			perms.add(new FilePermission(location.getAbsolutePath(), "read"));
			perms.add(new FilePermission(location.getAbsolutePath()+File.separator+"-", "read"));
		}
		
		perms.add(new SandboxPermission("*"));
		
		this.addSpecial(source, perms.toArray(new Permission[0]));
	}
	
	/**
	 * Add a set of special permissions for a given classpath.  Designed to support
	 * special overrides needed for specific libraries.
	 * 
	 * @param codesource
	 * @param specialPerms
	 */
	public void addSpecial(CodeSource source, Permission... specialPerms) {
		AccessController.checkPermission(SPECIAL_PERM);
		
		if (source == null) throw new NullPointerException("codesource");

		PermissionCollection dstPerms; 
		
		if (this.specialPerms.containsKey(source)) {
			dstPerms = this.specialPerms.get(source);
		} else {
			dstPerms = new Permissions();
			this.specialPerms.put(source, dstPerms);
		}
		
		for (Permission perm : specialPerms) {
			dstPerms.add(perm);
		}
	}
	
	/**
	 * Adds a prefix within the sandbox, and limits them to that prefix, 
	 *   any filepermissions that would violate the new sandbox prefix are silently 
	 *   removed.
	 *   
	 * @param otherPerms additional permmisions for this sandbox, file 
	 * 	permissions outside of prefix are ignored.
	 *  
	 */
	public void addPrefix(File prefix, String prefixActions, Permission... otherPerms) {
		AccessController.checkPermission(PREFIX_PERM);

		PermissionCollection dstPerms;
		if (prefixes.containsKey(prefix)) {
			dstPerms = prefixes.get(prefix);
		} else {
			dstPerms = new Permissions();
			dstPerms.add(new FilePermission(prefix.getAbsolutePath(), "read"));
			dstPerms.add(new FilePermission(prefix.getAbsolutePath()+File.pathSeparator+"-", 
					prefixActions));
			prefixes.put(prefix, dstPerms);
		}
		
		addToCollection(prefix, dstPerms, otherPerms);
	}
	
	/**
	 * Add given permissions to a collection unless it violates the sandbox path
	 * @param dest
	 * @param src
	 */
	private static void addToCollection(File basePath, 
			PermissionCollection dst, PermissionCollection src) {
		Permission perm;
		for (Enumeration<Permission> e = src.elements(); e.hasMoreElements();) {
			perm = e.nextElement();
			if (perm instanceof FilePermission &&
					!perm.getName().startsWith(basePath.getAbsolutePath())) {
				continue;
			}
			dst.add(perm);
		}
	}

	private static void addToCollection(File basePath, 
			PermissionCollection dst, Permission[] src) {
		for (Permission perm : src) {
			if (perm instanceof FilePermission &&
					!perm.getName().startsWith(basePath.getAbsolutePath())) {
				continue;
			}
			dst.add(perm);
		}
	}
	
	
	private static void addToCollection(PermissionCollection dst, PermissionCollection src) {
		Permission perm;
		for (Enumeration<Permission> e = src.elements(); e.hasMoreElements();) {
			perm = e.nextElement();
			dst.add(perm);
		}
	}

	private static void addToCollection(PermissionCollection dst, Permission[] src) {
		for (Permission perm : src) {
			dst.add(perm);
		}
	}
	
	/**
	 * A Sandbox policy that limits all file access to within the sandbox path.
	 *
	 */
	private class SandboxPolicy extends Policy {
		
		public SandboxPolicy() {
		}		


		@Override
		public PermissionCollection getPermissions(CodeSource codesource) {

			Permissions result = new Permissions();
			addPermsForSource(result, codesource);
			return result;
		}

		@Override
		public PermissionCollection getPermissions(ProtectionDomain domain) {
			CodeSource codesource = domain.getCodeSource();
			
			PermissionCollection result = new Permissions();
			File prefix = addPermsForSource(result, codesource);
			addToCollection(prefix, result, domain.getPermissions());
			
			return result;
		}

		@Override
		public boolean implies(ProtectionDomain domain, Permission permission) {
			return getPermissions(domain).implies(permission);
		}
		
		/**
		 * returns the most restrictive matching prefix in <code>prefixes</code>, 
		 *   if no match is found, <code>null</code> is returned
		 */
		private File findPrefix(CodeSource codesource) {
			try {
				File location = new File(codesource.getLocation().toURI()).getAbsoluteFile();

				File prefix = location;
				while((prefix = prefix.getParentFile()) != null) {
					if (prefixes.containsKey(prefix)) {
						return prefix;
					}
				}
			} catch (URISyntaxException ignore) {
			}
			
			return null;
		}
		
		/**
		 * Add permissions to the passed col for the given CodeSource 
		 * @return the prefix matching codesource(basePath if nothing else)
		 */
		private File addPermsForSource(PermissionCollection col, CodeSource codesource) {
//		// not right(I think)
//		if (codesource.getLocation().getPath().startsWith(javaHome.getAbsolutePath())) {
//			return parent.getPermissions(codesource);
//		}
		
		addToCollection(col, corePerms);
		
		File prefix = null;
		try {
			if (codesource != null) {
				File location = new File(codesource.getLocation().toURI());
				if (specialPerms.containsKey(location)) {
					addToCollection(col, specialPerms.get(location));
				}
				
				prefix = findPrefix(codesource);
			} 
		} catch (URISyntaxException e) {
		}
		
		
		if (prefix != null && prefixes.containsKey(prefix)) {
			addToCollection(prefix, col, prefixes.get(prefix));
			return prefix;
		} else {
			addToCollection(col, basePerms);
			return basePath;
		}
	}
		
		

		
	}

	private static class SandboxPermission extends BasicPermission {
		private static final long	serialVersionUID	= 1L;

		public SandboxPermission(String name) {
			super(name);
		}
	}
}
