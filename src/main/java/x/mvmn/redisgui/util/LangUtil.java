package x.mvmn.redisgui.util;

public class LangUtil {

	public static Integer parseInt(String value, Integer defaultValue) {
		Integer result = defaultValue;

		if (value != null) {
			value = value.trim();
			if (!value.isEmpty()) {
				try {
					result = Integer.parseInt(value);
				} catch (NumberFormatException nfe) {}
			}
		}

		return result;
	}

	public static Long parseLong(String value, Long defaultValue) {
		Long result = defaultValue;

		if (value != null) {
			value = value.trim();
			if (!value.isEmpty()) {
				try {
					result = Long.parseLong(value);
				} catch (NumberFormatException nfe) {}
			}
		}

		return result;
	}

}
