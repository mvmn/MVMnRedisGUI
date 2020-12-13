package x.mvmn.redisgui.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.RedisClient;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.StatefulRedisConnection;
import reactor.core.publisher.Mono;
import x.mvmn.redisgui.gui.util.SwingUtil;
import x.mvmn.redisgui.model.RedisConfigModel;

public class RedisClientGui {

	private final RedisClient redisClient;
	private final DefaultListModel<String> keysList = new DefaultListModel<>();
	private final AtomicReference<String> currentKeyScanCursor = new AtomicReference<>();
	private final JPanel contentSection;

	public RedisClientGui(String connectionName, RedisConfigModel config, File appHomeFolder) {
		this.redisClient = RedisClient.create(config.getClientResources(), config.toRedisUri());

		JFrame window = new JFrame(connectionName);
		Container contentPane = window.getContentPane();
		contentPane.setLayout(new BorderLayout());
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		contentPane.add(splitPane, BorderLayout.CENTER);

		splitPane.add(navPanel());
		contentSection = new JPanel();
		splitPane.add(contentSection);

		SwingUtil.minPrefWidth(window, 800);
		window.pack();
		SwingUtil.moveToScreenCenter(window);
		window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		window.addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {}

			@Override
			public void windowIconified(WindowEvent e) {}

			@Override
			public void windowDeiconified(WindowEvent e) {}

			@Override
			public void windowDeactivated(WindowEvent e) {}

			@Override
			public void windowClosing(WindowEvent e) {
				redisClient.shutdown();
			}

			@Override
			public void windowClosed(WindowEvent e) {}

			@Override
			public void windowActivated(WindowEvent e) {}
		});
		window.setVisible(true);
	}

	private JPanel navPanel() {
		JList<String> jlKeysList = new JList<>(keysList);
		JPanel pnl = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		JButton btnListKeys = new JButton("List keys");
		JCheckBox cbPaginate = new JCheckBox("Paging (SCAN)", true);
		JTextField tfListKeysPattern = new JTextField("*");
		tfListKeysPattern.getDocument().addDocumentListener(SwingUtil.onChange(e -> {
			btnListKeys.setEnabled(!tfListKeysPattern.getText().isEmpty());
		}));
		tfListKeysPattern.setBorder(BorderFactory.createTitledBorder("Pattern"));
		btnListKeys.addActionListener(e -> {
			String pattern = tfListKeysPattern.getText();
			boolean scan = cbPaginate.isSelected();
			String cursor = currentKeyScanCursor.get();
			SwingUtil.performSafely(() -> {
				List<String> redisKeys;
				try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
					if (scan) {
						KeyScanCursor<String> result = cursor != null
								? connection.sync().scan(ScanCursor.of(cursor), ScanArgs.Builder.matches(pattern))
								: connection.sync().scan(ScanArgs.Builder.matches(pattern));
						redisKeys = result.getKeys();
						currentKeyScanCursor.set(result.getCursor());
					} else {
						redisKeys = connection.sync().keys(pattern);
					}
				}
				setKeyList(redisKeys);
			});
		});

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.ipadx = 1;
		gbc.ipady = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		pnl.add(tfListKeysPattern, gbc);
		gbc.gridx = 1;
		gbc.gridy = 0;
		pnl.add(btnListKeys, gbc);
		gbc.gridx = 0;
		gbc.gridy = 1;
		pnl.add(cbPaginate, gbc);
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.BOTH;
		pnl.add(new JScrollPane(jlKeysList), gbc);
		return pnl;
	}

	private void setKeyList(List<String> redisKeys) {
		SwingUtilities.invokeLater(() -> {
			keysList.clear();
			AtomicInteger index = new AtomicInteger();
			redisKeys.forEach(e -> keysList.add(index.getAndIncrement(), e));
		});
	}

	public void connect() {
		SwingUtil.performSafely(() -> {
			// Perform ping as a test
			StatefulRedisConnection<String, String> connection = redisClient.connect();
			Mono<String> info = connection.reactive().info();
			Mono<Long> keyCount = connection.reactive().dbsize();

			System.out.println("Key count: " + keyCount.block());
			System.out.println("Info: " + info.block());
		});
	}
}
