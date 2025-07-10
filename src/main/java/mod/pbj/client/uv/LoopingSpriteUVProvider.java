package mod.pbj.client.uv;

public class LoopingSpriteUVProvider implements SpriteUVProvider {
	private final int totalSprites;
	private final double spriteFrameDurationMillis;
	private final int rows;
	private final int columns;
	private final long lifetimeMillis;

	public LoopingSpriteUVProvider(int rows, int columns, int spritesPerSecond, long lifetimeMillis) {
		this.rows = rows;
		this.columns = columns;
		this.totalSprites = rows * columns;
		this.spriteFrameDurationMillis = 1000.0D / (double)spritesPerSecond;
		this.lifetimeMillis = lifetimeMillis;
	}

	public int getSpriteIndex(float progress) {
		double elapsedTimeMillis = progress * (float)this.lifetimeMillis;
		int elapsedSprites = (int)(elapsedTimeMillis / this.spriteFrameDurationMillis);
		return elapsedSprites % this.totalSprites;
	}

	public float[] getSpriteUV(float progress) {
		int spriteIndex = this.getSpriteIndex(progress);
		return SpriteUVProvider.getSpriteUV(spriteIndex, this.rows, this.columns);
	}
}
