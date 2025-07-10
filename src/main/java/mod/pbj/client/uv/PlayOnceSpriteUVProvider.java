package mod.pbj.client.uv;

import net.minecraft.util.Mth;

public class PlayOnceSpriteUVProvider implements SpriteUVProvider {
	private final int totalSprites;
	private final double spriteFrameDurationMillis;
	private final int rows;
	private final int columns;
	private final long lifetimeMillis;

	public PlayOnceSpriteUVProvider(int rows, int columns, int spritesPerSecond, long lifetimeMillis) {
		this.rows = rows;
		this.columns = columns;
		this.totalSprites = rows * columns;
		this.spriteFrameDurationMillis = 1000.0D / (double)spritesPerSecond;
		this.lifetimeMillis = lifetimeMillis;
	}

	private int getSpriteIndex(float progress) {
		double elapsedTimeMillis = progress * (float)this.lifetimeMillis;
		return Mth.clamp((int)(elapsedTimeMillis / this.spriteFrameDurationMillis), 0, this.totalSprites);
	}

	public float[] getSpriteUV(float progress) {
		int spriteIndex = this.getSpriteIndex(progress);
		return spriteIndex >= this.totalSprites ? null
												: SpriteUVProvider.getSpriteUV(spriteIndex, this.rows, this.columns);
	}
}
