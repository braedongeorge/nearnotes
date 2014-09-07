/*
 * 	Copyright 2014 Braedon Reid
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 * 	You may obtain a copy of the License at
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * 
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */

package com.nearnotes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.dropbox.sync.android.DbxException;

import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements NoteList.OnNoteSelectedListener, NoteEdit.noteEditListener, NoteLocation.NoteLocationListener, NoteSettings.noteSettingsListener, ChecklistDialog.CheckDialogListener,
		OverflowDialog.OverflowDialogListener {
	// Set constants
	private static final int NOTE_EDIT = 1;
	private static final int NOTE_LIST = 2;
	private static final int NOTE_SETTINGS = 3;
	private static final int SELECTED_CLEAR = 0;
	private static final int SELECTED_DELETE = 1;
	private static final int SELECTED_SETTINGS = 3;
	private static final int REQUEST_LINK_TO_DBX = 0;

	// Set objects
	public NotesDbAdapter mDbHelper;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private NoteLocation mLoc;
	private AlertDialog mInvalidLocationDialog;
	private NotesDropbox mNotesDropbox;
	private ArrayAdapter<String> mDrawerAdapter;

	// Set primitive variables
	private CharSequence mDrawerTitle;
	private CharSequence mTitle;
	private String[] mMenuTitles;
	private double mLatitude;
	private double mLongitude;
	private int mFragType = 0;
	private boolean mOnlyOrientation = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mDbHelper = new NotesDbAdapter(this); // Create new custom database class for sqlite and pass the current context as a variable
		mDbHelper.open(); // Gets the writable database
		// enable ActionBar app icon to behave as action to toggle nav drawer

		mNotesDropbox = new NotesDropbox(this, getApplicationContext());
		LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL);
		View customNav = getLayoutInflater().inflate(R.layout.edit_title, null); // layout which contains your button.
		getActionBar().setDisplayShowCustomEnabled(true);
		getActionBar().setCustomView(customNav, lp);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		// getActionBar().setHomeButtonEnabled(true);

		// Start nav drawer
		mTitle = mDrawerTitle = getTitle();
		mMenuTitles = getResources().getStringArray(R.array.drawer_menu_array);

		mMenuTitles[2] = (mNotesDropbox.isLinked()) ? "Unlink your dropbox" : "Link your dropbox";

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		// mDrawerList.addFooterView((View) findViewById(R.id.drawer_footer), null, true);
		mDrawerAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item, mMenuTitles);

		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START); // set a custom shadow that overlays the main content when the drawer opens
		mDrawerList.setAdapter(mDrawerAdapter); // set up the drawer's list view with items and click listener
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		// ActionBarDrawerToggle ties together the the proper interactions between the sliding drawer and the action bar app icon
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
			@Override
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(mTitle);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}

			@Override
			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(mDrawerTitle);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		NoteList newFragment = new NoteList();
		Bundle args = new Bundle();
		args.putDouble("latitude", mLatitude);
		args.putDouble("longitude", mLongitude);
		newFragment.setArguments(args);
		FragmentTransaction transaction1 = getSupportFragmentManager().beginTransaction();
		transaction1.replace(R.id.fragment_container, newFragment);
		// transaction1.addToBackStack(null);
		transaction1.commit();
		//fetchAllNotes();
	}

	/**
	 * The callback function used for ChecklistDialog.java to check for which
	 * option was selected in the dialog and the unique rowId for the note
	 * currently opened.
	 * 
	 * @param which
	 *            Which button the user selected - clear, delete or do nothing
	 * @param temporaryDelId
	 *            Unique rowId for the note if the user seleted delete note
	 */
	@Override
	public void onOptionSelected(int which, long rowId) {
		switch (which) {
		case SELECTED_DELETE:
			mDbHelper.deleteNote(rowId);
			if (mDbHelper.fetchSetting() == rowId)
				mDbHelper.removeSetting();
			fetchAllNotes();
			break;
		case SELECTED_SETTINGS:
			fetchSettings();
			break;
		case SELECTED_CLEAR:
			NoteEdit articleFrag = (NoteEdit) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
			if (articleFrag != null)
				articleFrag.getCheckNumber();
			break;
		}
	}

	/**
	 * setActionItems is used to manipulate the actionbar to show the correct
	 * title and to change the fragment type to the currently displayed fragment
	 * (ie NoteEdit) InvalidateOptionsMenu is called to refresh the action bar
	 * title etc
	 * 
	 * @param fragType
	 *            The fragment type to be used (ie NoteEdit)
	 */
	@Override
	public void setActionItems(int fragType) {
		mFragType = fragType;
		switch (mFragType) {
		case NOTE_EDIT:
			getActionBar().setDisplayShowTitleEnabled(false);
			getActionBar().setDisplayShowCustomEnabled(true);
			setTitle("");
			break;
		case NOTE_SETTINGS:
			getActionBar().setDisplayShowTitleEnabled(true);
			getActionBar().setDisplayShowCustomEnabled(false);
			setTitle("Settings");
			break;
		case NOTE_LIST:
			getActionBar().setDisplayShowTitleEnabled(true);
			getActionBar().setDisplayShowCustomEnabled(false);
			setTitle("All Notes");
			break;
		}
		invalidateOptionsMenu();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_LINK_TO_DBX) {
			if (resultCode == RESULT_OK) {
				mMenuTitles[2] = "Unlink your dropbox";
				mDrawerAdapter.notifyDataSetChanged();
				Toast.makeText(this, "Link to Dropbox succeeded.", Toast.LENGTH_SHORT).show();
				try {
					mNotesDropbox.populateDropbox(mDbHelper.fetchAllNotes(this, mLongitude, mLatitude), true);
				} catch (DbxException e) {
					e.printStackTrace();
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
				}
			} else {
				// ... Link failed or was cancelled by the user.
				Toast.makeText(this, "Link to Dropbox failed.", Toast.LENGTH_SHORT).show();
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}

	}

	/**
	 * onNoteSelected creates a new NoteEdit object as a fragment and attachs
	 * the unique rowId for the not to be displayed in a bundle before calling
	 * the fragment manager to display
	 * 
	 * @param id
	 *            The unique rowId for the particular note.
	 */
	@Override
	public void onNoteSelected(long id) {
		NoteEdit newFragment = new NoteEdit();

		Bundle args = new Bundle();
		args.putLong("_id", id);
		newFragment.setArguments(args);

		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

		transaction.replace(R.id.fragment_container, newFragment);
		transaction.addToBackStack(null);
		transaction.commit();

	}

	/**
	 * onLocationFound is the callback listener for NoteLocation. The function
	 * extracts the current coordinates to global variables. Returns the
	 * fragment that initially called the NoteLocation dialog. If the location
	 * was not received successfully goes to the main list of note
	 * 
	 * @param location
	 *            The location object received from the NoteLocation dialog
	 * @param TypeFrag
	 *            The type of fragment that initially called the NoteLocation
	 *            dialog
	 */
	@Override
	public void onLocationFound(Location location, int typeFrag) {
		if (location == null) {
			fetchAllNotes();
		} else {
			mLatitude = location.getLatitude();
			mLongitude = location.getLongitude();
			mLoc.dismiss();
			if (typeFrag == NOTE_LIST)
				fetchAllNotes();
			else if (typeFrag == NOTE_EDIT)
				fetchFirstNote();
		}
	}

	/**
	 * Create the fragment and show it as a dialog.
	 * 
	 * @param typeFrag
	 *            The type of fragment to show after the NoteLocation dialog.
	 */
	public void showDialogs(int typeFrag) {
		mLoc = new NoteLocation();
		Bundle args = new Bundle();
		args.putInt("TypeFrag", typeFrag);
		mLoc.setArguments(args);
		mLoc.show(getSupportFragmentManager(), "dialog");
	}

	/**
	 * The click listener for ListView in the navigation drawer
	 * 
	 */
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

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
			if (mNotesDropbox.isLinked()) {
				mMenuTitles[2] = "Link your dropbox";
				mDrawerAdapter.notifyDataSetChanged();
				mNotesDropbox.unLink();
			} else 
				mNotesDropbox.dropboxLink();


			break;
		case 3:
			fetchSettings();
			break;
		}
		mDrawerList.setItemChecked(position, false); // update selected item and title, then close the drawer
		mDrawerLayout.closeDrawer(mDrawerList);
	}

	public void replaceFragment(Fragment newFragment, Bundle bundle) {
		newFragment.setArguments(bundle);
		FragmentTransaction transaction1 = getSupportFragmentManager().beginTransaction();
		transaction1.replace(R.id.fragment_container, newFragment);
		transaction1.addToBackStack(null);
		transaction1.commit();
		;
	}

	public boolean fetchFirstNote() {

		Cursor notesCursor = mDbHelper.fetchAllNotes(this, mLongitude, mLatitude);
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
		getFragmentManager().beginTransaction().replace(R.id.fragment_container, new NoteSettings()).addToBackStack(null).commit();

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
		// If the nav drawer is open, hide action items related to the content view
		// boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
		switch (mFragType) {
		case NOTE_EDIT:
			menu.findItem(R.id.action_new).setVisible(false);
			menu.findItem(R.id.action_done).setVisible(true);
			menu.findItem(R.id.action_location).setVisible(false);
			menu.findItem(R.id.action_overflow).setVisible(true);
			break;
		case NOTE_LIST:
			menu.findItem(R.id.action_new).setVisible(true);
			menu.findItem(R.id.action_done).setVisible(false);
			menu.findItem(R.id.action_location).setVisible(true);
			menu.findItem(R.id.action_overflow).setVisible(false);
			break;
		case NOTE_SETTINGS:
			menu.findItem(R.id.action_new).setVisible(false);
			menu.findItem(R.id.action_done).setVisible(false);
			menu.findItem(R.id.action_location).setVisible(false);
			menu.findItem(R.id.action_overflow).setVisible(false);
			break;
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		Log.d("setTitle", String.valueOf(title));
		getActionBar().setTitle(mTitle);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState(); // Sync the toggle state after onRestoreInstanceState has occurred.
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			mOnlyOrientation = true;
		} else {
			mOnlyOrientation = true;
		}
		mDrawerToggle.onConfigurationChanged(newConfig); // Pass any configuration change to the drawer toggls
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
		if (mDrawerToggle.onOptionsItemSelected(item)) { // The action bar home/up action should open or close the drawer. ActionBarDrawerToggle will take care of this.
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

			FragmentTransaction transaction1 = getSupportFragmentManager().beginTransaction();
			transaction1.replace(R.id.fragment_container, newFragment1);
			transaction1.addToBackStack(null);
			transaction1.commit();
			return true;
		case R.id.action_done:
			NoteEdit noteFrag = (NoteEdit) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
			if (noteFrag.mRowId != null)
				Toast.makeText(this, "Note Saved", Toast.LENGTH_SHORT).show();

			if (noteFrag.saveState() && !noteFrag.mNetworkTask) {
				try {
					mNotesDropbox.populateDropbox(mDbHelper.fetchAllNotes(this, mLongitude, mLatitude), false);
				} catch (DbxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				invalidateOptionsMenu();
				fetchAllNotes();

			} else if (noteFrag.mNetworkTask) {
				mInvalidLocationDialog = customAlert("Location Running", "Still updating location", "Cancel", "OK");
				mInvalidLocationDialog.show();
			} else {
				mInvalidLocationDialog = customAlert("Location Invalid", "The location needs to be selected from the drop down list to be valid. An active internet connection is also required.", "Cancel Note", "Fix Location");
				mInvalidLocationDialog.show();
			}
			return true;
		case R.id.action_location:
			showDialogs(NOTE_LIST);
			return true;
		case R.id.action_sub_toggle:
			NoteEdit noteFrag1 = (NoteEdit) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
			noteFrag1.toggleChecklist();
			return true;
		case R.id.action_sub_delete:
			NoteEdit noteFrag2 = (NoteEdit) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
			OverflowDialog newFragment = new OverflowDialog();

			Bundle args = new Bundle();
			if (noteFrag2.mRowId != null) {
				args.putLong("_id", noteFrag2.mRowId);
				args.putInt("confirmSelection", 1);
			} else
				args.putInt("confirmSelection", 2);

			newFragment.setArguments(args);
			if (getFragmentManager().findFragmentByTag("ConfirmDialog") == null)
				newFragment.show(getSupportFragmentManager(), "ConfirmDialog");
			return true;
		case R.id.action_sub_clear:
			NoteEdit noteFrag3 = (NoteEdit) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
			OverflowDialog newFragment2 = new OverflowDialog();

			Bundle args3 = new Bundle();
			if (noteFrag3.mRowId != null) {
				args3.putLong("_id", noteFrag3.mRowId);
			}
			args3.putInt("confirmSelection", 0);
			newFragment2.setArguments(args3);
			if (getFragmentManager().findFragmentByTag("ConfirmDialog") == null)
				newFragment2.show(getSupportFragmentManager(), "ConfirmDialog");
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public AlertDialog customAlert(String title, String body, String positive, String negative) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

		alertDialogBuilder.setTitle(title);

		alertDialogBuilder
				.setMessage(body)
				.setCancelable(false)
				.setPositiveButton(positive, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						fetchAllNotes();
					}
				})
				.setNegativeButton(negative, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});

		return alertDialogBuilder.create();
	}

	@Override
	public void onPostResume() {
		super.onPostResume();

		if (!mOnlyOrientation) {
			Cursor notesCursor = mDbHelper.fetchAllNotes(this, mLongitude, mLatitude);
			int settingsResult = mDbHelper.fetchSetting();
			mDrawerLayout.closeDrawer(mDrawerList);
			if (notesCursor.getCount() == 0) {
				showDialogs(NOTE_LIST);
			} else {
				if (settingsResult > 0) {
					onNoteSelected(settingsResult);
				} else {
					mFragType = NOTE_EDIT;
					showDialogs(NOTE_EDIT);
				}
			}
		} else
			mOnlyOrientation = false;
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mInvalidLocationDialog != null) {
			mInvalidLocationDialog.dismiss();
		}

	}
}
