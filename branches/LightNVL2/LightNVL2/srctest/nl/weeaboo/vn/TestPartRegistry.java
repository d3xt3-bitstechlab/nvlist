package nl.weeaboo.vn;

import nl.weeaboo.game.entity.PartRegistry;
import nl.weeaboo.game.entity.PartType;
import nl.weeaboo.vn.impl.DrawablePart;
import nl.weeaboo.vn.impl.ImagePart;
import nl.weeaboo.vn.impl.TransformablePart;
import nl.weeaboo.vn.script.impl.ScriptPart;

public class TestPartRegistry extends PartRegistry {

	private static final long serialVersionUID = 1L;

	public final PartType<DrawablePart> drawable;
	public final PartType<TransformablePart> transformable;
    public final PartType<ImagePart> image;
    public final PartType<ScriptPart> script;

	public TestPartRegistry() {
		drawable = register("drawable", DrawablePart.class);
		transformable = register("transformable", TransformablePart.class);
        image = register("image", ImagePart.class);
        script = register("script", ScriptPart.class);
	}

}