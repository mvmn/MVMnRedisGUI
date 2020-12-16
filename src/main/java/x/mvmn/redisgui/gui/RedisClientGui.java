package x.mvmn.redisgui.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
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
import reactor.core.publisher.Mono;
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
				}
			}
		});
		window.setVisible(true);
		info();
	}

	private JPanel navPanel() {
		JList<String> jlKeysList = new JList<>(keysList);
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
				try {
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
				} finally {
					btnListKeys.setEnabled(true);
					updateNextPageBtn();
				}
			});
		});
		btnKeysNextPage.addActionListener(e -> {
			btnListKeys.setEnabled(false);
			btnKeysNextPage.setEnabled(false);
			String cursor = currentKeyScanCursor.get();
			SwingUtil.performSafely(() -> {
				try {
					List<String> redisKeys;
					try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
						KeyScanCursor<String> result = connection.sync().scan(ScanCursor.of(cursor));
						redisKeys = result.getKeys();
						currentKeyScanCursor.set(result.getCursor());
					}
					setKeyList(redisKeys);
				} finally {
					btnListKeys.setEnabled(true);
					updateNextPageBtn();
				}
			});
		});

		return SwingUtil.panel(BorderLayout::new)
				.add(SwingUtil.panel(BorderLayout::new)
						.add(SwingUtil.panel(v -> new GridLayout(2, 1)).add(btnListKeys).add(btnKeysNextPage).panel(), BorderLayout.NORTH)
						.add(tfListKeysPattern, BorderLayout.CENTER)
						.add(cbPaginate, BorderLayout.SOUTH)
						.panel(), BorderLayout.NORTH)
				.add(new JScrollPane(jlKeysList), BorderLayout.CENTER)
				.add(btnPut, BorderLayout.SOUTH)
				.panel();
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

	public void info() {
		SwingUtil.performSafely(() -> {
			// Perform ping as a test
			StatefulRedisConnection<String, String> connection = redisClient.connect();
			Mono<String> info = connection.reactive().info();
			Mono<Long> keyCount = connection.reactive().dbsize();
			String text = "Key count: " + keyCount.block() + "\n\nInfo: " + info.block();
			SwingUtilities.invokeLater(() -> {
				contentSection.removeAll();
				contentSection.add(new JScrollPane(new JTextArea(text)), BorderLayout.CENTER);
				contentSection.invalidate();
				contentSection.revalidate();
				contentSection.repaint();
			});
		});
	}
}
