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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxDatastoreInfo;
import com.dropbox.sync.android.DbxDatastoreManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFields;
import com.dropbox.sync.android.DbxPrincipal;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;

public class MainActivity extends FragmentActivity implements NoteList.OnNoteSelectedListener, NoteEdit.noteEditListener, NoteLocation.NoteLocationListener, NoteSettings.noteSettingsListener, ChecklistDialog.CheckDialogListener,
		OverflowDialog.OverflowDialogListener, DbxDatastore.SyncStatusListener, DbxDatastoreManager.ListListener {

	private static final int NOTE_EDIT = 1;
	private static final int NOTE_LIST = 2;
	private static final int NOTE_SETTINGS = 3;
	private static final int SELECTED_CLEAR = 0;
	private static final int SELECTED_DELETE = 1;
	private static final int SELECTED_SETTINGS = 3;
	private static final int REQUEST_LINK_TO_DBX = 0;

	private static final int NEAREST_NOTE = 0;
	private static final int ALL_NOTES = 1;
	private static final int SYNC_DROPBOX = 2;
	private static final int SETTINGS = 3;

	private static final String APP_KEY = "ulivcu8snndodzq";
	private static final String APP_SECRET = "beja1ot9y4uhj50";

	public NotesDbAdapter mDbHelper;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private NoteLocation mLoc;
	private AlertDialog mInvalidLocationDialog;
	private MyDrawerAdapter mDrawerAdapter;

	private CharSequence mDrawerTitle;
	private CharSequence mTitle;
	private String[] mMenuTitles;
	private double mLatitude;
	private double mLongitude;
	private DbxAccountManager mAccountManager;
	private DbxAccount mAccount;
	private int mFragType = 0;
	private boolean mIncoming = false;
	private Context mContext;

	private boolean mOnlyOrientation = false;
	private DbxDatastore mDatastore;
	private Set<DbxDatastore> mDatastoreMap;
	private DbxDatastoreManager mDatastoreManager;
	private boolean mFirstRun = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mContext = this;
		mDbHelper = new NotesDbAdapter(this); // Create new custom database class for sqlite and pass the current context as a variable
		mDbHelper.open(); // Gets the writable database

		mAccountManager = DbxAccountManager.getInstance(getApplicationContext(), APP_KEY, APP_SECRET);

		mDatastoreMap = new HashSet<DbxDatastore>();

		if (mAccountManager.hasLinkedAccount()) {
			mAccount = mAccountManager.getLinkedAccount();
			try {
				mDatastoreManager = DbxDatastoreManager.forAccount(mAccountManager.getLinkedAccount());
				Set<DbxDatastoreInfo> dataList = mDatastoreManager.listDatastores();
				mDatastoreManager.addListListener(this);
				for (DbxDatastoreInfo info : dataList) {
					if (info.id.matches(DbxDatastoreManager.DEFAULT_DATASTORE_ID)) {
						mDatastore = mDatastoreManager.openDefaultDatastore();
						mDatastoreMap.add(mDatastore);
					} else {
						if (null != info.title) {
							if (info.title.startsWith("shared_note_")) {
								mDatastoreMap.add(mDatastoreManager.openDatastore(info.id));
							}
						}
					}
				}
				for (DbxDatastore stores : mDatastoreMap) {
					stores.addSyncStatusListener(this);
				}
			} catch (DbxException e) {
				e.printStackTrace();
			}
		}

		getActionBar().setDisplayShowCustomEnabled(true);
		getActionBar().setCustomView(R.layout.edit_title);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		mTitle = mDrawerTitle = getTitle();
		mMenuTitles = getResources().getStringArray(R.array.drawer_menu_array);
		mMenuTitles[SYNC_DROPBOX] = (mAccountManager.hasLinkedAccount()) ? "Unsync Dropbox" : "Sync Dropbox";

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START); // set a custom shadow that overlays the main content when the drawer opens
		mDrawerToggle = new actionToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		mDrawerAdapter = new MyDrawerAdapter(this, mMenuTitles);

		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		mDrawerList.setDivider(new ColorDrawable(Color.WHITE));
		mDrawerList.setDividerHeight(1);
		mDrawerList.setAdapter(mDrawerAdapter); // set up the drawer's list view with items and click listener
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		Intent intent = getIntent();

		// Get the datastore ID.
		if (intent.getAction() == Intent.ACTION_VIEW) {
			// This means we were opened via the user clicking a link somewhere.
			// In that case, the URL looks like "https://www.nearnotes.com/#<DSID>"
			try {
				DbxDatastore store = mDatastoreManager.openDatastore(intent.getData().getFragment());
				store.addSyncStatusListener(this);
				mDatastoreMap.add(store);

			} catch (DbxException e) {
				e.printStackTrace();
			}
			Toast.makeText(this, intent.getData().getFragment().toString(), Toast.LENGTH_SHORT).show();
			// Log.e("get datastore id",intent.getData().getFragment());
		} else {
			// This means we were opened from another activity, which will pass the DSID as an extra.
			//  dsid = intent.getStringExtra("com.dropbox.examples.lists.DSID");
		}

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
			ArrayList<Long> arraylist = new ArrayList<Long>();
			arraylist.add(rowId);
			try {
				deleteDropboxNote(arraylist);
			} catch (DbxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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

	private DbxDatastore.SyncStatusListener mFirstRunListener = new DbxDatastore.SyncStatusListener() {
		@Override
		public void onDatastoreStatusChange(DbxDatastore store) {
			if (!store.getSyncStatus().isDownloading && !store.getSyncStatus().hasIncoming) {
				try {
				    checkRemoteDeletions(mDatastore.sync());
					Set<DbxDatastoreInfo> dataList = mDatastoreManager.listDatastores();
					for (DbxDatastoreInfo info : dataList) {
						if (info.id.matches(DbxDatastoreManager.DEFAULT_DATASTORE_ID)) {
							mDatastore.removeSyncStatusListener(mFirstRunListener);
							mDatastore.addSyncStatusListener(MainActivity.this);
							mDatastoreMap.add(mDatastore);
						} else {
							if (null != info.title) {
								if (info.title.startsWith("shared_note_")) {
									DbxDatastore temp = mDatastoreManager.openDatastore(info.id);
									temp.addSyncStatusListener(MainActivity.this);
									mDatastoreMap.add(temp);
								}
							}
						}
					}
					populateDropbox(notes(), true);

				} catch (DbxException e) {
					e.printStackTrace();
				}
			}
			
			if (store.getSyncStatus().hasIncoming) {
				try {
					store.sync();
				} catch (DbxException e) {
					e.printStackTrace();
				}
			}
		}
	};

	public Cursor notes() {
		return mDbHelper.fetchAllNotes(this, mLongitude, mLatitude);
	}

	@Override
	public void onDatastoreListChange(DbxDatastoreManager manager) {
		try {
			Set<DbxDatastoreInfo> info = manager.listDatastores();
			for (DbxDatastoreInfo i : info) {
				boolean change = true;
				for (DbxDatastore store : mDatastoreMap) {
					if (i.id.matches(store.getId())) {
						change = false;
					}
				}
				if (change) {
					mDatastoreMap.add(manager.openDatastore(i.id));
					Toast.makeText(this, "New Datastore Add: " + i.id, Toast.LENGTH_SHORT).show();
				}
			}
		} catch (DbxException e) {
			e.printStackTrace();
		}

	}

	private void checkRemoteDeletions(Map<String, Set<DbxRecord>> changes) throws DbxException {
		if (!changes.isEmpty()) {
			Set<DbxRecord> mySet = changes.get("notes");
			ArrayList<String> deleteList = new ArrayList<String>();
			for (DbxRecord s : mySet) {
				if (s.isDeleted()) {
					deleteList.add(s.getId());
				}
			}
			deleteRemoteNote(deleteList);
		}
	}

	@Override
	public void onDatastoreStatusChange(DbxDatastore store) {
		Toast.makeText(this, store.getId() + ": " + store.getSyncStatus().toString(), Toast.LENGTH_SHORT).show();
		if (!store.getSyncStatus().isDownloading && !store.getSyncStatus().hasIncoming && !store.getSyncStatus().isUploading && !store.getSyncStatus().hasOutgoing) {
			if (store.isOpen()) {
				Set<DbxTable> tables = store.getTables();
				int counter = 0;
				for (DbxTable t : tables) {
					if (!t.getId().matches(":info") && !t.getId().matches(":acl")) {
						Log.e(t.getId(), "id");
						counter++;
					}
				}
				Log.e(String.valueOf(counter), "counter");
				if (counter == 0 && !store.getId().matches(DbxDatastoreManager.DEFAULT_DATASTORE_ID)) {
					store.close();
					try {
						mDatastoreMap.remove(store);
						mDatastoreManager.deleteDatastore(store.getId());
					} catch (DbxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return;
				}
			}
		}

		if (store.getSyncStatus().hasIncoming) {
			try {
				for (DbxDatastore store1 : mDatastoreMap) {
					Map<String, Set<DbxRecord>> changes = store1.sync();
					if (!changes.isEmpty()) {
						Set<DbxRecord> mySet = changes.get("notes");
						if (!mySet.isEmpty()) {
							ArrayList<String> deleteList = new ArrayList<String>();
							for (DbxRecord s : mySet) {
								if (s.isDeleted())
									deleteList.add(s.getId());
							}
							deleteRemoteNote(deleteList);
						}
					}
				}
				populateDropbox(mDbHelper.fetchAllNotes(this, mLongitude, mLatitude), true);
			} catch (DbxException e) {
				e.printStackTrace();
			}
		}
	}

	public void populateDropbox(Cursor cursor, boolean incoming) throws DbxException {
		for (DbxDatastore store : mDatastoreMap) {
			syncingNotes(cursor, store, incoming);
		}
	}

	public void syncingNotes(Cursor cursor, DbxDatastore store, boolean incoming) throws DbxException {
		DbxTable table = store.getTable("notes");

		if (incoming) {
			DbxTable.QueryResult results = table.query();
			Iterator<DbxRecord> iterator = results.iterator();
			while (iterator.hasNext()) {
				DbxRecord record = iterator.next();
				Log.e("Recordid", record.getId());
				if (!mDbHelper.hasDropboxid(record.getId()).moveToFirst()) {
					mDbHelper.createNote(record.getString("title"),
							record.getString("body"),
							record.getDouble("latitude"),
							record.getDouble("longitude"),
							record.getString("location"),
							record.getString("checklist"),
							record.getId(),
							store.getId());
				} else {
					Cursor c = mDbHelper.hasDropboxid(record.getId());
					if (c.moveToFirst()) {
						if (!record.getString("title").matches(c.getString(1)) || !record.getString("body").matches(c.getString(2))) {
							mDbHelper.updateNote(c.getLong(0), record.getString("title"), record.getString("body"), c.getDouble(3), c.getDouble(4), c.getString(5), c.getString(6));
						}
					}
				}
			}
			fetchAllNotes();
		}
		else {
			if (cursor.moveToFirst()) {
				while (!cursor.isAfterLast()) {
					DbxFields noteFields = new DbxFields()
							.set("ID", cursor.getLong(0))
							.set("title", cursor.getString(1))
							.set("body", cursor.getString(2))
							.set("latitude", cursor.getDouble(3))
							.set("longitude", cursor.getDouble(4))
							.set("location", cursor.getString(5))
							.set("checklist", cursor.getString(6));

					String dropboxId = cursor.getString(7);
					String datastoreId = cursor.getString(8);

					if (!dropboxId.isEmpty() && datastoreId.matches(store.getId())) {
						DbxRecord r = table.get(dropboxId);
						if (null == r) {
							Log.e("Local", "dropboxId = " + dropboxId + " DatastoreId = " + datastoreId);
							Log.e("Remote", "dropboxId = NULL" + " DatastoreId = " + store.getId());
							DbxRecord insertRecord = table.insert(noteFields);
							mDbHelper.updateDropboxid(cursor.getLong(0), insertRecord.getId());
							store.sync();
						} else {
							if (!r.getString("title").matches(cursor.getString(1)) || !r.getString("body").matches(cursor.getString(2))) {
								table.get(dropboxId).setAll(noteFields);
								store.sync();
							}
						}
					} else if (store.getId().matches(DbxDatastoreManager.DEFAULT_DATASTORE_ID) && dropboxId.isEmpty()) {
						Log.e("LocalR", "dropboxId = " + dropboxId + " DatastoreId = " + datastoreId);
						Log.e("RemoteR", "dropboxId = NULL" + " DatastoreId = " + store.getId());
						DbxRecord insertRecord = table.insert(noteFields);
						mDbHelper.updateDropboxid(cursor.getLong(0), insertRecord.getId());
						store.sync();
					}
					cursor.moveToNext();
				}
			}
			fetchAllNotes();
		}
	}

	public void deleteDropboxNote(ArrayList<Long> deleteList) throws DbxException {
		for (long s : deleteList) {
			if (!mAccountManager.hasLinkedAccount()) {
				mDbHelper.deleteNote(s);
				if (mDbHelper.fetchSetting() == s) {
					mDbHelper.removeSetting();
				}

			} else {
				Cursor tempNote = mDbHelper.fetchNote(s);
				for (DbxDatastore store : mDatastoreMap) {
					DbxTable table = store.getTable("notes");
					

					if (tempNote.moveToFirst()) {

						if (tempNote.getString(8).matches(store.getId()) || (tempNote.getString(8).isEmpty() && store.getId().matches("default"))) {
							DbxRecord record = table.get(tempNote.getString(7));
							if (record != null) {
								record.deleteRecord();
							}

							mDbHelper.deleteNote(s);
							if (mDbHelper.fetchSetting() == s)
								mDbHelper.removeSetting();

						}
					}
					

				}

				for (DbxDatastore store : mDatastoreMap) {
					store.sync();
				}
				tempNote.close();
			}
		}

	}

	public void deleteRemoteNote(ArrayList<String> deleteList) throws DbxException {
		for (String s : deleteList) {
			for (DbxDatastore store : mDatastoreMap) {
				Cursor tempNote = mDbHelper.hasDropboxid(s);
				if (tempNote.moveToFirst()) {
					if (tempNote.getString(8).matches(store.getId()) || (tempNote.getString(8).isEmpty() && store.getId().matches(DbxDatastoreManager.DEFAULT_DATASTORE_ID))) {

						mDbHelper.deleteNote(tempNote.getLong(0));
						if (mDbHelper.fetchSetting() == tempNote.getLong(0))
							mDbHelper.removeSetting();

					}
				}
				tempNote.close();
				store.sync();
			}

		}
	}

	private class actionToggle extends ActionBarDrawerToggle {
		public actionToggle(Activity activity, DrawerLayout drawerLayout, int drawerImageRes, int openDrawerContentDescRes, int closeDrawerContentDescRes) {
			super(activity, drawerLayout, drawerImageRes, openDrawerContentDescRes, closeDrawerContentDescRes);
		}

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
				mMenuTitles[SYNC_DROPBOX] = "Unsync Dropbox";
				mDrawerAdapter.notifyDataSetChanged();
				Toast.makeText(this, "Link to Dropbox succeeded.", Toast.LENGTH_SHORT).show();
				try {
					mAccount = mAccountManager.getLinkedAccount();
					mDatastoreManager = DbxDatastoreManager.forAccount(mAccountManager.getLinkedAccount());
					mDatastore = mDatastoreManager.openDefaultDatastore();
					mDatastore.addSyncStatusListener(mFirstRunListener);
					// populateDropbox(notes(),true);
					mDatastore.sync();
				} catch (DbxException e) {
					e.printStackTrace();
				}
			} else
				Toast.makeText(this, "Link to Dropbox failed.", Toast.LENGTH_SHORT).show();
		} else
			super.onActivityResult(requestCode, resultCode, data);
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

	public boolean checkConnectivity() {
		ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

		boolean networkType = true;
		if (!wifiInfo.isConnected() && !networkInfo.isConnected())
			networkType = false;

		return networkType;

	}

	private void selectItem(int position) {
		switch (position) {
		case NEAREST_NOTE:
			fetchFirstNote();
			break;
		case ALL_NOTES:
			fetchAllNotes();
			break;
		case SYNC_DROPBOX:
			if (checkConnectivity()) {
				if (mAccountManager.hasLinkedAccount()) {
					mMenuTitles[SYNC_DROPBOX] = "Sync Dropbox";
					mDrawerAdapter.notifyDataSetChanged();
					mDatastore.close();
					mAccountManager.getLinkedAccount().unlink();
					mDatastore = null;
				} else
					mAccountManager.startLink(this, REQUEST_LINK_TO_DBX);
			} else
				Toast.makeText(this, "No Network Connectivity", Toast.LENGTH_SHORT).show();
			break;
		case SETTINGS:
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
				if (mAccountManager.hasLinkedAccount()) {
					try {
						populateDropbox(notes(), false);
					} catch (DbxException e) {
						e.printStackTrace();
					}
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
			if (mAccountManager.hasLinkedAccount()) {
				try {
					populateDropbox(notes(), true);
				} catch (DbxException e) {
					e.printStackTrace();
				}
			}
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
		case R.id.action_sub_share:
			try {
				NoteEdit noteFrag4 = (NoteEdit) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
				DbxDatastore store = mDatastoreManager.createDatastore();
				store.setTitle("shared_note_" + store.getId());
				store.setRole(DbxPrincipal.PUBLIC, DbxDatastore.Role.EDITOR);
				DbxTable table = store.getTable("notes");
				store.sync();
				String oldDropboxId = mDbHelper.getDropboxid(noteFrag4.mRowId);
				Log.e(oldDropboxId, "Old Dropbox Id");
				DbxRecord tempRecord = table.insert(mDatastore.getTable("notes").get(oldDropboxId));
				store.sync();
				mDbHelper.updateDropboxid(noteFrag4.mRowId, tempRecord.getId());
				mDbHelper.updateDatastoreid(noteFrag4.mRowId, store.getId());
				DbxTable mainTable = mDatastore.getTable("notes");
				mainTable.get(oldDropboxId).deleteRecord();
				mDatastore.sync();
				mDatastoreMap.add(store);
				store.addSyncStatusListener(this);

				Toast.makeText(this, store.getTitle(), Toast.LENGTH_SHORT).show();
				new ShareDialog(store, mAccount).show(getSupportFragmentManager(), "ShareDialog");

			} catch (DbxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

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

			if (mAccountManager.hasLinkedAccount() && !mFirstRun) {
				try {
					for (DbxDatastore store : mDatastoreMap) {
						if (!store.isOpen()) {
							if (store.getId().matches(DbxDatastoreManager.DEFAULT_DATASTORE_ID)) {
									mDatastore = mDatastoreManager.openDefaultDatastore();
									mDatastore.addSyncStatusListener(this);
							} else {
									mDatastoreManager.openDatastore(store.getId());
									store.addSyncStatusListener(this);
							}
						}
					}
					populateDropbox(notesCursor, true);
				} catch (DbxException e1) {
					e1.printStackTrace();
				}
			}

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
		if (mAccountManager.hasLinkedAccount()) {
			for (DbxDatastore store : mDatastoreMap) {
				store.removeSyncStatusListener(this);
				store.close();
			}
		}

	}

}
