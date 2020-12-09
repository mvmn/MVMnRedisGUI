package x.mvmn.redisgui.model;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.lettuce.core.RedisURI;
import io.lettuce.core.RedisURI.Builder;
import io.lettuce.core.resource.ClientResources;
import x.mvmn.redisgui.lang.LangUtil;
import x.mvmn.redisgui.lang.Tuple;

public class RedisConfigModel {

	public static enum RedisConnectionType {
		STANDALONE("Standalone"), UNIX_SOCKET("Unix Socket"), SENTINEL("Sentinel"), CLUSTER("Cluster");

		private final String displayName;

		RedisConnectionType(String displayName) {
			this.displayName = displayName;
		}

		public String toString() {
			return displayName;
		}
	}

	private RedisConnectionType connectionType = RedisConnectionType.STANDALONE;
	private String host = "127.0.0.1";
	private int port = RedisURI.DEFAULT_REDIS_PORT;
	private String socket;
	private String sentinelMasterId;
	private int database = 0;
	private String clientName = "mvmnRedisClient";
	private String username;
	private char[] password;
	private boolean ssl = false;
	private boolean verifyPeer = true;
	private boolean startTls = false;
	private Duration timeout = RedisURI.DEFAULT_TIMEOUT_DURATION;
	private final List<Tuple<String, Integer, Void, Void, Void>> nodes = new ArrayList<>();

	public RedisConfigModel() {}

	public RedisConfigModel(Properties props) {
		if (props != null) {
			try {
				connectionType = RedisConnectionType.valueOf(props.getProperty("connectionType", RedisConnectionType.STANDALONE.name()));
			} catch (IllegalArgumentException iae) {
				connectionType = RedisConnectionType.STANDALONE;
			}
			host = props.getProperty("host");
			socket = props.getProperty("socket");
			sentinelMasterId = props.getProperty("sentinelMasterId");
			clientName = props.getProperty("clientName");
			username = props.getProperty("username");
			password = props.getProperty("password", "").toCharArray();
			port = LangUtil.parseInt(props.getProperty("port"), RedisURI.DEFAULT_REDIS_PORT);
			ssl = Boolean.valueOf(props.getProperty("ssl"));
			verifyPeer = Boolean.valueOf(props.getProperty("verifyPeer"));
			startTls = Boolean.valueOf(props.getProperty("startTls"));
			timeout = Duration.ofSeconds(LangUtil.parseLong(props.getProperty("timeout"), RedisURI.DEFAULT_TIMEOUT_DURATION.getSeconds()));
			String nodes = props.getProperty("nodes");
			if (nodes != null && !nodes.trim().isEmpty()) {
				this.nodes.clear();
				Stream.of(nodes.trim().split(";")).map(this::parseNode).forEach(this.nodes::add);
			}
		}
	}

	private Tuple<String, Integer, Void, Void, Void> parseNode(String node) {
		String host = node;
		int port = RedisURI.DEFAULT_REDIS_PORT;
		if (host.contains(":")) {
			int indexOfSep = host.indexOf(":");
			port = LangUtil.parseInt(host.substring(indexOfSep + 1), port);
			host = host.substring(0, indexOfSep);
		}

		return new Tuple<String, Integer, Void, Void, Void>(host, port, null, null, null);
	}

	public Properties modelToProperties() {
		Properties props = new Properties();

		setNonNull(props, "connectionType", connectionType.name());
		setNonNull(props, "host", host);
		setNonNull(props, "socket", socket);
		setNonNull(props, "sentinelMasterId", sentinelMasterId);
		setNonNull(props, "clientName", clientName);
		setNonNull(props, "username", username);
		// TODO: password encryption
		if (password != null) {
			props.setProperty("password", new String(password));
		}
		props.setProperty("port", String.valueOf(port));
		props.setProperty("ssl", String.valueOf(ssl));
		props.setProperty("verifyPeer", String.valueOf(verifyPeer));
		props.setProperty("startTls", String.valueOf(startTls));
		props.setProperty("timeout", String.valueOf(timeout.getSeconds()));
		props.setProperty("nodes", nodes.stream().map(node -> node.getA() + ":" + node.getB()).collect(Collectors.joining(";")));
		return props;
	}

	private void setNonNull(Properties props, String key, String value) {
		if (value != null) {
			props.setProperty(key, value);
		}
	}

	public RedisURI getUri() {
		Builder builder;
		switch (connectionType) {
			case STANDALONE:
			default:
				builder = RedisURI.Builder.redis(host, port);
			break;
			case SENTINEL:
				builder = RedisURI.Builder.sentinel(host, port);
				if (sentinelMasterId != null && !sentinelMasterId.isEmpty()) {
					builder.withSentinelMasterId(sentinelMasterId);
				}
				if (!nodes.isEmpty()) {
					nodes.stream().forEach(node -> builder.withSentinel(node.getA(), node.getB()));
				}
			break;
			case UNIX_SOCKET:
				builder = RedisURI.Builder.socket(socket);
			break;
			case CLUSTER:
				builder = RedisURI.Builder.redis(host, port);
			break;
		}
		builder.withSsl(ssl);
		if (password != null && password.length > 0) {
			if (username != null && !username.trim().isEmpty()) {
				builder.withAuthentication(username, password);
			} else {
				builder.withPassword(password);
			}
		}
		builder.withStartTls(startTls);
		builder.withVerifyPeer(verifyPeer);
		builder.withTimeout(timeout);
		if (clientName != null && !clientName.trim().isEmpty()) {
			builder.withClientName(clientName);
		}
		if (database >= 0) {
			builder.withDatabase(database);
		}

		return builder.build();
	}

	public ClientResources getClientResources() {
		return ClientResources.builder().build();
	}

	public RedisConnectionType getConnectionType() {
		return connectionType;
	}

	public void setConnectionType(RedisConnectionType connectionType) {
		this.connectionType = connectionType;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getSocket() {
		return socket;
	}

	public void setSocket(String socket) {
		this.socket = socket;
	}

	public String getSentinelMasterId() {
		return sentinelMasterId;
	}

	public void setSentinelMasterId(String sentinelMasterId) {
		this.sentinelMasterId = sentinelMasterId;
	}

	public int getDatabase() {
		return database;
	}

	public void setDatabase(int database) {
		this.database = database;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public char[] getPassword() {
		return password;
	}

	public void setPassword(char[] password) {
		this.password = password;
	}

	public boolean isSsl() {
		return ssl;
	}

	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}

	public boolean isVerifyPeer() {
		return verifyPeer;
	}

	public void setVerifyPeer(boolean verifyPeer) {
		this.verifyPeer = verifyPeer;
	}

	public boolean isStartTls() {
		return startTls;
	}

	public void setStartTls(boolean startTls) {
		this.startTls = startTls;
	}

	public Duration getTimeout() {
		return timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	public List<Tuple<String, Integer, Void, Void, Void>> getNodes() {
		return nodes;
	}

	public String getClientName() {
		return clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}
}
