package nl.weeaboo.vn.impl.nvlist;

import java.io.ObjectStreamException;

import nl.weeaboo.game.GameLog;
import nl.weeaboo.gl.texture.GLTexRect;
import nl.weeaboo.gl.texture.TextureException;
import nl.weeaboo.io.EnvironmentSerializable;
import nl.weeaboo.lua.io.LuaSerializable;
import nl.weeaboo.vn.IImageFactory;
import nl.weeaboo.vn.ITexture;
import nl.weeaboo.vn.impl.base.BaseImageFxLib;

@LuaSerializable
public class ImageFxLib extends BaseImageFxLib {

	private final EnvironmentSerializable es;
	
	public ImageFxLib(IImageFactory imgfac) {
		super(imgfac);
		
		this.es = new EnvironmentSerializable(this);
	}
	
	//Functions
	private Object writeReplace() throws ObjectStreamException {	
		return es.writeReplace();
	}

	@Override
	protected Bitmap tryGetBitmap(ITexture tex, boolean logFailure) {
		if (tex instanceof TextureAdapter) {
			TextureAdapter adapter = (TextureAdapter)tex;
			GLTexRect tr = adapter.getTexRect();
			if (tr != null) {
				try {
					int[] argb = tr.getARGB();
					if (argb != null) {
						return new Bitmap(argb, tr.getWidth(), tr.getHeight());
					}
				} catch (TextureException e) {
					GameLog.w("Error getting pixels from texture", e);
					return null;
				}
			}
		}
		
		if (logFailure) {
			GameLog.w("Unable to get pixels from texture: " + tex.toString());			
		}
		
		return null;
	}

	//Getters
	
	//Setters
	
}
