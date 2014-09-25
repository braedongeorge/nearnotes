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

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.dropbox.sync.android.DbxException;

public class NoteList extends ListFragment {
	private NotesDbAdapter mDbHelper;
	OnNoteSelectedListener mCallback;
	private static final int NOTE_LIST = 2;
	private double mLongitude;
	private double mLatitude;
	private ListView mListView;
	private SelectionAdapter mAdapter;
	private boolean mActionModeFlag = false;
	private ArrayList<Long> mSelectedIds;
	private NotesDropbox mNotesDropbox;

	public interface OnNoteSelectedListener { // Container Activity must implement this interface
		public void onNoteSelected(long id);

		public void setActionItems(int fragType);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// This makes sure that the container activity has implemented the callback interface. If not, it throws an exception
		try {
			mCallback = (OnNoteSelectedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnHeadlineSelectedListener");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.notes_list, container, false);
	}

	@Override
	public void onStart() {
		super.onStart();

		mCallback.setActionItems(NOTE_LIST);
		mSelectedIds = new ArrayList<Long>();
		mDbHelper = new NotesDbAdapter(getActivity()); // Create new custom database class for sqlite and pass the current context as a variable
		mDbHelper.open(); // Gets the writable database

		// mNotesDropbox = new NotesDropbox(getActivity(),getActivity().getApplicationContext());
		
		Bundle bundle = getArguments();
		mLongitude = bundle.getDouble("longitude");
		mLatitude = bundle.getDouble("latitude");
		fillData(mLongitude, mLatitude);
		mListView = getListView();
		
				
		getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);

		getListView().setMultiChoiceModeListener(new MultiChoiceModeListener() {

			private int nr = 0;

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				mActionModeFlag = false;
				Log.e("onDestroyActionMode", "onDestroyActionMode");
				mAdapter.clearSelection();
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				mActionModeFlag = true;

				nr = 0;
				MenuInflater inflater = getActivity().getMenuInflater();
				inflater.inflate(R.menu.contextual_menu, menu);
				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch (item.getItemId()) {

				case R.id.context_delete:

						try {
							MainActivity myActivity = (MainActivity) getActivity();
							myActivity.deleteDropboxNote(mSelectedIds);
						} catch (DbxException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					nr = 0;
					mAdapter.clearSelection();
					fillData(mLongitude, mLatitude);
					mode.finish();

					return true;
				case R.id.context_select_all:
					SparseBooleanArray checkSparse = getListView().getCheckedItemPositions();
					for (int i = 0; i < mListView.getCount(); i++) {
						if (!checkSparse.get(i)) {
							getListView().setItemChecked(i, true);

						}
					}
					return true;
				}
				return false;
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
				Log.e("checked", String.valueOf(checked) + " id: " + String.valueOf(id) + " position: " + String.valueOf(position));
				if (checked) {
					nr++;
					mSelectedIds.add(id);
					mAdapter.setNewSelection(position, checked);
				} else {
					mSelectedIds.remove(id);
					nr--;
					mAdapter.removeSelection(position);
				}

				TextView tv = (TextView) getActivity().getLayoutInflater().inflate(R.layout.contextual_title, null);
				tv.setText(nr + " selected");
				mode.setCustomView(tv);

			}
		});

		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

				getListView().setItemChecked(position, !mAdapter.isPositionChecked(position));
				return false;
			}
		});

	}

	@SuppressWarnings("deprecation")
	public void fillData(double longitude, double latitude) {

		Cursor notesCursor = mDbHelper.fetchAllNotes(getActivity(), longitude, latitude); // Get all of the rows from the database and create the item list
		getActivity().startManagingCursor(notesCursor);

		String[] from = new String[] { NotesDbAdapter.KEY_TITLE, NotesDbAdapter.KEY_LOCATION }; // Create an array to specify the fields we want to display in the list (TITLE and LOCATION)
		int[] to = new int[] { R.id.text1, R.id.text2 }; // and an array of the fields we want to bind those fields to (in this case just text1)

		mAdapter = new SelectionAdapter(getActivity(), R.layout.notes_row, notesCursor, from, to);
		setListAdapter(mAdapter); // Now create a simple cursor adapter and set it to display
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		if (mActionModeFlag) {
			getListView().setItemChecked(position, !mAdapter.isPositionChecked(position));

		} else {
			mCallback.onNoteSelected(id);
		}
	}

}
