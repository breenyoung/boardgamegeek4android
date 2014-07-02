package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.boardgamegeek.R;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.AutoCompleteAdapter;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

public class LogPlayerActivity extends SherlockFragmentActivity implements OnItemClickListener {
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_AUTO_POSITION = "AUTO_POSITION";
	public static final String KEY_END_PLAY = "SCORE_SHOWN";
	public static final String KEY_PLAYER = "PLAYER";
	private static final String KEY_TEAM_COLOR_SHOWN = "TEAM_COLOR_SHOWN";
	private static final String KEY_POSITION_SHOWN = "POSITION_SHOWN";
	private static final String KEY_SCORE_SHOWN = "SCORE_SHOWN";
	private static final String KEY_RATING_SHOWN = "RATING_SHOWN";
	private static final String KEY_NEW_SHOWN = "NEW_SHOWN";
	private static final String KEY_WIN_SHOWN = "WIN_SHOWN";

	private static final int HELP_VERSION = 1;

	private int mGameId;
	private String mGameName;

	private UsernameAdapter mUsernameAdapter;
	private Player mPlayer;
	private Player mOriginalPlayer;

	private ScrollView mScrollContainer;
	private TextView mHeader;
	private AutoCompleteTextView mUsername;
	private AutoCompleteTextView mName;
	private AutoCompleteTextView mTeamColor;
	private EditText mPosition;
	private Button mPositionButton;
	private EditText mScore;
	private Button mScoreButton;
	private EditText mRating;
	private CheckBox mNew;
	private CheckBox mWin;

	private boolean mPrefShowTeamColor;
	private boolean mPrefShowPosition;
	private boolean mPrefShowScore;
	private boolean mPrefShowRating;
	private boolean mPrefShowNew;
	private boolean mPrefShowWin;
	private boolean mTeamColorShown;
	private boolean mPositionShown;
	private boolean mScoreShown;
	private boolean mRatingShown;
	private boolean mNewShown;
	private boolean mWinShown;
	private int mAutoPosition;

	private final View.OnClickListener mActionBarListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			onActionBarItemSelected(v.getId());
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_logplayer);
		setUiVariables();

		// set up custom action bar
		getSupportActionBar().setDisplayOptions(
			ActionBar.DISPLAY_SHOW_CUSTOM,
			ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE
				| ActionBar.DISPLAY_SHOW_CUSTOM);
		LayoutInflater inflater = getLayoutInflater();
		View actionBarButtons = inflater.inflate(R.layout.actionbar_done_cancel, new LinearLayout(this), false);
		View cancelActionView = actionBarButtons.findViewById(R.id.menu_cancel);
		cancelActionView.setOnClickListener(mActionBarListener);
		View doneActionView = actionBarButtons.findViewById(R.id.menu_save);
		doneActionView.setOnClickListener(mActionBarListener);
		getSupportActionBar().setCustomView(actionBarButtons);

		final Intent intent = getIntent();
		mGameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		mGameName = intent.getStringExtra(KEY_GAME_NAME);
		mAutoPosition = intent.getIntExtra(KEY_AUTO_POSITION, Player.SEAT_UNKNOWN);
		if (intent.getBooleanExtra(KEY_END_PLAY, false)) {
			mScoreShown = true;
			mScore.requestFocus();
		}

		if (savedInstanceState == null) {
			mPlayer = intent.getParcelableExtra(KEY_PLAYER);
			if (mPlayer == null) {
				mPlayer = new Player();
			}
			if (hasAutoPosition()) {
				mPlayer.setSeat(mAutoPosition);
			}
			mOriginalPlayer = new Player(mPlayer);
		} else {
			mTeamColorShown = savedInstanceState.getBoolean(KEY_TEAM_COLOR_SHOWN);
			mPositionShown = savedInstanceState.getBoolean(KEY_POSITION_SHOWN);
			mScoreShown = savedInstanceState.getBoolean(KEY_SCORE_SHOWN);
			mRatingShown = savedInstanceState.getBoolean(KEY_RATING_SHOWN);
			mNewShown = savedInstanceState.getBoolean(KEY_NEW_SHOWN);
			mWinShown = savedInstanceState.getBoolean(KEY_WIN_SHOWN);

			mPlayer = savedInstanceState.getParcelable(KEY_PLAYER);
		}

		bindUi();

		mUsernameAdapter = new UsernameAdapter(this);
		mUsername.setAdapter(mUsernameAdapter);
		mName.setAdapter(new AutoCompleteAdapter(this, PlayPlayers.NAME, Plays.buildPlayersByNameWithoutUsernameUri(),
			PlayPlayers.NAME));
		mTeamColor.setAdapter(new AutoCompleteAdapter(this, GameColors.COLOR, Games.buildColorsUri(mGameId)));

		UIUtils.showHelpDialog(this, HelpUtils.HELP_LOGPLAYER_KEY, HELP_VERSION, R.string.help_logplayer);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mPrefShowTeamColor = PreferencesUtils.showLogPlayerTeamColor(this);
		mPrefShowPosition = PreferencesUtils.showLogPlayerPosition(this);
		mPrefShowScore = PreferencesUtils.showLogPlayerScore(this);
		mPrefShowRating = PreferencesUtils.showLogPlayerRating(this);
		mPrefShowNew = PreferencesUtils.showLogPlayerNew(this);
		mPrefShowWin = PreferencesUtils.showLogPlayerWin(this);
		setViewVisibility();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(KEY_PLAYER, mPlayer);
		outState.putBoolean(KEY_TEAM_COLOR_SHOWN, mTeamColorShown);
		outState.putBoolean(KEY_POSITION_SHOWN, mPositionShown);
		outState.putBoolean(KEY_SCORE_SHOWN, mScoreShown);
		outState.putBoolean(KEY_RATING_SHOWN, mRatingShown);
		outState.putBoolean(KEY_NEW_SHOWN, mNewShown);
		outState.putBoolean(KEY_WIN_SHOWN, mWinShown);
	}

	@Override
	public void onBackPressed() {
		cancel();
	}

	private void setUiVariables() {
		mScrollContainer = (ScrollView) findViewById(R.id.scroll_container);
		mHeader = (TextView) findViewById(R.id.header);
		mUsername = (AutoCompleteTextView) findViewById(R.id.log_player_username);
		mName = (AutoCompleteTextView) findViewById(R.id.log_player_name);
		mTeamColor = (AutoCompleteTextView) findViewById(R.id.log_player_team_color);
		mPosition = (EditText) findViewById(R.id.log_player_position);
		mPositionButton = (Button) findViewById(R.id.log_player_position_button);
		mScore = (EditText) findViewById(R.id.log_player_score);
		mScoreButton = (Button) findViewById(R.id.log_player_score_button);
		mRating = (EditText) findViewById(R.id.log_player_rating);
		mNew = (CheckBox) findViewById(R.id.log_player_new);
		mWin = (CheckBox) findViewById(R.id.log_player_win);

		mPositionButton.setOnClickListener(numberToTextClick());
		mScoreButton.setOnClickListener(numberToTextClick());
		mUsername.setOnItemClickListener(this);
	}

	private OnClickListener numberToTextClick() {
		return new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				EditText editText = null;
				if (v == mPositionButton) {
					editText = mPosition;
				} else if (v == mScoreButton) {
					editText = mScore;
				}
				if (editText == null) {
					return;
				}
				int type = editText.getInputType();
				if ((type & InputType.TYPE_CLASS_NUMBER) == InputType.TYPE_CLASS_NUMBER) {
					editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS
						| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
					((Button) v).setText(R.string.text_to_number);
				} else {
					editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
						| InputType.TYPE_NUMBER_FLAG_SIGNED);
					((Button) v).setText(R.string.number_to_text);
				}
				editText.requestFocus();
			}
		};
	}

	private void bindUi() {
		CharSequence title = getTitle() + (hasAutoPosition() ? " #" + mAutoPosition : "") + " - " + mGameName;
		mHeader.setText(title);
		mName.setTextKeepState(mPlayer.name);
		mUsername.setTextKeepState(mPlayer.username);
		mTeamColor.setTextKeepState(mPlayer.color);
		mPosition.setTextKeepState(mPlayer.getStartingPosition());
		mScore.setTextKeepState(mPlayer.score);
		mRating.setTextKeepState((mPlayer.rating == Player.DEFAULT_RATING) ? "" : String.valueOf(mPlayer.rating));
		mNew.setChecked(mPlayer.New());
		mWin.setChecked(mPlayer.Win());
	}

	private void setViewVisibility() {
		boolean enableButton = false;
		enableButton |= hideRow(shouldHideTeamColor(), findViewById(R.id.log_player_team_color_container));
		enableButton |= hideRow(hasAutoPosition() || shouldHidePosition(),
			findViewById(R.id.log_player_position_container));
		enableButton |= hideRow(shouldHideScore(), findViewById(R.id.log_player_score_container));
		enableButton |= hideRow(shouldHideRating(), findViewById(R.id.log_player_rating_container));
		enableButton |= hideRow(shouldHideNew(), mNew);
		enableButton |= hideRow(shouldHideWin(), mWin);
		findViewById(R.id.add_field).setEnabled(enableButton);
	}

	private boolean hideRow(boolean shouldHide, View view) {
		if (shouldHide) {
			view.setVisibility(View.GONE);
			return true;
		}
		view.setVisibility(View.VISIBLE);
		return false;
	}

	private boolean shouldHideTeamColor() {
		return !mPrefShowTeamColor && !mTeamColorShown && TextUtils.isEmpty(mPlayer.color);
	}

	private boolean shouldHidePosition() {
		return !mPrefShowPosition && !mPositionShown && TextUtils.isEmpty(mPlayer.getStartingPosition());
	}

	private boolean hasAutoPosition() {
		return mAutoPosition != Player.SEAT_UNKNOWN;
	}

	private boolean shouldHideScore() {
		return !mPrefShowScore && !mScoreShown && TextUtils.isEmpty(mPlayer.score);
	}

	private boolean shouldHideRating() {
		return !mPrefShowRating && !mRatingShown && !(mPlayer.rating > 0);
	}

	private boolean shouldHideNew() {
		return !mPrefShowNew && !mNewShown && !mPlayer.New();
	}

	private boolean shouldHideWin() {
		return !mPrefShowWin && !mWinShown && !mPlayer.Win();
	}

	private boolean onActionBarItemSelected(int itemId) {
		switch (itemId) {
			case R.id.menu_save:
				save();
				return true;
			case R.id.menu_cancel:
				cancel();
				return true;
		}
		return false;
	}

	public void addField(View v) {
		final CharSequence[] array = createAddFieldArray();
		if (array == null || array.length == 0) {
			return;
		}
		new AlertDialog.Builder(this).setTitle(R.string.add_field)
			.setItems(array, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Resources r = getResources();
					View viewToFocus = null;
					View viewToScroll = null;

					String selection = array[which].toString();
					if (selection == r.getString(R.string.team_color)) {
						mTeamColorShown = true;
						viewToFocus = mTeamColor;
						viewToScroll = findViewById(R.id.log_player_team_color_container);
					} else if (selection == r.getString(R.string.starting_position)) {
						mPositionShown = true;
						viewToFocus = mPosition;
						viewToScroll = findViewById(R.id.log_player_position_container);
					} else if (selection == r.getString(R.string.score)) {
						mScoreShown = true;
						viewToFocus = mScore;
						viewToScroll = findViewById(R.id.log_player_score_container);
					} else if (selection == r.getString(R.string.rating)) {
						mRatingShown = true;
						viewToFocus = mRating;
						viewToScroll = findViewById(R.id.log_player_rating);
					} else if (selection == r.getString(R.string.new_label)) {
						mNewShown = true;
						mNew.setChecked(true);
						viewToScroll = findViewById(R.id.log_player_checkbox_container);
						viewToFocus = mNew;
					} else if (selection == r.getString(R.string.win)) {
						mWinShown = true;
						mWin.setChecked(true);
						viewToScroll = findViewById(R.id.log_player_checkbox_container);
						viewToFocus = mWin;
					}
					setViewVisibility();
					if (viewToFocus != null) {
						viewToFocus.requestFocus();
					}
					if (viewToScroll != null) {
						final View finalView = viewToScroll;
						mScrollContainer.post(new Runnable() {
							@Override
							public void run() {
								mScrollContainer.smoothScrollTo(0, finalView.getBottom());
							}
						});
					}
				}
			}).show();
	}

	private CharSequence[] createAddFieldArray() {
		Resources r = getResources();
		List<CharSequence> list = new ArrayList<CharSequence>();

		if (shouldHideTeamColor()) {
			list.add(r.getString(R.string.team_color));
		}
		if (!hasAutoPosition() && shouldHidePosition()) {
			list.add(r.getString(R.string.starting_position));
		}
		if (shouldHideScore()) {
			list.add(r.getString(R.string.score));
		}
		if (shouldHideRating()) {
			list.add(r.getString(R.string.rating));
		}
		if (shouldHideNew()) {
			list.add(r.getString(R.string.new_label));
		}
		if (shouldHideWin()) {
			list.add(r.getString(R.string.win));
		}

		CharSequence[] csa = {};
		csa = list.toArray(csa);
		return csa;
	}

	private void save() {
		captureForm();
		Intent intent = new Intent();
		intent.putExtra(KEY_PLAYER, mPlayer);
		setResult(RESULT_OK, intent);
		finish();
	}

	private void cancel() {
		captureForm();
		if (mPlayer.equals(mOriginalPlayer)) {
			setResult(RESULT_CANCELED);
			finish();
		} else {
			ActivityUtils.createCancelDialog(this).show();
		}
	}

	private void captureForm() {
		mPlayer.name = mName.getText().toString().trim();
		mPlayer.username = mUsername.getText().toString().trim();
		mPlayer.color = mTeamColor.getText().toString().trim();
		mPlayer.setStartingPosition(mPosition.getText().toString().trim());
		mPlayer.score = mScore.getText().toString().trim();
		mPlayer.rating = StringUtils.parseDouble(mRating.getText().toString().trim());
		mPlayer.New(mNew.isChecked());
		mPlayer.Win(mWin.isChecked());
	}

	private class UsernameAdapter extends CursorAdapter {
		public UsernameAdapter(Context context) {
			super(context, null, false);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return getLayoutInflater().inflate(R.layout.dropdown_logplayer_buddy, parent, false);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			String userName = cursor.getString(BuddiesQuery.NAME);
			String firstName = cursor.getString(BuddiesQuery.FIRST_NAME);
			String lastName = cursor.getString(BuddiesQuery.LAST_NAME);
			String nickname = cursor.getString(BuddiesQuery.PLAY_NICKNAME);
			String fullName = (firstName + " " + lastName).trim();

			((TextView) view.findViewById(R.id.buddy_username)).setText(userName);
			((TextView) view.findViewById(R.id.buddy_description)).setText(fullName);
			((TextView) view.findViewById(R.id.buddy_nickname)).setText(nickname);
			view.setTag(TextUtils.isEmpty(nickname) ? firstName : nickname);
		}

		@Override
		public CharSequence convertToString(Cursor cursor) {
			return cursor.getString(BuddiesQuery.NAME);
		}

		@Override
		public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
			String selection = null;
			String[] selectionArgs = null;
			if (!TextUtils.isEmpty(constraint)) {
				selection = Buddies.BUDDY_NAME + " LIKE ? OR " + Buddies.BUDDY_FIRSTNAME + " LIKE ? OR "
					+ Buddies.BUDDY_LASTNAME + " LIKE ? OR " + Buddies.PLAY_NICKNAME + " LIKE ?";
				String selectionArg = constraint + "%";
				selectionArgs = new String[] { selectionArg, selectionArg, selectionArg, selectionArg };
			}
			return getContentResolver().query(Buddies.CONTENT_URI, BuddiesQuery.PROJECTION, selection, selectionArgs,
				Buddies.NAME_SORT);
		}
	}

	private interface BuddiesQuery {
		String[] PROJECTION = { Buddies._ID, Buddies.BUDDY_NAME, Buddies.BUDDY_FIRSTNAME, Buddies.BUDDY_LASTNAME,
			Buddies.PLAY_NICKNAME };
		int NAME = 1;
		int FIRST_NAME = 2;
		int LAST_NAME = 3;
		int PLAY_NICKNAME = 4;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		mName.setText((String) view.getTag());
	}
}
