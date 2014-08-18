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
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements NoteList.OnNoteSelectedListener, NoteEdit.noteEditListener,
		NoteLocation.NoteLocationListener, NoteSettings.noteSettingsListener, ChecklistDialog.CheckDialogListener, OverflowDialog.OverflowDialogListener {
	// Set class objects
	private static final int NOTE_EDIT = 1;
	private static final int NOTE_LIST = 2;
	private static final int NOTE_SETTINGS = 3;
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
	private double mLongitude;
	private int mFragType = 0;
	private boolean mOnlyOrientation = false;

	 @Override 
	 public void onOptionSelected(int which, long temporaryDelId) {

	       Log.e("which", String.valueOf(which));

			if (which == 1) {
				mDbHelper.deleteNote(temporaryDelId);
				if (mDbHelper.fetchSetting() == temporaryDelId) {
					mDbHelper.removeSetting();
				}
				fetchAllNotes();
			}
			if (which == 3) {
				
				fetchSettings();
			}
	       NoteEdit articleFrag = (NoteEdit) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
	       if (articleFrag != null) {
	            // If article frag is available, we're in two-pane layout...

	            // Call a method in the ArticleFragment to update its content
	            articleFrag.getCheckNumber(which);
	        } 
	    }
	
	 public void onConfirmSelected(int which, long mRowId) {
		 if (which == 1) {
				mDbHelper.deleteNote(mRowId);
				if (mDbHelper.fetchSetting() == mRowId) {
					mDbHelper.removeSetting();
				}
				fetchAllNotes();
			}
		 if (which == 0) {
			 NoteEdit articleFrag = (NoteEdit) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
		       if (articleFrag != null) {
		            // If article frag is available, we're in two-pane layout...

		            // Call a method in the ArticleFragment to update its content
		            articleFrag.getCheckNumber(which);
		        } 
		 }
	 }
	 
	 
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mDbHelper = new NotesDbAdapter(this); // Create new custom database class for sqlite and pass the current context as a variable
		mDbHelper.open(); // Gets the writable database
		
		// enable ActionBar app icon to behave as action to toggle nav drawer
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		LayoutParams lp = new LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
				Gravity.NO_GRAVITY);

		View customNav = LayoutInflater.from(this).inflate(R.layout.edit_title, null); // layout which contains your button.

		getActionBar().setDisplayShowCustomEnabled(true);
		getActionBar().setCustomView(customNav, lp);
		
		
		
		// Start nav drawer
		mTitle = mDrawerTitle = getTitle();
		mMenuTitles = getResources().getStringArray(R.array.menu_array);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START); // set a custom shadow that overlays the main content when the drawer opens
		mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, mMenuTitles)); // set up the drawer's list view with items and click listener
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		// ActionBarDrawerToggle ties together the the proper interactions between the sliding drawer and the action bar app icon
		mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		mDrawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
		R.string.drawer_open, /* "open drawer" description for accessibility */
		R.string.drawer_close /* "close drawer" description for accessibility */
		) {
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

		if (savedInstanceState == null) {
			// selectItem(0);
		}

		if (findViewById(R.id.fragment_container) != null) {

			// However, if we're being restored from a previous state, then we don't need to do anything and should return or else we could end up with overlapping fragments.
			if (savedInstanceState != null) {
				return;
			}
		}
	}

	@Override
	public void setActionItems() {
		getActionBar().setDisplayShowCustomEnabled(false);
		mFragType = NOTE_LIST;
		setTitle("All Notes");
		invalidateOptionsMenu();

	}

	@Override
	public void setEditItems() {
		mFragType = NOTE_EDIT;
		getActionBar().setDisplayShowCustomEnabled(true);
		setTitle("");
		invalidateOptionsMenu();

	}

	@Override
	public void setSettingsItems() {
		mFragType = NOTE_SETTINGS;
		getActionBar().setDisplayShowCustomEnabled(false);
		setTitle("Settings");
		invalidateOptionsMenu();

	}

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
		;

	}

	@Override
	public Location onLocationFound(Location location, int TypeFrag) {
		if (location == null) {
			fetchAllNotes();
		} else {
			mLatitude = location.getLatitude();
			mLongitude = location.getLongitude();
			// mAccuracy = location.getAccuracy();
			mLoc.dismiss();
			if (TypeFrag == NOTE_LIST) {
				fetchAllNotes();

			} else if (TypeFrag == NOTE_EDIT) {
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
		// If the nav drawer is open, hide action items related to the content
		// view
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
		// The action bar home/up action should open or close the drawer. ActionBarDrawerToggle will take care of this.
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

			FragmentTransaction transaction1 = getSupportFragmentManager().beginTransaction();
			transaction1.replace(R.id.fragment_container, newFragment1);
			transaction1.addToBackStack(null);
			transaction1.commit();
			;
			return true;
		case R.id.action_done:
			Toast.makeText(this, "Note Saved", Toast.LENGTH_SHORT).show();
			NoteEdit noteFrag = (NoteEdit) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
			noteFrag.saveState();
			invalidateOptionsMenu();
			fetchAllNotes();
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
			if (noteFrag2.mRowId == null) {
				noteFrag2.saveState();
			}
			Bundle args = new Bundle();
			args.putLong("_id", noteFrag2.mRowId);
			args.putInt("confirmSelection",1);
			newFragment.setArguments(args);
		
			if (getFragmentManager().findFragmentByTag("ConfirmDialog") == null) {
				newFragment.show(getSupportFragmentManager(), "ConfirmDialog");
			}
			
			
			return true;
		case R.id.action_sub_clear:
			NoteEdit noteFrag3 = (NoteEdit) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
			OverflowDialog newFragment2 = new OverflowDialog();
			if (noteFrag3.mRowId == null) {
				noteFrag3.saveState();
			}
			Bundle args3 = new Bundle();
			args3.putLong("_id", noteFrag3.mRowId);
			args3.putInt("confirmSelection",0);
			newFragment2.setArguments(args3);
		
			if (getFragmentManager().findFragmentByTag("ConfirmDialog") == null) {
				newFragment2.show(getSupportFragmentManager(), "ConfirmDialog");
			}
			
			
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onPostResume() {
		super.onPostResume();

		if (!mOnlyOrientation) {
			Cursor notesCursor = mDbHelper.fetchAllNotes(this, mLongitude, mLatitude);
			int settingsResult = mDbHelper.fetchSetting();

			if (notesCursor.getCount() == 0) {
				fetchAllNotes();
			} else {
				if (settingsResult > 0) {
					Log.e("db1", String.valueOf(settingsResult));
					onNoteSelected(settingsResult);
				} else {
					mFragType = NOTE_EDIT;
					showDialogs(NOTE_EDIT);
				}
			}
		} else
			mOnlyOrientation = false;
	}
}
