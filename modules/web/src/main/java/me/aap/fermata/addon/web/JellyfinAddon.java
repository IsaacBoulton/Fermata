package me.aap.fermata.addon.web;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.IdRes;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import me.aap.fermata.addon.AddonInfo;
import me.aap.fermata.addon.FermataAddon;
import me.aap.utils.function.Supplier;
import me.aap.utils.misc.ChangeableCondition;
import me.aap.utils.pref.PreferenceSet;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.ui.fragment.ActivityFragment;

/** Direct access to the configured home Jellyfin server. */
@Keep
@SuppressWarnings("unused")
public class JellyfinAddon extends WebBrowserAddon {
	private static final String DEFAULT_HOME_URL = "http://homelab:8096/moonfin/web/";
	private static final Pref<Supplier<String>> HOME_URL = Pref.s("JELLYFIN_HOME_URL", DEFAULT_HOME_URL);
	private String sessionUrl;

	@NonNull
	private static final AddonInfo info =
			FermataAddon.findAddonInfo(JellyfinAddon.class.getName());

	@IdRes
	@Override
	public int getAddonId() {
		return me.aap.fermata.R.id.jellyfin_fragment;
	}

	@NonNull
	@Override
	public AddonInfo getInfo() {
		return info;
	}

	@NonNull
	@Override
	public ActivityFragment createFragment() {
		return new JellyfinFragment();
	}

	@Override
	public void contributeSettings(Context ctx, PreferenceStore store, PreferenceSet set,
																 ChangeableCondition visibility) {
		super.contributeSettings(ctx, store, set, visibility);

		set.addStringPref(o -> {
			o.store = getPreferenceStore();
			o.pref = HOME_URL;
			o.title = R.string.jellyfin_home_url;
			o.stringHint = DEFAULT_HOME_URL;
			o.visibility = visibility;
		});
	}

	@Override
	String getLastUrl() {
		String home = getHomeUrl();
		return ((sessionUrl != null) && isNavigationAllowed(Uri.parse(sessionUrl))) ? sessionUrl : home;
	}

	@Override
	void setLastUrl(String url) {
		if (isNavigationAllowed(Uri.parse(url))) sessionUrl = url;
	}

	@Override
	protected boolean isJavascriptBridgeAllowed(Uri uri) {
		return isNavigationAllowed(uri);
	}

	@Override
	protected boolean isNavigationAllowed(Uri uri) {
		Uri home = Uri.parse(getHomeUrl());
		return equal(home.getScheme(), uri.getScheme()) && equal(home.getHost(), uri.getHost()) &&
				(home.getPort() == uri.getPort());
	}

	private String getHomeUrl() {
		return WebSecurity.normalizeHttpUrl(getPreferenceStore().getStringPref(HOME_URL), DEFAULT_HOME_URL);
	}

	private static boolean equal(String first, String second) {
		return (first != null) && first.equalsIgnoreCase(second);
	}
}