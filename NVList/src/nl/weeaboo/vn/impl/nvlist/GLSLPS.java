package nl.weeaboo.vn.impl.nvlist;

import java.io.Serializable;

import javax.media.opengl.GL2;

import nl.weeaboo.common.Rect;
import nl.weeaboo.gl.GLManager;
import nl.weeaboo.gl.shader.GLShader;
import nl.weeaboo.lua.io.LuaSerializable;
import nl.weeaboo.lua.platform.LuajavaLib;
import nl.weeaboo.vn.INotifier;
import nl.weeaboo.vn.IPixelShader;
import nl.weeaboo.vn.IRenderer;
import nl.weeaboo.vn.impl.base.BaseTweenShader;

import org.luaj.vm.LFunction;
import org.luaj.vm.LTable;
import org.luaj.vm.LuaErrorException;
import org.luaj.vm.LuaState;

@LuaSerializable
public class GLSLPS extends BaseTweenShader implements IPixelShader {
		
	private static final long serialVersionUID = NVListImpl.serialVersionUID;

	protected final ImageFactory imageFactory;
	protected final INotifier notifier;
	protected final String filename;
	
	private transient GLShader shader;
	
	public GLSLPS(ImageFactory fac, INotifier ntf, String filename) {
		this.imageFactory = fac;
		this.notifier = ntf;
		this.filename = filename;
	}

	//Functions 
	public static void install(LTable globals, final ImageFactory ifac, final INotifier ntf) {
		LTable table = new LTable();
		GLSLLib.install(table, ifac, ntf);
		globals.put("GLSL", table);
	}
	
	@Override
	public void preDraw(IRenderer r) {
		Renderer rr = (Renderer)r;
		GLManager glm = rr.getGLManager();
				
		if (shader == null) {
			shader = imageFactory.getGLShader(filename);
		}
		
		if (shader != null) {
			Rect rect = new Rect(r.getRealX(), r.getRealY(), r.getRealWidth(), r.getRealHeight());
			
			shader.forceLoad(glm);
			glm.setShader(shader);
			setShaderParams(glm, shader, rect);
		}
	}

	@Override
	public void postDraw(IRenderer r) {
		Renderer rr = (Renderer)r;
		GLManager glm = rr.getGLManager();
		
		glm.setShader(null);
	}

	protected void setShaderParams(GLManager glm, GLShader shader, Rect screenRect) {
		GL2 gl2 = GLManager.getGL2(glm.getGL());
		int texId = (glm.getTexture() != null ? glm.getTexture().getTexId() : 0);
		shader.setTextureParam(gl2, 0, "tex", texId);
		shader.setFloatParam(gl2, "time", (float)getTime());
		shader.setVec4Param(gl2, "screen", screenRect.x, screenRect.y, screenRect.w, screenRect.h);
	}
	
	//Getters
	
	//Setters
	
	//Inner Classes
	@LuaSerializable
	private static class GLSLLib extends LFunction implements Serializable {
		
		private static final long serialVersionUID = NVListImpl.serialVersionUID;

		private static final String[] NAMES = {
			"new"
		};

		private static final int NEW = 0;
		
		private final int id;
		private final ImageFactory fac;
		private final INotifier ntf;
		
		private GLSLLib(int id, ImageFactory fac, INotifier ntf) {
			this.id = id;
			this.fac = fac;
			this.ntf = ntf;
		}
		
		public static void install(LTable table, ImageFactory fac, INotifier ntf) {
			for (int n = 0; n < NAMES.length; n++) {
				table.put(NAMES[n], new GLSLLib(n, fac, ntf));
			}
		}

		@Override
		public int invoke(LuaState vm) {
			switch (id) {
			case NEW: return newInstance(vm);
			default:
				throw new LuaErrorException("Invalid function id: " + id);
			}
		}
		
		protected int newInstance(LuaState vm) {
			String filename = vm.optstring(1, null);
			vm.resettop();
			GLSLPS shader = new GLSLPS(fac, ntf, filename);
			vm.pushlvalue(LuajavaLib.toUserdata(shader, GLSLPS.class));
			return 1;
		}
		
	}
	
}
