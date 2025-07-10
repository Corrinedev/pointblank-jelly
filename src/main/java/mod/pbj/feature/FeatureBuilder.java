package mod.pbj.feature;

import com.google.gson.JsonObject;

public interface FeatureBuilder<T extends FeatureBuilder<T, F>, F extends Feature> {
	T withJsonObject(JsonObject var1);

	F build(FeatureProvider var1);
}
