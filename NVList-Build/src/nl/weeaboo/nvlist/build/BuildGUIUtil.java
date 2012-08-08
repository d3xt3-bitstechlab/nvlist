package nl.weeaboo.nvlist.build;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.border.LineBorder;
import javax.swing.text.JTextComponent;

public final class BuildGUIUtil {

	private BuildGUIUtil() {
	}
	
	public static void recursiveSetOpaque(JComponent c, boolean opaque) {
		c.setOpaque(opaque);
		
		final int L = c.getComponentCount();
		for (int n = 0; n < L; n++) {
			Component child = c.getComponent(n);
			if (child instanceof JComponent) {
				recursiveSetOpaque((JComponent)child, opaque);
			}
		}
	}

	@SuppressWarnings("serial")
	public static void setTextFieldDefaults(JTextComponent c, Color bg) {
		c.setSelectionColor(darker(bg));
		c.setBackground(brighter(bg));
		c.setBorder(new LineBorder(bg.darker()) {
		    public Insets getBorderInsets(Component c)       {
		        return getBorderInsets(c, new Insets(0, 0, 0, 0));
		    }
		    public Insets getBorderInsets(Component c, Insets insets) {
		        insets.set(thickness, 5+thickness, thickness, 5+thickness);
		        return insets;
		    }			
		});
	}
	
	public static Color darker(Color bg) {
		return bg.darker();
	}
	
	public static Color brighter(Color bg) {
		float s = 1.1f;
		float rgb[] = bg.getColorComponents(null);
		return new Color(rgb[0]*s, rgb[1]*s, rgb[2]*s);
	}

}
