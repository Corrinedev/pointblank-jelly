package mod.pbj.item;

import javax.annotation.Nullable;
import mod.pbj.PointBlankJelly;
import mod.pbj.script.Script;

public interface ScriptHolder {
	@Nullable Script getScript();

	default boolean hasScript() {
		return getScript() != null;
	}

	default boolean hasFunction(String functionName) {
		return getScript() != null && getScript().hasFunction(functionName);
	}

	default<T> T invokeFunction(String functionName, Class<T> clazz, Object... args) {
		if (getScript() == null)
			return null;
		if (!hasFunction(functionName)) {
			PointBlankJelly.LOGGER.debug(
				"Function {} not found in script: {}", functionName, getScript().getClass().getName());
			return null;
		}
		try {
			return getScript().invokeMethod(functionName, clazz, args);
		} catch (Exception e) {
			PointBlankJelly.LOGGER.debug(
				"Failed to invoke function {} in script: {}", functionName, getScript().getClass().getName(), e);
			return null;
		}
	}

	default Object invokeFunction(String functionName, Object... args) {
		if (getScript() == null)
			return null;
		if (!hasFunction(functionName)) {
			PointBlankJelly.LOGGER.debug(
				"Function {} not found in script: {}", functionName, getScript().getClass().getName());
			return null;
		}
		try {
			return getScript().invokeMethod(functionName, args);
		} catch (Exception e) {
			PointBlankJelly.LOGGER.debug(
				"Failed to invoke function {} in script: {}", functionName, getScript().getClass().getName(), e);
			return null;
		}
	}
}
