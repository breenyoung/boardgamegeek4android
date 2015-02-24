package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.ui.GameInfoFragment.GameInfo;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DetachableResultReceiver;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.UIUtils;

import timber.log.Timber;

public class GameActivity extends PagedDrawerActivity implements GameInfoFragment.Callbacks, PlaysFragment.Callbacks,
	OnSharedPreferenceChangeListener {

	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_FROM_SHORTCUT = "FROM_SHORTCUT";
	private static final int REQUEST_EDIT_PLAY = 1;

	private int mGameId;
	private String mGameName;
	private String mThumbnailUrl;
	private String mImageUrl;
	private boolean mCustomPlayerSort;
	private SyncStatusUpdaterFragment mSyncStatusUpdaterFragment;
	private Menu mOptionsMenu;
	private Fragment mGameInfoFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);

		mGameId = Games.getGameId(getIntent().getData());
		changeName(getIntent().getStringExtra(KEY_GAME_NAME));

		new Handler().post(new Runnable() {
			@Override
			public void run() {
				ContentValues values = new ContentValues();
				values.put(Games.LAST_VIEWED, System.currentTimeMillis());
				getContentResolver().update(getIntent().getData(), values, null, null);
			}
		});

		FragmentManager fm = getSupportFragmentManager();
		mSyncStatusUpdaterFragment = (SyncStatusUpdaterFragment) fm.findFragmentByTag(SyncStatusUpdaterFragment.TAG);
		if (mSyncStatusUpdaterFragment == null) {
			mSyncStatusUpdaterFragment = new SyncStatusUpdaterFragment();
			fm.beginTransaction().add(mSyncStatusUpdaterFragment, SyncStatusUpdaterFragment.TAG).commit();
		}

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		setupActionBarTabs(actionBar);
	}

	@Override
	protected PagerAdapter getAdapter(FragmentManager fm) {
		return new GamePagerAdapter(fm);
	}

	@Override
	protected void setupActionBarTabs(ActionBar actionBar) {
		actionBar.removeAllTabs();
		createTab(actionBar, R.string.title_info);
		if (showCollection()) {
			createTab(actionBar, R.string.title_collection);
		}
		if (showPlays()) {
			createTab(actionBar, R.string.title_plays);
		}
	}

	@Override
	protected int getContentViewId() {
		return R.layout.activity_viewpager;
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.game;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		mOptionsMenu = menu;
		updateRefreshStatus(mSyncStatusUpdaterFragment.mSyncing);
		menu.findItem(R.id.menu_log_play).setVisible(PreferencesUtils.showLogPlay(this));
		menu.findItem(R.id.menu_log_play_quick).setVisible(PreferencesUtils.showQuickLogPlay(this));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				Intent upIntent = new Intent(this, HotnessActivity.class);
				if (Authenticator.isSignedIn(this)) {
					upIntent = new Intent(this, CollectionActivity.class);
				}
				if (shouldUpRecreateTask(this, upIntent)) {
					TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities();
				} else {
					NavUtils.navigateUpTo(this, upIntent);
				}
				return true;
			case R.id.menu_language_poll:
				Bundle arguments = new Bundle(2);
				arguments.putInt(PollFragment.KEY_GAME_ID, mGameId);
				arguments.putString(PollFragment.KEY_TYPE, "language_dependence");
				ActivityUtils.launchDialog(mGameInfoFragment, new PollFragment(), "poll-dialog", arguments);
				return true;
			case R.id.menu_share:
				ActivityUtils.shareGame(this, mGameId, mGameName);
				return true;
			case R.id.menu_shortcut:
				ActivityUtils.sendGameShortcut(this, mGameId, mGameName, mThumbnailUrl);
				return true;
			case R.id.menu_log_play:
				Intent intent = ActivityUtils.createEditPlayIntent(this, 0, mGameId, mGameName, mThumbnailUrl,
					mImageUrl);
				intent.putExtra(LogPlayActivity.KEY_CUSTOM_PLAYER_SORT, mCustomPlayerSort);
				startActivityForResult(intent, REQUEST_EDIT_PLAY);
				return true;
			case R.id.menu_log_play_quick:
				Toast.makeText(this, R.string.msg_logging_play, Toast.LENGTH_SHORT).show();
				ActivityUtils.logQuickPlay(this, mGameId, mGameName);
				return true;
			case R.id.menu_link_bgg:
				ActivityUtils.linkBgg(this, mGameId);
				return true;
			case R.id.menu_link_bg_prices:
				ActivityUtils.linkBgPrices(this, mGameName);
				return true;
			case R.id.menu_link_amazon:
				ActivityUtils.linkAmazon(this, mGameName);
				return true;
			case R.id.menu_link_ebay:
				ActivityUtils.linkEbay(this, mGameName);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_EDIT_PLAY && resultCode == Activity.RESULT_OK && showPlays()) {
			changeTab((showCollection() ? 1 : 0) + 1);
		}
	}

	private boolean shouldUpRecreateTask(Activity activity, Intent targetIntent) {
		return activity.getIntent().getBooleanExtra(KEY_FROM_SHORTCUT, false);
	}

	private class GamePagerAdapter extends FragmentPagerAdapter {

		public GamePagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			Fragment fragment = null;
			if (position > 0 && !showCollection()) {
				position++;
			}
			if (position > 1 && !showPlays()) {
				position += 3;
			}
			switch (position) {
				case 0:
					fragment = new GameInfoFragment();
					mGameInfoFragment = fragment;
					break;
				case 1:
					fragment = new GameCollectionFragment();
					break;
				case 2:
					fragment = new PlaysFragment();
					break;
			}
			if (fragment != null) {
				fragment.setArguments(UIUtils.intentToFragmentArguments(getIntent()));
			}
			return fragment;
		}

		@Override
		public int getCount() {
			return 1 + (showCollection() ? 1 : 0) + (showPlays() ? 1 : 0);
		}
	}

	@Override
	public void onGameInfoChanged(GameInfo gameInfo) {
		changeName(gameInfo.gameName);
		changeSubtype(gameInfo.subtype);
		mThumbnailUrl = gameInfo.thumbnailUrl;
		mImageUrl = gameInfo.imageUrl;
		mCustomPlayerSort = gameInfo.customPlayerSort;
	}

	@Override
	public DetachableResultReceiver getReceiver() {
		return mSyncStatusUpdaterFragment.mReceiver;
	}

	private void changeName(String gameName) {
		mGameName = gameName;
		if (!TextUtils.isEmpty(gameName)) {
			getIntent().putExtra(KEY_GAME_NAME, gameName);
			getSupportActionBar().setTitle(gameName);
		}
	}

	private void changeSubtype(String subtype) {
		if (subtype == null) {
			return;
		}
		int resId = R.string.title_game;
		switch (subtype) {
			case BggService.THING_SUBTYPE_BOARDGAME:
				resId = R.string.title_board_game;
				break;
			case BggService.THING_SUBTYPE_BOARDGAME_EXPANSION:
				resId = R.string.title_board_game_expansion;
				break;
			case BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY:
				resId = R.string.title_board_game_accessory;
				break;
		}
		getSupportActionBar().setSubtitle(getString(resId));
	}

	private void updateRefreshStatus(boolean refreshing) {
		if (mOptionsMenu == null) {
			return;
		}

		final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
		if (refreshItem != null) {
			if (refreshing) {
				MenuItemCompat.setActionView(refreshItem, R.layout.actionbar_indeterminate_progress);
			} else {
				MenuItemCompat.setActionView(refreshItem, null);
			}
		}
	}

	public static class SyncStatusUpdaterFragment extends Fragment implements DetachableResultReceiver.Receiver {
		private static final String TAG = SyncStatusUpdaterFragment.class.toString();

		private boolean mSyncing = false;
		private DetachableResultReceiver mReceiver;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			mReceiver = new DetachableResultReceiver(new Handler());
			mReceiver.setReceiver(this);
		}

		/**
		 * {@inheritDoc}
		 */
		public void onReceiveResult(int resultCode, Bundle resultData) {
			GameActivity activity = (GameActivity) getActivity();
			if (activity == null) {
				return;
			}

			switch (resultCode) {
				case UpdateService.STATUS_RUNNING: {
					mSyncing = true;
					break;
				}
				case UpdateService.STATUS_COMPLETE: {
					mSyncing = false;
					break;
				}
				case UpdateService.STATUS_ERROR:
				default: {
					final String error = resultData.getString(Intent.EXTRA_TEXT);
					if (error != null) {
						Timber.d("Received unexpected result: " + error);
						Toast.makeText(activity, error, Toast.LENGTH_LONG).show();
					}
					break;
				}
			}

			activity.updateRefreshStatus(mSyncing);
		}
	}

	private boolean showCollection() {
		return Authenticator.isSignedIn(this);
	}

	private boolean showPlays() {
		return Authenticator.isSignedIn(this) && PreferencesUtils.getSyncPlays(this);
	}

	public void onThumbnailClick(View v) {
		String imageUrl = (String) v.getTag(R.id.image);
		if (!TextUtils.isEmpty(imageUrl)) {
			final Intent intent = new Intent(this, ImageActivity.class);
			intent.putExtra(ImageActivity.KEY_IMAGE_URL, imageUrl);
			startActivity(intent);
		}
	}

	@Override
	public boolean onPlaySelected(int playId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		ActivityUtils.startPlayActivity(this, playId, gameId, gameName, thumbnailUrl, imageUrl);
		return false;
	}

	@Override
	public void onPlayCountChanged(int count) {
	}

	@Override
	public void onSortChanged(String sortName) {
		// sorting not supported yet
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (PreferencesUtils.isSyncPlays(key)) {
			updateTabs();
		}
	}

	@Override
	protected void onSignInSuccess() {
		super.onSignInSuccess();
		updateTabs();
	}
}
