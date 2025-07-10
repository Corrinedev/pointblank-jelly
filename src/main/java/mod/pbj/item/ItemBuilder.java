package mod.pbj.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import mod.pbj.InvalidExtensionComponentException;
import mod.pbj.Nameable;
import mod.pbj.entity.EntityBuilderProvider;
import mod.pbj.registry.ExtensionRegistry;
import net.minecraft.world.item.Item;

public abstract class ItemBuilder<T extends ItemBuilder<T>> implements Nameable {
	public String nameSpace;
	public ExtensionRegistry.Extension extension;

	public static ItemBuilder<?> fromZipEntry(ZipFile zipFile, ZipEntry entry, ExtensionRegistry.Extension extension)
		throws InvalidExtensionComponentException {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)));

			ItemBuilder var3;
			try {
				var3 = fromReader(reader, extension);
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
			throw new InvalidExtensionComponentException("Zip file: " + zipFile + ", zip entry: " + entry, var7);
		}
	}

	public static ItemBuilder<?> fromPath(Path path, ExtensionRegistry.Extension extension)
		throws InvalidExtensionComponentException {
		try {
			BufferedReader br = Files.newBufferedReader(path);

			ItemBuilder var2;
			try {
				var2 = fromReader(br, extension);
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
			throw new InvalidExtensionComponentException("Component path: " + path, var6);
		}
	}

	public static ItemBuilder<?> fromReader(Reader reader, ExtensionRegistry.Extension extension) {
		try {
			JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
			String itemTypeStr =
				obj.has("type") ? obj.getAsJsonPrimitive("type").getAsString() : ItemType.GUN.toString();
			ItemType itemType = ItemType.valueOf(itemTypeStr.toUpperCase(Locale.ROOT));
			if (itemType == ItemType.GUN) {
				return (new GunItem.Builder(extension)).withJsonObject(obj);
			} else if (itemType == ItemType.AMMO) {
				return (new AmmoItem.Builder(extension)).withJsonObject(obj);
			} else if (itemType == ItemType.THROWABLE) {
				return (new ThrowableItem.Builder(extension)).withJsonObject(obj);
			} else if (itemType == ItemType.MISC) {
				return (new MiscItem.MiscItemBuilder(extension)).withJsonObject(obj);
			} else if (itemType == ItemType.ATTACHMENT) {
				return (new AttachmentItem.Builder(extension)).withJsonObject(obj);
			} else if (itemType == ItemType.ARMOR) {
				return (new ArmorItem.Builder()).withJsonObject(obj);
			} else {
				throw new IllegalArgumentException("Invalid item type: " + itemType);
			}
		} catch (Exception var4) {
			throw new RuntimeException("Error processing JSON: " + var4.getMessage(), var4);
		}
	}

	public abstract T withJsonObject(JsonObject var1);

	public EntityBuilderProvider getEntityBuilderProvider() {
		return null;
	}

	public abstract Item build();

	public enum ItemType { GUN, AMMO, MISC, ATTACHMENT, THROWABLE, ARMOR }
}
