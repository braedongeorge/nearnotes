package com.nearnotes;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.nearnotes.R;

public class NoteList extends ListFragment {
    private static final int DELETE_ID = Menu.FIRST + 1;
    private NotesDbAdapter mDbHelper;
    OnNoteSelectedListener mCallback;
    private double mLongitude;
    private double mLatitude;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
    	return inflater.inflate(R.layout.notes_list, container, false);
    }
    
    
    @Override
    public void onStart() {
        super.onStart();
        mCallback.setActionItems();
        mDbHelper = new NotesDbAdapter(getActivity());  // Create new custom database class for sqlite and pass the current context as a variable
        mDbHelper.open(); // Gets the writable database
        Bundle bundle=getArguments();
        mLongitude = bundle.getDouble("longitude");
        mLatitude = bundle.getDouble("latitude");
        fillData(mLongitude, mLatitude);  // Fills the listview using a cursor with the names location of the notes
        registerForContextMenu(getListView());
        View v = (View) getActivity().findViewById(R.layout.notes_row);
    }
    
    
    
    public interface OnNoteSelectedListener {  // Container Activity must implement this interface
        public void onNoteSelected(long id);
        public void setActionItems();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnNoteSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }
    
    
    public void fillData(double longitude, double latitude) {
        Cursor notesCursor = mDbHelper.fetchAllNotes(getActivity(), longitude, latitude); // Get all of the rows from the database and create the item list
        getActivity().startManagingCursor(notesCursor);

        String[] from = new String[]{NotesDbAdapter.KEY_TITLE, NotesDbAdapter.KEY_LOCATION}; // Create an array to specify the fields we want to display in the list (TITLE and LOCATION)
        int[] to = new int[]{R.id.text1, R.id.text2}; // and an array of the fields we want to bind those fields to (in this case just text1)
        
        SimpleCursorAdapter notes = new SimpleCursorAdapter(getActivity(), R.layout.notes_row, notesCursor, from, to);  
        setListAdapter(notes); // Now create a simple cursor adapter and set it to display
    }

    
    @Override
	public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        mCallback.onNoteSelected(id);
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.menu_delete);
    }

    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case DELETE_ID:
                AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
                
                mDbHelper.deleteNote(info.id);
                if (mDbHelper.fetchSetting() == info.id) {
                	mDbHelper.removeSetting();
                }
                fillData(mLongitude,mLatitude);
                return true;
        }
        return super.onContextItemSelected(item);
    }



}
