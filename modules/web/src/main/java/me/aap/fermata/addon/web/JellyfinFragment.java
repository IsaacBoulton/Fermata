package me.aap.fermata.addon.web;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import me.aap.fermata.addon.AddonManager;
import me.aap.utils.ui.view.ToolBarView;

@Keep
@SuppressWarnings("unused")
public class JellyfinFragment extends WebBrowserFragment {
	@Override
	public int getFragmentId() {
		return me.aap.fermata.R.id.jellyfin_fragment;
	}

	@Override
	public ToolBarView.Mediator getToolBarMediator() {
		return ToolBarView.Mediator.Invisible.instance;
	}

	@Nullable
	@Override
	protected WebBrowserAddon getAddon() {
		return AddonManager.get().getAddon(JellyfinAddon.class);
	}
}