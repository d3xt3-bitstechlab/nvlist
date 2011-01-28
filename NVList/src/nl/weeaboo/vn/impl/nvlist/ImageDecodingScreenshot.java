package nl.weeaboo.vn.impl.nvlist;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import nl.weeaboo.vn.impl.base.DecodingScreenshot;

class ImageDecodingScreenshot extends DecodingScreenshot {
	
	private static final long serialVersionUID = 1L;
	
	private final int targetW, targetH;
	
	public ImageDecodingScreenshot(byte b[], int targetW, int targetH) {
		super(b);
		
		this.targetW = targetW;
		this.targetH = targetH;
	}
	
	protected void tryLoad(byte[] data) {
		BufferedImage image = null;
		if (data != null) {
			try {
				ImageInputStream iin = ImageIO.createImageInputStream(new ByteArrayInputStream(data));
				Iterator<ImageReader> itr = ImageIO.getImageReaders(iin);
				while (itr.hasNext()) {
					ImageReader reader = itr.next();
					reader.setInput(iin);
					int w = reader.getWidth(0);
					int h = reader.getHeight(0);
					
					ImageReadParam readParam = reader.getDefaultReadParam();
					int sampleFactor = Math.max(1, Math.min(w/targetW, h/targetH));
					readParam.setSourceSubsampling(sampleFactor, sampleFactor, sampleFactor>>1, sampleFactor>>1);
										
					image = reader.read(0, readParam);
					if (image != null) {
						break;
					}
				}
			} catch (IOException ioe) {
				//Ignore
			}
		}
		
		if (image == null) {
			cancel();
		} else {
			int w = image.getWidth();
			int h = image.getHeight();
			int argb[] = new int[w * h];
			image.getRGB(0, 0, w, h, argb, 0, w);
			set(argb, w, h, w, h);
		}
	}
	
}