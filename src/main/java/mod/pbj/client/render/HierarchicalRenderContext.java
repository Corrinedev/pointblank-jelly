package mod.pbj.client.render;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import mod.pbj.Nameable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class HierarchicalRenderContext implements AutoCloseable {
	private static final Deque<HierarchicalRenderContext> contextStack = new ArrayDeque<>();
	private final ItemStack itemStack;
	private String path;
	private final ItemDisplayContext itemDisplayContext;
	private final Map<String, Object> attributes = new HashMap<>();

	private HierarchicalRenderContext(
		ItemStack itemStack, HierarchicalRenderContext parent, ItemDisplayContext itemDisplayContext) {
		this.itemStack = itemStack;
		this.itemDisplayContext = itemDisplayContext;
		if (parent == null) {
			this.path = "/";
		} else {
			Item var6 = itemStack.getItem();
			String var10000;
			if (var6 instanceof Nameable nameable) {
				var10000 = nameable.getName();
			} else {
				var10000 = itemStack.getItem().toString();
			}

			String name = var10000;
			String var10001 = parent.getPath();
			this.path = var10001 + "/" + name;
		}
	}

	public ItemStack getItemStack() {
		return this.itemStack;
	}

	public String getPath() {
		return this.path;
	}

	public ItemDisplayContext getItemDisplayContext() {
		return this.itemDisplayContext;
	}

	public void setAttribute(String name, Object value) {
		this.attributes.put(name, value);
	}

	public Object getAttribute(String name) {
		return this.attributes.get(name);
	}

	public <T> T getAttribute(String name, T _default) {
		return (T)(this.attributes.getOrDefault(name, _default));
	}

	public static HierarchicalRenderContext push(ItemStack itemStack, ItemDisplayContext itemDisplayContext) {
		HierarchicalRenderContext parent = contextStack.peekFirst();
		HierarchicalRenderContext context = new HierarchicalRenderContext(itemStack, parent, itemDisplayContext);
		contextStack.addFirst(context);
		return context;
	}

	public static HierarchicalRenderContext push() {
		HierarchicalRenderContext current = current();
		if (current == null) {
			throw new IllegalStateException("No parent hierarchical render context to inherit from");
		} else {
			HierarchicalRenderContext context =
				new HierarchicalRenderContext(current.itemStack, current, current.itemDisplayContext);
			context.path = current.path;
			contextStack.addFirst(context);
			return context;
		}
	}

	public static void pop() {
		contextStack.removeFirst();
	}

	public static HierarchicalRenderContext current() {
		return contextStack.peekFirst();
	}

	public static HierarchicalRenderContext getRoot() {
		return contextStack.peekLast();
	}

	public static ItemStack getRootItemStack() {
		HierarchicalRenderContext root = getRoot();
		return root != null ? root.itemStack : null;
	}

	public void close() {
		pop();
	}
}
