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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxDatastore.Role;
import com.dropbox.sync.android.DbxDatastoreInfo;
import com.dropbox.sync.android.DbxDatastoreManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFields;
import com.dropbox.sync.android.DbxPrincipal;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;

public class MainActivity extends FragmentActivity implements NoteList.OnNoteSelectedListener, NoteEdit.noteEditListener, NoteLocation.NoteLocationListener, NoteSettings.noteSettingsListener, ChecklistDialog.CheckDialogListener,
		OverflowDialog.OverflowDialogListener, DbxDatastore.SyncStatusListener, DbxDatastoreManager.ListListener, DatastoreDialog.DatastoreDialogListener, ShareDialog.ShareDialogListener,
		NoteHelp.noteHelpListener {

	private static final int NOTE_EDIT = 1;
	private static final int NOTE_LIST = 2;
	private static final int NOTE_SETTINGS = 3;
	private static final int NOTE_HELP = 4;
	private static final int SELECTED_CLEAR = 0;
	private static final int SELECTED_DELETE = 1;
	private static final int SELECTED_SETTINGS = 3;
	private static final int REQUEST_LINK_TO_DBX = 0;

	private static final int NEAREST_NOTE = 0;
	private static final int ALL_NOTES = 1;
	private static final int SYNC_DROPBOX = 2;
	private static final int SETTINGS = 3;
	private static final int HELP = 4;

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

	private boolean mOnlyOrientation = false;
	private DbxDatastore mDatastore;
	private ArrayList<DbxDatastore> mDatastoreMap;
	private DbxDatastoreManager mDatastoreManager;
	private boolean mFirstRun = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mDbHelper = new NotesDbAdapter(this); // Create new custom database class for sqlite and pass the current context as a variable
		mDbHelper.open(); // Gets the writable database

		mAccountManager = DbxAccountManager.getInstance(getApplicationContext(), APP_KEY, APP_SECRET);

		mDatastoreMap = new ArrayList<DbxDatastore>();

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
								Log.e("ADDED DATASTORE onCreate2", info.id);
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
			if (mAccountManager.hasLinkedAccount()) {
				// This means we were opened via the user clicking a link somewhere.
				// In that case, the URL looks like "https://www.nearnotes.com/#<DSID>"
				try {
					DbxDatastore store = mDatastoreManager.openDatastore(intent.getData().getFragment());
					store.addSyncStatusListener(this);
					Log.e("ADDED DATASTORE on Open via url", store.getId());
					mDatastoreMap.add(store);

				} catch (DbxException e) {
					e.printStackTrace();
				}
				Toast.makeText(this, intent.getData().getFragment().toString(), Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, "Sync dropbox to get access to shared note", Toast.LENGTH_SHORT).show();
			}
		} else {
			// This means we were opened from another activity, which will pass the DSID as an extra.
			//  dsid = intent.getStringExtra("com.dropbox.examples.lists.DSID");
		}

	}

	public void toggle_contents(View v) {
		View addHelpContents = (View) findViewById(R.id.help_add_note_layout);
		TextView t = (TextView) findViewById(R.id.help_adding_note_title);
		if (addHelpContents.isShown()) {
			t.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_expand, 0, 0, 0);
			
			slide_up(this, addHelpContents);
			// addHelpContents.setVisibility(View.GONE);
		}
		else {
			t.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_collapse, 0, 0, 0);
			addHelpContents.setVisibility(View.VISIBLE);
			slide_down(this, addHelpContents);
		}
	}
	
	public void toggle_contents_delete(View v) {
		View addHelpContents = (View) findViewById(R.id.help_delete_note_layout);
		TextView t = (TextView) findViewById(R.id.help_deleting_note_title);
		if (addHelpContents.isShown()) {
			t.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_expand, 0, 0, 0);
			
			slide_up(this, addHelpContents);
			// addHelpContents.setVisibility(View.GONE);
		}
		else {
			t.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_collapse, 0, 0, 0);
			addHelpContents.setVisibility(View.VISIBLE);
			slide_down(this, addHelpContents);
		}
	}
	
	public void toggle_contents_sync(View v) {
		View addHelpContents = (View) findViewById(R.id.help_sync_notes_layout);
		TextView t = (TextView) findViewById(R.id.help_sync_notes_title);
		if (addHelpContents.isShown()) {
			t.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_expand, 0, 0, 0);
			
			slide_up(this, addHelpContents);
			// addHelpContents.setVisibility(View.GONE);
		}
		else {
			t.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_collapse, 0, 0, 0);
			addHelpContents.setVisibility(View.VISIBLE);
			slide_down(this, addHelpContents);
		}
	}
	
	public void toggle_contents_share(View v) {
		View addHelpContents = (View) findViewById(R.id.help_share_notes_layout);
		TextView t = (TextView) findViewById(R.id.help_share_notes_title);
		if (addHelpContents.isShown()) {
			t.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_expand, 0, 0, 0);
			
			slide_up(this, addHelpContents);
			// addHelpContents.setVisibility(View.GONE);
		}
		else {
			t.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_collapse, 0, 0, 0);
			addHelpContents.setVisibility(View.VISIBLE);
			slide_down(this, addHelpContents);
		}
	}

	public static void slide_down(Context ctx, View v) {

		Animation a = AnimationUtils.loadAnimation(ctx, R.anim.slide_down);
		if (a != null) {
			a.reset();
			if (v != null) {
				v.clearAnimation();
				v.startAnimation(a);
			}
		}
	}

	public static void slide_up(Context ctx, View v) {
		final View tempView = (View) v;
		
		Animation a = AnimationUtils.loadAnimation(ctx, R.anim.slide_up);
		if (a != null) {
			a.reset();
			if (v != null) {
				v.clearAnimation();
				v.startAnimation(a);
			}
		}

		a.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation arg0) {
			}

			@Override
			public void onAnimationRepeat(Animation arg0) {
			}

			@Override
			public void onAnimationEnd(Animation arg0) {
				tempView.setVisibility(View.GONE);
			}
		});
	}

	public void onDatastoreSelected(DbxRecord record) {
		if (null != record) {
			mDbHelper.createNote(record.getString("title"),
					record.getString("body"),
					record.getDouble("latitude"),
					record.getDouble("longitude"),
					record.getString("location"),
					record.getString("checklist"),
					"",
					"");
			populateDropbox(notes(), false);
		} else {
			populateDropbox(notes(), true);
		}
		/*
		 * try { Cursor cursor = mDbHelper.fetchAllNotes(this, mLongitude,
		 * mLatitude); DbxTable table = mDatastore.getTable("notes"); if
		 * (cursor.moveToFirst()) { while (!cursor.isAfterLast()) { boolean
		 * change = true; Set<DbxDatastoreInfo> info1 =
		 * mDatastoreManager.listDatastores(); for (DbxDatastoreInfo i : info1)
		 * { Log.e(i.id, "Database in onDatastoreListChange"); if
		 * (mDbHelper.getDatastoreid(cursor.getLong(0)).matches(i.id) ||
		 * mDbHelper.getDatastoreid(cursor.getLong(0)).isEmpty()) { change =
		 * false; } } if (change) { DbxFields noteFields = new DbxFields()
		 * .set("ID", cursor.getLong(0)) .set("title", cursor.getString(1))
		 * .set("body", cursor.getString(2)) .set("latitude",
		 * cursor.getDouble(3)) .set("longitude", cursor.getDouble(4))
		 * .set("location", cursor.getString(5)) .set("checklist",
		 * cursor.getString(6));
		 * 
		 * DbxRecord insertRecord = table.insert(noteFields);
		 * mDbHelper.updateDropboxid(cursor.getLong(0), insertRecord.getId());
		 * mDbHelper.updateDatastoreid(cursor.getLong(0), mDatastore.getId());
		 * mDatastore.sync();
		 * 
		 * } cursor.moveToNext(); } } } catch (DbxException e) {
		 * e.printStackTrace(); }
		 */
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
			String datastoreId = mDbHelper.getDatastoreid(rowId);
			if (!datastoreId.isEmpty() && !datastoreId.contains("default")) {
				try {
					Iterator<DbxDatastore> iter = mDatastoreMap.iterator();
					while (iter.hasNext()) {
						DbxDatastore store = iter.next();
						if (store.getId().matches(datastoreId)) {
							if (store.getEffectiveRole().equals(Role.EDITOR)) {
								store.close();
								mDatastoreManager.deleteDatastore(datastoreId);
								iter.remove();
							} else {
								deleteDropboxNote(arraylist);
							}

						}
					}
				} catch (DbxException e) {
					e.printStackTrace();
				}

			} else {
				deleteDropboxNote(arraylist);
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
			Log.w(store.getId(), "Is current using mFirstRunListener");
			if (!store.getSyncStatus().isDownloading && !store.getSyncStatus().hasIncoming) {
				mDatastore.removeSyncStatusListener(mFirstRunListener);
				mDatastore.addSyncStatusListener(MainActivity.this);
				try {
					checkRemoteDeletions(mDatastore.sync());
					Set<DbxDatastoreInfo> dataList = mDatastoreManager.listDatastores();
					for (DbxDatastoreInfo info : dataList) {
						Log.e(info.id, "List datastores in mFirstRunListenr");
						if (info.id.matches(DbxDatastoreManager.DEFAULT_DATASTORE_ID)) {

						} else {
							if (null != info.title) {
								if (info.title.startsWith("shared_note_")) {
									DbxDatastore temp = mDatastoreManager.openDatastore(info.id);
									temp.addSyncStatusListener(MainActivity.this);
									Log.e("ADDED DATASTORE mFirstRunListener", temp.getId());
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

	private DbxDatastore.SyncStatusListener addListener = new DbxDatastore.SyncStatusListener() {

		@Override
		public void onDatastoreStatusChange(DbxDatastore store) {
			Log.w(store.getId(), store.getSyncStatus().toString());
			Log.w(store.getId(), "Is current using addListener");
			if (store.getSyncStatus().hasIncoming) {
				try {
					store.sync();
				} catch (DbxException e) {
					e.printStackTrace();
				}
				populateDropbox(notes(), true);
				store.removeSyncStatusListener(this);
				Log.w("Getting to add sync status", "true");
				store.addSyncStatusListener(MainActivity.this);

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
				Log.e(i.id, "Database in onDatastoreListChange");
				boolean newRemote = true;
				for (DbxDatastore store : mDatastoreMap) {

					if (i.id.matches(store.getId())) {
						newRemote = false;
					}
				}
				if (newRemote) {
					Log.e("ADDED DATASTORE onDatastoreListChange", i.id);
					DbxDatastore store = manager.openDatastore(i.id);
					store.addSyncStatusListener(addListener);
					mDatastoreMap.add(store);

					// Toast.makeText(this, "New Datastore Add: " + i.id, Toast.LENGTH_SHORT).show();
				}
			}
			Iterator<DbxDatastore> storeIter = mDatastoreMap.iterator();
			while (storeIter.hasNext()) {
				DbxDatastore store = storeIter.next();
				boolean deleteRemote = true;
				for (DbxDatastoreInfo i : info) {
					if (store.getId().matches(i.id)) {
						deleteRemote = false;
					}

				}
				if (deleteRemote) {
					DbxTable.QueryResult results = store.getTable("notes").query();
					Iterator<DbxRecord> iterator = results.iterator();
					Log.e("Delete Remote Trigger", store.getSyncStatus().toString());
					while (iterator.hasNext()) {
						DbxRecord tempRecord = iterator.next();
						DatastoreDialog datastoreDialog = new DatastoreDialog(tempRecord);
						Cursor recordCursor = mDbHelper.hasDropboxid(tempRecord.getId());
						if (recordCursor.moveToFirst()) {
							mDbHelper.deleteNote(recordCursor.getLong(0));
						}

						Log.e(store.getId(), "Store doesnt exist on the server - do something");

						if (getFragmentManager().findFragmentByTag("DatastoreDialog") == null && !store.getEffectiveRole().equals(Role.OWNER))
							datastoreDialog.show(getSupportFragmentManager(), "DatastoreDialog");

					}
					storeIter.remove();

				}
			}
			populateDropbox(notes(), true);
			// populateDropbox(notes(), false);
		} catch (DbxException e) {
			e.printStackTrace();
		}
		invalidateOptionsMenu();
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
		Log.e(store.getId(), store.getSyncStatus().toString());

		if (store.getSyncStatus().isConnected && !store.getSyncStatus().isDownloading && !store.getSyncStatus().hasIncoming && !store.getSyncStatus().isUploading && !store.getSyncStatus().hasOutgoing) {
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
					Log.e(store.getId(), "oh shit its closing store");
					store.close();
					try {
						mDatastoreMap.remove(store);
						mDatastoreManager.deleteDatastore(store.getId());
					} catch (DbxException e) {
						e.printStackTrace();
					}
					return;
				}
			}
		} else {
			Toast.makeText(this, "Syncing notes", Toast.LENGTH_SHORT).show();
		}

		if (store.getSyncStatus().hasIncoming) {

			try {
				Map<String, Set<DbxRecord>> changes = store.sync();

				checkDeletions(changes);
				populateDropbox(mDbHelper.fetchAllNotes(this, mLongitude, mLatitude), true);
				Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

				if (fragment instanceof NoteEdit) {
					final NoteEdit noteEdit = (NoteEdit) fragment;
					if (!changes.isEmpty()) {
						Set<DbxRecord> mySet = changes.get("notes");
						if (null != mySet) {
							for (DbxRecord s : mySet) {
								if (s.getId().matches(mDbHelper.getDropboxid(noteEdit.mRowId))) {
									AlertDialog currentNote = new AlertDialog.Builder(this)
											.setTitle("Note Changed")
											.setMessage("Your current note has been changed remotely")
											.setPositiveButton("Keep local copy", new DialogInterface.OnClickListener() {
												public void onClick(DialogInterface dialog, int which) {

												}
											})
											.setNegativeButton("Update to remote copy", new DialogInterface.OnClickListener() {
												public void onClick(DialogInterface dialog, int which) {
													noteEdit.populateFields();
												}
											}).create();

									if (!currentNote.isShowing()) {
										currentNote.show();
									}
								}
							}
						}
					}
				}
			} catch (DbxException e) {
				e.printStackTrace();
			}
		}
		if (store.getSyncStatus().hasOutgoing) {
			try {
				checkDeletions(store.sync());
				populateDropbox(mDbHelper.fetchAllNotes(this, mLongitude, mLatitude), false);
			} catch (DbxException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean populateDropbox(Cursor cursor, boolean incoming) {
		for (DbxDatastore store : mDatastoreMap) {

			try {
				store.sync();
				Log.e(store.getId(), "Is owner?" + store.getEffectiveRole().equals(Role.OWNER));
				syncingNotes(cursor, store, incoming);
			} catch (DbxException e) {
				e.printStackTrace();
				return false;
			}
		}

		try {
			Set<DbxDatastoreInfo> dataList;
			dataList = mDatastoreManager.listDatastores();
			mDatastoreManager.addListListener(this);

			if (cursor.moveToFirst()) {
				while (!cursor.isAfterLast()) {
					boolean keep = false;
					for (DbxDatastoreInfo info : dataList) {
						if (cursor.getString(8).matches(info.id) || cursor.getString(8).isEmpty()) {
							keep = true;
						}
					}
					if (!keep) {
						Log.e("mDbHelper.deleteNote", String.valueOf(cursor.getLong(0)));
						mDbHelper.deleteNote(cursor.getLong(0));
					}

					cursor.moveToNext();
				}
			}
		} catch (DbxException e) {
			e.printStackTrace();
		}

		Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
		if (fragment instanceof NoteList) {
			fetchAllNotes();
		}

		return true;
	}

	public void checkDeletions(Map<String, Set<DbxRecord>> changes) {
		if (!changes.isEmpty()) {
			Set<DbxRecord> mySet = changes.get("notes");
			if (null != mySet) {
				ArrayList<String> deleteList = new ArrayList<String>();
				for (DbxRecord s : mySet) {
					if (s.isDeleted())
						deleteList.add(s.getId());
				}
				try {
					deleteRemoteNote(deleteList);
				} catch (DbxException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void syncingNotes(Cursor cursor, DbxDatastore store, boolean incoming) throws DbxException {
		DbxTable table = store.getTable("notes");

		if (incoming) {
			DbxTable.QueryResult results = table.query();
			Iterator<DbxRecord> iterator = results.iterator();
			while (iterator.hasNext()) {
				DbxRecord record = iterator.next();
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
						if (!record.getString("title").matches(Pattern.quote(c.getString(1))) || !record.getString("body").matches(Pattern.quote(c.getString(2)))) {
							mDbHelper.updateNote(c.getLong(0), record.getString("title"), record.getString("body"), c.getDouble(3), c.getDouble(4), c.getString(5), c.getString(6));
						}
					}
				}

				if (!record.getString("dropboxidold").isEmpty()) {
					Cursor c = mDbHelper.hasDropboxid(record.getString("dropboxidold"));
					if (c.moveToFirst()) {
						mDbHelper.deleteNote(c.getLong(0));
					}
				}
			}
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
							.set("checklist", cursor.getString(6))
							.set("dropboxidold", "");

					String dropboxId = cursor.getString(7);
					String datastoreId = cursor.getString(8);

					if (!dropboxId.isEmpty() && datastoreId.matches(store.getId())) {
						DbxRecord r = table.get(dropboxId);
						if (null == r) {
							DbxRecord insertRecord = table.insert(noteFields);
							mDbHelper.updateDropboxid(cursor.getLong(0), insertRecord.getId());
							//store.sync();
						} else {
							if (!r.getString("title").matches(Pattern.quote(cursor.getString(1))) || !r.getString("body").matches(Pattern.quote(cursor.getString(2)))) {
								table.get(dropboxId).setAll(noteFields);
								store.sync();
							}
						}
					} else if (store.getId().matches(DbxDatastoreManager.DEFAULT_DATASTORE_ID) && dropboxId.isEmpty()) {
						DbxRecord insertRecord = table.insert(noteFields);
						mDbHelper.updateDropboxid(cursor.getLong(0), insertRecord.getId());
						store.sync();
					}
					cursor.moveToNext();
				}
			}
		}
	}

	public void deleteDropboxNote(ArrayList<Long> deleteList) {
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
							DbxRecord record;
							try {
								record = table.get(tempNote.getString(7));
								if (record != null) {
									record.deleteRecord();
								}
							} catch (DbxException e) {
								e.printStackTrace();
							}

							mDbHelper.deleteNote(s);
							if (mDbHelper.fetchSetting() == s)
								mDbHelper.removeSetting();

						}
					}
				}

				for (DbxDatastore store : mDatastoreMap) {
					try {
						store.sync();
					} catch (DbxException e) {
						e.printStackTrace();
					}
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
		case NOTE_HELP:
			getActionBar().setDisplayShowTitleEnabled(true);
			getActionBar().setDisplayShowCustomEnabled(false);
			setTitle("Help");
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
					mDatastoreManager.addListListener(this);
					mDatastore = mDatastoreManager.openDefaultDatastore();
					mDatastoreMap.add(mDatastore);
					mDatastore.addSyncStatusListener(mFirstRunListener);
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
					mDatastoreMap.clear();
					mDatastore = null;
				} else
					mAccountManager.startLink(this, REQUEST_LINK_TO_DBX);
			} else
				Toast.makeText(this, "No Network Connectivity", Toast.LENGTH_SHORT).show();
			break;
		case SETTINGS:
			fetchSettings();
			break;
		case HELP:
			NoteHelp helpFragment = new NoteHelp();
			replaceFragment(helpFragment, new Bundle());
			break;
		}
		mDrawerList.setItemChecked(position, false); // update selected item and title, then close the drawer
		mDrawerLayout.closeDrawer(mDrawerList);
	}

	public void replaceFragment(Fragment fragment, Bundle bundle) {

		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		if (fragment instanceof NoteEdit) {
			NoteEdit customFragment = (NoteEdit) fragment;
			customFragment.setArguments(bundle);
			transaction.replace(R.id.fragment_container, customFragment);
		} else if (fragment instanceof NoteList) {
			NoteList customFragment = (NoteList) fragment;
			customFragment.setArguments(bundle);
			transaction.replace(R.id.fragment_container, customFragment);
		} else if (fragment instanceof NoteHelp) {
			NoteHelp customFragment = (NoteHelp) fragment;
			customFragment.setArguments(bundle);
			transaction.replace(R.id.fragment_container, customFragment);
		}

		transaction.addToBackStack(null);
		transaction.commit();

	}

	public boolean fetchFirstNote() {

		Cursor cursor = mDbHelper.fetchAllNotes(this, mLongitude, mLatitude);
		if (cursor.moveToFirst()) {

			NoteEdit noteEdit = new NoteEdit();
			Bundle bundle = new Bundle();
			bundle.putLong(NotesDbAdapter.KEY_ROWID, cursor.getLong(cursor.getColumnIndexOrThrow(NotesDbAdapter.KEY_ROWID)));
			replaceFragment(noteEdit, bundle);

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
			Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
			if (fragment instanceof NoteEdit) {
				NoteEdit noteEdit = (NoteEdit) fragment;
				if (noteEdit.mRowId != null) {
					String tempstoreId = mDbHelper.getDatastoreid(noteEdit.mRowId);
					if (tempstoreId.startsWith(".")) {
						menu.findItem(R.id.action_sub_share).setTitle("Manage share");
						if (!mAccountManager.hasLinkedAccount()) {
							menu.findItem(R.id.action_sub_share).setEnabled(false);
						}
					} else {
						if (!mAccountManager.hasLinkedAccount()) {
							menu.findItem(R.id.action_sub_share).setEnabled(false);
						}
					}
				}
			}
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
		case NOTE_HELP:
			menu.findItem(R.id.action_new).setVisible(false);
			menu.findItem(R.id.action_done).setVisible(false);
			menu.findItem(R.id.action_location).setVisible(false);
			menu.findItem(R.id.action_overflow).setVisible(false);
			break;
		default:
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

		Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
		NoteEdit noteEdit = new NoteEdit();
		if (fragment instanceof NoteEdit) {
			noteEdit = (NoteEdit) fragment;
		}
		OverflowDialog overflowDialog = new OverflowDialog();
		Bundle bundle = new Bundle();

		switch (item.getItemId()) {
		case R.id.action_new:
			invalidateOptionsMenu();
			bundle.putDouble("latitude", mLatitude);
			bundle.putDouble("longitude", mLongitude);
			replaceFragment(noteEdit, bundle);

			return true;
		case R.id.action_done:
			actionDone(true, true);

			return true;
		case R.id.action_location:
			Log.e(String.valueOf(mAccountManager.hasLinkedAccount()), "mAccountManager");
			if (mAccountManager.hasLinkedAccount()) {
				for (DbxDatastore syncStore : mDatastoreMap) {
					try {
						syncStore.sync();
					} catch (DbxException e) {
						e.printStackTrace();
					}
				}

				populateDropbox(notes(), true);
				populateDropbox(notes(), false);
			}
			showDialogs(NOTE_LIST);

			return true;
		case R.id.action_sub_toggle:
			noteEdit.toggleChecklist();

			return true;
		case R.id.action_sub_delete:
			if (noteEdit.mRowId != null) {
				bundle.putLong("_id", noteEdit.mRowId);
				bundle.putInt("confirmSelection", 1);
			} else {
				bundle.putInt("confirmSelection", 2);
			}

			showOverflowDialog(overflowDialog, bundle);
			return true;
		case R.id.action_sub_clear:
			if (noteEdit.mRowId != null) {
				bundle.putLong("_id", noteEdit.mRowId);
			}
			bundle.putInt("confirmSelection", 0);
			showOverflowDialog(overflowDialog, bundle);

			return true;
		case R.id.action_sub_share:
			if (actionDone(false, false)) {
				if (item.getTitle().toString().matches("Share Note")) {
					try {
						DbxDatastore store = mDatastoreManager.createDatastore();
						store.setTitle("shared_note_" + store.getId());
						store.setRole(DbxPrincipal.PUBLIC, DbxDatastore.Role.EDITOR);
						DbxTable table = store.getTable("notes");
						store.sync();
						String oldDropboxId = mDbHelper.getDropboxid(noteEdit.mRowId);
						DbxRecord tempRecord = table.insert(mDatastore.getTable("notes").get(oldDropboxId));
						tempRecord.set("dropboxidold", oldDropboxId);
						store.sync();
						mDbHelper.updateDropboxid(noteEdit.mRowId, tempRecord.getId());
						mDbHelper.updateDatastoreid(noteEdit.mRowId, store.getId());
						DbxTable mainTable = mDatastore.getTable("notes");
						mainTable.get(oldDropboxId).deleteRecord();
						mDatastore.sync();
						mDatastoreMap.add(store);
						store.addSyncStatusListener(this);
						Toast.makeText(this, "Successfully shared note", Toast.LENGTH_SHORT).show();
						new ShareDialog(store, mAccount, false).show(getSupportFragmentManager(), "ShareDialog");
						invalidateOptionsMenu();
					} catch (DbxException e) {
						e.printStackTrace();
					}
				} else {
					try {
						Iterator<DbxDatastore> iter = mDatastoreMap.iterator();
						String dataString = mDbHelper.getDatastoreid(noteEdit.mRowId);
						while (iter.hasNext()) {
							DbxDatastore store = iter.next();
							if (store.getId().matches(dataString)) {
								if (store.getEffectiveRole().equals(Role.OWNER)) {
									new ShareDialog(store, mAccount, true).show(getSupportFragmentManager(), "ShareDialog");

								}
							}
						}
						invalidateOptionsMenu();
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			}

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onUnshareSelected(DbxDatastore datastore) {

		Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
		NoteEdit noteEdit = new NoteEdit();
		if (fragment instanceof NoteEdit) {
			noteEdit = (NoteEdit) fragment;
		}

		try {
			Iterator<DbxDatastore> iter = mDatastoreMap.iterator();
			String dataString = mDbHelper.getDatastoreid(noteEdit.mRowId);
			String dropString = mDbHelper.getDropboxid(noteEdit.mRowId);
			while (iter.hasNext()) {
				DbxDatastore store = iter.next();
				if (store.getId().matches(dataString) && store.getEffectiveRole().equals(Role.OWNER)) {
					store.setRole(DbxPrincipal.PUBLIC, Role.NONE);
					store.setRole(DbxPrincipal.TEAM, Role.NONE);
					store.sync();
					Log.e(store.getId(), store.getRole(DbxPrincipal.PUBLIC).toString());
					DbxRecord record = store.getTable("notes").get(dropString);
					DbxRecord newRecord = mDatastore.getTable("notes").insert(record);
					Log.e(newRecord.getString("title"), newRecord.getId());
					mDatastore.sync();
					mDbHelper.updateDropboxid(noteEdit.mRowId, newRecord.getId());
					mDbHelper.updateDatastoreid(noteEdit.mRowId, mDatastore.getId());
					store.close();
					mDatastoreManager.deleteDatastore(store.getId());
					iter.remove();
				}
			}
		} catch (DbxException e) {
			e.printStackTrace();
		}
	}

	public void showOverflowDialog(OverflowDialog dialog, Bundle bundle) {
		dialog.setArguments(bundle);
		if (getFragmentManager().findFragmentByTag("ConfirmDialog") == null)
			dialog.show(getSupportFragmentManager(), "ConfirmDialog");
	}

	public boolean actionDone(boolean fetchNotes, boolean toaster) {
		NoteEdit noteFrag = (NoteEdit) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
		if (noteFrag.mRowId != null && toaster)
			Toast.makeText(this, "Note Saved", Toast.LENGTH_SHORT).show();

		if (noteFrag.saveState() && !noteFrag.mNetworkTask) {
			if (mAccountManager.hasLinkedAccount()) {
				populateDropbox(notes(), false);
			}
			invalidateOptionsMenu();
			if (fetchNotes) {
				fetchAllNotes();
			}
			return true;
		} else if (noteFrag.mNetworkTask) {
			mInvalidLocationDialog = customAlert("Location Running", "Still updating location", "Cancel", "OK");
			mInvalidLocationDialog.show();
			return false;

		} else {
			mInvalidLocationDialog = customAlert("Location Invalid", "The location needs to be selected from the drop down list to be valid. An active internet connection is also required.", "Cancel Note", "Fix Location");
			mInvalidLocationDialog.show();
			return false;
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
			mMenuTitles[SYNC_DROPBOX] = (mAccountManager.hasLinkedAccount()) ? "Unsync Dropbox" : "Sync Dropbox";
			Cursor notesCursor = mDbHelper.fetchAllNotes(this, mLongitude, mLatitude);
			int settingsResult = mDbHelper.fetchSetting();

			if (mAccountManager.hasLinkedAccount() && !mFirstRun) {
				Log.e("mAccountManager.hasLinkedAccount()", String.valueOf(mAccountManager.hasLinkedAccount()));
				try {
					mDatastoreManager = DbxDatastoreManager.forAccount(mAccountManager.getLinkedAccount());
					Set<DbxDatastoreInfo> dataList;
					dataList = mDatastoreManager.listDatastores();

					mDatastoreManager.addListListener(this);
					for (DbxDatastoreInfo info : dataList) {
						if (info.id.matches(DbxDatastoreManager.DEFAULT_DATASTORE_ID)) {
							try {

								mDatastore = mDatastoreManager.openDefaultDatastore();
								mDatastore.addSyncStatusListener(this);
								mDatastore.sync();
								mDatastoreMap.add(mDatastore);
							} catch (DbxException e) {
								Log.e(info.id, "already open dawg");
							}

						} else {
							if (null != info.title) {
								if (info.title.startsWith("shared_note_")) {
									try {
										DbxDatastore shareStore = mDatastoreManager.openDatastore(info.id);
										shareStore.addSyncStatusListener(this);
										shareStore.sync();
										mDatastoreMap.add(shareStore);
									} catch (DbxException e) {
										Log.e(info.id, "already open dawg");
									}
								}
							}
						}
					}
				} catch (DbxException e) {
					e.printStackTrace();
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
			mDatastoreManager.removeListListener(this);
			for (DbxDatastore store : mDatastoreMap) {
				store.removeSyncStatusListener(this);
				store.close();

			}
			mDatastoreManager.removeListListener(this);
			mDatastoreManager.shutDown();
			mDatastoreMap.clear();
		}

	}

}
