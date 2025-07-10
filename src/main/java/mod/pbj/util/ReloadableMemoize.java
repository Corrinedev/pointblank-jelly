package mod.pbj.util;

import java.util.function.BiFunction;
import net.minecraft.Util;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class ReloadableMemoize<T, U, R> implements ResourceManagerReloadListener {
	private final BiFunction<T, U, R> originalFunction;
	private BiFunction<T, U, R> memoizedFunction;

	public ReloadableMemoize(BiFunction<T, U, R> originalFunction) {
		this.originalFunction = originalFunction;
		this.memoizedFunction = this.memoize(originalFunction);
	}

	public BiFunction<T, U, R> getMemoizedFunction() {
		return this::apply;
	}

	private R apply(T t, U u) {
		return this.memoizedFunction.apply(t, u);
	}

	private BiFunction<T, U, R> memoize(BiFunction<T, U, R> function) {
		return Util.memoize(function);
	}

	public void reload() {
		this.memoizedFunction = this.memoize(this.originalFunction);
	}

	public void onResourceManagerReload(ResourceManager resourceManager) {
		this.reload();
	}
}
