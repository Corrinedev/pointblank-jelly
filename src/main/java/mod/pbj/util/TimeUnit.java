package mod.pbj.util;

public enum TimeUnit {
	TICK(50000000L),
	SECOND(1000000000L),
	MILLISECOND(1000000L),
	NANOSECOND(1L);

	private long nanosPerUnit;

	private TimeUnit(long nanosPerUnit) {
		this.nanosPerUnit = nanosPerUnit;
	}

	public long toMillis(long sourceUnits) {
		return (long)this.convert(sourceUnits, MILLISECOND);
	}

	public long toNanos(long sourceUnits) {
		return (long)this.convert(sourceUnits, NANOSECOND);
	}

	public long toTicks(long sourceUnits) {
		return (long)this.convert(sourceUnits, TICK);
	}

	public double toSeconds(long sourceUnits) {
		return this.convert(sourceUnits, SECOND);
	}

	public double convert(long sourceUnits, TimeUnit targetTimeUnit) {
		return this == targetTimeUnit
			? (double)sourceUnits
			: (double)sourceUnits * (double)this.nanosPerUnit / (double)targetTimeUnit.nanosPerUnit;
	}

	// $FF: synthetic method
	private static TimeUnit[] $values() {
		return new TimeUnit[] {TICK, SECOND, MILLISECOND, NANOSECOND};
	}
}
