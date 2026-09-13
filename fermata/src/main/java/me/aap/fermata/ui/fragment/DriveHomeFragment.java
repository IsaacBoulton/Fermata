package me.aap.fermata.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import me.aap.fermata.FermataApplication;
import me.aap.fermata.R;
import me.aap.fermata.addon.AddonManager;
import me.aap.fermata.ui.activity.MainActivityDelegate;

/** The small, driver-oriented landing screen used by Fermata Auto. */
public class DriveHomeFragment extends MainActivityFragment {

	@Override
	public int getFragmentId() {
		return R.id.drive_home_fragment;
	}

	@Override
	public CharSequence getTitle() {
		return getString(R.string.drive_home);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.drive_home_fragment, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		MainActivityDelegate activity = getActivityDelegate();
		AddonManager addons = FermataApplication.get().getAddonManager();

		bindDestination(view, R.id.drive_home_youtube, R.id.youtube_fragment,
				addons.hasAddon(R.id.youtube_fragment), activity);
		bindDestination(view, R.id.drive_home_jellyfin, R.id.jellyfin_fragment,
				addons.hasAddon(R.id.jellyfin_fragment), activity);

		View resume = view.findViewById(R.id.drive_home_resume);
		resume.setVisibility(activity.hasCurrent() ? View.VISIBLE : View.GONE);
		resume.setOnClickListener(v -> activity.goToCurrent());
		view.findViewById(R.id.drive_home_more).setOnClickListener(
				v -> activity.getNavBarMediator().showMenu(activity));
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		if (!hidden && (getView() != null)) {
			getView().findViewById(R.id.drive_home_resume).setVisibility(
					getActivityDelegate().hasCurrent() ? View.VISIBLE : View.GONE);
		}
	}

	private static void bindDestination(View root, int viewId, int fragmentId, boolean available,
			MainActivityDelegate activity) {
		View destination = root.findViewById(viewId);
		destination.setVisibility(available ? View.VISIBLE : View.GONE);
		if (available) destination.setOnClickListener(v -> activity.showFragment(fragmentId));
	}
}
