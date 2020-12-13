package x.mvmn.redisgui.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import io.lettuce.core.RedisURI;
import x.mvmn.redisgui.gui.util.SwingUtil;
import x.mvmn.redisgui.model.RedisConfigModel;
import x.mvmn.redisgui.model.RedisConfigModel.RedisConnectionType;

public class RedisConfigPanel extends JPanel {
	private static final long serialVersionUID = -4990237494301838452L;

	private volatile boolean dirty;
	private final CopyOnWriteArrayList<Runnable> dirtyListeners = new CopyOnWriteArrayList<>();
	private final JTextField tfRedisUri = new JTextField("");
	private final JComboBox<RedisConnectionType> cbxConnectionType = new JComboBox<>(RedisConnectionType.values());
	private final RedisConfigModel model;

	// _____________ | alone | sock | snt | cluster
	// host+port ___ | __V__ | _-__ | _V_ | V
	// +nodes ______ | __-__ | _-__ | _V_ | V
	// username ____ | __V__ | _V__ | _V_ | V
	// password ____ | __V__ | _V__ | _V_ | V
	// db number ___ | __V__ | _V__ | _V_ | V
	// socket ______ | __-__ | _V__ | _-_ | -
	// snt master ID | __-__ | _-__ | _V_ | -
	// ssl _________ | __V__ | _-__ | _V_ | V
	// verify peer _ | __V__ | _-__ | _V_ | V
	// tls _________ | __V__ | _-__ | _V_ | V
	// clientName __ | __V__ | _V__ | _V_ | V
	// timeout _____ | __V__ | _V__ | _V_ | V

	private final JTextField host = SwingUtil.withTitle(new JTextField(), "Host");
	private final JFormattedTextField port = SwingUtil
			.withTitle(SwingUtil.numericOnlyTextField((long) RedisURI.DEFAULT_REDIS_PORT, 0L, 65535L, false), "Port");
	private final JTextField username = SwingUtil.withTitle(new JTextField(), "Username");
	private final JPasswordField password = SwingUtil.withTitle(new JPasswordField(), "Password");
	private final JFormattedTextField dbNumber = SwingUtil
			.withTitle(SwingUtil.numericOnlyTextField(0L, 0L, (long) Integer.MAX_VALUE, false), "DB number");
	private final JTextField clientName = SwingUtil.withTitle(new JTextField(), "Client name");
	private final JTextField socket = SwingUtil.withTitle(new JTextField(), "UNIX socket");
	private final JTextField sentinelMasterId = SwingUtil.withTitle(new JTextField(), "Sentinel master ID");
	private final JCheckBox ssl = new JCheckBox("SSL");
	private final JCheckBox verifyPeer = new JCheckBox("Verify peer");
	private final JCheckBox tls = new JCheckBox("TLS");
	private final JFormattedTextField timeout = SwingUtil.withTitle(SwingUtil.numericOnlyTextField(60L, 0L, Long.MAX_VALUE, false),
			"Timeout (seconds)");

	private final JPanel cfgContent = new JPanel(new BorderLayout());

	public RedisConfigPanel() {
		this(null);
	}

	public RedisConfigPanel(RedisConfigModel configModel) {
		super(new BorderLayout());

		// TODO: add additional nodes config
		this.model = configModel != null ? configModel : new RedisConfigModel(null);
		host.setText(model.getHost());
		port.setText(String.valueOf(model.getPort()));
		dbNumber.setText(String.valueOf(model.getDatabase()));
		clientName.setText(model.getClientName());
		timeout.setText(String.valueOf(model.getTimeout().getSeconds()));

		Arrays.asList(host, username, password, clientName, socket, sentinelMasterId).forEach(tf -> SwingUtil.bind(tf, e -> onChange()));
		Arrays.asList(port, dbNumber, timeout).forEach(tf -> SwingUtil.bind(tf, (PropertyChangeEvent e) -> onChange()));
		Arrays.asList(ssl, verifyPeer, tls).forEach(cb -> SwingUtil.bind(cb, e -> onChange()));
		cbxConnectionType.addActionListener(e -> onConnectionTypeChange());

		tfRedisUri.setEditable(false);
		tfRedisUri.setBorder(BorderFactory.createTitledBorder("URI"));
		cbxConnectionType.setBorder(BorderFactory.createTitledBorder("Connection type"));
		cbxConnectionType.setSelectedItem(model.getConnectionType());
		onConnectionTypeChange();
		setNotDirty();

		SwingUtil.bind(host, e -> model.setHost(host.getText()));
		SwingUtil.bindNumeric(port, v -> model.setPort(v.intValue()));
		SwingUtil.bind(username, e -> model.setUsername(username.getText()));
		SwingUtil.bind(password, e -> model.setPassword(password.getPassword()));
		SwingUtil.bindNumeric(dbNumber, v -> model.setDatabase(v.intValue()));
		SwingUtil.bind(sentinelMasterId, e -> model.setSentinelMasterId(sentinelMasterId.getText()));
		SwingUtil.bind(ssl, v -> model.setSsl(v));
		SwingUtil.bind(verifyPeer, v -> model.setVerifyPeer(v));
		SwingUtil.bind(tls, v -> model.setStartTls(v));
		SwingUtil.bind(clientName, v -> model.setClientName(clientName.getText()));
		SwingUtil.bindNumeric(timeout, v -> model.setTimeout(Duration.ofSeconds(v)));
		SwingUtil.bind(socket, e -> model.setSocket(socket.getText()));

		this.add(SwingUtil.panel(pnl -> new GridLayout(2, 1)).add(tfRedisUri).add(cbxConnectionType).panel(), BorderLayout.NORTH);
		this.add(cfgContent, BorderLayout.CENTER);
	}

	private JPanel buildCfgPanel(RedisConnectionType connectionType) {
		switch (connectionType) {
			default:
			case STANDALONE:
			case CLUSTER:
				return SwingUtil.panel(pnl -> new GridLayout(10, 1))
						.add(host)
						.add(port)
						.add(username)
						.add(password)
						.add(dbNumber)
						.add(ssl)
						.add(verifyPeer)
						.add(tls)
						.add(clientName)
						.add(timeout)
						.panel();
			case UNIX_SOCKET:
				return SwingUtil.panel(pnl -> new GridLayout(5, 1))
						.add(socket)
						.add(username)
						.add(password)
						.add(clientName)
						.add(timeout)
						.panel();
			case SENTINEL:
				return SwingUtil.panel(pnl -> new GridLayout(11, 1))
						.add(host)
						.add(port)
						.add(username)
						.add(password)
						.add(dbNumber)
						.add(sentinelMasterId)
						.add(ssl)
						.add(verifyPeer)
						.add(tls)
						.add(clientName)
						.add(timeout)
						.panel();
		}
	}

	private void onConnectionTypeChange() {
		RedisConnectionType connectionType = (RedisConnectionType) cbxConnectionType.getSelectedItem();
		model.setConnectionType(connectionType);
		JPanel panel = buildCfgPanel(connectionType);
		cfgContent.removeAll();
		cfgContent.add(new JScrollPane(panel), BorderLayout.CENTER);
		cfgContent.invalidate();
		cfgContent.revalidate();
		cfgContent.repaint();
		onChange();
	}

	public RedisConfigModel getCurrentState() {
		return model;
	}

	private void onChange() {
		try {
			this.tfRedisUri.setText(model.toRedisUri().toString());
		} catch (IllegalArgumentException iae) {
			this.tfRedisUri.setText(iae.getMessage());
		}
		this.dirty = true;
		notifyListeners();
		// TODO: validation
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setNotDirty() {
		this.dirty = false;
		notifyListeners();
	}

	protected void notifyListeners() {
		Runnable[] listeners;
		synchronized (dirtyListeners) {
			listeners = dirtyListeners.toArray(new Runnable[0]);
		}
		for (Runnable listener : listeners) {
			listener.run();
		}
	}

	public void registerDirtyListener(Runnable dirtyListener) {
		synchronized (dirtyListeners) {
			this.dirtyListeners.add(dirtyListener);
		}
	}

	public void deregisterDirtyListeners() {
		synchronized (dirtyListeners) {
			this.dirtyListeners.clear();
		}
	}
}
