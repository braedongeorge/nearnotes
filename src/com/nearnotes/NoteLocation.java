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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.DialogFragment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

@SuppressLint("InflateParams")
public class NoteLocation extends DialogFragment implements LocationListener {
	private static final int NOTE_EDIT = 1;
	private static final int NOTE_LIST = 2;
	private static final int LOCATION_MODE_OFF = 0;
	private static final int LOCATION_MODE_SENSORS_ONLY = 1;
	private static final int LOCATION_MODE_BATTERY_SAVING = 2;
	private static final int LOCATION_MODE_HIGH_ACCURACY = 3;
	private NoteLocationListener mCallback;
	public LocationManager mLocationManager;
	private String mProvider;
	private Criteria mCriteria;
	private int mTypeFrag;
	private int mServiceState = 4;
	private AlertDialog mRealDialog;
	private boolean mAbortRequest = false;

	public NoteLocation() {
		// Empty Constructor required for DialogFragment
	}

	public interface NoteLocationListener {
		public void onLocationFound(Location location, int TypeFrag);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the Builder class for convenient dialog construction
		Bundle extras = getArguments();
		mTypeFrag = extras.getInt("TypeFrag");

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		Boolean gpsPref = sharedPref.getBoolean("pref_key_ignore_gps", false);
		mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
		mCriteria = new Criteria();
		if (gpsPref) {
			mCriteria.setPowerRequirement(Criteria.POWER_HIGH);
		} else mCriteria.setPowerRequirement(Criteria.POWER_MEDIUM);
		mProvider = mLocationManager.getBestProvider(mCriteria, true);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		boolean oldApi = false;
		int locationMode = 4;
		Log.e("mProvider",mProvider);
		
		ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	    boolean networkAvailable = true;
	    
	    
	    
	    TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
	    boolean networkType = true;
	    if (tm.getNetworkType() == TelephonyManager.NETWORK_TYPE_UNKNOWN && !networkInfo.isConnected()) {
	    	networkType = false;
	    }
 	   
	   
 	   
 	    
 	    
	    Log.d("Phone state before if statement", "Phone State: " + mServiceState);
	    Log.e("network isavailable",String.valueOf(networkInfo.isAvailable()));
	    if (!networkType || !mLocationManager.isProviderEnabled("network")) {
	    	networkAvailable = false;
	    }
		
		try {
			Log.e("Location_mode", String.valueOf(Settings.Secure.getInt(getActivity().getContentResolver(), Settings.Secure.LOCATION_MODE)));
			locationMode = Settings.Secure.getInt(getActivity().getContentResolver(), Settings.Secure.LOCATION_MODE);
		} catch (SettingNotFoundException e) {
			oldApi = true;
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if ((oldApi && mProvider.matches("passive")) || locationMode == LOCATION_MODE_OFF || (!networkAvailable && (mProvider.matches("network") || (!gpsPref && mProvider.matches("gps"))))) {
			builder.setTitle(getString(R.string.dialog_location_no_location_services_title));
			builder.setMessage(getString(R.string.dialog_location_no_location_services_message));
			builder.setNeutralButton(R.string.dialog_location_button_settings, noNetworkButton);
			mAbortRequest = true;
		} else if ((oldApi && mProvider.matches("gps") && gpsPref) || (mProvider.matches("gps") && gpsPref && (locationMode == LOCATION_MODE_SENSORS_ONLY || locationMode == LOCATION_MODE_HIGH_ACCURACY))) {
			if (mTypeFrag == NOTE_EDIT) {
				builder.setTitle(getString(R.string.dialog_location_finding_note_gps));
			} else if (mTypeFrag == NOTE_LIST) {
				builder.setTitle(getString(R.string.dialog_location_updating_note_gps));
			}
			if (locationMode == LOCATION_MODE_SENSORS_ONLY || (oldApi && mProvider.matches("gps")) || !networkAvailable) {
				builder.setMessage(getString(R.string.dialog_location_only_gps_message));
				builder.setNeutralButton(R.string.dialog_location_button_settings, noNetworkButton);
			
			} else builder.setPositiveButton(R.string.dialog_location_use_network, null);
			
			builder.setView(getActivity().getLayoutInflater().inflate(R.layout.dialogue_location, null));
			
		} else if ((oldApi && mProvider.matches("network")) || (mProvider.matches("network") && (locationMode == LOCATION_MODE_BATTERY_SAVING || locationMode == LOCATION_MODE_HIGH_ACCURACY))) {
			builder.setView(getActivity().getLayoutInflater().inflate(R.layout.dialogue_location, null));
			if (mTypeFrag == NOTE_EDIT) {
				builder.setTitle(getString(R.string.dialog_location_finding_note_network));
			} else if (mTypeFrag == NOTE_LIST) {
				builder.setTitle(getString(R.string.dialog_location_updating_note_network));
			}

		} 
		builder.setNegativeButton(R.string.cancel, cancelListener);
		// Create the AlertDialog object and return it
		
		// builder.create();

		
		builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
			
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					mCallback.onLocationFound(null, mTypeFrag);
					mLocationManager.removeUpdates(NoteLocation.this);
					Toast.makeText(getActivity(), "Location request cancelled", Toast.LENGTH_SHORT).show();
					dialog.cancel();
					return true;
		        }
			
				return false;
			}
		});
		
		mRealDialog = builder.create();
		// final LocationListener getFragment() = this.;

		mRealDialog.setOnShowListener(usingNetwork);
		mRealDialog.setCanceledOnTouchOutside(false);
		// mRealDialog.setCancelable(false);
		return mRealDialog;

	}

	private DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int id) {
			mCallback.onLocationFound(null, mTypeFrag);
			mLocationManager.removeUpdates(NoteLocation.this);
		}
	};



	private DialogInterface.OnClickListener noNetworkButton = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivity(callGPSSettingIntent);
			
		}

	};

	
	private DialogInterface.OnShowListener usingNetwork =  new DialogInterface.OnShowListener() {

		@Override
		public void onShow(DialogInterface dialog) {

			final Button b = mRealDialog.getButton(DialogInterface.BUTTON_POSITIVE);
			b.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View view) {
					mCriteria.setPowerRequirement(1);
					mProvider = mLocationManager.getBestProvider(mCriteria, true);

					if (mProvider.matches("network")) {

						mLocationManager.requestLocationUpdates(mProvider, 200, 0, NoteLocation.this);
						b.setText("Using Network...");
						mRealDialog.setTitle("Finding Nearest Note (Network)...");
						// Dismiss once everything is OK.
						// d.dismiss();
					}
				}
			});
		}
	};
	
	
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			mCallback = (NoteLocationListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement NoteLocationListener");
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!mAbortRequest) {
		mLocationManager.requestLocationUpdates(mProvider, 200, 0, this);
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		mCallback.onLocationFound(location, mTypeFrag);
		Toast.makeText(getActivity(), "Location accurate to " + String.valueOf(Math.round(location.getAccuracy())) + "m", Toast.LENGTH_SHORT).show();
		mLocationManager.removeUpdates(this);

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

	@Override
	public void onProviderEnabled(String provider) {
		Toast.makeText(getActivity(), "Enabled new provider " + provider, Toast.LENGTH_SHORT).show();
		Criteria criteria = new Criteria();
		provider = mLocationManager.getBestProvider(criteria, true);

		mLocationManager.requestLocationUpdates(provider, 0, 0, this);

	}

	@Override
	public void onProviderDisabled(String provider) {
		Toast.makeText(getActivity(), "Disabled provider " + provider, Toast.LENGTH_SHORT).show();
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
