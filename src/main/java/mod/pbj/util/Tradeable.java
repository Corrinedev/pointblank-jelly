package mod.pbj.util;

import mod.pbj.Config;

public interface Tradeable {
	int MAX_BUNDLE_QUANTITY = 64;

	float getPrice();

	int getTradeLevel();

	default int getBundleQuantity() {
		float price = this.getPrice();
		int bundleQuantity;
		if ((double)price < Config.emeraldExchangeRate) {
			bundleQuantity = (int)(Config.emeraldExchangeRate / (double)price);
		} else {
			bundleQuantity = 1;
		}

		return Math.min(bundleQuantity, 64);
	}
}
