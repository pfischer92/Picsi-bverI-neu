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
 * Image processing module magb
 * @author Christoph Stamm
 *
 */
public class MAGB {
	private TwinView m_views;
	private ArrayList<ImageMenuItem> m_menuItems = new ArrayList<ImageMenuItem>();
	
	/**
	 * Registration of image operations
	 * @param views
	 */
	public MAGB(TwinView views) {
		assert views != null : "views are null";
		m_views = views;
		
		m_menuItems.add(new ImageMenuItem("&Invert\tF1",SWT.F1, new Inverter()));
		m_menuItems.add(new ImageMenuItem("&Grauwertbild\tF2", SWT.F2, new Grauwertbild()));
		m_menuItems.add(new ImageMenuItem("&Dithering\tF3", SWT.F3, new Dithering()));
		m_menuItems.add(new ImageMenuItem("&Rotate\tF4", SWT.F4, new Rotate()));
		m_menuItems.add(new ImageMenuItem("&Scale\tF5", SWT.F5, new Scale()));
		m_menuItems.add(new ImageMenuItem("&Filter\tF6", SWT.F6, new Filter()));
		m_menuItems.add(new ImageMenuItem("&Edge Detection\tF7", SWT.F7, new EdgeDetection()));
		m_menuItems.add(new ImageMenuItem("&Convolve Efficient\tF8", SWT.F8, new ConvolveEfficient()));
		m_menuItems.add(new ImageMenuItem("&Filter Master\tF9", SWT.F9, new FilterMaster()));
		m_menuItems.add(new ImageMenuItem("&Flip at axis\tF10", SWT.F10, new FlipAtYAxis()));
		m_menuItems.add(new ImageMenuItem("&Affine\tF11", SWT.F11, new Affine()));
		m_menuItems.add(new ImageMenuItem("&All RGB\tF12", SWT.F12, new AllRGB()));
		// TODO add here further image processing entries (they are inserted into the MAGB menu)
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
