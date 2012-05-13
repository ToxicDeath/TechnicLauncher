package org.spoutcraft.launcher.sandbox;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.security.AccessController;


public class SandBoxAgent {
	private static final SandboxPermission	REDEFINE_PERM = new SandboxPermission("redefineClasses");
  private static Instrumentation 					instrumentation;
  
  public static void agentmain(String agentArgs, Instrumentation inst) {
  	SandBoxAgent.instrumentation = inst;
  }

  static void redefineClasses(ClassDefinition ...defs) throws Exception {
  	AccessController.checkPermission(REDEFINE_PERM);
  	
  	SandBoxAgent.instrumentation.redefineClasses(defs);
  }
}
