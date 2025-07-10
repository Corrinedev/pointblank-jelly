package mod.pbj.client.render;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;

public interface RenderPassGeoRenderer<T extends GeoAnimatable> extends GeoRenderer<T>, RenderPassRenderer<T> {}
