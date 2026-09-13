package me.aap.fermata.ui.fragment;

import static android.view.View.FOCUS_DOWN;
import static android.view.View.FOCUS_LEFT;
import static android.view.View.FOCUS_RIGHT;
import static android.view.View.FOCUS_UP;
import static me.aap.utils.collection.CollectionUtils.newLinkedHashSet;
import static me.aap.utils.ui.UiUtils.isVisible;
import static me.aap.utils.ui.view.NavBarItem.create;
import static me.aap.utils.ui.view.NavBarView.POSITION_LEFT;
import static me.aap.utils.ui.view.NavBarView.POSITION_RIGHT;

import android.content.Context;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.widget.ImageViewCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.aap.fermata.BuildConfig;
import me.aap.fermata.FermataApplication;
import me.aap.fermata.R;
import me.aap.fermata.addon.AddonInfo;
import me.aap.fermata.addon.AddonManager;
import me.aap.fermata.addon.FermataAddon;
import me.aap.fermata.addon.FermataFragmentAddon;
import me.aap.fermata.ui.activity.MainActivityDelegate;
import me.aap.fermata.ui.view.BodyLayout;
import me.aap.fermata.ui.view.ControlPanelView;
import me.aap.fermata.ui.view.MediaItemListView;
import me.aap.utils.collection.CollectionUtils;
import me.aap.utils.function.Supplier;
import me.aap.utils.log.Log;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.pref.PreferenceStore.Pref;
import me.aap.utils.ui.activity.ActivityDelegate;
import me.aap.utils.ui.fragment.ActivityFragment;
import me.aap.utils.ui.menu.OverlayMenu;
import me.aap.utils.ui.menu.OverlayMenuItem;
import me.aap.utils.ui.view.NavBarItem;
import me.aap.utils.ui.view.NavBarView;
import me.aap.utils.ui.view.NavButtonView;
import me.aap.utils.ui.view.PrefNavBarMediator;
import me.aap.utils.ui.view.ToolBarView;

/**
 * @author Andrey Pavlenko
 */
public class NavBarMediator extends PrefNavBarMediator
		implements AddonManager.Listener, OverlayMenu.SelectionHandler {
	private static final String FOLDERS = "folders";
	private static final String HOME = "home";
	private static final String FAVORITES = "favorites";
	private static final String PLAYLISTS = "playlists";
	private static final String MENU = "menu";
	private static final String YOUTUBE = "me.aap.fermata.addon.web.yt.YoutubeAddon";
	private static final String JELLYFIN = "me.aap.fermata.addon.web.JellyfinAddon";
	private static final PositionPrefs BOTTOM_PREFS = new PositionPrefs("B");
	private static final PositionPrefs LEFT_PREFS = new PositionPrefs("L");
	private static final PositionPrefs RIGHT_PREFS = new PositionPrefs("R");

	private enum ItemState {
		PINNED, MORE, HIDDEN
	}

	private static final class PositionPrefs {
		final Pref<Supplier<String[]>> order;
		final Pref<Supplier<String[]>> hidden;
		final Pref<Supplier<String[]>> unpinned;

		PositionPrefs(String suffix) {
			order = Pref.sa("NAV_BAR_ITEMS_" + suffix, (String[]) null);
			hidden = Pref.sa("NAV_BAR_HIDDEN_" + suffix);
			unpinned = Pref.sa("NAV_BAR_UNPINNED_" + suffix);
		}
	}

	private static final class Entry {
		final String name;
		final int id;
		final int icon;
		final int title;

		Entry(String name, int id, int icon, int title) {
			this.name = name;
			this.id = id;
			this.icon = icon;
			this.title = title;
		}

		NavBarItem toNavItem(Context context, boolean pinned) {
			return create(context, id, icon, title, pinned);
		}
	}

	@Override
	protected Collection<NavBarItem> getItems(NavBarView nb) {
		List<Entry> entries = getEntries(nb, false);
		Set<String> pinned = getPinnedNames(nb, entries);
		List<NavBarItem> items = new ArrayList<>(entries.size());

		for (Entry entry : entries) {
			items.add(entry.toNavItem(nb.getContext(), pinned.contains(entry.name)));
		}

		return items;
	}

	@Override
	public void addView(NavBarView nb, View v, int id, View.OnClickListener onClick) {
		super.addView(nb, v, id, onClick);
		v.setBackgroundResource(R.drawable.nav_item_background);
		if (!nb.isBottom() && (v instanceof NavButtonView button)) {
			Context context = v.getContext();
			LinearLayoutCompat.LayoutParams lp =
					(LinearLayoutCompat.LayoutParams) v.getLayoutParams();
			lp.height = context.getResources().getDimensionPixelSize(R.dimen.nav_item_size);
			lp.weight = 0;
			v.setLayoutParams(lp);
			button.setGravity(Gravity.CENTER);

			int iconSize = context.getResources().getDimensionPixelSize(R.dimen.nav_item_icon_size);
			LinearLayoutCompat.LayoutParams iconParams =
					(LinearLayoutCompat.LayoutParams) button.getIcon().getLayoutParams();
			iconParams.width = iconSize;
			iconParams.height = iconSize;
			iconParams.weight = 0;
			iconParams.gravity = Gravity.CENTER;
			button.getIcon().setLayoutParams(iconParams);
		}
		if (((id == R.id.youtube_fragment) || (id == R.id.jellyfin_fragment)) &&
				(v instanceof NavButtonView button)) {
			ImageViewCompat.setImageTintList(button.getIcon(), null);
		}
	}

	@Override
	protected boolean canSwap(NavBarView nb) {
		return true;
	}

	@Override
	protected boolean swap(NavBarView nb, @IdRes int id1, @IdRes int id2) {
		List<Entry> entries = getEntries(nb, false);
		List<String> names = new ArrayList<>(entries.size());
		for (Entry entry : entries) names.add(entry.name);
		String name1 = findName(entries, id1);
		String name2 = findName(entries, id2);
		int idx1 = names.indexOf(name1);
		int idx2 = names.indexOf(name2);

		if ((idx1 != -1) && (idx2 != -1)) {
			Collections.swap(names, idx1, idx2);
			getPreferenceStore(nb).applyStringArrayPref(getPref(nb), names.toArray(new String[0]));
			return true;
		} else {
			Log.e("Unable to swap ", name1, " and ", name2);
			return false;
		}
	}

	@Override
	public void enable(NavBarView nb, ActivityFragment f) {
		super.enable(nb, f);
		FermataApplication.get().getAddonManager().addBroadcastListener(this);
	}

	@Override
	public void disable(NavBarView nb) {
		super.disable(nb);
		FermataApplication.get().getAddonManager().removeBroadcastListener(this);
	}

	@Override
	public void onAddonChanged(AddonManager mgr, AddonInfo info, boolean installed) {
		NavBarView nb = navBar;
		if (nb != null) reload(nb);
	}

	@Override
	protected PreferenceStore getPreferenceStore(NavBarView nb) {
		return MainActivityDelegate.get(nb.getContext()).getPrefs();
	}

	@Override
	protected Pref<Supplier<String[]>> getPref(NavBarView nb) {
		return getPositionPrefs(nb).order;
	}

	private PositionPrefs getPositionPrefs(NavBarView nb) {
		return switch (nb.getPosition()) {
			default -> BOTTOM_PREFS;
			case POSITION_LEFT -> LEFT_PREFS;
			case POSITION_RIGHT -> RIGHT_PREFS;
		};
	}

	@Override
	public void itemSelected(View item, int id, ActivityDelegate a) {
		if (id == R.id.menu) {
			showMenu(MainActivityDelegate.get(item.getContext()));
		} else {
			super.itemSelected(item, id, a);
		}
	}

	@Override
	protected boolean extItemSelected(OverlayMenuItem item) {
		if (item.getItemId() == R.id.menu) {
			NavButtonView.Ext ext = getExtButton();

			if ((ext != null) && !ext.isSelected()) {
				NavBarItem i = item.getData();
				setExtButton(null, i);
			}

			showMenu(MainActivityDelegate.get(item.getContext()));
			return true;
		} else {
			return super.extItemSelected(item);
		}
	}

	@Override
	public void itemReselected(View item, int id, ActivityDelegate a) {
		BodyLayout b = ((MainActivityDelegate) a).getBody();
		if (b.isVideoMode()) b.setMode(BodyLayout.Mode.BOTH);
		else super.itemReselected(item, id, a);
	}

	@Nullable
	@Override
	public View focusSearch(NavBarView nb, View focused, int direction) {
		if (direction == FOCUS_UP) {
			if (!nb.isBottom()) return null;
			Context ctx = nb.getContext();
			ControlPanelView p = MainActivityDelegate.get(ctx).getControlPanel();
			return isVisible(p) ? p.focusSearch() : MediaItemListView.focusSearchLast(ctx, focused);
		} else if (direction == FOCUS_DOWN) {
			if (!nb.isBottom()) return null;
			Context ctx = nb.getContext();
			ToolBarView tb = MainActivityDelegate.get(ctx).getToolBar();
			if (isVisible(tb)) return tb.focusSearch();
		} else if (direction == FOCUS_RIGHT) {
			if (nb.isLeft()) return MediaItemListView.focusSearchActive(nb.getContext(), focused);
		} else if (direction == FOCUS_LEFT) {
			if (nb.isRight()) return MediaItemListView.focusSearchActive(nb.getContext(), focused);
		}

		return null;
	}

	@Override
	public void showMenu(NavBarView nb) {
		showMenu(MainActivityDelegate.get(nb.getContext()));
	}

	public void showMenu(MainActivityDelegate a) {
		OverlayMenu menu = a.findViewById(R.id.nav_menu_view);
		menu.show(b -> {
			b.setSelectionHandler(this);

			if (a.hasCurrent())
				b.addItem(R.id.nav_got_to_current, R.drawable.go_to_current, R.string.got_to_current);

			ActivityFragment f = a.getActiveFragment();
			if (f instanceof MainActivityFragment) ((MainActivityFragment) f).contributeToNavBarMenu(b);

			b.addItem(R.id.nav_customize, R.drawable.edit, R.string.customize_navigation)
					.setSubmenu(customize -> buildCustomizeMenu(a, customize));
			b.addItem(R.id.settings_fragment, R.drawable.settings, R.string.settings);
			b.addItem(R.id.nav_exit, R.drawable.exit, a.isCarActivityNotMirror() ? R.string.restart : R.string.exit);
		});
	}

	@Override
	public boolean menuItemSelected(OverlayMenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.nav_got_to_current) {
			MainActivityDelegate.get(item.getContext()).goToCurrent();
			return true;
		} else if (itemId == R.id.settings_fragment) {
			MainActivityDelegate.get(item.getContext()).showFragment(R.id.settings_fragment);
			return true;
		} else if (itemId == R.id.nav_exit) {
			MainActivityDelegate a = MainActivityDelegate.get(item.getContext());
			a.finish();
			if (a.isCarActivityNotMirror()) {
				a.getHandler().postDelayed(() -> System.exit(0), 500);
			}
			return true;
		}
		return false;
	}

	private void buildCustomizeMenu(MainActivityDelegate a, OverlayMenu.Builder builder) {
		NavBarView nb = a.getNavBar();
		Context ctx = nb.getContext();
		PositionPrefs prefs = getPositionPrefs(nb);
		Set<String> hidden = getNames(nb, prefs.hidden);
		Set<String> pinned = getPinnedNames(nb, getEntries(nb, false));

		builder.setTitle(R.string.customize_navigation);
		for (Entry entry : getEntries(nb, true)) {
			if (MENU.equals(entry.name)) continue;
			NavBarItem navItem = entry.toNavItem(ctx, false);
			ItemState itemState = hidden.contains(entry.name) ? ItemState.HIDDEN :
					(pinned.contains(entry.name) ? ItemState.PINNED : ItemState.MORE);
			int state = switch (itemState) {
				case PINNED -> R.string.navigation_state_pinned;
				case MORE -> R.string.navigation_state_more;
				case HIDDEN -> R.string.navigation_state_hidden;
			};
			String title = ctx.getString(R.string.navigation_item_state, navItem.getText(),
					ctx.getString(state));

			builder.addItem(navItem.getId(), navItem.getIcon(), title).setSubmenu(actions -> {
				actions.setTitle(navItem.getText());
				if (itemState != ItemState.PINNED) {
					actions.addItem(R.id.nav_pin, R.drawable.playlist_add,
							R.string.pin_to_navigation).setHandler(item -> {
						setNavigationState(nb, entry.name, ItemState.PINNED);
						return true;
					});
				}
				if (itemState != ItemState.MORE) {
					actions.addItem(R.id.nav_unpin, R.drawable.playlist_remove,
							R.string.unpin_from_navigation).setHandler(item -> {
						setNavigationState(nb, entry.name, ItemState.MORE);
						return true;
					});
				}
				if (itemState != ItemState.HIDDEN) {
					actions.addItem(R.id.nav_hide, R.drawable.delete,
							R.string.hide_from_navigation).setHandler(item -> {
						setNavigationState(nb, entry.name, ItemState.HIDDEN);
						return true;
					});
				}
			});
		}
	}

	private Set<String> getNames(NavBarView nb, Pref<Supplier<String[]>> pref) {
		Set<String> names = newLinkedHashSet(BuildConfig.ADDONS.length + 3);
		CollectionUtils.addAll(names, getPreferenceStore(nb).getStringArrayPref(pref));
		return names;
	}

	private void setNavigationState(NavBarView nb, String name, ItemState state) {
		PreferenceStore store = getPreferenceStore(nb);
		PositionPrefs prefs = getPositionPrefs(nb);
		Set<String> hidden = getNames(nb, prefs.hidden);
		Set<String> unpinned = getNames(nb, prefs.unpinned);
		hidden.remove(name);
		unpinned.remove(name);

		switch (state) {
			case HIDDEN -> hidden.add(name);
			case MORE -> unpinned.add(name);
			case PINNED -> {
				List<String> order = new ArrayList<>();
				for (Entry entry : getEntries(nb, true)) order.add(entry.name);
				order.remove(name);
				order.add(0, name);
				store.applyStringArrayPref(prefs.order, order.toArray(new String[0]));
			}
		}

		store.applyStringArrayPref(prefs.hidden, hidden.toArray(new String[0]));
		store.applyStringArrayPref(prefs.unpinned, unpinned.toArray(new String[0]));
		reload(nb);
	}

	private List<Entry> getEntries(NavBarView nb, boolean includeHidden) {
		Map<String, Entry> available = getAvailableEntries();
		Set<String> names = newLinkedHashSet(available.size());
		if (BuildConfig.AUTO) names.add(HOME);
		String[] savedOrder = getPreferenceStore(nb).getStringArrayPref(getPref(nb));
		CollectionUtils.addAll(names, savedOrder);
		if (BuildConfig.AUTO && ((savedOrder == null) || (savedOrder.length == 0))) {
			names.add(YOUTUBE);
			names.add(JELLYFIN);
		}
		names.add(FOLDERS);
		names.add(FAVORITES);
		names.add(PLAYLISTS);
		names.addAll(available.keySet());

		if (!includeHidden) {
			names.removeAll(getNames(nb, getPositionPrefs(nb).hidden));
			names.add(MENU);
		}

		List<Entry> entries = new ArrayList<>(names.size());
		for (String name : names) {
			Entry entry = available.get(name);
			if (entry != null) entries.add(entry);
			else Log.e("Unknown NavBarItem name: ", name);
		}
		return entries;
	}

	private Map<String, Entry> getAvailableEntries() {
		Map<String, Entry> entries = new LinkedHashMap<>(BuildConfig.ADDONS.length + 4);
		if (BuildConfig.AUTO) {
			entries.put(HOME, new Entry(HOME, R.id.drive_home_fragment,
					R.drawable.drive_home, R.string.drive_home));
		}
		entries.put(FOLDERS, new Entry(FOLDERS, R.id.folders_fragment,
				me.aap.utils.R.drawable.folder, R.string.folders));
		entries.put(FAVORITES, new Entry(FAVORITES, R.id.favorites_fragment,
				R.drawable.favorite_filled, R.string.favorites));
		entries.put(PLAYLISTS, new Entry(PLAYLISTS, R.id.playlists_fragment,
				R.drawable.playlist, R.string.playlists));

		AddonManager amgr = getAddonManager();
		for (AddonInfo ai : BuildConfig.ADDONS) {
			FermataAddon addon = amgr.getAddon(ai.className);
			if (addon instanceof FermataFragmentAddon) {
				entries.put(ai.className,
						new Entry(ai.className, addon.getAddonId(), ai.icon, ai.addonName));
			}
		}
		entries.put(MENU, new Entry(MENU, R.id.menu, me.aap.utils.R.drawable.menu,
				R.string.menu));
		return entries;
	}

	private Set<String> getPinnedNames(NavBarView nb, List<Entry> entries) {
		Set<String> unpinned = getNames(nb, getPositionPrefs(nb).unpinned);
		Set<String> pinned = newLinkedHashSet(nb.suggestItemCount());
		int max = nb.suggestItemCount() - 1;
		for (Entry entry : entries) {
			if ((pinned.size() >= max) || unpinned.contains(entry.name)) continue;
			pinned.add(entry.name);
		}
		return pinned;
	}

	@Nullable
	private static String findName(List<Entry> entries, @IdRes int id) {
		for (Entry entry : entries) {
			if (entry.id == id) return entry.name;
		}
		return null;
	}

	private static AddonManager getAddonManager() {
		return FermataApplication.get().getAddonManager();
	}
}
