package com.nearnotes;

import android.app.ActionBar.LayoutParams;
import android.content.res.Configuration;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.nearnotes.R;

public class MainActivity extends FragmentActivity implements
		NoteList.OnNoteSelectedListener, NoteEdit.noteEditListener,
		NoteLocation.NoteLocationListener, NoteSettings.noteSettingsListener {
	// Set class objects
	public NotesDbAdapter mDbHelper;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private NoteLocation mLoc;

	// Set simple variables
	private CharSequence mDrawerTitle;
	private CharSequence mTitle;
	private String[] mMenuTitles;
	private double mLatitude;
	private double mAccuracy;
	private double mLongitude;
	private int mFragType = 0;
	private boolean mOnlyOrientation = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mDbHelper = new NotesDbAdapter(this); // Create new custom database
												// class for sqlite and pass the
												// current context as a variable
		mDbHelper.open(); // Gets the writable database
		// mLoc = new NoteLocation(this);
		// double[] locations = mLoc.getLocation();
		// showDialogs(1);

		// enable ActionBar app icon to behave as action to toggle nav drawer
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		getActionBar().setBackgroundDrawable(
				getResources().getDrawable(R.drawable.ab_gradient));

		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT, Gravity.NO_GRAVITY);

		View customNav = LayoutInflater.from(this).inflate(R.layout.edit_title,
				null); // layout which contains your button.

		getActionBar().setDisplayShowCustomEnabled(true);

		getActionBar().setCustomView(customNav, lp);

		// START NAV DRAWER
		mTitle = mDrawerTitle = getTitle();
		mMenuTitles = getResources().getStringArray(R.array.menu_array);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		// set a custom shadow that overlays the main content when the drawer
		// opens
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);
		// set up the drawer's list view with items and click listener
		mDrawerList.setAdapter(new ArrayAdapter<String>(this,
				R.layout.drawer_list_item, mMenuTitles));
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		// ActionBarDrawerToggle ties together the the proper interactions
		// between the sliding drawer and the action bar app icon
		mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		mDrawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
		R.string.drawer_open, /* "open drawer" description for accessibility */
		R.string.drawer_close /* "close drawer" description for accessibility */
		) {
			@Override
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(mTitle);
				invalidateOptionsMenu(); // creates call to
											// onPrepareOptionsMenu()
			}

			@Override
			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(mDrawerTitle);
				invalidateOptionsMenu(); // creates call to
											// onPrepareOptionsMenu()
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		if (savedInstanceState == null) {
			// selectItem(0);
		}

		if (findViewById(R.id.fragment_container) != null) {

			// However, if we're being restored from a previous state,
			// then we don't need to do anything and should return or else
			// we could end up with overlapping fragments.
			if (savedInstanceState != null) {
				return;
			}

		}

	}

	public void setActionItems() {
		getActionBar().setDisplayShowCustomEnabled(false);
		mFragType = 2;
		setTitle("All Notes");
		invalidateOptionsMenu();

	}

	public void setEditItems() {
		mFragType = 1;
		getActionBar().setDisplayShowCustomEnabled(true);
		setTitle("");
		invalidateOptionsMenu();

	}

	public void setSettingsItems() {
		mFragType = 3;
		getActionBar().setDisplayShowCustomEnabled(false);
		setTitle("Settings");
		invalidateOptionsMenu();

	}

	public void onNoteSelected(long id) {
		NoteEdit newFragment = new NoteEdit();

		Bundle args = new Bundle();
		args.putLong("_id", id);
		newFragment.setArguments(args);

		FragmentTransaction transaction = getSupportFragmentManager()
				.beginTransaction();

		transaction.replace(R.id.fragment_container, newFragment);
		transaction.addToBackStack(null);
		transaction.commit();
		;

	}

	public Location onLocationFound(Location location, int TypeFrag) {
		if (location == null) {
			fetchAllNotes();
		} else {
			mLatitude = location.getLatitude();
			mLongitude = location.getLongitude();
			mAccuracy = location.getAccuracy();
			mLoc.dismiss();
			if (TypeFrag == 2) {
				fetchAllNotes();

			} else if (TypeFrag == 1) {
				fetchFirstNote();

			}
		}
		return location;
	}

	public void showDialogs(int TypeFrag) {
		// Create the fragment and show it as a dialog.
		mLoc = new NoteLocation();
		Bundle args = new Bundle();
		args.putInt("TypeFrag", TypeFrag);
		mLoc.setArguments(args);
		mLoc.show(getSupportFragmentManager(), "dialog");
	}

	/* The click listener for ListView in the navigation drawer */
	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			selectItem(position);
		}
	}

	private void selectItem(int position) {
		switch (position) {
		case 0:
			fetchFirstNote();
			break;
		case 1:
			fetchAllNotes();
			break;
		case 2:
			fetchSettings();
			break;

		}

		// update selected item and title, then close the drawer
		mDrawerList.setItemChecked(position, false);
		// setTitle(mMenuTitles[position]);
		mDrawerLayout.closeDrawer(mDrawerList);
	}

	public void replaceFragment(Fragment newFragment, Bundle bundle) {

		newFragment.setArguments(bundle);

		FragmentTransaction transaction1 = getSupportFragmentManager()
				.beginTransaction();

		transaction1.replace(R.id.fragment_container, newFragment);
		transaction1.addToBackStack(null);
		transaction1.commit();
		;

	}

	public boolean fetchFirstNote() {

		Cursor notesCursor = mDbHelper.fetchAllNotes(this, mLongitude,
				mLatitude);
		if (notesCursor.getCount() > 0) {

			notesCursor.moveToFirst();
			NoteEdit newFragment1 = new NoteEdit();

			Bundle newBundle = new Bundle();
			newBundle.putLong(NotesDbAdapter.KEY_ROWID, notesCursor.getLong(0));
			replaceFragment(newFragment1, newBundle);

			return true;
		} else {
			Toast.makeText(this, "No Notes Yet", Toast.LENGTH_SHORT).show();
			return false;
		}

	}

	public void fetchSettings() {

		getFragmentManager().beginTransaction()

		.replace(R.id.fragment_container, new NoteSettings())
				.addToBackStack(null).commit();

	}

	public void fetchAllNotes() {

		NoteList newFragment = new NoteList();

		Bundle args = new Bundle();
		args.putDouble("latitude", mLatitude);
		args.putDouble("longitude", mLongitude);

		replaceFragment(newFragment, args);

	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content
		// view
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
		switch (mFragType) {
		case 1:
			menu.findItem(R.id.action_new).setVisible(false);
			menu.findItem(R.id.action_done).setVisible(true);
			menu.findItem(R.id.action_location).setVisible(false);
			menu.findItem(R.id.action_note_menu).setVisible(true);
			break;
		case 2:
			menu.findItem(R.id.action_new).setVisible(true);
			menu.findItem(R.id.action_done).setVisible(false);
			menu.findItem(R.id.action_location).setVisible(true);
			menu.findItem(R.id.action_note_menu).setVisible(false);
			break;
		case 3:
			menu.findItem(R.id.action_new).setVisible(false);
			menu.findItem(R.id.action_done).setVisible(false);
			menu.findItem(R.id.action_location).setVisible(false);
			menu.findItem(R.id.action_note_menu).setVisible(false);
			break;
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getActionBar().setTitle(mTitle);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

			mOnlyOrientation = true;
		} else {
			mOnlyOrientation = true;
			Log.e("On Config Change", "PORTRAIT");
		}

		// Pass any configuration change to the drawer toggls
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_actions, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// The action bar home/up action should open or close the drawer.
		// ActionBarDrawerToggle will take care of this.
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_new:

			invalidateOptionsMenu();

			NoteEdit newFragment1 = new NoteEdit();

			Bundle args2 = new Bundle();
			args2.putDouble("latitude", mLatitude);
			args2.putDouble("longitude", mLongitude);
			newFragment1.setArguments(args2);

			FragmentTransaction transaction1 = getSupportFragmentManager()
					.beginTransaction();

			transaction1.replace(R.id.fragment_container, newFragment1);
			transaction1.addToBackStack(null);

			// Commit the transaction
			transaction1.commit();
			;
			return true;
		case R.id.action_done:
			NoteEdit noteFrag = (NoteEdit) getSupportFragmentManager()
					.findFragmentById(R.id.fragment_container);
			noteFrag.saveState();
			invalidateOptionsMenu();

			fetchAllNotes();

			return true;

		case R.id.action_location:

			showDialogs(2);

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}

	@Override
	public void onPostResume() {
		super.onPostResume();

		if (!mOnlyOrientation) {

			Cursor notesCursor = mDbHelper.fetchAllNotes(this, mLongitude,
					mLatitude);

			int settingsResult = mDbHelper.fetchSetting();

			if (notesCursor.getCount() == 0) {
				fetchAllNotes();
			} else {
				if (settingsResult > 0) {
					Log.e("db1", String.valueOf(settingsResult));
					onNoteSelected(settingsResult);
				} else {
					mFragType = 1;

					showDialogs(1);
				}
			}

		} else
			mOnlyOrientation = false;

	}

}
