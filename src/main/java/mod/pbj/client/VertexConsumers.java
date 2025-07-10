package mod.pbj.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Function;
import org.joml.Matrix4f;

public class VertexConsumers {
	public static Function<VertexConsumer, VertexConsumer> PARTICLE = ParticleVertexConsumer::new;
	public static Function<VertexConsumer, VertexConsumer> ENTITY = ColorTexLightmapWrapper::new;

	public VertexConsumers() {}

	private abstract static class VertexConsumerWrapper implements VertexConsumer {
		protected Matrix4f transform;
		protected float u;
		protected float v;
		protected double x;
		protected double y;
		protected double z;
		protected int r;
		protected int g;
		protected int b;
		protected int a;
		protected int o1;
		protected int o2;
		protected int uv1;
		protected int uv2;
		protected VertexConsumer vertexConsumer;

		VertexConsumerWrapper(VertexConsumer vertexConsumer) {
			this.vertexConsumer = vertexConsumer;
		}

		public VertexConsumer vertex(Matrix4f transform, float x, float y, float z) {
			this.transform = transform;
			this.x = x;
			this.y = y;
			this.z = z;
			return this;
		}

		public VertexConsumer vertex(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
			return this;
		}

		public VertexConsumer color(int r, int g, int b, int a) {
			this.r = r;
			this.g = g;
			this.b = b;
			this.a = a;
			return this;
		}

		public VertexConsumer uv(float u, float v) {
			this.u = u;
			this.v = v;
			return this;
		}

		public VertexConsumer overlayCoords(int o1, int o2) {
			this.o1 = o1;
			this.o2 = o2;
			return this;
		}

		public VertexConsumer uv2(int uv1, int uv2) {
			this.uv1 = uv1;
			this.uv2 = uv2;
			return this;
		}

		public VertexConsumer normal(float p_86005_, float p_86006_, float p_86007_) {
			throw new UnsupportedOperationException();
		}

		public abstract void endVertex();

		public void defaultColor(int p_166901_, int p_166902_, int p_166903_, int p_166904_) {
			throw new UnsupportedOperationException();
		}

		public void unsetDefaultColor() {
			throw new UnsupportedOperationException();
		}
	}

	private static class ParticleVertexConsumer extends VertexConsumerWrapper {
		ParticleVertexConsumer(VertexConsumer vertexConsumer) {
			super(vertexConsumer);
		}

		public void endVertex() {
			VertexConsumer vc;
			if (this.transform != null) {
				vc = this.vertexConsumer.vertex(this.transform, (float)this.x, (float)this.y, (float)this.z);
			} else {
				vc = this.vertexConsumer.vertex((float)this.x, (float)this.y, (float)this.z);
			}

			vc.uv(this.u, this.v).color(this.r, this.g, this.b, this.a).uv2(this.uv1, this.uv2).endVertex();
		}
	}

	private static class ColorTexLightmapWrapper extends VertexConsumerWrapper {
		ColorTexLightmapWrapper(VertexConsumer vertexConsumer) {
			super(vertexConsumer);
		}

		public void endVertex() {
			this.vertexConsumer.vertex(this.transform, (float)this.x, (float)this.y, (float)this.z)
				.color(this.r, this.g, this.b, this.a)
				.uv(this.u, this.v)
				.overlayCoords(0)
				.uv2(this.uv1, this.uv2)
				.normal(0.0F, 0.0F, 1.0F)
				.endVertex();
		}
	}
}
