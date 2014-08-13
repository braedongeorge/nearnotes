/*
 * Copyright (C) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.nearnotes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

/**
 * Simple notes database access helper class. Defines the basic CRUD operations
 * for the notepad example, and gives the ability to list all notes as well as
 * retrieve or modify a specific note.
 * 
 * This has been improved from the first version of this tutorial through the
 * addition of better error handling and also using returning a Cursor instead
 * of using a collection of inner classes (which is less scalable and not
 * recommended).
 */
public class NotesDbAdapter {

	public static final String KEY_TITLE = "title";
	public static final String KEY_BODY = "body";
	public static final String KEY_ROWID = "_id";
	public static final String KEY_LAT = "latitude";
	public static final String KEY_LNG = "longitude";
	public static final String KEY_LOCATION = "location";
	public static final String KEY_CHECK = "checklist";
	public static final String KEY_SETTINGS_ONTOP = "ontop";

	private static final String TAG = "NotesDbAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	/**
	 * Database creation sql statement
	 */
	private static final String DATABASE_CREATE = "create table notes (_id integer primary key autoincrement, "
			+ "title text not null, body text not null, latitude real not null, longitude real not null, location text not null, checklist text not null);";

	private static final String DATABASE_CREATE_SETTINGS = "create table settings (_id integer primary key autoincrement, "
			+ "ontop integer);";

	private static final String DATABASE_NAME = "data";
	private static final String DATABASE_TABLE = "notes";
	private static final String DATABASE_TABLE_SETTINGS = "settings";
	private static final int DATABASE_VERSION = 4;

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.e("getting here","public void onCreate(SQLiteDatabase db)");
			db.execSQL(DATABASE_CREATE);
			db.execSQL(DATABASE_CREATE_SETTINGS);
			db.execSQL("insert into settings values (NULL, NULL);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			if (oldVersion == 3) {
				db.execSQL("ALTER TABLE notes ADD COLUMN checklist text DEFAULT 'false'");
			}
			//db.execSQL("ALTER TABLE notes ADD COLUMN checklist integer DEFAULT 0");
			//db.execSQL("DROP TABLE IF EXISTS settings");
			//onCreate(db);
		}
	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public NotesDbAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	/**
	 * Open the notes database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public NotesDbAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}

	/**
	 * Create a new note using the title and body provided. If the note is
	 * successfully created return the new rowId for that note, otherwise return
	 * a -1 to indicate failure.
	 * 
	 * @param title
	 *            the title of the note
	 * @param body
	 *            the body of the note
	 * @return rowId or -1 if failed
	 */
	public long createNote(String title, String body, double lat, double lng,
			String location, String checklist) {
		Log.e("checklist","anything? + " + checklist);
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_TITLE, title);
		initialValues.put(KEY_BODY, body);
		initialValues.put(KEY_LAT, lat);
		initialValues.put(KEY_LNG, lng);
		initialValues.put(KEY_LOCATION, location);
		initialValues.put(KEY_CHECK, checklist);
		long idsql = -1;
		try {
			idsql = mDb.insertOrThrow(DATABASE_TABLE, null, initialValues);
		} catch (SQLiteException exception) {
			String locationNull = "notes.location may not be NULL";

			if (exception.toString().contains(locationNull)) {
				Toast.makeText(mCtx, "Location cannot be empty",
						Toast.LENGTH_LONG).show();
			}

		}
		return idsql;
	}

	/**
	 * Delete the note with the given rowId
	 * 
	 * @param rowId
	 *            id of note to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteNote(long rowId) {

		return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Return a Cursor over the list of all notes in the database
	 * 
	 * @return Cursor over all notes
	 */
	public Cursor fetchAllNotes(Context context, double longitude,
			double latitude) {
		// NoteLocation nLoc = new NoteLocation(context);
		// Location aLoc = nLoc.getLocation();
		String mlatitude = String.valueOf(latitude);
		String mlongitude = String.valueOf(longitude);
		Log.e("current", "Current Position" + mlatitude + mlongitude);

		String fudge = String.valueOf(Math.pow(
				Math.cos(Math.toRadians(latitude)), 2));
		return mDb.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_TITLE,
				KEY_BODY, KEY_LAT, KEY_LNG, KEY_LOCATION }, null, null, null,
				null, "(" + mlatitude + " - latitude) * (" + mlatitude
						+ " - latitude) + (" + mlongitude + " - longitude) * ("
						+ mlongitude + " - longitude) * " + fudge);

	}

	/**
	 * Return a Cursor positioned at the note that matches the given rowId
	 * 
	 * @param rowId
	 *            id of note to retrieve
	 * @return Cursor positioned to matching note, if found
	 * @throws SQLException
	 *             if note could not be found/retrieved
	 */
	public Cursor fetchNote(long rowId) throws SQLException {

		Cursor mCursor =

		mDb.query(true, DATABASE_TABLE, new String[] { KEY_ROWID, KEY_TITLE,
				KEY_BODY, KEY_LAT, KEY_LNG, KEY_LOCATION, KEY_CHECK }, KEY_ROWID + "="
				+ rowId, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

	}
	

	/**
	 * Update the note using the details provided. The note to be updated is
	 * specified using the rowId, and it is altered to use the title and body
	 * values passed in
	 * 
	 * @param rowId
	 *            id of note to update
	 * @param title
	 *            value to set note title to
	 * @param body
	 *            value to set note body to
	 * @return true if the note was successfully updated, false otherwise
	 */
	public boolean updateNote(long rowId, String title, String body,
			double lat, double lng, String location, String checklist) {
		Log.e("checklist",checklist);
		ContentValues args = new ContentValues();
		args.put(KEY_TITLE, title);
		args.put(KEY_BODY, body);
		args.put(KEY_LAT, lat);
		args.put(KEY_LNG, lng);
		args.put(KEY_LOCATION, location);
		args.put(KEY_CHECK, checklist);

		return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean updateSetting(long rowId) {
		Cursor mCursor = mDb.query(true, DATABASE_TABLE_SETTINGS, new String[] {
				KEY_ROWID, KEY_SETTINGS_ONTOP }, null, null, null, null, null,
				null);
		if (mCursor != null) {
			mCursor.moveToFirst();
			int settingRowId = mCursor.getInt(0);
			ContentValues args = new ContentValues();
			args.put(KEY_SETTINGS_ONTOP, rowId);
			return mDb.update(DATABASE_TABLE_SETTINGS, args, KEY_ROWID + "="
					+ settingRowId, null) > 0;
		} else
			return false;

	}

	public void removeSetting() {
		Cursor mCursor = mDb.query(true, DATABASE_TABLE_SETTINGS, new String[] {
				KEY_ROWID, KEY_SETTINGS_ONTOP }, null, null, null, null, null,
				null);
		if (mCursor != null) {
			mCursor.moveToFirst();
			int settingRowId = mCursor.getInt(0);
			ContentValues args = new ContentValues();
			args.put(KEY_SETTINGS_ONTOP, "null");
			mDb.update(DATABASE_TABLE_SETTINGS, args, KEY_ROWID
					+ "=" + settingRowId, null);
		}

	}

	public int fetchSetting() throws SQLException {
		int rowResult = -1;
		Log.e("fetching", "fetching");
		Cursor mCursor =

		mDb.query(true, DATABASE_TABLE_SETTINGS, new String[] { KEY_ROWID,
				KEY_SETTINGS_ONTOP }, null, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
			rowResult = mCursor.getInt(1);
			Log.e("db", String.valueOf(rowResult));
		}
		return rowResult;

	}
}
