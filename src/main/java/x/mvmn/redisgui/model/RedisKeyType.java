package x.mvmn.redisgui.model;

public enum RedisKeyType {
	STRING, LIST, SET, ZSET, HASH, STREAM, UNKNOWN;

	public static RedisKeyType of(String value) {
		RedisKeyType result = null;

		if (value != null) {
			result = UNKNOWN;
			for (RedisKeyType candidate : RedisKeyType.values()) {
				if (candidate.name().equalsIgnoreCase(value)) {
					return candidate;
				}
			}
		}

		return result;
	}
}
