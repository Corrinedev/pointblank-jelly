package mod.pbj.client.uv;

public interface SpriteUVProvider {
	float[] getSpriteUV(float var1);

	static float[] getSpriteUV(int spriteIndex, int rows, int columns) {
		if (spriteIndex >= 0 && spriteIndex < rows * columns) {
			int spriteRow = spriteIndex / columns;
			int spriteColumn = spriteIndex % columns;
			float uMin = (float)spriteColumn / (float)columns;
			float vMin = (float)spriteRow / (float)rows;
			float uMax = (float)(spriteColumn + 1) / (float)columns;
			float vMax = (float)(spriteRow + 1) / (float)rows;
			return new float[] {uMin, vMin, uMax, vMax};
		} else {
			throw new IllegalArgumentException("Sprite index is out of bounds");
		}
	}
}
