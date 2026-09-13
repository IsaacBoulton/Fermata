package me.aap.fermata.addon.web;

import androidx.annotation.Nullable;

import java.net.URI;
import java.net.URISyntaxException;

/** Small, platform-independent helpers used at the WebView trust boundary. */
public final class WebSecurity {
	private WebSecurity() {
	}

	public static boolean isHostOrSubdomain(@Nullable String host, String domain) {
		if (host == null) return false;
		host = host.toLowerCase();
		domain = domain.toLowerCase();
		return host.equals(domain) || host.endsWith('.' + domain);
	}

	/** Returns a JavaScript string literal without relying on Android's JSON implementation. */
	public static String quoteJs(CharSequence value) {
		StringBuilder out = new StringBuilder(value.length() + 16).append('"');
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '"' -> out.append("\\\"");
				case '\\' -> out.append("\\\\");
				case '\b' -> out.append("\\b");
				case '\f' -> out.append("\\f");
				case '\n' -> out.append("\\n");
				case '\r' -> out.append("\\r");
				case '\t' -> out.append("\\t");
				default -> {
					if ((c < 0x20) || (c == '\u2028') || (c == '\u2029')) {
						out.append(String.format("\\u%04x", (int) c));
					} else {
						out.append(c);
					}
				}
			}
		}
		return out.append('"').toString();
	}

	public static String normalizeHttpUrl(@Nullable String value, String fallback) {
		if (value == null) return fallback;
		value = value.trim();
		if (value.isEmpty()) return fallback;
		if (!value.contains("://")) value = "http://" + value;
		try {
			URI uri = new URI(value);
			String scheme = uri.getScheme();
			if ((uri.getHost() == null) ||
					(!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
				return fallback;
			}
			return uri.toASCIIString();
		} catch (URISyntaxException ex) {
			return fallback;
		}
	}
}