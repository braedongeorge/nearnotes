package com.nearnotes;

import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxDatastoreManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFields;
import com.dropbox.sync.android.DbxTable;

public class NotesDropbox {
	private static final String APP_KEY = "ulivcu8snndodzq";
	private static final String APP_SECRET = "beja1ot9y4uhj50";
	private static final String TAG = "NotesDropbox";

	private static final int REQUEST_LINK_TO_DBX = 0;

	private DbxAccountManager mDbxAcctMgr;
	private DbxDatastore mDatastore;
	private DbxTable mTable;

	private DbxDatastoreManager mLocalManager;
	private DbxDatastoreManager mDatastoreManager;
	private Context mContext;

	public NotesDropbox(Context activityContext, Context applicationContext) {
		mContext = activityContext;
		mDbxAcctMgr = DbxAccountManager.getInstance(applicationContext, APP_KEY, APP_SECRET);
	}

	private DbxDatastore.SyncStatusListener mDatastoreListener = new DbxDatastore.SyncStatusListener() {
		@Override
		public void onDatastoreStatusChange(DbxDatastore ds) {
			Log.d(TAG, "SYNC STATUS: " + ds.getSyncStatus().toString());
			if (ds.getSyncStatus().hasIncoming) {
				try {
					mDatastore.sync();
				} catch (DbxException e) {
					//  handleException(e);
				}
			}
			// updateList();
		}
	};

	public void dropboxLink() {
		mLocalManager = mDatastoreManager;
		// mDatastore.close();
		mDatastore = null;
		mDatastoreManager = null;
		MainActivity myActivity = (MainActivity) mContext;
		mDbxAcctMgr.startLink(myActivity, REQUEST_LINK_TO_DBX);
	}

	public void populateDropbox(Cursor cursor) throws DbxException {
		mDatastoreManager = DbxDatastoreManager.forAccount(mDbxAcctMgr.getLinkedAccount());
		mDatastore = mDatastoreManager.openDefaultDatastore();
		mTable = mDatastore.getTable("notes");

		if (cursor.moveToFirst()) {

			do {

				DbxFields noteFields = new DbxFields().set("_id", cursor.getInt(0)).set("title", cursor.getString(1)).set("body", cursor.getString(2)).set("latitude", cursor.getLong(3)).set("longitude", cursor.getLong(4))
						.set("location", cursor.getString(5));
				mTable.insert(noteFields);


			} while (cursor.moveToNext());
		}
		mDatastore.sync();

	}

}
