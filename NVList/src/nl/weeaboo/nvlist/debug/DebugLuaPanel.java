package nl.weeaboo.nvlist.debug;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.undo.UndoManager;

import nl.weeaboo.awt.TTextField;
import nl.weeaboo.lua.LuaException;
import nl.weeaboo.string.HtmlUtil;
import nl.weeaboo.vn.impl.nvlist.Novel;

@SuppressWarnings("serial")
public class DebugLuaPanel extends JPanel {

	private final Object lock;
	private final Novel novel;
	private final StringBuilder outputBuffer;
	private final JLabel outputArea;
	private final JScrollPane outputScrollPane;
	private final TTextField commandField;
	
	public DebugLuaPanel(Object l, Novel nvl) {
		lock = l;
		novel = nvl;
		
		outputBuffer = new StringBuilder();
		
		outputArea = new JLabel() {
			public void setBounds(int x, int y, int w, int h) {
				int oldWidth = getWidth();
				
				super.setBounds(x, y, w, h);

				if (w != oldWidth) {										
					updateOutputArea();
				}
			}
		};
		outputArea.setBorder(new EmptyBorder(5, 5, 5, 5));
		outputArea.setVerticalTextPosition(JLabel.TOP);
		outputArea.setVerticalAlignment(JLabel.TOP);
				
		outputScrollPane = new JScrollPane(outputArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		//Command field
		commandField = new TTextField();
		
		commandField.registerKeyboardAction(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UndoManager um = commandField.getUndoManager();
				if (um.canUndo()) um.undo();
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), JComponent.WHEN_FOCUSED);
		
		commandField.registerKeyboardAction(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UndoManager um = commandField.getUndoManager();
				if (um.canRedo()) um.redo();
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), JComponent.WHEN_FOCUSED);
		
		Font font = commandField.getFont();
		commandField.setFont(new Font(Font.MONOSPACED, font.getStyle(), font.getSize()));
		
		commandField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				String text = "return (" + commandField.getText() + ")";
				commandField.setText("");
				
				synchronized (lock) {
					try {
						Object result = novel.exec(text);
						if (result != null) {
							outputBuffer.append(HtmlUtil.escapeHtml(String.valueOf(result)));
							outputBuffer.append("<br>");
						}
					} catch (LuaException e) {
						outputBuffer.append("<font color=red>");
						outputBuffer.append(HtmlUtil.escapeHtml(e.toString()));
						outputBuffer.append("</font><br>");
					}
				}
				
				updateOutputArea();
			}
		});
		
		setLayout(new BorderLayout(5, 5));
		add(outputScrollPane, BorderLayout.CENTER);
		add(commandField, BorderLayout.SOUTH);
	}
	
	//Functions
	public void updateOutputArea() {
		JScrollBar vbar = outputScrollPane.getVerticalScrollBar();
		
		int w = outputArea.getWidth();
		outputArea.setText("<html><div width=" + (w-11) + ">"
				+ outputBuffer.toString() + "</div></html>");
		outputScrollPane.revalidate();
		
		vbar.setValue(vbar.getMaximum());
	}
	
	//Getters
	
	//Setters
	
}
