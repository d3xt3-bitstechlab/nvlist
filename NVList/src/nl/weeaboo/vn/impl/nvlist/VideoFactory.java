package nl.weeaboo.vn.impl.nvlist;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import javax.media.opengl.GL2;

import nl.weeaboo.filemanager.FileManager;
import nl.weeaboo.gl.GLResourceCache;
import nl.weeaboo.gl.PBO;
import nl.weeaboo.gl.texture.GLGeneratedTexture;
import nl.weeaboo.gl.texture.TextureCache;
import nl.weeaboo.io.EnvironmentSerializable;
import nl.weeaboo.lua.io.LuaSerializable;
import nl.weeaboo.vn.INotifier;
import nl.weeaboo.vn.ISeenLog;
import nl.weeaboo.vn.IVideo;
import nl.weeaboo.vn.impl.base.BaseVideoFactory;

@LuaSerializable
public class VideoFactory extends BaseVideoFactory implements Serializable {

	private final FileManager fm;
	private final TextureCache texCache;
	private final GLResourceCache resCache;
	private final String pathPrefix;
	private final EnvironmentSerializable es;
	
	public VideoFactory(FileManager fm, TextureCache tc, GLResourceCache rc,
			ISeenLog sl, INotifier ntf)
	{
		super(sl, ntf);
		
		this.fm = fm;
		this.texCache = tc;
		this.resCache = rc;
		this.pathPrefix = "video/";
		this.es = new EnvironmentSerializable(this);
	}
	
	//Functions
	private Object writeReplace() throws ObjectStreamException {	
		return es.writeReplace();
	}
	
	@Override
	protected IVideo movieNormalized(String filename) throws IOException {
		Movie movie = new Movie(this, filename);
		movie.start();
		return movie;
	}
		
	public GLGeneratedTexture generateTexture(int w, int h) {
		return texCache.generateTexture(w, h);
	}
	
	public PBO createPBO(GL2 gl) {
		return resCache.createPBO(gl);
	}
	
	//Getters
	InputStream getVideoInputStream(String filename) throws IOException {
		return fm.getInputStream(pathPrefix + filename);
	}
	
	@Override
	protected boolean isValidFilename(String filename) {
		return fm.getFileExists(pathPrefix + filename);
	}
	
	//Setters
	
}
