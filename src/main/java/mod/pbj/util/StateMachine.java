package mod.pbj.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StateMachine<T extends Enum<T>, Context> {
	private static final Logger LOGGER = LogManager.getLogger("pointblank");
	private T currentState;
	private final List<Transition<T, Context>> allTransitions;
	private final Set<T> allStates = new HashSet<>();
	private final Map<T, Action<T, Context>> onSetStateActions;
	private final Action<T, Context> onChangeStateAction;
	private final StateHistory<T> stateHistory;
	private final Map<T, List<Transition<T, Context>>> transitionsByFromState;
	private final Map<T, List<Transition<T, Context>>> transitionsByToState;

	private StateMachine(
		T initialState,
		List<Transition<T, Context>> allowedTransitions,
		Map<T, Action<T, Context>> onSetStateActions,
		Action<T, Context> onChangeStateAction) {
		this.currentState = initialState;
		this.allTransitions = Collections.unmodifiableList(allowedTransitions);
		this.onChangeStateAction = onChangeStateAction;
		this.transitionsByFromState = new HashMap<>();
		this.transitionsByToState = new HashMap<>();

		for (Transition<T, Context> transition : allowedTransitions) {
			this.allStates.add(transition.fromState);
			this.allStates.add(transition.toState);
			this.transitionsByFromState.computeIfAbsent(transition.fromState, (k) -> new ArrayList<>()).add(transition);
			this.transitionsByToState.computeIfAbsent(transition.toState, (k) -> new ArrayList<>()).add(transition);
		}

		this.onSetStateActions = Collections.unmodifiableMap(onSetStateActions);
		this.stateHistory = new StateHistory<>(this.allStates.size() + 1);
		this.stateHistory.add(initialState);
	}

	public T getCurrentState() {
		return this.currentState;
	}

	public void resetToState(T newState) {
		if (newState != this.currentState) {
			this.currentState = newState;
		}
	}

	public T setState(Context context, T newState) {
		boolean result = false;
		List<Transition<T, Context>> possibleTransitions = this.transitionsByToState.get(newState);
		if (possibleTransitions == null) {
			throw new IllegalArgumentException("Unknown state: " + newState);
		} else {
			for (Transition<T, Context> transition : possibleTransitions) {
				if (Objects.equals(this.currentState, transition.fromState) &&
					Objects.equals(newState, transition.toState) && transition.p.test(context)) {
					this.setState(context, transition);
					result = true;
					break;
				}
			}

			if (result) {
				this.update(context);
			}

			return result ? this.currentState : null;
		}
	}

	public T setStateToAnyOf(Context context, Collection<T> toStates) {
		T result = null;

		for (T toState : toStates) {
			result = this.setState(context, toState);
			if (result != null) {
				break;
			}
		}

		return result;
	}

	public T update(Context context) {
		int maxAllowedTransitions = this.allStates.size();

		for (int i = 0; i < maxAllowedTransitions; ++i) {
			Transition<T, Context> transition = this.next(context, StateMachine.TransitionMode.AUTO);
			if (transition == null) {
				break;
			}

			this.setState(context, transition);
		}

		return this.currentState;
	}

	private void setState(Context context, Transition<T, Context> transition) {
		if (transition.preAction != null) {
			transition.preAction.execute(context, transition.fromState, transition.toState);
		}

		this.currentState = transition.toState;
		this.stateHistory.add(this.currentState);
		if (transition.postAction != null) {
			transition.postAction.execute(context, transition.fromState, transition.toState);
		}

		Action<T, Context> onSetStateAction = this.onSetStateActions.get(this.currentState);
		if (onSetStateAction != null) {
			onSetStateAction.execute(context, transition.fromState, transition.toState);
		}

		if (this.onChangeStateAction != null) {
			this.onChangeStateAction.execute(context, transition.fromState, transition.toState);
		}

		LOGGER.trace("Transitioned: {} -> {}", transition.fromState, transition.toState);
	}

	private Transition<T, Context> next(Context context, TransitionMode transitionMode) {
		for (Transition<T, Context> transition : this.transitionsByFromState.get(this.currentState)) {
			if (Objects.equals(this.currentState, transition.fromState) && transition.p.test(context) &&
				transition.mode == transitionMode) {
				return transition;
			}
		}

		return null;
	}

	public List<Transition<T, Context>> getAllTransitions() {
		return this.allTransitions;
	}

	public Collection<T> getStateHistory() {
		return Collections.unmodifiableCollection(this.stateHistory.deque);
	}

	public enum TransitionMode {
		AUTO,
		EVENT_OR_AUTO,
		EVENT;

		TransitionMode() {}
	}

	public static class StateHistory<E> {
		private final ArrayDeque<E> deque;
		private final int maxSize;

		public StateHistory(int size) {
			this.deque = new ArrayDeque<>(size);
			this.maxSize = size;
		}

		public void add(E element) {
			if (this.deque.size() == this.maxSize) {
				this.deque.poll();
			}

			this.deque.offer(element);
		}

		public E remove() {
			return this.deque.poll();
		}

		public int size() {
			return this.deque.size();
		}

		public boolean contains(E element) {
			return this.deque.contains(element);
		}
	}

	private static class Transition<T extends Enum<T>, Context> {
		T fromState;
		T toState;
		Predicate<Context> p;
		Action<T, Context> preAction;
		Action<T, Context> postAction;
		TransitionMode mode;

		public Transition(
			T fromState,
			T toState,
			Predicate<Context> p,
			Action<T, Context> preAction,
			Action<T, Context> postAction,
			TransitionMode transitionMode) {
			this.fromState = fromState;
			this.toState = toState;
			this.p = p;
			this.preAction = preAction;
			this.postAction = postAction;
			this.mode = transitionMode;
		}

		public String toString() {
			return String.format(
				"Tr[%s->%s, %s, pre: %s, post: %s]",
				this.fromState,
				this.toState,
				this.mode,
				this.preAction,
				this.postAction);
		}
	}

	public static class Builder<T extends Enum<T>, Context> {
		private final List<Transition<T, Context>> allowedTransitions = new ArrayList<>();
		private final Set<T> allStates = new HashSet<>();
		private final Map<T, Action<T, Context>> onSetStateActions = new HashMap<>();
		private Action<T, Context> onChangeStateAction;

		public Builder() {}

		public Builder<T, Context> withOnSetStateAction(T toState, Action<T, Context> action) {
			this.onSetStateActions.put(toState, action);
			return this;
		}

		public Builder<T, Context> withOnChangeStateAction(Action<T, Context> onChangeStateAction) {
			this.onChangeStateAction = onChangeStateAction;
			return this;
		}

		public Builder<T, Context> withTransition(
			List<T> fromStates,
			T toState,
			Predicate<Context> predicate,
			TransitionMode transitionMode,
			Action<T, Context> preAction,
			Action<T, Context> postAction) {
			if (predicate == null) {
				predicate = (context) -> true;
			}

			for (T fromState : fromStates) {
				this.withTransition(fromState, toState, predicate, transitionMode, preAction, postAction);
			}

			return this;
		}

		public Builder<T, Context> withTransition(
			T fromState,
			T toState,
			Predicate<Context> predicate,
			TransitionMode transitionMode,
			Action<T, Context> preAction,
			Action<T, Context> postAction) {
			if (predicate == null) {
				predicate = (context) -> true;
			}

			this.allowedTransitions.add(
				new Transition<>(fromState, toState, predicate, preAction, postAction, transitionMode));
			this.allStates.add(fromState);
			this.allStates.add(toState);
			return this;
		}

		public Builder<T, Context>
		withTransition(T fromState, T toState, Predicate<Context> predicate, TransitionMode transitionMode) {
			return this.withTransition(fromState, toState, predicate, transitionMode, null, null);
		}

		public Builder<T, Context> withTransition(T fromState, T toState) {
			return this.withTransition(fromState, toState, null, StateMachine.TransitionMode.EVENT, null, null);
		}

		public Builder<T, Context> withTransition(T fromState, T toState, Predicate<Context> predicate) {
			return this.withTransition(fromState, toState, predicate, StateMachine.TransitionMode.AUTO, null, null);
		}

		private void validate() {
			Set<T> statesWithOutgoingTransitions = new HashSet<>();

			for (Transition<T, Context> transition : this.allowedTransitions) {
				statesWithOutgoingTransitions.add(transition.fromState);
			}

			for (T state : this.allStates) {
				if (!statesWithOutgoingTransitions.contains(state)) {
					throw new IllegalArgumentException("No outgoing transitions found for state " + state);
				}
			}

			for (T state : this.onSetStateActions.keySet()) {
				if (!this.allStates.contains(state)) {
					throw new IllegalArgumentException("No transitions defined for state used in action: " + state);
				}
			}
		}

		public StateMachine<T, Context> build(T initialState) {
			this.validate();
			return new StateMachine<>(
				initialState, this.allowedTransitions, this.onSetStateActions, this.onChangeStateAction);
		}
	}

	public interface Action<T extends Enum<T>, Context> {
		void execute(Context var1, T var2, T var3);
	}
}
