package com.nearnotes;

import java.util.ArrayList;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
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

public class NoteList extends ListFragment {
	private NotesDbAdapter mDbHelper;
	OnNoteSelectedListener mCallback;
	private double mLongitude;
	private double mLatitude;
	private SelectionAdapter mAdapter;
	private boolean mActionModeFlag = false;
	private ArrayList<Long> mSelectedIds;

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

		mCallback.setActionItems();
		mSelectedIds = new ArrayList<Long>();
		mDbHelper = new NotesDbAdapter(getActivity()); // Create new custom database class for sqlite and pass the current context as a variable
		mDbHelper.open(); // Gets the writable database

		Bundle bundle = getArguments();
		mLongitude = bundle.getDouble("longitude");
		mLatitude = bundle.getDouble("latitude");
		fillData(mLongitude, mLatitude);

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
					for (Long s : mSelectedIds) {
						mDbHelper.deleteNote(s);
						if (mDbHelper.fetchSetting() == s) {
							mDbHelper.removeSetting();
						}
					}
					nr = 0;
					mAdapter.clearSelection();
					fillData(mLongitude, mLatitude);
					mode.finish();
				}
				return true;
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
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


	public interface OnNoteSelectedListener { // Container Activity must implement this interface
		public void onNoteSelected(long id);

		public void setActionItems();
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