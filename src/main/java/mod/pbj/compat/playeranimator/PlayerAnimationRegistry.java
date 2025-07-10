package mod.pbj.compat.playeranimator;

import java.io.Reader;
import java.util.List;
import java.util.function.Supplier;

public interface PlayerAnimationRegistry<T> {
	void reload();

	void register(String var1, Supplier<Reader> var2);

	boolean isRegistered(String var1);

	List<PlayerAnimation<T>> getAnimations(String var1, PlayerAnimationType var2);
}
