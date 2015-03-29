package nl.weeaboo.vn.math;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

import nl.weeaboo.common.FastMath;
import nl.weeaboo.common.StringUtil;

abstract class AbstractMatrix implements Serializable {

	private static final long serialVersionUID = 1L;

	protected double m00, m01, m02, m10, m11, m12;

	public AbstractMatrix(double m00, double m01, double m02,
			double m10, double m11, double m12)
	{
		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;
		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;
	}
	public AbstractMatrix(AbstractMatrix m) {
		this(m.m00, m.m01, m.m02, m.m10, m.m11, m.m12);
	}

	//Functions
    private void writeObject(ObjectOutputStream out) throws IOException {
    	out.writeDouble(m00); out.writeDouble(m01); out.writeDouble(m02);
		out.writeDouble(m10); out.writeDouble(m11); out.writeDouble(m12);
	}

	private void readObject(ObjectInputStream in) throws IOException {
		m00 = in.readDouble(); m01 = in.readDouble(); m02 = in.readDouble();
		m10 = in.readDouble(); m11 = in.readDouble(); m12 = in.readDouble();
	}

	@Override
	public String toString() {
		return String.format(StringUtil.LOCALE, "%s[%.2f, %.2f, %.2f, %.2f, %.2f, %.2f]",
				getClass().getSimpleName(), m00, m01, m02, m10, m11, m12);
	}

	@Override
	public final int hashCode() {
		return Arrays.hashCode(new double[] {m00, m01, m02, m10, m11, m12});
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj instanceof AbstractMatrix) {
			AbstractMatrix m = (AbstractMatrix)obj;
			return equals(m, 0.0);
		}
		return false;
	}

	public final boolean equals(AbstractMatrix m, double epsilon) {
		if (this == m) {
			return true;
		}

		if (epsilon == 0.0) {
			return m00 == m.m00 && m01 == m.m01 && m02 == m.m02
				&& m10 == m.m10 && m11 == m.m11 && m12 == m.m12;
		} else {
			return FastMath.approxEquals(m00, m.m00, epsilon)
				&& FastMath.approxEquals(m01, m.m01, epsilon)
				&& FastMath.approxEquals(m02, m.m02, epsilon)
				&& FastMath.approxEquals(m10, m.m10, epsilon)
				&& FastMath.approxEquals(m11, m.m11, epsilon)
				&& FastMath.approxEquals(m12, m.m12, epsilon);
		}
	}

	public void transform(Vec2 v) {
		double newX = m00 * v.x + m01 * v.y + m02;
		double newY = m10 * v.x + m11 * v.y + m12;
		v.x = newX;
		v.y = newY;
	}
	public Vec2 transform(double x, double y) {
		return new Vec2(
			m00 * x + m01 * y + m02,
			m10 * x + m11 * y + m12);
	}
	public void transform(float[] verts, int off, int len) {
		if (len <= 0) return;

		float m00 = (float)this.m00, m01 = (float)this.m01, m02 = (float)this.m02;
		float m10 = (float)this.m10, m11 = (float)this.m11, m12 = (float)this.m12;

		int lim = off + len;
		for (int n = off; n < lim; n += 2) {
			float x = verts[n  ];
			float y = verts[n+1];
			verts[n  ] = m00 * x + m01 * y + m02;
			verts[n+1] = m10 * x + m11 * y + m12;
		}
	}

    public float[] toGLMatrix() {
    	return new float[] {
    			(float)m00, (float)m10,         0f,         0f,
    			(float)m01, (float)m11,         0f,         0f,
    			        0f,         0f,         1f,         0f,
    			(float)m02, (float)m12,         0f,         1f
    	};
    }

	//Getters
    public boolean hasShear() {
    	return m01 != 0 || m10 != 0;
    }
    public boolean hasScale() {
    	return m00 != 1 || m11 != 1;
    }
    public boolean hasTranslation() {
    	return m02 != 0 || m12 != 0;
    }

    public double getScaleX() {
    	return m00;
    }
    public double getScaleY() {
    	return m11;
    }
    public double getShearX() {
    	return m01;
    }
    public double getShearY() {
    	return m10;
    }
    public double getTranslationX() {
    	return m02;
    }
    public double getTranslationY() {
    	return m12;
    }

	//Setters

}
