package mod.pbj.util;

public class UpDownCounter {
	private State state;
	private long maxValue;
	private long currentValue;
	private long previousValue;
	private Long pendingMaxValue;

	public UpDownCounter(long maxValue) {
		this.state = State.NONE;
		this.maxValue = maxValue;
	}

	public boolean countUp(boolean overrideState) {
		if (this.state == State.DOWN && !overrideState) {
			return false;
		} else {
			this.previousValue = this.currentValue;
			if (this.currentValue < this.maxValue) {
				++this.currentValue;
				this.state = State.UP;
			}

			return true;
		}
	}

	public boolean countDown(boolean overrideState) {
		if (this.state == State.UP && !overrideState) {
			return false;
		} else {
			this.previousValue = this.currentValue;
			if (this.currentValue > 0L) {
				--this.currentValue;
				this.state = State.DOWN;
			} else {
				this.reset();
			}

			return true;
		}
	}

	public long getCurrentValue() {
		return this.currentValue;
	}

	public long getPreviousValue() {
		return this.previousValue;
	}

	public long getMaxValue() {
		return this.maxValue;
	}

	public boolean isAtMax() {
		return this.currentValue > 0L && this.currentValue == this.maxValue;
	}

	public boolean isAtMin() {
		return this.currentValue == 0L;
	}

	public State getState() {
		return this.state;
	}

	public void setMaxValue(long maxValue) {
		this.pendingMaxValue = maxValue;
	}

	public void reset() {
		this.currentValue = 0L;
		this.previousValue = 0L;
		this.state = State.NONE;
		if (this.pendingMaxValue != null) {
			this.maxValue = this.pendingMaxValue;
			this.pendingMaxValue = null;
		}
	}

	public static enum State {
		UP,
		DOWN,
		NONE;

		// $FF: synthetic method
		private static State[] $values() {
			return new State[] {UP, DOWN, NONE};
		}
	}
}
