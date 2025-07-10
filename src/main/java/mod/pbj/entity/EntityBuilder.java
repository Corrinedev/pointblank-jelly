package mod.pbj.entity;

import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import mod.pbj.client.EntityRendererBuilder;
import mod.pbj.item.EffectBuilderInfo;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public interface EntityBuilder<T extends EntityBuilder<T, E>, E extends Entity & ProjectileLike> {
	static EntityBuilder<?, ?> fromZipEntry(ZipFile zipFile, ZipEntry entry) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)));

			EntityBuilder<?, ?> var3;
			try {
				var3 = fromReader(reader);
			} catch (Throwable var6) {
				try {
					reader.close();
				} catch (Throwable var5) {
					var6.addSuppressed(var5);
				}

				throw var6;
			}

			reader.close();
			return var3;
		} catch (IOException var7) {
			throw new RuntimeException(var7);
		}
	}

	static EntityBuilder<?, ?> fromPath(Path path) {
		try {
			BufferedReader br = Files.newBufferedReader(path);

			EntityBuilder<?, ?> var2;
			try {
				var2 = fromReader(br);
			} catch (Throwable var5) {
				if (br != null) {
					try {
						br.close();
					} catch (Throwable var4) {
						var5.addSuppressed(var4);
					}
				}

				throw var5;
			}

			if (br != null) {
				br.close();
			}

			return var2;
		} catch (IOException var6) {
			throw new RuntimeException(var6);
		}
	}

	static EntityBuilder<?, ?> fromReader(Reader reader) {
		throw new UnsupportedOperationException();
	}

	T withJsonObject(JsonObject var1);

	String getName();

	EntityTypeExt getEntityTypeExt();

	Builder<E> getEntityTypeBuilder();

	boolean hasRenderer();

	EntityRenderer<?> createEntityRenderer(Context var1);

	EntityBuilder<?, ?> withName(String var1);

	EntityBuilder<?, ?> withItem(Supplier<Item> var1);

	EntityBuilder<?, ?> withInitialVelocity(double var1);

	EntityBuilder<?, ?> withMaxLifetime(long var1);

	EntityBuilder<?, ?> withRenderer(Supplier<EntityRendererBuilder<?, Entity, EntityRenderer<Entity>>> var1);

	EntityBuilder<?, ?> withGravity(double var1);

	EntityBuilder<?, ?> withEffect(EffectBuilderInfo var1);

	EntityBuilder<?, ?> withRicochet(boolean var1);

	E build(Level var1);

	enum EntityTypeExt { PROJECTILE }
}
