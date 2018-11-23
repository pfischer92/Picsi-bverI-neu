package imageprocessing;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import gui.TwinView;

/**
 * Image processing in module bverI
 * @author Christoph Stamm
 *
 */
public class BVER {
	private TwinView m_views;
	private ArrayList<ImageMenuItem> m_menuItems = new ArrayList<ImageMenuItem>();
	
	/**
	 * Registration of image operations
	 * @param views
	 */
	public BVER(TwinView views) {
		assert views != null : "views are null";
		m_views = views;

		m_menuItems.add(new ImageMenuItem("Channel R\tCtrl+1",SWT.CTRL | '1', new ChannelRGB(0)));
		m_menuItems.add(new ImageMenuItem("Channel G\tCtrl+2",SWT.CTRL | '2', new ChannelRGB(1)));
		m_menuItems.add(new ImageMenuItem("Channel B\tCtrl+3",SWT.CTRL | '3', new ChannelRGB(2)));
		m_menuItems.add(new ImageMenuItem("C&ropping\tCtrl+R",SWT.CTRL | 'R', new Cropping()));
		m_menuItems.add(new ImageMenuItem("Debayering\tCtrl+4",SWT.CTRL | '4', new Debayering()));
		m_menuItems.add(new ImageMenuItem("Histogram Equalization\tCtrl+5",SWT.CTRL | '5', new HistogramEqualization()));
		m_menuItems.add(new ImageMenuItem("Gamma Corection\tCtrl+6",SWT.CTRL | '6', new GammaCorrection()));
		m_menuItems.add(new ImageMenuItem("sRGBtoYUV\tCtrl+7",SWT.CTRL | '7', new sRGBtoYUV()));
		m_menuItems.add(new ImageMenuItem("Gewichteter Median-Filter\tCtrl+8",SWT.CTRL | '8', new GewichtMedianFilter()));
        m_menuItems.add(new ImageMenuItem("SobelEdgeDetection\tCtrl+9",SWT.CTRL | '9', new SobelEdgeDetection()));
        m_menuItems.add(new ImageMenuItem("Binarization\tCtrl+10",SWT.CTRL | '0', new Binarization()));
        m_menuItems.add(new ImageMenuItem("Morphologic Filter\tCtrl+11",SWT.CTRL | 'm', new MorphologicFilter()));


        // TODO add here further image processing entries (they are inserted into the BVER menu)
	}
	
	public void createMenuItems(Menu menu) {
		for(final ImageMenuItem item : m_menuItems) {
			MenuItem mi = new MenuItem(menu, SWT.PUSH);
			mi.setText(item.m_text);
			mi.setAccelerator(item.m_accelerator);
			mi.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					ImageData output = null;
					try {
						output = item.m_process.run(m_views.getFirstImage(), m_views.getFirstImageType());
					} catch(Throwable e) {
						int last = item.m_text.indexOf('\t');
						if (last == -1) last = item.m_text.length();
						String location = item.m_text.substring(0, last).replace("&", "");
						m_views.m_mainWnd.showErrorDialog("ImageProcessing", location, e);
					}						
					if (output != null) {
						m_views.showImageInSecondView(output);
					}
				}
			});
		}
	}
	
	public boolean isEnabled(int i) {
		return m_menuItems.get(i).m_process.isEnabled(m_views.getFirstImageType());
	}

}
