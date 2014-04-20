package com.nearnotes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
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
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class NoteList extends ListFragment {
	private static final int DELETE_ID = Menu.FIRST + 1;
	private NotesDbAdapter mDbHelper;
	OnNoteSelectedListener mCallback;
	private double mLongitude;
	private double mLatitude;
	private SelectionAdapter mAdapter;
	private boolean mActionModeFlag = false;
	private ArrayList<Long> mSelectedIds;

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
		
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		getListView().setMultiChoiceModeListener(new MultiChoiceModeListener() {

			private int nr = 0;

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				// TODO Auto-generated method stub
				mActionModeFlag = false;
				Log.e("onDestroyActionMode","onDestroyActionMode");
				mAdapter.clearSelection();
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				// TODO Auto-generated method stub
				mActionModeFlag = true;
				
				nr = 0;
				MenuInflater inflater = getActivity().getMenuInflater();
				inflater.inflate(R.menu.contextual_menu, menu);
				return true;
			}

			 @Override
	            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
	                // TODO Auto-generated method stub
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
            public void onItemCheckedStateChanged(ActionMode mode, int position,
                    long id, boolean checked) {
                // TODO Auto-generated method stub
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
				// TODO Auto-generated method stub

				getListView().setItemChecked(position, !mAdapter.isPositionChecked(position));
				return false;
			}
		});

		
		
		
	}

	private class SelectionAdapter extends SimpleCursorAdapter {

		

		public SelectionAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
			super(context, layout, c, from, to);
			// TODO Auto-generated constructor stub
		}

		private HashMap<Integer, Boolean> mSelection = new HashMap<Integer, Boolean>();
		

		public void setNewSelection(int position, boolean value) {
			mSelection.put(position, value);
			notifyDataSetChanged();
		}

		public boolean isPositionChecked(int position) {
			Boolean result = mSelection.get(position);
			return result == null ? false : result;
		}

		public Set<Integer> getCurrentCheckedPosition() {
			return mSelection.keySet();
		}

		public void removeSelection(int position) {
			mSelection.remove(position);
			notifyDataSetChanged();
		}

		public void clearSelection() {
			mSelection = new HashMap<Integer, Boolean>();
			notifyDataSetChanged();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = super.getView(position, convertView, parent);//let the adapter handle setting up the row views
			v.setBackgroundColor(getResources().getColor(android.R.color.transparent)); //default color

			if (mSelection.get(position) != null) {
				v.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));// this is a selected position so make it red
			}
			return v;
		}
	}

	public interface OnNoteSelectedListener { // Container Activity must implement this interface
		public void onNoteSelected(long id);

		public void setActionItems();
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

	/*
	 * @Override public void onCreateContextMenu(ContextMenu menu, View v,
	 * ContextMenuInfo menuInfo) { super.onCreateContextMenu(menu, v, menuInfo);
	 * menu.add(0, DELETE_ID, 0, R.string.menu_delete); }
	 * 
	 * 
	 * @Override public boolean onContextItemSelected(MenuItem item) {
	 * switch(item.getItemId()) { case DELETE_ID: AdapterContextMenuInfo info =
	 * (AdapterContextMenuInfo) item.getMenuInfo();
	 * 
	 * mDbHelper.deleteNote(info.id); if (mDbHelper.fetchSetting() == info.id) {
	 * mDbHelper.removeSetting(); } fillData(mLongitude,mLatitude); return true;
	 * } return super.onContextItemSelected(item); }
	 */
}
