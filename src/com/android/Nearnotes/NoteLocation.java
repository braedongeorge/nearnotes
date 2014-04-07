package com.android.Nearnotes;

import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class NoteLocation extends DialogFragment implements LocationListener {
		// LocationListener mListener;
		NoteLocationListener mCallback;
		public LocationManager locationManager;
		private String provider;
		private boolean mGpsFix;
		private Location mfinalLocation;
		private boolean mSkip = false;
		private int mTypeFrag;
		
		public NoteLocation() {
			// Empty Constructor required for DialogFragment
		}
		
		public interface NoteLocationListener {
	        public Location onLocationFound(Location location, int TypeFrag);
	    }

	    @Override
	    public void onAttach(Activity activity) {
	        super.onAttach(activity);
	        
	        // This makes sure that the container activity has implemented
	        // the callback interface. If not, it throws an exception
	        try {
	            mCallback = (NoteLocationListener) activity;
	        } catch (ClassCastException e) {
	            throw new ClassCastException(activity.toString()
	                    + " must implement NoteLocationListener");
	        }
	    }
		
		
		
		
		
		
		
		
		
	  @Override
	   public View onCreateView(LayoutInflater inflater, ViewGroup container,
		            Bundle savedInstanceState) {
		        View view = inflater.inflate(R.layout.dialogue_location, container);
		        getDialog().setTitle("Finding Nearest Note...");

		        return view;
		    }
		
		
	  /*
	  public GpsStatus.Listener mGPSStatusListener = new GpsStatus.Listener()
		{    
		    public void onGpsStatusChanged(int event) 
		    {       
		        switch(event) 
		        {
		            case GpsStatus.GPS_EVENT_STARTED:
		                Toast.makeText(getActivity(), "Waiting on GPS Lock", Toast.LENGTH_SHORT).show();
		             
		                System.out.println("TAG - GPS searching: ");                        
		                 mGpsFix = false;
		                break;
		            case GpsStatus.GPS_EVENT_STOPPED:    
		                System.out.println("TAG - GPS Sftopped");
		                mGpsFix = false;
		                break;
		            case GpsStatus.GPS_EVENT_FIRST_FIX:
		            	mGpsFix = true;
		            	
		            	//mCallback.onLocationFound(location);
		            	
		            		
		              
		            		
		                    Toast.makeText(getActivity(), "GPS_LOCKED", Toast.LENGTH_SHORT).show();
		                    Location gpslocation = locationManager
		                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);
		                    if (isBetterLocation(gpslocation, mfinalLocation)) {
		    		    		mfinalLocation = gpslocation;	
		    		    	} 
		                    

		                    if(gpslocation != null)
		                    {       
		                    System.out.println("GPS Info:"+gpslocation.getLatitude()+":"+gpslocation.getLongitude());

		                    //locationManager.removeUpdates(mListener);
		                   //  locationManager.removeGpsStatusListener(mGPSStatusListener);   
		                   
		                    }               

		                break;
		            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
		 //                 System.out.println("TAG - GPS_EVENT_SATELLITE_STATUS");
		                break;                  
		       }
		   }
		};  
	*/
		
		@Override
		public void onStart() {
			super.onStart();
			
			Bundle extras = getArguments();
			mTypeFrag = extras.getInt("TypeFrag");
			
			locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
		    Criteria criteria = new Criteria();
		  
		    provider = locationManager.getBestProvider(criteria, true);
		   
		    
		    locationManager.requestLocationUpdates(provider, 200, 0, this);
			
			
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
		mCallback.onLocationFound(location, mTypeFrag);
		 locationManager.removeUpdates(this);
	  
	  }

	  
	  @Override
	  
	  public void onStatusChanged(String provider, int status, Bundle extras) {
	    
	  }

	  
	  @Override
	  public void onProviderEnabled(String provider) {
	    Toast.makeText(getActivity(), "Enabled new provider " + provider,
	        Toast.LENGTH_SHORT).show();
	    
	    Criteria criteria = new Criteria();
		  
	    provider = locationManager.getBestProvider(criteria, true);
	   
	    
	    locationManager.requestLocationUpdates(provider, 0, 0, this);
	    
	  }

	  
	  @Override
	  public void onProviderDisabled(String provider) {
	    Toast.makeText(getActivity(), "Disabled provider " + provider,
	        Toast.LENGTH_SHORT).show();
	    Criteria criteria = new Criteria();
		  
	    provider = locationManager.getBestProvider(criteria, true);
	   
	    
	    locationManager.requestLocationUpdates(provider, 0, 0, this);
	  }
	  
	  @Override
	  public void onPause() {
		  super.onPause();
	    	
		  locationManager.removeUpdates(this);
	  }
	  
	  @Override
		public void onResume() {
		  super.onResume();
			
		  
	  }
}
