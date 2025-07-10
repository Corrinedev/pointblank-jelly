package mod.pbj.client.uv;

import java.util.Random;

public class RandomSpriteUVProvider implements SpriteUVProvider {
	private final Random random = new Random();
	private final int totalSprites;
	private final int rows;
	private final int columns;
	private final int seed;
	private final long frameDurationMillis;
	private final long lifetimeMillis;

	public RandomSpriteUVProvider(int rows, int columns, int spritesPerSecond, long lifetimeMillis) {
		this.rows = rows;
		this.columns = columns;
		this.totalSprites = rows * columns;
		this.frameDurationMillis = (long)(1000.0D / (double)spritesPerSecond);
		this.seed = this.random.nextInt();
		this.lifetimeMillis = lifetimeMillis;
	}

	public float[] getSpriteUV(float progress) {
		double elapsedTimeMillis = progress * (float)this.lifetimeMillis;
		int elapsedTimeSeed = this.seed + (int)(elapsedTimeMillis / (double)this.frameDurationMillis);
		this.random.setSeed(elapsedTimeSeed);
		int spriteIndex = this.random.nextInt(this.totalSprites);
		return SpriteUVProvider.getSpriteUV(spriteIndex, this.rows, this.columns);
	}
}
