package mod.pbj.client.uv;

public class StaticSpriteUVProvider implements SpriteUVProvider {
	public static final StaticSpriteUVProvider INSTANCE = new StaticSpriteUVProvider();

	private StaticSpriteUVProvider() {}

	public float[] getSpriteUV(float progress) {
		return SpriteUVProvider.getSpriteUV(0, 1, 1);
	}
}
