package gui;
import main.Picsi;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.*;

/**
 * Viewer class
 * 
 * @author Christoph Stamm
 *
 */
public class View extends Canvas {
	private TwinView m_twins;
	private int m_scrollPosX, m_scrollPosY; // origin of the visible view (= pixel when zoom = 1)
	private Image m_image;					// device dependent image used in painting
	private ImageData m_imageData;			// device independent image used in image processing
	private int m_imageType;
	private PrinterData m_printerData;
	private float m_zoom = 1.0f;
	
	public View(TwinView compo) {
		super(compo, SWT.V_SCROLL | SWT.H_SCROLL | SWT.NO_REDRAW_RESIZE | SWT.NO_BACKGROUND);
		m_twins = compo;
		
		setBackground(new Color(getDisplay(), 128, 128, 255));
		
		// Hook resize listener
		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent event) {
				if (m_image != null) m_twins.refresh(true);
			}
		});
		
		// Set up the scroll bars.
		ScrollBar horizontal = getHorizontalBar();
		horizontal.setVisible(true);
		horizontal.setMinimum(0);
		horizontal.setEnabled(false);
		horizontal.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				int x = -((ScrollBar)event.widget).getSelection();
				scrollHorizontally(x);
				m_twins.synchronizeHorizontally(View.this, x);
			}
		});
		ScrollBar vertical = getVerticalBar();
		vertical.setVisible(true);
		vertical.setMinimum(0);
		vertical.setEnabled(false);
		vertical.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				int y = -((ScrollBar)event.widget).getSelection();
				scrollVertically(y);
				m_twins.synchronizeVertically(View.this, y);
			}
		});
		
		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent event) {
				if (m_image != null) {
					paint(event);
				} else {
					Rectangle bounds = getBounds();
					event.gc.fillRectangle(0, 0, bounds.width, bounds.height);
				}
			}
		});
		addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent event) {
				View.this.setFocus();
				if (m_image != null) {
					m_twins.m_mainWnd.showColorForPixel(getPixelInfoAt(event.x,  event.y));
				}
			}
		});
		addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent event) {
				if (m_image != null && !m_twins.hasAutoZoom()) {
					//System.out.println("" + event.x + " " + event.y + " " + event.count);
					if (event.count < 0) {
						zoom(1.5f);
					} else if (event.count > 0) {
						zoom(1/1.5f);
					}
				}
			}
		});
	}

	public ImageData getImageData() {
		return m_imageData;
	}
	
	public boolean isPortrait() {
		// assume 16:10 screen resolution
		assert m_imageData != null : "m_imageData is null";
		return m_imageData.width*10 < m_imageData.height*16;
	}
	
	public float getZoom() {
		return m_zoom;
	}

	public void setZoom(float f) {
		m_zoom = f;
	}
	
	private void zoom(float f) {
		m_zoom *= f;
		if (m_zoom > 100) {
			m_zoom = 100;
		} else if (m_zoom < 0.01f) {
			m_zoom = 0.01f;
		}
		m_twins.updateZoom(this);
	}
	
	public void computeBestZoomFactor() {
		assert m_imageData != null : "m_imageData is null";

		Rectangle canvasBounds = getClientArea();
		float xFactor = (float)canvasBounds.width/m_imageData.width;
		float yFactor = (float)canvasBounds.height/m_imageData.height;
		
		m_zoom = Math.min(xFactor, yFactor);
	}
	
	public PrinterData getPrinterData() {
		return m_printerData;
	}
	
	public int getImageType() {
		return m_imageType;
	}
	
	public int getImageWidth() {
		return m_imageData.width;
	}
	
	public int getImageHeight() {
		return m_imageData.height;
	}
	
	public void setImageData(ImageData imageData) {
		if (m_image != null) m_image.dispose();
		m_imageData = imageData;
		if (m_imageData != null) {
			m_imageType = Picsi.determineImageType(m_imageData);
			m_image = new Image(getDisplay(), imageData);
		} else {
			m_image = null;
		}
		updateScrollBars();
	}
	
	public void scrollHorizontally(int x) {
		if (m_imageData != null) {
			Rectangle canvasBounds = getClientArea();
			int width = zoomedWidth();
			int height = zoomedHeight();
			if (width > canvasBounds.width) {
				// Only scroll if the image is bigger than the canvas.
				if (x + width < canvasBounds.width) {
					// Don't scroll past the end of the image.
					x = canvasBounds.width - width;
				}
				scroll(x, m_scrollPosY, m_scrollPosX, m_scrollPosY, width, height, false);
				getHorizontalBar().setSelection(-x); // place scroll bar
				m_scrollPosX = x;
			}
		}
	}
	
	public void scrollVertically(int y) {
		if (m_imageData != null) {
			Rectangle canvasBounds = getClientArea();
			int width = zoomedWidth();
			int height = zoomedHeight();
			if (height > canvasBounds.height) {
				// Only scroll if the image is bigger than the canvas.
				if (y + height < canvasBounds.height) {
					// Don't scroll past the end of the image.
					y = canvasBounds.height - height;
				}
				scroll(m_scrollPosX, y, m_scrollPosX, m_scrollPosY, width, height, false);
				getVerticalBar().setSelection(-y); // place scroll bar
				m_scrollPosY = y;
			}
		}
	}

	public void updateScrollBars() {
		// Set the max and thumb for the image canvas scroll bars.
		ScrollBar horizontal = getHorizontalBar();
		ScrollBar vertical = getVerticalBar();
		Rectangle canvasBounds = getClientArea();
		
		int width = zoomedWidth();
		if (width > canvasBounds.width) {
			// The image is wider than the canvas.
			horizontal.setEnabled(true);
			horizontal.setMaximum(width);
			horizontal.setThumb(canvasBounds.width);
			horizontal.setPageIncrement(canvasBounds.width);
		} else {
			// The canvas is wider than the image.
			horizontal.setEnabled(false);
			if (m_scrollPosX != 0) {
				// Make sure the image is completely visible.
				m_scrollPosX = 0;
			}
		}
		int height = zoomedHeight();
		if (height > canvasBounds.height) {
			// The image is taller than the canvas.
			vertical.setEnabled(true);
			vertical.setMaximum(height);
			vertical.setThumb(canvasBounds.height);
			vertical.setPageIncrement(canvasBounds.height);
		} else {
			// The canvas is taller than the image.
			vertical.setEnabled(false);
			if (m_scrollPosY != 0) {
				// Make sure the image is completely visible.
				m_scrollPosY = 0;
			}
		}
		redraw();
	}
	
	public Object[] getPixelInfoAt(int x, int y) {
		if (m_imageData == null) return null;
		
		x = client2ImageX(x);
		y = client2ImageY(y);
		
		if (x >= 0 && x < m_imageData.width && y >= 0 && y < m_imageData.height) {
			int pixel = m_imageData.getPixel(x, y);
			RGB rgb = m_imageData.palette.getRGB(pixel);
			boolean hasAlpha = false;
			int alphaValue = 0;
			if (m_imageData.alphaData != null && m_imageData.alphaData.length > 0) {
				hasAlpha = true;
				alphaValue = m_imageData.getAlpha(x, y);
			}
			String rgbMessageFormat = (hasAlpha) ? "RGBA '{'{0}, {1}, {2}, {3}'}'" : "RGB '{'{0}, {1}, {2}'}'";
			String rgbHexMessageFormat = (hasAlpha) ? "0x{0}, 0x{1}, 0x{2}, 0x{3}" : "0x{0}, 0x{1}, 0x{2}";
			Object[] rgbArgs = {
					Integer.toString(rgb.red),
					Integer.toString(rgb.green),
					Integer.toString(rgb.blue),
					Integer.toString(alphaValue)
			};
			Object[] rgbHexArgs = {
					Integer.toHexString(rgb.red),
					Integer.toHexString(rgb.green),
					Integer.toHexString(rgb.blue),
					Integer.toHexString(alphaValue)
			};
			Object[] args = {
					x, y, pixel,
					Integer.toHexString(pixel),
					Picsi.createMsg(rgbMessageFormat, rgbArgs),
					Picsi.createMsg(rgbHexMessageFormat, rgbHexArgs),
					(pixel == m_imageData.transparentPixel) ? "(transparent)" : ""};
			return args;
		} else {
			return null;
		}
	}
	
	int client2ImageX(int x) {
		return (int)Math.floor((x - m_scrollPosX)/m_zoom);
	}
	
	int client2ImageY(int y) {
		return (int)Math.floor((y - m_scrollPosY)/m_zoom);
	}
	
	int client2Image(int d) {
		return (int)Math.floor(d/m_zoom);
	}
	
	int image2Client(int d) {
		return (int)Math.floor(d*m_zoom);
	}
	
	private void paint(PaintEvent event) {		
		GC gc = event.gc;
		final int w = zoomedWidth();
		final int h = zoomedHeight();
		//System.out.println("w = " + w + ", h = " + h + ", w/h = " + (double)w/h + ", scrollX = " + m_scrollPosX + ", scrollY = " + m_scrollPosY);
		
		/* If any of the background is visible, fill it with the background color. */
		Rectangle bounds = getBounds();
		if (m_imageData.getTransparencyType() != SWT.TRANSPARENCY_NONE) {
			/* If there is any transparency at all, fill the whole background. */
			gc.fillRectangle(0, 0, bounds.width, bounds.height);
		} else {
			/* Otherwise, just fill in the backwards L. */
			if (m_scrollPosX + w < bounds.width) gc.fillRectangle(m_scrollPosX + w, 0, bounds.width - (m_scrollPosX + w), bounds.height);
			if (m_scrollPosY + h < bounds.height) gc.fillRectangle(0, m_scrollPosY + h, m_scrollPosX + w, bounds.height - (m_scrollPosY + h));
		}

		if (m_image != null) {
			/* Draw the image */
			gc.drawImage(
				m_image,
				0,
				0,
				m_imageData.width,
				m_imageData.height,
				m_scrollPosX,
				m_scrollPosY,
				w,
				h);		
		}
	}
	
	public Throwable print(Display display) {
		try {
			Printer printer = new Printer(m_printerData);
			Point screenDPI = display.getDPI();
			Point printerDPI = printer.getDPI();
			int scaleFactor = printerDPI.x/screenDPI.x;
			Rectangle trim = printer.computeTrim(0, 0, 0, 0);
			if (printer.startJob(m_twins.getDocument(this).getFileName())) {
				if (printer.startPage()) {
					GC gc = new GC(printer);
					Image printerImage = new Image(printer, m_imageData);
					gc.drawImage(
						printerImage,
						0,
						0,
						m_imageData.width,
						m_imageData.height,
						-trim.x,
						-trim.y,
						scaleFactor*m_imageData.width,
						scaleFactor*m_imageData.height);
					printerImage.dispose();
					gc.dispose();
					printer.endPage();
				}
				printer.endJob();
			}
			printer.dispose();
		} catch (SWTError e) {
			return e;
		}
		return null;
	}
	
	private int zoomedWidth() {
		return (m_imageData == null) ? 0 : image2Client(m_imageData.width);
	}

	private int zoomedHeight() {
		return (m_imageData == null) ? 0 : image2Client(m_imageData.height);
	}

}
