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
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class NoteLocation extends DialogFragment implements LocationListener {
	private static final int POWER_MEDIUM = 2;
	private static final int NOTE_EDIT = 1;
	private static final int NOTE_LIST = 2;
	private NoteLocationListener mCallback;
	public LocationManager mLocationManager;
	private String mProvider;
	private Criteria mCriteria;
	private int mTypeFrag;

	public NoteLocation() {
		// Empty Constructor required for DialogFragment
	}

	public interface NoteLocationListener {
		public Location onLocationFound(Location location, int TypeFrag);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the Builder class for convenient dialog construction
		Bundle extras = getArguments();
		mTypeFrag = extras.getInt("TypeFrag");
		

		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		Boolean gpsPref = sharedPref.getBoolean("pref_key_ignore_gps", false);
		Log.e("hello",String.valueOf(gpsPref));
		mLocationManager = (LocationManager) getActivity().getSystemService(
				Context.LOCATION_SERVICE);
		mCriteria = new Criteria();
		if (gpsPref) {
			mCriteria.setPowerRequirement(POWER_MEDIUM);
		}
		mProvider = mLocationManager.getBestProvider(mCriteria, true);
		Log.e("mprovider",mProvider);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		if (mProvider.matches("gps")) {
			if (mTypeFrag == NOTE_EDIT) {
				builder.setTitle("Finding Nearest Note (GPS)...");
			} else if (mTypeFrag == NOTE_LIST) {
				builder.setTitle("Updating Location (GPS)...");
			}

			Log.e("gps", String.valueOf(mProvider));
			builder.setPositiveButton(R.string.dialog_network_location, null);
		} else if (mProvider.matches("network")) {
			if (mTypeFrag == NOTE_EDIT) {
				builder.setTitle("Finding Nearest Note (Network)...");
			} else if (mTypeFrag == NOTE_LIST) {
				builder.setTitle("Updating Location (Network)...");
			}

		}
		builder.setNegativeButton(R.string.dialog_cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						mCallback.onLocationFound(null, mTypeFrag);
						mLocationManager.removeUpdates(NoteLocation.this);
					}
				});
		// Create the AlertDialog object and return it
		builder.setView(getActivity().getLayoutInflater().inflate(
				R.layout.dialogue_location, null));
		// builder.create();

		final AlertDialog realDialog = builder.create();
		// final LocationListener getFragment() = this.;

		realDialog.setOnShowListener(new DialogInterface.OnShowListener() {

			@Override
			public void onShow(DialogInterface dialog) {

				final Button b = realDialog
						.getButton(DialogInterface.BUTTON_POSITIVE);
				b.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View view) {
						mProvider = "network";
						mLocationManager.requestLocationUpdates(mProvider, 200,
								0, NoteLocation.this);
						b.setText("Using Network...");
						realDialog
								.setTitle("Finding Nearest Note (Network)...");
						// Dismiss once everything is OK.
						// d.dismiss();
					}
				});
			}
		});

		return realDialog;

	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			mCallback = (NoteLocationListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement NoteLocationListener");
		}
	}


	@Override
	public void onStart() {
		super.onStart();
		mLocationManager.requestLocationUpdates(mProvider, 200, 0, this);

	}

	@Override
	public void onLocationChanged(Location location) {
		mCallback.onLocationFound(location, mTypeFrag);
		Toast.makeText(
				getActivity(),
				"Location accurate to "
						+ String.valueOf(Math.round(location.getAccuracy()))
						+ "m", Toast.LENGTH_SHORT).show();
		mLocationManager.removeUpdates(this);

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

	@Override
	public void onProviderEnabled(String provider) {
		Toast.makeText(getActivity(), "Enabled new provider " + provider,
				Toast.LENGTH_SHORT).show();
		Criteria criteria = new Criteria();
		provider = mLocationManager.getBestProvider(criteria, true);

		mLocationManager.requestLocationUpdates(provider, 0, 0, this);

	}

	@Override
	public void onProviderDisabled(String provider) {
		Toast.makeText(getActivity(), "Disabled provider " + provider,
				Toast.LENGTH_SHORT).show();
		Criteria criteria = new Criteria();

		provider = mLocationManager.getBestProvider(criteria, true);

		mLocationManager.requestLocationUpdates(provider, 0, 0, this);
	}

	@Override
	public void onPause() {
		super.onPause();

		mLocationManager.removeUpdates(this);
		this.dismiss();
	}

}
