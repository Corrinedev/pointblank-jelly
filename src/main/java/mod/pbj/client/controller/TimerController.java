package mod.pbj.client.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import mod.pbj.client.GunClientState;
import mod.pbj.feature.ConditionContext;
import mod.pbj.item.GunItem;
import mod.pbj.util.TimeUnit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class TimerController extends AbstractProceduralAnimationController {
	private final List<ScheduleEntry> eventHandlers = new ArrayList<>();
	private long lastExecutionTime = 0L;

	public TimerController(long duration) {
		super(duration);
	}

	public void schedule(
		GunItem.ReloadPhase reloadPhase,
		long time,
		TimeUnit timeUnit,
		AbstractProceduralAnimationController handler,
		Predicate<ConditionContext> condition) {
		this.eventHandlers.add(new ScheduleEntry(
			reloadPhase, timeUnit.toNanos(time), handler, condition != null ? condition : (c) -> { return true; }));
		this.eventHandlers.sort(Comparator.comparingLong(ScheduleEntry::time));
	}

	public void onRenderTick(
		LivingEntity player,
		GunClientState state,
		ItemStack itemStack,
		ItemDisplayContext itemDisplayContext,
		float partialTicks) {
		super.onRenderTick(player, state, itemStack, itemDisplayContext, partialTicks);
		if (!this.isDone) {
			long currentTime = System.nanoTime() - this.startTime;

			for (ScheduleEntry scheduleEntry : this.eventHandlers) {
				long handlerTime = scheduleEntry.time;
				AbstractProceduralAnimationController handler = scheduleEntry.handler;
				if (handlerTime <= currentTime && state.getReloadPhase() == scheduleEntry.reloadPhase &&
					scheduleEntry.condition.test(new ConditionContext(player, itemStack, state, null))) {
					if (handlerTime > this.lastExecutionTime) {
						handler.reset();
					}

					if (!handler.isDone()) {
						handler.onRenderTick(player, state, itemStack, itemDisplayContext, partialTicks);
					}
				}
			}

			this.lastExecutionTime = currentTime;
		}
	}

	public void reset() {
		super.reset();
		this.lastExecutionTime = -1L;
	}

	public List<AbstractProceduralAnimationController>
	getActiveHandlers(Player player, GunClientState state, ItemStack itemStack) {
		long currentTime = System.nanoTime() - this.startTime;
		return this.eventHandlers.stream()
			.filter((se) -> {
				return se.reloadPhase == state.getReloadPhase() && se.time <= currentTime &&
					se.condition.test(
						new ConditionContext(player, itemStack, state, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)) &&
					!se.handler.isDone();
			})
			.map((t) -> { return t.handler; })
			.toList();
	}

	private record ScheduleEntry(
		GunItem.ReloadPhase reloadPhase,
		long time,
		AbstractProceduralAnimationController handler,
		Predicate<ConditionContext> condition) {
		public GunItem.ReloadPhase reloadPhase() {
			return this.reloadPhase;
		}

		public long time() {
			return this.time;
		}

		public AbstractProceduralAnimationController handler() {
			return this.handler;
		}

		public Predicate<ConditionContext> condition() {
			return this.condition;
		}
	}
}
