package mod.pbj.client;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class GunStateTicker extends Thread {
	public static final long STATE_TICK_INTERVAL_MILLIS = 5L;
	private final AtomicBoolean running = new AtomicBoolean(true);
	private final ClientEventHandler clientEventHandler;

	public GunStateTicker(ClientEventHandler clientEventHandler) {
		this.setDaemon(true);
		this.clientEventHandler = clientEventHandler;
	}

	public void run() {
		while (true) {
			if (this.running.get()) {
				try {
					ClientEventHandler var10000 = this.clientEventHandler;
					Objects.requireNonNull(var10000);
					ClientEventHandler.runSyncTick(var10000::tickMainHeldGun);
					sleep(5L);
					continue;
				} catch (InterruptedException var2) {
					var2.printStackTrace();
				}
			}

			return;
		}
	}

	void shutdown() {
		this.running.set(false);
	}
}
