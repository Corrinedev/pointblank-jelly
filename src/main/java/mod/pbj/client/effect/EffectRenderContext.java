package mod.pbj.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.pbj.client.uv.SpriteUVProvider;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class EffectRenderContext {
	private float initialAngle;
	private float progress;
	private float partialTick;
	private VertexConsumer vertexBuffer;
	private int lightColor;
	private PoseStack poseStack;
	private Camera camera;
	private Vec3 position;
	private Vec3 velocity;
	private Quaternionf rotation;
	private SpriteUVProvider spriteUVProvider;
	private MultiBufferSource bufferSource;

	public EffectRenderContext withInitialAngle(float initialAngle) {
		this.initialAngle = initialAngle;
		return this;
	}

	public EffectRenderContext withProgress(float progress) {
		this.progress = progress;
		return this;
	}

	public EffectRenderContext withPartialTick(float partialTick) {
		this.partialTick = partialTick;
		return this;
	}

	public EffectRenderContext withVertexBuffer(VertexConsumer vertexBuffer) {
		this.vertexBuffer = vertexBuffer;
		return this;
	}

	public EffectRenderContext withBufferSource(MultiBufferSource bufferSource) {
		this.bufferSource = bufferSource;
		return this;
	}

	public EffectRenderContext withLightColor(int lightColor) {
		this.lightColor = lightColor;
		return this;
	}

	public EffectRenderContext withPoseStack(PoseStack poseStack) {
		this.poseStack = poseStack;
		return this;
	}

	public EffectRenderContext withPosition(Vec3 position) {
		this.position = position;
		return this;
	}

	public EffectRenderContext withVelocity(Vec3 velocity) {
		this.velocity = velocity;
		return this;
	}

	public EffectRenderContext withVelocity(double dx, double dy, double dz) {
		this.velocity = new Vec3(dx, dy, dz);
		return this;
	}

	public EffectRenderContext withCamera(Camera camera) {
		this.camera = camera;
		return this;
	}

	public EffectRenderContext withRotation(Quaternionf rotation) {
		this.rotation = rotation;
		return this;
	}

	public EffectRenderContext withSpriteUVProvider(SpriteUVProvider spriteUVProvider) {
		this.spriteUVProvider = spriteUVProvider;
		return this;
	}

	public float getInitialAngle() {
		return this.initialAngle;
	}

	public float getProgress() {
		return this.progress;
	}

	public VertexConsumer getVertexBuffer() {
		return this.vertexBuffer;
	}

	public MultiBufferSource getBufferSource() {
		return this.bufferSource;
	}

	public int getLightColor() {
		return this.lightColor;
	}

	public PoseStack getPoseStack() {
		return this.poseStack;
	}

	public Camera getCamera() {
		return this.camera;
	}

	public Vec3 getPosition() {
		return this.position;
	}

	public Vec3 getVelocity() {
		return this.velocity;
	}

	public Quaternionf getRotation() {
		return this.rotation;
	}

	public SpriteUVProvider getSpriteUVProvider() {
		return this.spriteUVProvider;
	}

	public float getPartialTick() {
		return this.partialTick;
	}
}
