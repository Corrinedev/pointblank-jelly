package mod.pbj.compat.iris;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.texture.InternalTextureFormat;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.properties.PackRenderTargetDirectives.RenderTargetSettings;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import net.irisshaders.iris.targets.RenderTargets;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

public class IrisShaderUtil {
	public static final String GLSL_VAR_AUX_POINTBLANK_TEX_COORD = "auxPointblankTexCoord";

	public static ProgramSource createProgramSource(
		String name,
		String vertexShaderSource,
		String fragmentShaderSource,
		ProgramSet parent,
		Supplier<ShaderProperties> shaderPropertiesSupplier,
		BlendModeOverride defaultBlendModeOverride) {
		return new ProgramSource(
			name,
			vertexShaderSource,
			null,
			null,
			null,
			fragmentShaderSource,
			parent,
			shaderPropertiesSupplier.get(),
			defaultBlendModeOverride);
	}

	public static String getResource(String resourceName) throws IOException {
		return new String(
			IOUtils.toByteArray(Objects.requireNonNull(IrisShaderUtil.class.getResourceAsStream(resourceName))),
			StandardCharsets.UTF_8);
	}

	public static String replaceRenderTargets(String template, int value) {
		String pattern = "/* RENDERTARGETS: {} */";
		String replacement = "/* RENDERTARGETS: " + value + " */";
		return template.replace(pattern, replacement);
	}

	public static int findAvailableRenderTarget(PackDirectives packDirectives, RenderTargets renderTargets) {
		Map<Integer, RenderTargetSettings> renderTargetSettings =
			packDirectives.getRenderTargetDirectives().getRenderTargetSettings();
		int result = -1;

		for (int i = 4; i < 16; ++i) {
			RenderTargetSettings settings = renderTargetSettings.get(i);
			if (settings != null && settings.shouldClear() &&
				settings.getInternalFormat() == InternalTextureFormat.RGBA && renderTargets.get(i) == null) {
				result = i;
				break;
			}
		}

		return result;
	}

	@NotNull
	public static String patchFinalPassVertexShader(String vsh, String auxPointblankTexCoordVar) {
		vsh = GlslPatcher.appendBeforeUniformDeclaration(vsh, "out vec2 " + auxPointblankTexCoordVar + ";\n");
		vsh = GlslPatcher.appendToMain(vsh, auxPointblankTexCoordVar + " = (mat4(1.0f) * vec4(UV0, 0.0f, 1.0f)).xy;\n");
		return vsh;
	}

	@NotNull
	public static String patchFinalPassFragmentShader(String fsh, String auxPointblankTexCoordVar, int auxIndex) {
		fsh = GlslPatcher.appendBeforeUniformDeclaration(fsh, "in vec2 " + auxPointblankTexCoordVar + ";\n");
		String colorTexIdentifierName = "colortex" + auxIndex;
		if (!GlslPatcher.containsIdentifier(fsh, colorTexIdentifierName)) {
			fsh =
				GlslPatcher.appendBeforeSamplerDeclaration(fsh, "uniform sampler2D " + colorTexIdentifierName + ";\n");
		}

		String outIdentifier = "iris_FragData0";
		String color = null;
		if (GlslPatcher.containsIdentifier(fsh, outIdentifier)) {
			color = "vec4 pointblankAuxColor = texture(colortex" + auxIndex + ", " + auxPointblankTexCoordVar +
					");\nif (pointblankAuxColor.a > 0.0) " + outIdentifier + " = pointblankAuxColor;\n";
		} else {
			List<String> layoutVars = GlslPatcher.extractVec3LayoutVariables(fsh);
			if (!layoutVars.isEmpty()) {
				outIdentifier = layoutVars.get(0);
				color = "vec4 pointblankAuxColor = texture(colortex" + auxIndex + ", " + auxPointblankTexCoordVar +
						");\nif (pointblankAuxColor.a > 0.0) " + outIdentifier + " = pointblankAuxColor.xyz;\n";
			}
		}

		if (color != null) {
			fsh = GlslPatcher.appendToMain(fsh, color);
		}

		return fsh;
	}
}
