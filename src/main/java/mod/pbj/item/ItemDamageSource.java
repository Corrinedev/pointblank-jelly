package mod.pbj.item;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;

public class ItemDamageSource extends DamageSource {
	private final Item item;

	public ItemDamageSource(DamageSource source, Item item) {
		super(source.typeHolder(), source.getDirectEntity(), source.getEntity(), source.getSourcePosition());
		this.item = item;
	}

	public Item getItem() {
		return this.item;
	}
}
