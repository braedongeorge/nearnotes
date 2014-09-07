package com.nearnotes;

import java.util.Iterator;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFields;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;

public class NotesDropbox {
	private static final String APP_KEY = "ulivcu8snndodzq";
	private static final String APP_SECRET = "beja1ot9y4uhj50";
	private static final String TAG = "NotesDropbox";

	private static final int REQUEST_LINK_TO_DBX = 0;

	private DbxAccountManager mDbxAcctMgr;
	private DbxDatastore mDatastore;
	private DbxTable mTable;
	private DbxAccount mAccount;

	private Context mContext;
	private Cursor mCursor;
	public NotesDbAdapter mDbHelper;

	public NotesDropbox(Context activityContext, Context applicationContext) {
		mContext = activityContext;
		mDbxAcctMgr = DbxAccountManager.getInstance(applicationContext, APP_KEY, APP_SECRET);

		if (mDbxAcctMgr.hasLinkedAccount()) 
			mAccount = mDbxAcctMgr.getLinkedAccount();

		mDbHelper = new NotesDbAdapter(activityContext); // Create new custom database class for sqlite and pass the current context as a variable

	}

	private DbxAccount.Listener mAccountListener = new DbxAccount.Listener() {

		@Override
		public void onAccountChange(DbxAccount acct) {
			String accountToast = (acct.isLinked()) ? "Using dropbox account: " : "Unlinked dropbox account: ";
			Toast.makeText(mContext, accountToast + acct.getAccountInfo().userName, Toast.LENGTH_SHORT).show();
		}

	};

	private DbxDatastore.SyncStatusListener mDatastoreListener = new DbxDatastore.SyncStatusListener() {
		@Override
		public void onDatastoreStatusChange(DbxDatastore ds) {
			Log.d(TAG, "SYNC STATUS: " + ds.getSyncStatus().toString());
			if (ds.getSyncStatus().hasIncoming) {
				try {
					mDatastore.sync();
				} catch (DbxException e) {
					e.printStackTrace();
				}
			} else if (!ds.getSyncStatus().isDownloading) {
				Log.d(TAG, "SYNC STATUS - NOT DOWNLOADING: " + ds.getSyncStatus().toString());
				try {
					populateDropbox(mCursor, false);
				} catch (DbxException e) {
					e.printStackTrace();
				}
			}
		}
	};

	public void dropboxLink() {
		MainActivity myActivity = (MainActivity) mContext;
		if (!mDbxAcctMgr.hasLinkedAccount()) {
			mDbxAcctMgr.startLink(myActivity, REQUEST_LINK_TO_DBX);
		} else
			Toast.makeText(mContext, "Account already linked", Toast.LENGTH_SHORT).show();

	}

	public boolean isLinked() {
		return mDbxAcctMgr.hasLinkedAccount();
	}

	public void deleteDropboxNote(long rowId) throws DbxException {
		mDbHelper.open();
		if (!mDbxAcctMgr.hasLinkedAccount()) {
			mDbHelper.deleteNote(rowId);
			if (mDbHelper.fetchSetting() == rowId) {
				mDbHelper.removeSetting();
			}
			return;
		}

		mDatastore = DbxDatastore.openDefault(mAccount);
		mTable = mDatastore.getTable("notes");
		Cursor tempNote = mDbHelper.fetchNote(rowId);
		tempNote.moveToFirst();
		DbxFields queryParams = new DbxFields().set("ID", rowId);
		try {
			DbxTable.QueryResult results = mTable.query(queryParams);
			Iterator<DbxRecord> r = results.iterator();
			while (r.hasNext()) {
				DbxRecord tempQuery = r.next();
				if (tempQuery.getId().matches(tempNote.getString(7))) {
					tempQuery.deleteRecord();
					mDatastore.sync();
				}
			}
		} catch (DbxException e) {
			e.printStackTrace();
		}

		mDbHelper.deleteNote(rowId);
		if (mDbHelper.fetchSetting() == rowId) 
			mDbHelper.removeSetting();
		

		mDatastore.close();
		mDbHelper.close();
	}

	public void unLink() {
		if (mDbxAcctMgr.hasLinkedAccount()) {
			mDbxAcctMgr.getLinkedAccount().unlink();
			mDatastore = null;
		} else
			Toast.makeText(mContext, "No dropbox to unlink", Toast.LENGTH_SHORT).show();
	}

	public void populateDropbox(Cursor cursor, boolean firstRun) throws DbxException {
		mCursor = cursor;
		if (!mDbxAcctMgr.hasLinkedAccount()) {
			return;
		} else if (firstRun) {
			mAccount = mDbxAcctMgr.getLinkedAccount();
			mAccount.addListener(mAccountListener);

			mDatastore = DbxDatastore.openDefault(mAccount);
			mDatastore.addSyncStatusListener(mDatastoreListener);
			return;
		} else {
			mAccount = mDbxAcctMgr.getLinkedAccount();
			mAccount.addListener(mAccountListener);
			if (null != mDatastore) {
				if (!mDatastore.isOpen()) 
					mDatastore = DbxDatastore.openDefault(mAccount);
			} else 
				mDatastore = DbxDatastore.openDefault(mAccount);
		}
		
		mDbHelper.open();
		mDatastore.removeSyncStatusListener(mDatastoreListener);

		mTable = mDatastore.getTable("notes");
		mDatastore.sync();
		
		DbxTable.QueryResult allResults = mTable.query();
		Iterator<DbxRecord> allResultIter = allResults.iterator();
		while (allResultIter.hasNext()) {
			DbxRecord tempAllQuery = allResultIter.next();
			if (!mDbHelper.hasDropboxid(tempAllQuery.getId())) {
				long tempRowID = mDbHelper.createNote(tempAllQuery.getString("title"),
														tempAllQuery.getString("body"),
														tempAllQuery.getDouble("latitude"),
														tempAllQuery.getDouble("longitude"),
														tempAllQuery.getString("location"),
														tempAllQuery.getString("checklist"),
														tempAllQuery.getId());
				tempAllQuery.set("ID", tempRowID);
			}
		}
		if (cursor.moveToFirst()) {
			do {
				DbxFields noteFields = new DbxFields()
						.set("ID", cursor.getLong(0))
						.set("title", cursor.getString(1))
						.set("body", cursor.getString(2))
						.set("latitude", cursor.getDouble(3))
						.set("longitude", cursor.getDouble(4))
						.set("location", cursor.getString(5))
						.set("checklist", cursor.getString(6));

				DbxFields queryParams = new DbxFields().set("ID", cursor.getLong(0));
				DbxTable.QueryResult results = mTable.query(queryParams);
				if (results.count() > 0) {
					DbxRecord firstResult = results.iterator().next();
					if (mDbHelper.getDropboxid(cursor.getLong(0)).matches(firstResult.getId())) 
						firstResult.setAll(noteFields);
				} else {
					DbxRecord tempRecord = mTable.insert(noteFields);
					mDbHelper.updateDropboxid(cursor.getLong(0), tempRecord.getId());
				}
			} while (cursor.moveToNext());
		}

		mDatastore.sync();
		mDbHelper.close();
		mDatastore.close();

		MainActivity myActivity = (MainActivity) mContext;
		myActivity.fetchAllNotes();

	}

}
