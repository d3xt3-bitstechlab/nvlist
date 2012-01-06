package nl.weeaboo.vn.impl.nvlist;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;

import nl.weeaboo.common.Dim;
import nl.weeaboo.common.Dim2D;
import nl.weeaboo.common.Rect;
import nl.weeaboo.common.Rect2D;
import nl.weeaboo.gl.GLManager;
import nl.weeaboo.gl.shader.GLShader;
import nl.weeaboo.gl.texture.GLTexture;
import nl.weeaboo.lua.io.LuaSerializable;
import nl.weeaboo.lua.platform.LuajavaLib;
import nl.weeaboo.vn.IPixelShader;
import nl.weeaboo.vn.IRenderer;
import nl.weeaboo.vn.ITexture;
import nl.weeaboo.vn.impl.base.BaseNotifier;
import nl.weeaboo.vn.impl.base.BaseShader;

import org.luaj.vm.LFunction;
import org.luaj.vm.LTable;
import org.luaj.vm.LuaErrorException;
import org.luaj.vm.LuaState;

@LuaSerializable
public class GLSLPS extends BaseShader implements IPixelShader {
		
	private static final long serialVersionUID = NVListImpl.serialVersionUID;

	protected final ImageFactory imageFactory;
	protected final BaseNotifier notifier;
	protected final String filename;
	
	private final Map<String, Object> params;
	
	private transient GLShader shader;
	
	public GLSLPS(ImageFactory fac, BaseNotifier ntf, String filename) {
		this.imageFactory = fac;
		this.notifier = ntf;
		this.filename = filename;
		
		params = new HashMap<String, Object>();
	}

	//Functions 
	public static void install(LTable globals, final ImageFactory ifac, final BaseNotifier ntf) {
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
			Rect screen = new Rect(r.getRealX(), r.getRealY(), r.getRealWidth(), r.getRealHeight());
			
			shader.forceLoad(glm);
			glm.setShader(shader);

			GL2 gl2 = GLManager.getGL2(glm.getGL());
			applyShaderParam(gl2, shader, "tex", glm.getTexture());
			applyShaderParam(gl2, shader, "time", getTime());
			applyShaderParam(gl2, shader, "screen", screen);
			for (Entry<String, Object> entry : params.entrySet()) {
				try {
					applyShaderParam(gl2, shader, entry.getKey(), entry.getValue());
				} catch (IllegalArgumentException iae) {
					params.remove(entry.getKey());					
					throw iae; //Must exit loop now to avoid ConcurrentModificationException
				}
			}
		}
	}

	@Override
	public void postDraw(IRenderer r) {
		Renderer rr = (Renderer)r;
		GLManager glm = rr.getGLManager();
		
		glm.setShader(null);
	}

	protected void applyShaderParam(GL2ES2 gl2, GLShader shader, String name, Object value) {
		if (value instanceof TextureAdapter) {
			shader.setTextureParam(gl2, name, 0, ((TextureAdapter)value).getTexId());
		} else if (value instanceof GLTexture) {
			shader.setTextureParam(gl2, name, 0, ((GLTexture)value).getTexId());
		} else if (value instanceof float[]) {			
			float[] f = (float[])value;
			//System.out.println(name + " " + Arrays.toString(f));
			if (f.length == 1) {
				shader.setFloatParam(gl2, name, f[0]);
			} else if (f.length == 2) {
				shader.setVec2Param(gl2, name, f, 0);
			} else if (f.length == 3) {
				shader.setVec3Param(gl2, name, f, 0);
			} else if (f.length >= 4) {
				shader.setVec4Param(gl2, name, f, 0);
			}
		} else if (value instanceof Rect2D) {
			Rect2D r = (Rect2D)value;
			shader.setVec4Param(gl2, name, (float)r.x, (float)r.y, (float)r.w, (float)r.h);
		} else if (value instanceof Rect) {
			Rect r = (Rect)value;
			shader.setVec4Param(gl2, name, r.x, r.y, r.w, r.h);
		} else if (value instanceof Dim2D) {
			Dim2D d = (Dim2D)value;
			shader.setVec2Param(gl2, name, (float)d.w, (float)d.h);
		} else if (value instanceof Dim) {
			Dim d = (Dim)value;
			shader.setVec2Param(gl2, name, d.w, d.h);
		} else if (value instanceof Number) {
			shader.setFloatParam(gl2, name, ((Number)value).floatValue());
		} else {
			throw new IllegalArgumentException("Unsupported param type: " + (value != null ? value.getClass() : null));
		}
	}
	
	//Getters
	
	//Setters
	public void setParam(String name, ITexture tex) {
		if (tex == null) {
			params.remove(name);
		} else {
			params.put(name, tex);
		}
	}
	public void setParam(String name, float[] values) {
		if (values == null) {
			params.remove(name);
		} else {
			params.put(name, values);
		}
	}
	
	//Inner Classes
	@LuaSerializable
	private static class GLSLLib extends LFunction implements Serializable {
		
		private static final long serialVersionUID = NVListImpl.serialVersionUID;

		private static final String[] NAMES = {
			"new",
			"getVersion",
			"isVersionSupported"
		};

		private static final int NEW                   = 0;
		private static final int GET_VERSION           = 1;
		private static final int IS_VERSION_SUPPORTED  = 2;
		
		private final int id;
		private final ImageFactory fac;
		private final BaseNotifier ntf;
		
		private GLSLLib(int id, ImageFactory fac, BaseNotifier ntf) {
			this.id = id;
			this.fac = fac;
			this.ntf = ntf;
		}
		
		public static void install(LTable table, ImageFactory fac, BaseNotifier ntf) {
			for (int n = 0; n < NAMES.length; n++) {
				table.put(NAMES[n], new GLSLLib(n, fac, ntf));
			}
		}

		@Override
		public int invoke(LuaState vm) {
			switch (id) {
			case NEW: return newInstance(vm);
			case GET_VERSION: return getVersion(vm);
			case IS_VERSION_SUPPORTED: return isVersionSupported(vm);
			default:
				throw new LuaErrorException("Invalid function id: " + id);
			}
		}
		
		protected int newInstance(LuaState vm) {
			String filename = vm.optstring(1, null);
			vm.resettop();
			GLSLPS shader = new GLSLPS(fac, ntf, filename);
			vm.pushlvalue(LuajavaLib.toUserdata(shader, shader.getClass()));
			return 1;
		}
		
		protected int getVersion(LuaState vm) {
			vm.resettop();
			vm.pushstring(fac.getGlslVersion());
			return 1;
		}
		
		protected int isVersionSupported(LuaState vm) {
			String a = vm.tostring(1);
			String b = fac.getGlslVersion();
			vm.resettop();
			vm.pushboolean(b != null && !b.equals("") && a.compareTo(b) <= 0);
			return 1;
		}
		
	}
	
}
