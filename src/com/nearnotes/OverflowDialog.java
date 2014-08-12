package com.nearnotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class OverflowDialog extends DialogFragment {
	OverflowDialogListener mListener;
	 int mWhich = 0;

	public interface OverflowDialogListener {
		public void onConfirmSelected(int which, long mRowId);
		
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Verify that the host activity implements the callback interface
		try {
			// Instantiate the NoticeDialogListener so we can send events to the host
			mListener = (OverflowDialogListener) activity;
		} catch (ClassCastException e) {
			// The activity doesn't implement the interface, throw exception
			throw new ClassCastException(activity.toString() + " must implement OverflowDialogListener");
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		Bundle extras = getArguments();
		final Long mRowId = extras.getLong(NotesDbAdapter.KEY_ROWID);
		final int mSelection = extras.getInt("confirmSelection");	
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			
			// Set the dialog title
			switch (mSelection) {
			case 0: builder.setTitle(R.string.check_clear);
					mWhich = 0;
					break;
			case 1: builder.setTitle(R.string.check_delete);
					mWhich = 1;
					break;
			}
			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					mListener.onConfirmSelected(mWhich,mRowId);
					// User clicked OK, so save the mSelectedItems results somewhere
					// or return them to the component that opened the dialog

				}
			}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {

				}
			});
			
		return builder.create();
	}

}
