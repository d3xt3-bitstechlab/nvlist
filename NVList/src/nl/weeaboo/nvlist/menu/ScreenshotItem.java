package nl.weeaboo.nvlist.menu;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import javax.imageio.ImageIO;
import javax.jnlp.FileContents;
import javax.jnlp.FileSaveService;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import nl.weeaboo.common.StringUtil;
import nl.weeaboo.game.GameLog;
import nl.weeaboo.io.ByteChunkOutputStream;
import nl.weeaboo.io.FileUtil;
import nl.weeaboo.jnlp.JnlpUtil;
import nl.weeaboo.nvlist.Game;
import nl.weeaboo.vn.IScreenshot;
import nl.weeaboo.vn.impl.nvlist.Novel;

public class ScreenshotItem extends GameMenuAction {

	@Override
	public JMenuItem createItem(Game game, Novel nvl) {
		JMenuItem item = new JMenuItem("Screenshot...");
		//item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PRINTSCREEN, 0));
		return item;
	}

	@Override
	public void actionPerformed(JMenuItem item, ActionEvent e, final Game game, Novel nvl) {
		final IScreenshot ss = nvl.getImageFactory().screenshot(Short.MIN_VALUE);
		nvl.getImageState().getTopLayer().getScreenshotBuffer().add(ss);
		waitForScreenshot(game.getExecutor(), ss);
	}

	/**
	 * Called on the event-dispatch thread
	 * @param ss A valid screenshot 
	 */
	protected void onScreenshotTaken(final IScreenshot ss) {
		final String format = "png";
		final String formatExt = "png";
		final String formatDesc = "PNG Image (*.png)";
		
		BufferedImage image = new BufferedImage(ss.getWidth(), ss.getHeight(), BufferedImage.TYPE_INT_BGR);
		image.setRGB(0, 0, ss.getWidth(), ss.getHeight(), ss.getARGB(), 0, ss.getWidth());
		
		final ByteArrayInputStream bin;
		try {
			ByteChunkOutputStream bout = new ByteChunkOutputStream();
			ImageIO.write(image, format, bout);
			bin = new ByteArrayInputStream(bout.toByteArray());				
		} catch (IOException ioe) {
			GameLog.w("Error saving screenshot", ioe);
			return;
		}
		
		final String folder = "";
		final String filename = "screenshot.png";
		
		FileSaveService fss = JnlpUtil.getFileSaveService();
		if (fss != null) {
			FileContents fc = null;
			try {
				fc = fss.saveFileDialog(folder, new String[] {formatExt}, bin, filename);
			} catch (IOException ioe) {
				GameLog.w("Error saving screenshot", ioe);
				return;
			}
			
			if (fc == null) {
				return;
			}

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					showSuccessDialog();
				}
			});
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JFileChooser fc = new JFileChooser(folder);
					fc.setSelectedFile(new File(folder, filename));
					fc.setFileFilter(new FileFilter() {
						public boolean accept(File f) {
							if (f.isDirectory()) return true;
							return StringUtil.getExtension(f.getName()).equals(formatExt);
						}
						public String getDescription() {
							return formatDesc;
						}
					});
					
					int res = fc.showSaveDialog(null);
					if (res != JFileChooser.APPROVE_OPTION) {
						return;
					}
					
					File file = fc.getSelectedFile();
					if (file == null || file.isDirectory()) {
						return;
					}

					try {
						FileUtil.writeBytes(file, bin);
						showSuccessDialog();
					} catch (IOException ioe) {
						GameLog.w("Error saving screenshot", ioe);
					}
				}
			});
		}
	}
	
	protected void showSuccessDialog() {
		JOptionPane.showMessageDialog(null, "Image saved successfully",
				"Screenshot Saved", JOptionPane.PLAIN_MESSAGE);		
	}
	
	protected void waitForScreenshot(ExecutorService exec, final IScreenshot ss) {
		exec.execute(new Runnable() {
			public void run() {
				//Wait for screenshot
				for (int n = 0; n < 5000 && !ss.isAvailable() && !ss.isCancelled(); n++) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						//Ignore
					}
				}
				
				if (ss.isAvailable()) {
					onScreenshotTaken(ss);
				}
			}
		});		
	}
	
}
