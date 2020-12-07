package x.mvmn.redisgui.gui.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.text.NumberFormat;
import java.util.Arrays;

import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.text.NumberFormatter;

import x.mvmn.redisgui.lang.StackTraceUtil;
import x.mvmn.redisgui.lang.UnsafeOperation;

public class SwingUtil {

	protected static ErrorMessageDialog errorMessageDialog = new ErrorMessageDialog(null);

	public static void performSafely(final UnsafeOperation operation) {
		new Thread(new Runnable() {
			public void run() {
				try {
					operation.run();
				} catch (final Exception e) {
					e.printStackTrace();
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							errorMessageDialog.show(null, "Error occurred: " + e.getClass().getName() + " " + e.getMessage(),
									StackTraceUtil.toString(e));
						}
					});
				}
			}
		}).start();
	}

	public static void showError(final String message, final Throwable e) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				errorMessageDialog.show(null, message + ": " + e.getClass().getName() + " " + e.getMessage(), StackTraceUtil.toString(e));
			}
		});
	}

	public static void moveToScreenCenter(final Component component) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension componentSize = component.getSize();
		int newComponentX = screenSize.width - componentSize.width;
		if (newComponentX >= 0) {
			newComponentX = newComponentX / 2;
		} else {
			newComponentX = 0;
		}
		int newComponentY = screenSize.height - componentSize.height;
		if (newComponentY >= 0) {
			newComponentY = newComponentY / 2;
		} else {
			newComponentY = 0;
		}
		component.setLocation(newComponentX, newComponentY);
	}

	public static JPanel twoComponentPanel(Component a, Component b) {
		JPanel result = new JPanel(new GridLayout(1, 2));

		result.add(a);
		result.add(b);

		return result;
	}

	public static <T extends Component> T prefSizeRatioOfScreenSize(T component, float ratio) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension prefSize = new Dimension((int) (screenSize.width * ratio), (int) (screenSize.height * ratio));
		component.setPreferredSize(prefSize);
		return component;
	}

	public static <T extends Component> T minPrefWidth(T component, int minimumPreferredWidth) {
		component.setPreferredSize(
				new Dimension(Math.max(minimumPreferredWidth, component.getPreferredSize().width), component.getPreferredSize().height));
		return component;
	}

	public static JTextField numericOnlyTextField(Long initialValue, Long min, Long max, boolean allowNegative) {
		NumberFormatter formatter = new NumberFormatter(NumberFormat.getInstance());
		formatter.setValueClass(Long.class);
		formatter.setMinimum(min != null ? min : (allowNegative ? Long.MIN_VALUE : 0));
		formatter.setMaximum(max != null ? max : Long.MAX_VALUE);
		formatter.setAllowsInvalid(false);
		formatter.setCommitsOnValidEdit(true);
		JFormattedTextField txf = new JFormattedTextField(formatter);
		txf.setValue(initialValue != null ? initialValue : 0L);

		return txf;
	}

	public static String getLookAndFeelName(LookAndFeel lnf) {
		return Arrays.stream(UIManager.getInstalledLookAndFeels())
				.filter(lnfInfo -> lnfInfo.getClassName().equals(lnf.getClass().getCanonicalName())).map(LookAndFeelInfo::getName).findAny()
				.orElse(null);
	}

	public static void updateComponentTreeUIForAllWindows() {
		for (Frame frame : Frame.getFrames()) {
			updateComponentTreeUI(frame);
		}
	}

	public static void updateComponentTreeUI(Window window) {
		for (Window childWindow : window.getOwnedWindows()) {
			updateComponentTreeUI(childWindow);
		}
		SwingUtilities.updateComponentTreeUI(window);
	}

	public static void setLookAndFeel(String lookAndFeelName) {
		Arrays.stream(UIManager.getInstalledLookAndFeels()).filter(lnf -> lnf.getName().equals(lookAndFeelName)).findAny()
				.ifPresent(lnf -> {
					try {
						if (!UIManager.getLookAndFeel().getName().equals(lnf.getName())) {
							UIManager.setLookAndFeel(lnf.getClassName());
							updateComponentTreeUIForAllWindows();
						}
					} catch (Exception error) {
						showError("Error setting look&feel to " + lookAndFeelName, error);
					}
				});
	}
}
