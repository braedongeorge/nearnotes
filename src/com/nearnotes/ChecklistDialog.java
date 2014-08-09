package com.nearnotes;

import java.util.ArrayList;

import com.nearnotes.NoteLocation.NoteLocationListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class ChecklistDialog extends DialogFragment {
	CheckDialogListener mListener;

	public interface CheckDialogListener {
		public void onOptionSelected(int which, long mRowId);

		
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Verify that the host activity implements the callback interface
		try {
			// Instantiate the NoticeDialogListener so we can send events to the host
			mListener = (CheckDialogListener) activity;
		} catch (ClassCastException e) {
			// The activity doesn't implement the interface, throw exception
			throw new ClassCastException(activity.toString() + " must implement CheckDialogListener");
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		Bundle extras = getArguments();
		final Long mRowId = extras.getLong(NotesDbAdapter.KEY_ROWID);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		// Set the dialog title
		builder.setTitle(R.string.checkfinished)
		// Specify the list array, the items to be selected by default (null for none),
		// and the listener through which to receive callbacks when items are selected
				.setItems(R.array.check_array, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {

						mListener.onOptionSelected(which,mRowId);

						// The 'which' argument contains the index position
						// of the selected item
					}
				})

				// Set the action buttons
				.setPositiveButton(R.string.oncecheck, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						// User clicked OK, so save the mSelectedItems results somewhere
						// or return them to the component that opened the dialog

					}
				}).setNegativeButton(R.string.alwayscheck, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {

					}
				});

		return builder.create();
	}

}
