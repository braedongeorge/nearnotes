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
		public void onOptionSelected(int which, long mRowId);
		
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Verify that the host activity implements the callback interface
		try {
			mListener = (OverflowDialogListener) activity; 	// Instantiate the NoticeDialogListener so we can send events to the host
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OverflowDialogListener");  // The activity doesn't implement the interface, throw exception
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
					mListener.onOptionSelected(mWhich,mRowId);
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
