package org.spoutcraft.launcher.sandbox;

import java.security.BasicPermission;

class SandboxPermission extends BasicPermission {
	private static final long	serialVersionUID	= 1L;

	public SandboxPermission(String name) {
		super(name);
	}
}