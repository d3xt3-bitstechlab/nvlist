package nl.weeaboo.nvlist.menu;

import static nl.weeaboo.game.BaseGameConfig.MAX_TEX_SIZE;

public class MaxTexSizeMenu extends RangeMenu<Integer> {

	private static final String labels[] = {
		"64x64", "128x128", "256x256", "512x512", "1024x1024", "2048x2048", "4096x4096"
	};
	
	private static final Integer values[] = {
		64, 128, 256, 512, 1024, 2048, 4096
	};
	
	public MaxTexSizeMenu() {
		super(MAX_TEX_SIZE, "Texture Size Limit", '\0', labels, values);
	}
	
}
