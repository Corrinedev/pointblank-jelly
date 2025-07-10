package mod.pbj.compat.iris;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlslPatcher {
	public static final Pattern END_OF_MAIN_MARKER = Pattern.compile("\\}[^}]*$");
	public static final Pattern UNIFORM_MARKER = Pattern.compile("\\buniform\\b");
	public static final Pattern UNIFORM_SAMPLER_MARKER = Pattern.compile("uniform\\s+sampler2D\\s+\\S+;");
	public static final Pattern COLORTEX_MARKER = Pattern.compile("\\bcolortex\\d+\\b");
	public static final Pattern LAYOUT_VEC3_VAR_MARKER =
		Pattern.compile("layout\\s*\\(\\s*location\\s*=\\s*0\\s*\\)\\s*out\\s+vec3\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*;");

	public static String injectBeforeRegex(String glslSource, Pattern pattern, String customString) {
		Matcher matcher = pattern.matcher(glslSource);
		StringBuffer result = new StringBuffer();
		if (matcher.find()) {
			matcher.appendReplacement(result, customString + matcher.group());
		}

		matcher.appendTail(result);
		return result.toString();
	}

	public static String appendToMain(String glslSource, String content) {
		return injectBeforeRegex(glslSource, END_OF_MAIN_MARKER, content);
	}

	public static String appendBeforeSamplerDeclaration(String glslSource, String content) {
		return injectBeforeRegex(glslSource, UNIFORM_SAMPLER_MARKER, content);
	}

	public static String appendBeforeUniformDeclaration(String glslSource, String content) {
		return injectBeforeRegex(glslSource, UNIFORM_MARKER, content);
	}

	public static boolean containsIdentifier(String input, String identifier) {
		String escapedIdentifier = Pattern.quote(identifier);
		String regex = "\\b" + escapedIdentifier + "\\b";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(input);
		return matcher.find();
	}

	public static List<String> extractVec3LayoutVariables(String glslSource) {
		Matcher matcher = LAYOUT_VEC3_VAR_MARKER.matcher(glslSource);
		ArrayList<String> variableNames = new ArrayList<>();

		while (matcher.find()) {
			variableNames.add(matcher.group(1));
		}

		return variableNames;
	}
}
