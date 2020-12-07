package x.mvmn.redisgui;

import java.io.File;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;

import io.lettuce.core.RedisClient;
import x.mvmn.redisgui.gui.ConnectionsManagerWindow;
import x.mvmn.redisgui.gui.util.SwingUtil;
import x.mvmn.redisgui.lang.CallUtil;
import x.mvmn.redisgui.util.FileBackedProperties;

public class MVMnRedisGUIApplication {

	public static void main(String args[]) {
		File userHome = new File(System.getProperty("user.home"));
		File appHomeFolder = new File(userHome, ".mvmnredisgui");
		if (!appHomeFolder.exists()) {
			appHomeFolder.mkdir();
		}
		SortedSet<String> existingConnectionConfigs = Arrays.asList(appHomeFolder.listFiles())
				.stream()
				.filter(File::isFile)
				.map(File::getName)
				.filter(fn -> fn.toLowerCase().endsWith(".properties"))
				.map(fn -> fn.substring(0, fn.length() - ".properties".length()))
				.collect(Collectors.toCollection(TreeSet::new));
		File configFile = new File(appHomeFolder, "config.cfg");
		FileBackedProperties appConfig = new FileBackedProperties(configFile);

		String lookAndFeelName = appConfig.getProperty("gui.lookandfeel");
		SwingUtilities.invokeLater(() -> {
			Stream.of(FlatLightLaf.class, FlatIntelliJLaf.class, FlatDarkLaf.class, FlatDarculaLaf.class)
					.forEach(lafClass -> UIManager.installLookAndFeel(lafClass.getSimpleName(), lafClass.getCanonicalName()));

			if (lookAndFeelName != null) {
				SwingUtil.setLookAndFeel(lookAndFeelName);
			}

			JFrame connectionsManagerWindow = new ConnectionsManagerWindow(appConfig, appHomeFolder, existingConnectionConfigs,
					CallUtil.unsafe(cfg -> {
						RedisClient rc = RedisClient.create(cfg.getB(), cfg.getA());
						// Perform ping as a test
						rc.connect().sync().ping();
						SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Connection successfull"));
					}), cfg -> new RedisClientGui(cfg.getA(), cfg.getB(), cfg.getC(), appHomeFolder));
			SwingUtil.prefSizeRatioOfScreenSize(connectionsManagerWindow, 0.7f);
			connectionsManagerWindow.pack();
			SwingUtil.moveToScreenCenter(connectionsManagerWindow);
			connectionsManagerWindow.setVisible(true);
		});
	}
}