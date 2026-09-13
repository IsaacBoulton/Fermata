package me.aap.fermata.addon.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WebSecurityTest {
	@Test
	public void hostMatchingRequiresLabelBoundary() {
		assertTrue(WebSecurity.isHostOrSubdomain("youtube.com", "youtube.com"));
		assertTrue(WebSecurity.isHostOrSubdomain("m.youtube.com", "youtube.com"));
		assertFalse(WebSecurity.isHostOrSubdomain("evilyoutube.com", "youtube.com"));
		assertFalse(WebSecurity.isHostOrSubdomain(null, "youtube.com"));
	}

	@Test
	public void javascriptStringIsSafelyQuoted() {
		assertEquals("\"don't \\\"run\\\"\\n\\\\code\"",
				WebSecurity.quoteJs("don't \"run\"\n\\code"));
		assertEquals("\"\\u2028\\u0001\"", WebSecurity.quoteJs("\u2028\u0001"));
	}

	@Test
	public void normalizesOnlyHttpUrls() {
		assertEquals("http://homelab:8096/moonfin/web",
				WebSecurity.normalizeHttpUrl(" homelab:8096/moonfin/web ", "fallback"));
		assertEquals("fallback", WebSecurity.normalizeHttpUrl("javascript:alert(1)", "fallback"));
		assertEquals("fallback", WebSecurity.normalizeHttpUrl("", "fallback"));
	}
}
