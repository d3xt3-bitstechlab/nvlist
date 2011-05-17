package nl.weeaboo.nvlist.debug;

import java.awt.BorderLayout;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import nl.weeaboo.game.GameLog;
import nl.weeaboo.game.GameLogFormatter;
import nl.weeaboo.vn.impl.nvlist.Novel;

@SuppressWarnings("serial")
public class DebugOutputPanel extends JPanel {

	private final StringBuilder outputBuffer;
	private final JTextArea outputArea;
	private final JScrollPane outputScrollPane;
	
	private final LogHandler logHandler;
	
	public DebugOutputPanel(Object l, Novel nvl) {
		outputBuffer = new StringBuilder();
		
		outputArea = new JTextArea();
		outputArea.setWrapStyleWord(true);
		//outputArea.setLineWrap(true);
		outputScrollPane = new JScrollPane(outputArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);		
		
		setLayout(new BorderLayout(5, 5));
		add(outputScrollPane, BorderLayout.CENTER);
		
		logHandler = new LogHandler();
		
		addHierarchyListener(new HierarchyListener() {
			public void hierarchyChanged(HierarchyEvent e) {
				if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) {
					if (isDisplayable()) {
						addLoggerHandler();
					} else {					
						removeLoggerHandler();
					}
				}
			}			
		});
	}
	
	//Functions
	private void addLoggerHandler() {
		try {
			Logger logger = GameLog.getLogger();
			logger.addHandler(logHandler);
		} catch (SecurityException se) {
			//Ignore
		}		
	}
	
	private void removeLoggerHandler() {
		try {
			Logger logger = GameLog.getLogger();
			logger.removeHandler(logHandler);
		} catch (SecurityException se) {
			//Ignore
		}
	}
	
	protected void onOutputBufferChanged() {
		final String string = outputBuffer.toString();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JScrollBar vbar = outputScrollPane.getVerticalScrollBar();				
				outputArea.setText(string);
				outputScrollPane.revalidate();
				vbar.setValue(vbar.getMaximum());
			}
		});
	}
	
	//Getters
	
	//Setters
	
	//Inner Classes
	private class LogHandler extends Handler {

		private final Formatter formatter;
		
		public LogHandler() {
			setLevel(Level.ALL);
			
			formatter = new GameLogFormatter(false);
		}
		
		@Override
		public void publish(LogRecord record) {			
			outputBuffer.append(formatter.format(record));
			outputBuffer.append('\n');
			
			onOutputBufferChanged();
		}

		@Override
		public void flush() {
		}

		@Override
		public void close() throws SecurityException {
		}
		
	}
	
}
