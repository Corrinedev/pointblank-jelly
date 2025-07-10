package mod.pbj;

public interface Enableable {
	default boolean isEnabled() {
		return true;
	}
}
