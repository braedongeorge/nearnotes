package com.nearnotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.dropbox.sync.android.DbxRecord;

public class DatastoreDialog extends DialogFragment {
	 private DatastoreDialogListener mListener;
	 private String mDatastoreId;
	 private DbxRecord mRecord;
	
	
	public interface DatastoreDialogListener {
		public void onDatastoreSelected(DbxRecord record);
	}
	
	public DatastoreDialog(DbxRecord record) {
		mRecord = record;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (DatastoreDialogListener) activity; 	// Instantiate the NoticeDialogListener so we can send events to the host
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement DatastoreDialogListener");  // The activity doesn't implement the interface, throw exception
		}
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		Bundle extras = getArguments();
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		builder.setTitle(R.string.datastore_deleted_title);
		builder.setMessage("The note has been unshared by its owner\n" + "Title: " + mRecord.getString("title") + "\nLocation: " + mRecord.getString("location"));
		
		builder.setNegativeButton(R.string.datastore_deleted_delete, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				mListener.onDatastoreSelected(null);
			}
		});
		
		builder.setPositiveButton(R.string.datastore_deleted_keep, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				mListener.onDatastoreSelected(mRecord);
	

			}
		});
		return builder.create();
	}
}
