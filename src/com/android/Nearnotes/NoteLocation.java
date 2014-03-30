package com.android.Nearnotes;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class NoteLocation implements LocationListener {
		Context mContext;
		LocationListener mListener;
		public LocationManager locationManager;
		private String provider;
		

		
		public NoteLocation(Context mContext) {
			this.mContext = mContext;
			this.mListener = this;
		}
		

		public double[] getLocation() {
			locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
		    // Setup criteria to be used (Using default)
		    Criteria criteria = new Criteria();
		    provider = locationManager.getBestProvider(criteria, true);
		    /*
		    if (provider == "gps") {
		    	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES_GPS, MIN_DISTANCE_CHANGE_FOR_UPDATES_GPS,
		                gpslocationListener);     
		    }
	        */            
		    locationManager.requestLocationUpdates(provider, 1000, 0, mListener);
		    Log.e("test", provider);
		    Location location = locationManager.getLastKnownLocation(provider);
		    Log.e("test", "getting here");	
		    // Initialise the location fields
		    if (location != null) {
		    	System.out.println("Provider " + provider + " has been selected.");
		    	onLocationChanged(location);
		    	return new double[] {location.getLatitude(), location.getLongitude(), location.getAccuracy()};
		    } else {
		    	while (location == null) {
		    	System.out.println("No last known location.");
		    	locationManager.requestLocationUpdates(provider, 1000, 0, mListener);
		    	location = locationManager.getLastKnownLocation(provider);
		    	
		    	}
		    }
		    return new double[] {location.getLatitude(), location.getLongitude(), location.getAccuracy()};
		}
		
		
	  @Override
	  public void onLocationChanged(Location location) {
		  Log.e("test", String.valueOf(location.getAccuracy()));
	    // double lat = (double) (location.getLatitude());
	   // double lng = (double) (location.getLongitude());
	  }

	  
	  @Override
	  public void onStatusChanged(String provider, int status, Bundle extras) {
	    
	  }

	  
	  @Override
	  public void onProviderEnabled(String provider) {
	    Toast.makeText(mContext, "Enabled new provider " + provider,
	        Toast.LENGTH_SHORT).show();
	  }

	  
	  @Override
	  public void onProviderDisabled(String provider) {
	    Toast.makeText(mContext, "Disabled provider " + provider,
	        Toast.LENGTH_SHORT).show();
	  }
}
