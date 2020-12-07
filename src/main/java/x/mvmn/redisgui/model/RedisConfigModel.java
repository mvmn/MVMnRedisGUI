package x.mvmn.redisgui.model;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import x.mvmn.redisgui.lang.Tuple;

public class RedisConfigModel {

	private final RedisURI.Builder uriBuilder = RedisURI.builder();
	private final ClientResources.Builder clientResourcesBuilder = ClientResources.builder();

	public static enum RedisConnectionType {
		STANDALONE, STANDALONE_SSL, UNIX_SOCKET, SENTINEL, CLUSTER
	}

	private RedisConnectionType connectionType;
	private String host;
	private int port = RedisURI.DEFAULT_REDIS_PORT;
	private String socket;
	private String sentinelMasterId;
	private int database = 0;
	private String clientName;
	private String username;
	private String password;
	private String sentinelPassword;
	private boolean ssl = false;
	private boolean verifyPeer = true;
	private boolean startTls = false;
	private Duration timeout = RedisURI.DEFAULT_TIMEOUT_DURATION;
	private final List<Tuple<String, Integer, Void, Void, Void>> nodes = new ArrayList<>();

	public RedisConfigModel(Properties props) {
		// FIXME: populate builders
	}

	public Properties modelToProperties() {
		// FIXME: implement
		return null;
	}

	public RedisURI getUri() {
		return uriBuilder.build();
	}

	public ClientResources getClientResources() {
		return clientResourcesBuilder.build();
	}
}
