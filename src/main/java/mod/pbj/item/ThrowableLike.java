package mod.pbj.item;

import java.util.UUID;
import mod.pbj.client.ThrowableClientState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface ThrowableLike {
	long getDrawCooldownDuration(LivingEntity var1, ThrowableClientState var2, ItemStack var3);

	boolean hasIdleAnimations();

	void requestThrowFromServer(ThrowableClientState var1, Player var2, ItemStack var3, Entity var4);

	ThrowableClientState createState(UUID var1);

	long getIdleCooldownDuration(LivingEntity var1, ThrowableClientState var2, ItemStack var3);

	long getInspectCooldownDuration(LivingEntity var1, ThrowableClientState var2, ItemStack var3);

	long getPrepareIdleCooldownDuration();

	long getCompleteThrowCooldownDuration(LivingEntity var1, ThrowableClientState var2, ItemStack var3);

	long getPrepareThrowCooldownDuration(LivingEntity var1, ThrowableClientState var2, ItemStack var3);

	long getThrowCooldownDuration(LivingEntity var1, ThrowableClientState var2, ItemStack var3);

	void prepareThrow(ThrowableClientState var1, Player var2, ItemStack var3, Entity var4);

	void handleClientThrowRequest(ServerPlayer var1, UUID var2, int var3);
}
