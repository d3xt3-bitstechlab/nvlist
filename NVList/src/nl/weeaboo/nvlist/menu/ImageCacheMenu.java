package nl.weeaboo.nvlist.menu;

import static nl.weeaboo.game.BaseGameConfig.IMAGE_CACHE_SIZE;

public class ImageCacheMenu extends RangeMenu<Integer> {

	private static final String labels[] = {
		"Off", "4MB", "8MB", "16MB", "32MB", "64MB", "128MB"
	};
	
	private static final Integer values[] = {
		0, 4<<20, 8<<20, 16<<20, 32<<20, 64<<20, 128<<20
	};
	
	public ImageCacheMenu() {
		super(IMAGE_CACHE_SIZE, "Image Cache", '\0', labels, values);
	}
	
}
