package mod.pbj.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Stream;
import mod.pbj.Enableable;
import mod.pbj.block.entity.PrinterBlockEntity;
import mod.pbj.block.entity.PrinterBlockEntity.State;
import mod.pbj.crafting.PointBlankIngredient;
import mod.pbj.crafting.PointBlankRecipe;
import mod.pbj.registry.ItemRegistry;
import mod.pbj.registry.MenuRegistry;
import mod.pbj.util.InventoryUtils;
import mod.pbj.util.MiscUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.searchtree.FullTextSearchTree;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag.Default;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.util.ClientUtils;

public class CraftingContainerMenu extends AbstractContainerMenu {
	private static final int SEARCH_COLS = 6;
	private static final int SEARCH_ROWS = 7;
	private static final int INGREDIENT_COLS = 5;
	private static final int INGREDIENT_ROWS = 2;
	public static final SimpleContainer INGREDIENT_CONTAINER = new SimpleContainer(10);
	public static final SimpleContainer SEARCH_CONTAINER = new SimpleContainer(42);
	private final NonNullList<ItemStack> items;
	private final AbstractContainerMenu inventoryMenu;
	private final List<ItemStack> displayItems;
	private final PrinterBlockEntity craftingBlockEntity;
	private final ContainerData containerData;
	private FullTextSearchTree<ItemStack> searchTree;
	private final Player player;

	public CraftingContainerMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, null, new SimpleContainerData(2));
	}

	public CraftingContainerMenu(
		int containerId, Inventory playerInventory, PrinterBlockEntity craftingBlockEntity, ContainerData dataAccess) {
		super(MenuRegistry.CRAFTING.get(), containerId);
		this.items = NonNullList.create();
		this.displayItems = new ArrayList<>();
		this.craftingBlockEntity = craftingBlockEntity;
		this.containerData = dataAccess;
		this.player = playerInventory.player;
		this.inventoryMenu = playerInventory.player.inventoryMenu;
		this.addDataSlots(dataAccess);

		for (int i = 0; i < 7; ++i) {
			for (int j = 0; j < 6; ++j) {
				SearchSlot slot = new SearchSlot(SEARCH_CONTAINER, i * 6 + j, 9 + j * 18, 18 + i * 18, true);
				this.addSlot(slot);
			}
		}

		for (int i = 0; i < 2; ++i) {
			for (int j = 0; j < 5; ++j) {
				IngredientSlot slot = new IngredientSlot(INGREDIENT_CONTAINER, i * 5 + j, 151 + j * 18, 108 + i * 18);
				this.addSlot(slot);
			}
		}

		for (Supplier<? extends Item> itemSupplier : ItemRegistry.ITEMS.getItemsByName().values()) {
			Item item = itemSupplier.get();
			PointBlankRecipe recipe = PointBlankRecipe.getRecipe(playerInventory.player.level(), item);
			if (recipe != null) {
				if (item instanceof Enableable e) {
					if (!e.isEnabled()) {
						continue;
					}
				}

				this.displayItems.add(recipe.getInitializedStack());
			}
		}

		if (MiscUtil.isClientSide(playerInventory.player)) {
			this.searchTree = new FullTextSearchTree<>(
				(itemStack)
					-> itemStack.getTooltipLines(null, Default.NORMAL.asCreative())
						   .stream()
						   .map((component) -> ChatFormatting.stripFormatting(component.getString()).trim())
						   .filter((p_210809_) -> !p_210809_.isEmpty()),
				(itemStack)
					-> Stream.of(BuiltInRegistries.ITEM.getKey(itemStack.getItem())),
				this.displayItems);
			this.searchTree.refresh();
		}

		this.scrollTo(0.0F);
	}

	public PrinterBlockEntity getWorkstationBlockEntity() {
		return this.craftingBlockEntity;
	}

	public void setItem(int slotIndex, int stateId, ItemStack itemStack) {
		super.setItem(slotIndex, stateId, itemStack);
	}

	public void initializeContents(int stateId, List<ItemStack> itemStacks, ItemStack carriedStack) {}

	public boolean stillValid(Player player) {
		return true;
	}

	protected int calculateRowCount() {
		return Mth.positiveCeilDiv(this.items.size(), 6) - 7;
	}

	public int getRowIndexForScroll(float scroll) {
		return Math.max((int)((double)(scroll * (float)this.calculateRowCount()) + (double)0.5F), 0);
	}

	public float getScrollForRowIndex(int rowIndex) {
		return Mth.clamp((float)rowIndex / (float)this.calculateRowCount(), 0.0F, 1.0F);
	}

	public float subtractInputFromScroll(float scrolloffs, double scroll) {
		return Mth.clamp(scrolloffs - (float)(scroll / (double)this.calculateRowCount()), 0.0F, 1.0F);
	}

	public void scrollTo(float scroll) {
		int i = this.getRowIndexForScroll(scroll);

		for (int j = 0; j < 7; ++j) {
			for (int k = 0; k < 6; ++k) {
				int l = k + (j + i) * 6;
				if (l >= 0 && l < this.items.size()) {
					SEARCH_CONTAINER.setItem(k + j * 6, this.items.get(l));
				} else {
					SEARCH_CONTAINER.setItem(k + j * 6, ItemStack.EMPTY);
				}
			}
		}
	}

	public boolean canScroll() {
		return this.items.size() > 42;
	}

	public ItemStack quickMoveStack(Player player, int slotIndex) {
		if (slotIndex >= this.slots.size() - 6 && slotIndex < this.slots.size()) {
			Slot slot = this.slots.get(slotIndex);
			if (slot != null && slot.hasItem()) {
				slot.setByPlayer(ItemStack.EMPTY);
			}
		}

		return ItemStack.EMPTY;
	}

	public boolean canTakeItemForPickAll(ItemStack itemStack, Slot slot) {
		return slot.container != SEARCH_CONTAINER;
	}

	public boolean canDragTo(Slot p_98653_) {
		return p_98653_.container != SEARCH_CONTAINER;
	}

	public ItemStack getCarried() {
		return this.inventoryMenu.getCarried();
	}

	public void setCarried(ItemStack itemStack) {
		this.inventoryMenu.setCarried(itemStack);
	}

	public void refreshSearchResults(String s) {
		this.items.clear();
		if (s.isEmpty()) {
			this.items.addAll(this.displayItems);
		} else {
			this.items.addAll(this.searchTree.search(s.toLowerCase(Locale.ROOT)));
		}

		this.scrollTo(0.0F);
	}

	public boolean updateIngredientSlots(PointBlankRecipe selectedItemRecipe) {
		boolean isCraftable = true;
		List<PointBlankIngredient> ingredients = selectedItemRecipe.getPointBlankIngredients();
		if (ingredients.size() <= INGREDIENT_CONTAINER.getContainerSize()) {
			int ingredientSlotOffset = SEARCH_CONTAINER.getContainerSize();

			for (int i = 0; i < ingredients.size(); ++i) {
				PointBlankIngredient ingredient = ingredients.get(i);
				IngredientSlot ingredientSlot = (IngredientSlot)this.slots.get(ingredientSlotOffset + i);
				ClientUtils.getClientPlayer();
				boolean hasIngredient = InventoryUtils.hasIngredient(ClientUtils.getClientPlayer(), ingredient);
				ingredientSlot.setIngredient(ingredient, hasIngredient);
				isCraftable &= hasIngredient;
			}
		}

		return isCraftable;
	}

	public void clearIngredientSlots() {
		int ingredientSlotOffset = SEARCH_CONTAINER.getContainerSize();

		for (int i = 0; i < INGREDIENT_CONTAINER.getContainerSize(); ++i) {
			IngredientSlot ingredientSlot = (IngredientSlot)this.slots.get(ingredientSlotOffset + i);
			ingredientSlot.setIngredient(null, true);
		}

		INGREDIENT_CONTAINER.clearContent();
	}

	public boolean isCreativeSlot(Slot slot) {
		return slot != null && slot.container == SEARCH_CONTAINER;
	}

	public boolean isIdle() {
		return State.values()[this.containerData.get(0)] == State.IDLE;
	}

	public boolean isCrafting() {
		boolean result = false;
		PrinterBlockEntity.State state = State.values()[this.containerData.get(0)];
		if (state == State.CRAFTING) {
			Level level = MiscUtil.getLevel(this.player);
			int playerEntityId = this.containerData.get(1);
			if (playerEntityId >= 0) {
				result = this.player == level.getEntity(playerEntityId);
			}
		}

		return result;
	}
}
