package mod.pbj.util;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;

public class LRUCache<K, V> {
	private final CacheImpl<K, V> clientCache;
	private final CacheImpl<K, V> serverCache;

	public LRUCache(int cacheSize) {
		this.clientCache = new CacheImpl<>(cacheSize);
		this.serverCache = new CacheImpl<>(cacheSize);
	}

	private CacheImpl<K, V> getCache() {
		return EffectiveSide.get() == LogicalSide.CLIENT ? this.clientCache : this.serverCache;
	}

	public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
		return this.getCache().computeIfAbsent(key, mappingFunction);
	}

	public V get(K key) {
		return this.getCache().get(key);
	}

	public V put(K key, V value) {
		return this.getCache().put(key, value);
	}

	public V remove(K key) {
		return this.getCache().remove(key);
	}

	public int size() {
		return this.getCache().size();
	}

	public void clear() {
		this.getCache().clear();
	}

	private static class CacheImpl<K, V> extends LinkedHashMap<K, V> {
		private static final long serialVersionUID = 1L;
		private final int cacheSize;

		private CacheImpl(int cacheSize) {
			super(16, 0.75F, false);
			this.cacheSize = cacheSize;
		}

		protected boolean removeEldestEntry(Entry<K, V> eldest) {
			return this.size() > this.cacheSize;
		}
	}
}
