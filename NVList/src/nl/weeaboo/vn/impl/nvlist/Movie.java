package nl.weeaboo.vn.impl.nvlist;

import java.io.IOException;
import java.io.ObjectInputStream;

import nl.weeaboo.gl.GLManager;
import nl.weeaboo.gl.texture.GLGeneratedTexture;
import nl.weeaboo.lua.io.LuaSerializable;
import nl.weeaboo.ogg.StreamUtil;
import nl.weeaboo.ogg.player.DefaultVideoSink;
import nl.weeaboo.ogg.player.Player;
import nl.weeaboo.ogg.player.PlayerListener;
import nl.weeaboo.vn.impl.base.BaseVideo;

@LuaSerializable
public final class Movie extends BaseVideo {

	private static final long serialVersionUID = NVListImpl.serialVersionUID;

	private final VideoFactory vfac;
	private final String filename;
	
	private transient GLGeneratedTexture tex;	
	private transient Player player;
	private transient DefaultVideoSink videoSink;
	
	public Movie(VideoFactory vfac, String filename) {
		this.vfac = vfac;
		this.filename = filename;
	}
	
	//Functions
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();

		//Restart automatically when loaded
		if (!isStopped()) {
			_start();
			if (isPaused()) {
				_pause();
			}
		}
	}	
	
	protected void createPlayer() throws IOException {
		videoSink = new DefaultVideoSink();		
		player = new Player(new PlayerListener() {
			public void onPauseChanged(boolean p) {
				//Ignore
			}
			public void onTimeChanged(double t, double et, double frac) {
			}
		}, videoSink);				
		player.setInput(StreamUtil.getOggInput(vfac.getVideoInputStream(filename)));
	}
	
	@Override
	protected void _prepare() throws IOException {
		if (player == null) {
			createPlayer();
		}
	}
	
	@Override
	protected void _start() throws IOException {
		if (player == null) {
			createPlayer();
		}		
		player.start();
	}

	@Override
	protected void _stop() {
		if (player != null) {
			player.stop();
		}
	}

	@Override
	protected void _pause() {
		if (player != null) {
			player.setPaused(true);
		}
	}
	
	@Override
	protected void _resume() {
		if (player != null) {
			player.setPaused(false);
		}
	}
	
	@Override
	protected void onVolumeChanged() {
		if (player != null) {
			player.setVolume(getVolume());
		}
	}
	
	public void draw(GLManager glm, int drawW, int drawH) {
		if (player == null) {
			return;
		}
		
		int w = player.getWidth();
		int h = player.getHeight();

		int pixels[] = videoSink.get();
		if (pixels != null && w > 0 && h > 0) {
			if (tex != null && (tex.getCropWidth() != w || tex.getCropHeight() != h)) {
				tex.dispose();
				tex = null;
			}
				
			if (tex == null) {
				tex = vfac.generateTexture(w, h);
			}
			tex.setARGB(pixels);
			tex.forceLoad(glm);
		}

		if (tex != null && !tex.isDisposed()) {
			glm.setTexture(tex);
			glm.fillRect(0, 0, drawW, drawH);
			glm.setTexture(null);
		}
	}
	
	//Getters
	@Override
	public boolean isStopped() {
		if (player != null && player.isEnded()) {
			stop();
		}
		return super.isStopped();
	}
	
	//Setters
	
}
