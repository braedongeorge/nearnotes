package com.nearnotes;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class NoteHelp extends Fragment {
	private static final int NOTE_HELP = 4;
	noteHelpListener mCallback;
	
	public interface noteHelpListener { // Container Activity must implement this interface
		public void setActionItems(int fragType);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			mCallback = (noteHelpListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement noteHelpListener");
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		mCallback.setActionItems(NOTE_HELP);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.note_help, container, false);
	}
}
