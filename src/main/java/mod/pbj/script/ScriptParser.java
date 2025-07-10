package mod.pbj.script;

import java.io.Reader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.logging.Logger;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ScriptParser {
	public static final HashMap<Thread, Object> shell = new HashMap<>();
	public static final PBJImporter scope = new PBJImporter();
	public static final Logger LOGGER = Logger.getLogger(ScriptParser.class.getName());
	public static final HashMap<ResourceLocation, Script> SCRIPTCACHE = new HashMap<>();

	public static Object shell() {
		// Thread currentThread = Thread.currentThread();
		// if (!shell.containsKey(currentThread)) {
		//     Object Object = Object.enter();
		//     Object.initStandardObjects();
		//     Object.setApplicationClassLoader(PointBlankJelly.class.getClassLoader());
		//     Object.setLanguageVersion(Object.VERSION_ES6);
		//     shell.put(currentThread, Object);
		// }
		return new Object(); // shell.get(currentThread);
	}

	public static Object getScript(Path pathFromRun) {
		// try {
		//     Script parse = new Script(pathFromRun.toString(), shell().compileReader(new
		//     FileReader(FMLPaths.GAMEDIR.get().resolve(pathFromRun).toFile()), pathFromRun.toString(), 1, null),
		//     shell().initStandardObjects(scope, true)); System.out.println("Script Parsed: " + parse); return parse;
		// } catch (IOException e) {
		//     LOGGER.severe("Failed to parse script: " + pathFromRun);
		//     throw new RuntimeException(e);
		// }
		return new Object();
	}

	public static Object getScript(Reader reader, String scriptName) {
		// try {
		//     return new Script(scriptName, shell().compileReader(reader, scriptName, 1, null),
		//     shell().initStandardObjects(scope, true));
		// } catch (IOException e) {
		//     throw new RuntimeException(e);
		// }
		return new Object();
	}

	public static Object cacheScript(Path toScript, ResourceLocation scriptId) {
		// if (SCRIPTCACHE.containsKey(scriptId)) {
		//     return SCRIPTCACHE.get(scriptId);
		// }
		// try {
		//     Script script = new Script(scriptId.getPath(), shell().compileReader(new
		//     FileReader(FMLPaths.GAMEDIR.get().resolve(toScript).toFile()), scriptId.getPath(), 1, null),
		//     shell().initStandardObjects(scope, true)); SCRIPTCACHE.put(scriptId, script); return script;
		// } catch (IOException e) {
		//     LOGGER.severe("Failed to parse script: " + toScript);
		//     throw new RuntimeException(e);
		// }
		return new Object();
	}

	public static Object cacheScript(Reader scriptReader, ResourceLocation scriptId) {
		// if (SCRIPTCACHE.containsKey(scriptId)) {
		//     return SCRIPTCACHE.get(scriptId);
		// }
		// try {
		//     Script script = new Script(scriptId.getPath(),shell().compileReader(scriptReader, scriptId.getPath(), 1,
		//     null), shell().initStandardObjects(scope, true)); SCRIPTCACHE.put(scriptId, script); return script;
		// } catch (Exception e) {
		//     LOGGER.severe("Failed to parse script from zip: " + scriptId);
		//     throw new RuntimeException(e);
		// }
		return new Object();
	}

	public static void runScripts(IEventBus modEventBus) {
		// for (Script staticScript : SCRIPTCACHE.values()) {
		//     staticScript.script().exec(shell(), staticScript.scope());
		// }
	}

	@OnlyIn(Dist.CLIENT)
	public static class ClientScriptParser {
		public static final HashMap<ResourceLocation, Script> SCRIPTCACHE = new HashMap<>();

		public static void cacheClientScript(Path scriptFile, ResourceLocation resourceLocation) {
			// try {
			// SCRIPTCACHE.put(resourceLocation, shell.parse(scriptFile.toFile()));
			//} catch (IOException e) {
			//    throw new RuntimeException(e);
			//}
		}
	}
}
