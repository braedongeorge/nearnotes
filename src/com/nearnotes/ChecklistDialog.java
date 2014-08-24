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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;

public class ChecklistDialog extends DialogFragment {
	CheckDialogListener mListener;
	 int mWhich = 2;

	public interface CheckDialogListener {
		public void onOptionSelected(int which, long mRowId);
		
	}

	/**
	 * onAttach is called to check that the MainActivity is implementing the callback
	 * 
	 * @param activity
	 *            the activity that should have the callback implemented
	 * @throws ClassCastException
	 *             if the activity does not implement CheckDialogListener
	 */
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

	/**
	 * Creates a dialog to be returned depending if there is a preference to
	 * automatically delete or clear the note it will return a confirmation dialog.
	 * If no preference is selected it will come up with option to delete, clear
	 * or do nothing to the note.
	 * 
	 * @param savedInstanceState
	 *            this bundle has the unique identifier for the note in the database
	 *            and the preferences to use a default action and which action
	 *            the user is currently using.
	 * @return Dialog depending on user settings
	 * 
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		Bundle extras = getArguments();
		final Long mRowId = extras.getLong(NotesDbAdapter.KEY_ROWID);
		final boolean mUseDefault = extras.getBoolean("useDefault");
		final int listPref = extras.getInt("listPref");
		
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		
		if (mUseDefault) {
			
			// Set the dialog title
			switch (listPref) {
			case 0: builder.setTitle(R.string.checkfinished_clearnote);
					builder.setMessage(R.string.checkfinished_message);
					mWhich = 0;
					break;
			case 1: builder.setTitle(R.string.checkfinished_deletenote);
					builder.setMessage(R.string.checkfinished_message);
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
			}).setNeutralButton(R.string.settings, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					mListener.onOptionSelected(3,mRowId);
				}
			});
				
		} else {
		
		// Set the dialog title
		builder.setTitle(R.string.checkfinished)
		// Specify the list array, the items to be selected by default (null for none),
		// and the listener through which to receive callbacks when items are selected
				.setSingleChoiceItems(R.array.check_array, 2, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {

						mWhich = which;

						// The 'which' argument contains the index position
						// of the selected item
					}
				})

				// Set the action buttons
				.setPositiveButton(R.string.oncecheck, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						mListener.onOptionSelected(mWhich,mRowId);
						// User clicked OK, so save the mSelectedItems results somewhere
						// or return them to the component that opened the dialog

					}
				}).setNegativeButton(R.string.alwayscheck, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
						SharedPreferences.Editor editor1 = settings.edit();
						editor1.putBoolean("pref_key_use_checklist_default",true);
						editor1.putString("pref_key_checklist_listPref", String.valueOf(mWhich));
						editor1.commit();
						
						
						mListener.onOptionSelected(mWhich,mRowId);	
					}
				});
		
		}

		return builder.create();
	}

}
