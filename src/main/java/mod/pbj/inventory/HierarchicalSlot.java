package mod.pbj.inventory;

public interface HierarchicalSlot {
	String getPath();

	HierarchicalSlot getParentSlot();

	void setParentSlot(HierarchicalSlot var1);
}
