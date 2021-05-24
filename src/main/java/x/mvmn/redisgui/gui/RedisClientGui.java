package x.mvmn.redisgui.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.RedisClient;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import x.mvmn.redisgui.gui.util.DefaultWindowListener;
import x.mvmn.redisgui.gui.util.SwingUtil;
import x.mvmn.redisgui.model.RedisConfigModel;

public class RedisClientGui {

	private final RedisClient redisClient;
	private final DefaultListModel<String> keysList = new DefaultListModel<>();
	private final AtomicReference<String> currentKeyScanCursor = new AtomicReference<>();
	private final JPanel contentSection;
	private final JCheckBox cbPaginate;
	private final JButton btnListKeys;
	private final JButton btnKeysNextPage;
	private final JButton btnPut;
	private final JTextField tfKeyCount = new JTextField("n/a");
	private final JButton btnGetKeyCount = new JButton("Get");
	private final JButton btnGetServerInfo = new JButton("Get server info");

	public RedisClientGui(String connectionName, RedisConfigModel config, File appHomeFolder) {
		this.redisClient = RedisClient.create(config.getClientResources(), config.toRedisUri());

		btnKeysNextPage = new JButton("Next page");
		btnKeysNextPage.setEnabled(false);
		cbPaginate = new JCheckBox("Paging (SCAN)", true);
		cbPaginate.addChangeListener(e -> updateNextPageBtn());
		btnListKeys = new JButton("List keys");
		btnPut = new JButton("Put");

		JFrame window = new JFrame(connectionName);
		Container contentPane = window.getContentPane();
		contentPane.setLayout(new BorderLayout());
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		contentPane.add(splitPane, BorderLayout.CENTER);

		splitPane.add(navPanel());
		contentSection = new JPanel(new BorderLayout());
		splitPane.add(contentSection);

		SwingUtil.prefSizeRatioOfScreenSize(window, 0.6f);
		window.pack();
		SwingUtil.moveToScreenCenter(window);
		window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		window.addWindowListener(new DefaultWindowListener() {
			@Override
			public void windowClosing(WindowEvent e) {
				try {
					redisClient.shutdown();
					redisClient.getResources().shutdown();
				} catch (Exception ex) {
					ex.printStackTrace();
					SwingUtil.showError("Error shutting down Redis client", ex);
				}
			}
		});
		window.setVisible(true);
		SwingUtilities.invokeLater(() -> refreshKeyCount());
		SwingUtilities.invokeLater(() -> showServerInfo());
	}

	private JPanel navPanel() {
		JList<String> jlKeysList = new JList<>(keysList);
		jlKeysList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 2) {
					int selectedKey = jlKeysList.getSelectedIndex();
					if (selectedKey >= 0 && selectedKey < keysList.size()) {
						String key = keysList.get(selectedKey);
						SwingUtil.performSafely(() -> {
							try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
								RedisCommands<String, String> syncConn = connection.sync();
								String keyTypeStr = syncConn.type(key);
								Long ttl = syncConn.ttl(key);
								SwingUtilities.invokeLater(() -> {
									contentSection.removeAll();
									contentSection.add(
											new JScrollPane(new JTextArea("Key [" + key + "] type " + keyTypeStr + ". TTL: " + ttl)),
											BorderLayout.CENTER);
									contentSection.invalidate();
									contentSection.revalidate();
									contentSection.repaint();
								});
							}
						});
					}
				}
			}
		});
		JTextField tfListKeysPattern = new JTextField("*");
		tfListKeysPattern.getDocument()
				.addDocumentListener(SwingUtil.onChange(e -> btnListKeys.setEnabled(!tfListKeysPattern.getText().isEmpty())));
		tfListKeysPattern.setBorder(BorderFactory.createTitledBorder("Pattern"));
		btnListKeys.addActionListener(e -> {
			btnListKeys.setEnabled(false);
			btnKeysNextPage.setEnabled(false);
			String pattern = tfListKeysPattern.getText();
			boolean scan = cbPaginate.isSelected();
			SwingUtil.performSafely(() -> {
				List<String> redisKeys;
				try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
					String newCursor = null;
					if (scan) {
						KeyScanCursor<String> result = connection.sync().scan(ScanArgs.Builder.matches(pattern));
						redisKeys = result.getKeys();
						newCursor = result.getCursor();
					} else {
						redisKeys = connection.sync().keys(pattern);
					}
					currentKeyScanCursor.set(newCursor);
				}
				setKeyList(redisKeys);

			}, () -> {
				btnListKeys.setEnabled(true);
				updateNextPageBtn();
			}, true);
		});
		btnKeysNextPage.addActionListener(e -> {
			btnListKeys.setEnabled(false);
			btnKeysNextPage.setEnabled(false);
			String cursor = currentKeyScanCursor.get();
			SwingUtil.performSafely(() -> {
				List<String> redisKeys;
				try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
					KeyScanCursor<String> result = connection.sync().scan(ScanCursor.of(cursor));
					redisKeys = result.getKeys();
					currentKeyScanCursor.set(result.getCursor());
				}
				setKeyList(redisKeys);
			}, () -> {
				btnListKeys.setEnabled(true);
				updateNextPageBtn();
			}, true);
		});

		tfKeyCount.setEditable(false);
		tfKeyCount.setBorder(BorderFactory.createTitledBorder("Key count"));
		btnGetKeyCount.addActionListener(e -> refreshKeyCount());

		btnGetServerInfo.addActionListener(e -> showServerInfo());

		return SwingUtil.panel(BorderLayout::new)
				.add(SwingUtil.panel(v -> new GridLayout(3, 2))
						.add(tfKeyCount)
						.add(btnGetKeyCount)
						.add(tfListKeysPattern)
						.add(btnListKeys)
						.add(cbPaginate, BorderLayout.CENTER)
						.add(btnKeysNextPage, BorderLayout.SOUTH)
						.panel(), BorderLayout.NORTH)
				.add(new JScrollPane(jlKeysList), BorderLayout.CENTER)
				.add(SwingUtil.panel(v -> new GridLayout(2, 1)).add(btnPut).add(btnGetServerInfo).panel(), BorderLayout.SOUTH)
				.panel();
	}

	private void refreshKeyCount() {
		btnGetKeyCount.setEnabled(false);
		SwingUtil.performSafely(() -> {
			try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
				Long keyCount = connection.sync().dbsize();
				if (keyCount != null) {
					SwingUtilities.invokeLater(() -> {
						tfKeyCount.setText(keyCount.toString());
						tfKeyCount.invalidate();
					});
				}
			}
		}, () -> btnGetKeyCount.setEnabled(true), true);
	}

	private void updateNextPageBtn() {
		String cursor = currentKeyScanCursor.get();
		btnKeysNextPage.setEnabled(cbPaginate.isSelected() && cursor != null && !"0".equals(cursor));
	}

	private void setKeyList(List<String> redisKeys) {
		SwingUtilities.invokeLater(() -> {
			keysList.clear();
			AtomicInteger index = new AtomicInteger();
			redisKeys.forEach(e -> keysList.add(index.getAndIncrement(), e));
		});
	}

	public void showServerInfo() {
		btnGetServerInfo.setEnabled(false);
		SwingUtil.performSafely(() -> {
			try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
				String info = connection.sync().info();
				SwingUtilities.invokeLater(() -> {
					contentSection.removeAll();
					JTextArea txa = new JTextArea(info);
					txa.setEditable(false);
					contentSection.add(new JScrollPane(txa), BorderLayout.CENTER);
					contentSection.invalidate();
					contentSection.revalidate();
					contentSection.repaint();
				});
			}
		}, () -> btnGetServerInfo.setEnabled(true), true);
	}
}
