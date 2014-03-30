package com.android.Nearnotes;

import android.content.Context;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
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
		private boolean mGpsFix;
		private Location mfinalLocation;
		

		
		public NoteLocation(Context mContext) {
			this.mContext = mContext;
			this.mListener = this;
		
		}
		
		
		
		public GpsStatus.Listener mGPSStatusListener = new GpsStatus.Listener()
		{    
		    public void onGpsStatusChanged(int event) 
		    {       
		        switch(event) 
		        {
		            case GpsStatus.GPS_EVENT_STARTED:
		                Toast.makeText(mContext, "Waiting on GPS Lock", Toast.LENGTH_SHORT).show();
		                
		                System.out.println("TAG - GPS searching: ");                        
		                 mGpsFix = false;
		                break;
		            case GpsStatus.GPS_EVENT_STOPPED:    
		                System.out.println("TAG - GPS Sftopped");
		                mGpsFix = false;
		                break;
		            case GpsStatus.GPS_EVENT_FIRST_FIX:
		            	mGpsFix = true;
		            	
		            		
		                /*
		                 * GPS_EVENT_FIRST_FIX Event is called when GPS is locked            
		                 */
		            		
		                    Toast.makeText(mContext, "GPS_LOCKED", Toast.LENGTH_SHORT).show();
		                    Location gpslocation = locationManager
		                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);
		                    if (isBetterLocation(gpslocation, mfinalLocation)) {
		    		    		mfinalLocation = gpslocation;	
		    		    	} 
		                    

		                    if(gpslocation != null)
		                    {       
		                    System.out.println("GPS Info:"+gpslocation.getLatitude()+":"+gpslocation.getLongitude());

		                    /*
		                     * Removing the GPS status listener once GPS is locked  
		                     */
		                    //    locationManager.removeGpsStatusListener(mGPSStatusListener);                
		                    }               

		                break;
		            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
		 //                 System.out.println("TAG - GPS_EVENT_SATELLITE_STATUS");
		                break;                  
		       }
		   }
		};  
		

		public double[] getLocation() {
			Toast.makeText(mContext, "mgpsfix is " + String.valueOf(mGpsFix), Toast.LENGTH_SHORT).show();
			locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
		    Criteria criteria = new Criteria();
		  
		    provider = locationManager.getBestProvider(criteria, true);
		    locationManager.addGpsStatusListener(mGPSStatusListener);
		    
		    if (provider.matches("gps") && !mGpsFix) {
		    	Toast.makeText(mContext, "no gps fix and gps location", Toast.LENGTH_SHORT).show();
		    	locationManager.requestLocationUpdates(provider, 0, 0, mListener);
		    	provider = "network"; 
		    	
		    } else if (provider.matches("gps") && mGpsFix) {
		    	Toast.makeText(mContext, "gps fix and gps location", Toast.LENGTH_SHORT).show();
		    	provider = "gps"; 
		    	
		    	
		    	
		    }
		  
	                   
		    locationManager.requestLocationUpdates(provider, 0, 0, mListener);
		    Location location = locationManager.getLastKnownLocation(provider);


		    if (location != null) {
		    	if (!isBetterLocation(location, mfinalLocation)) {
		    		Toast.makeText(mContext, "not a better location", Toast.LENGTH_SHORT).show();
		    		location = mfinalLocation;	
		    	} 
		    } else {
		    	while (location == null) {
		    	System.out.println("No last known location.");
		    	locationManager.requestLocationUpdates(provider, 1000, 0, mListener);
		    	location = locationManager.getLastKnownLocation(provider);
		    	
		    	// locationManager.addGpsStatusListener(listener)
		    	
		    	}
		    	if (!isBetterLocation(location, mfinalLocation)) {
		    		location = mfinalLocation;	
		    	} 
		    }
		    
		    mfinalLocation = location;
		    Toast.makeText(mContext, "provider" + mfinalLocation.getProvider(), Toast.LENGTH_SHORT).show();
		    return new double[] {mfinalLocation.getLatitude(), mfinalLocation.getLongitude(), mfinalLocation.getAccuracy()};
		}
		
	  
	  
		
		
		
		
		private static final int TWO_MINUTES = 1000 * 60 * 2;

		/** Determines whether one Location reading is better than the current Location fix
		  * @param location  The new Location that you want to evaluate
		  * @param currentBestLocation  The current Location fix, to which you want to compare the new one
		  */
		protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		    if (currentBestLocation == null) {
		        // A new location is always better than no location
		        return true;
		    }

		    // Check whether the new location fix is newer or older
		    long timeDelta = location.getTime() - currentBestLocation.getTime();
		    boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		    boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		    boolean isNewer = timeDelta > 0;

		    // If it's been more than two minutes since the current location, use the new location
		    // because the user has likely moved
		    if (isSignificantlyNewer) {
		        return true;
		    // If the new location is more than two minutes older, it must be worse
		    } else if (isSignificantlyOlder) {
		        return false;
		    }

		    // Check whether the new location fix is more or less accurate
		    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		    boolean isLessAccurate = accuracyDelta > 0;
		    boolean isMoreAccurate = accuracyDelta < 0;
		    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		    // Check if the old and new location are from the same provider
		    boolean isFromSameProvider = isSameProvider(location.getProvider(),
		            currentBestLocation.getProvider());

		    // Determine location quality using a combination of timeliness and accuracy
		    if (isMoreAccurate) {
		        return true;
		    } else if (isNewer && !isLessAccurate) {
		        return true;
		    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
		        return true;
		    }
		    return false;
		}

		/** Checks whether two providers are the same */
		private boolean isSameProvider(String provider1, String provider2) {
		    if (provider1 == null) {
		      return provider2 == null;
		    }
		    return provider1.equals(provider2);
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
