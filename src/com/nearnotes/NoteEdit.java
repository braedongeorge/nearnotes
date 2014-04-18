package com.nearnotes;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ProgressBar;
import com.nearnotes.R;


public class NoteEdit extends Fragment implements OnItemClickListener {
	private static final String LOG_TAG = "ExampleApp";
	private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
	private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
	private static final String TYPE_DETAILS = "/details";
	private static final String OUT_JSON = "/json";
	private static final String API_KEY = "AIzaSyDu8TsJIM1Ui3uNaWxh-OMkOgivYW2nsB4";
	
    private EditText mTitleText;
    private EditText mBodyText;
    private DelayAutoCompleteTextView autoCompView;
    private Long mRowId;
    private PlacesAutoCompleteAdapter acAdapter;
    
	private String location;
	private double latitude = 0;
	private double longitude = 0;
	private int tempPosition = 0;
	
	private double mLongitude;
    private double mLatitude;
    private CheckBox mCheckBox;
    
    private NotesDbAdapter mDbHelper;
    noteEditListener mCallback;
    
    private ArrayList<String> referenceList;
    
    public interface noteEditListener {  // Container Activity must implement this interface
        public void setEditItems();
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
       
        try {
            mCallback = (noteEditListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement noteEditListener");
        }
    } 

    
    @Override
	public void onStart() {
		super.onStart();
		mTitleText = (EditText) getActivity().findViewById(R.id.title_edit);
	
		mCallback.setEditItems();
		if (mRowId == null) {
			Bundle extras = getArguments();
			if (!extras.containsKey(NotesDbAdapter.KEY_ROWID)) {
				//mTitleText.setText("");
			}
			mRowId = extras.containsKey(NotesDbAdapter.KEY_ROWID) ? extras.getLong(NotesDbAdapter.KEY_ROWID)
     								: null;
		}    
		
		getActivity().setTitle(R.string.edit_note);

		 Bundle bundle=getArguments();
	     mLongitude = bundle.getDouble("longitude");
	     mLatitude = bundle.getDouble("latitude");
		
		mTitleText = (EditText) getActivity().findViewById(R.id.title_edit);
		mBodyText = (EditText) getView().findViewById(R.id.body);
	
		acAdapter = new PlacesAutoCompleteAdapter(getActivity(), R.layout.list_item);
		
		mCheckBox = (CheckBox) getActivity().findViewById(R.id.checkbox_on_top);
		mCheckBox.setOnClickListener(new OnClickListener() {

		      @Override
		      public void onClick(View v) {
	        	  if (((CheckBox) v).isChecked() && mRowId != null) {
	        		  Log.e("db",String.valueOf(mRowId));
	        		  mDbHelper.updateSetting(mRowId);
	        		  
	        	  }
		               
		         else if (mRowId != null) {
		                mDbHelper.removeSetting();
		         }
		      }
		    });
		
		// mProgressAPI = (ProgressBar) getView().findViewById(R.id.progressAPI);
	
		autoCompView = (DelayAutoCompleteTextView) getView().findViewById(R.id.autoCompleteTextView1);
		autoCompView.setAdapter(acAdapter);
		autoCompView.setOnItemClickListener(this);
		autoCompView.setLoadingIndicator((ProgressBar) getView().findViewById(R.id.progressAPI),(ImageView) getView().findViewById(R.id.location_icon));
	

 
		populateFields();
	}
    
    
    
    private class PlacesAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {
	    private ArrayList<String> resultList;
	    

	    public PlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
	        super(context, textViewResourceId);
	    }


	    @Override
	    public int getCount() {
	    	return resultList.size();
	    }

	    @Override
	    public String getItem(int index) {
	        return resultList.get(index);
	    }

	    @Override
	    public Filter getFilter() {
	    	
	        Filter filter = new Filter() {
	            @Override
	            protected FilterResults performFiltering(CharSequence constraint) {
	                FilterResults filterResults = new FilterResults();
	              
	            if (constraint != null) {
	            		// Retrieve the autocomplete results.
	            		resultList = autocomplete(constraint.toString());

	                    // Assign the data to the FilterResults
	                	if (resultList != null) {
	                		filterResults.values = resultList;
	                		filterResults.count = resultList.size();
	                	}
	                
	                }
	                return filterResults;
	            }

	            @Override
	            protected void publishResults(CharSequence constraint, FilterResults results) {
	                if (results != null && results.count > 0) {
	                	// mProgressAPI.setVisibility(1);
	                    notifyDataSetChanged();
	                }
	                else {
	                    notifyDataSetInvalidated();
	                }
	            }};
	        return filter;
	    }
	}
	
    private class NetworkTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... params) {
        	String link = params[0];
            HttpGet request = new HttpGet(link);
            AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
            try {
                HttpResponse result = client.execute(request);
                if (result != null) {
                	StringBuilder jsonResults = new StringBuilder();
           
    				try {
    					InputStreamReader in = new InputStreamReader(result.getEntity().getContent());		
    					int read;
    					char[] buff = new char[1024];
    					while ((read = in.read(buff)) != -1) {
    					    jsonResults.append(buff, 0, read);
    					}
    		     	        
    					JSONObject jsonObj = new JSONObject(jsonResults.toString());
    					return jsonObj;
    					
    				} catch (JSONException e) {
    					Log.e(LOG_TAG, "Cannot process JSON results", e);
    					return null;
    				} catch (IOException e) {
    			        Log.e(LOG_TAG, "Error connecting to Places API", e);
    			        return null;
    			    } 
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
            	client.close();
            }
			return null;
           
        }

        @Override
        protected void onPostExecute(JSONObject result) {  
        	try {
			result = result.getJSONObject("result");
			result = result.getJSONObject("geometry");
			result = result.getJSONObject("location");
						
			Log.e(LOG_TAG, result.toString());
			latitude = result.getDouble("lat");
			longitude = result.getDouble("lng");
        	} catch (JSONException e) {
				Log.e(LOG_TAG, "Cannot process JSON results", e);
        	}
        }
    }
    
    
    
	
	private ArrayList<String> autocomplete(String input) {
	    ArrayList<String> resultList = null;

	    HttpURLConnection conn = null;
	    StringBuilder jsonResults = new StringBuilder();
	    try {
	        StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
	        sb.append("?sensor=true&key=" + API_KEY);
	        sb.append("&location=" + String.valueOf(mLongitude) + "," + String.valueOf(mLatitude) + "&rankBy=distance");
	        sb.append("&input=" + URLEncoder.encode(input, "utf8"));

	        URL url = new URL(sb.toString());
	        conn = (HttpURLConnection) url.openConnection();
	        InputStreamReader in = new InputStreamReader(conn.getInputStream());

	        // Load the results into a StringBuilder
	        int read;
	        char[] buff = new char[1024];
	        while ((read = in.read(buff)) != -1) {
	            jsonResults.append(buff, 0, read);
	        }
	    } catch (MalformedURLException e) {
	        Log.e(LOG_TAG, "Error processing Places API URL", e);
	        return resultList;
	    } catch (IOException e) {
	        Log.e(LOG_TAG, "Error connecting to Places API", e);
	        return resultList;
	    } finally {
	        if (conn != null) {
	            conn.disconnect();
	        }
	    }

	    try {
	        // Create a JSON object hierarchy from the results
	        JSONObject jsonObj = new JSONObject(jsonResults.toString());
	        JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

	        // Extract the Place descriptions from the results
	        resultList = new ArrayList<String>(predsJsonArray.length());
	        referenceList = new ArrayList<String>(predsJsonArray.length());
	        for (int i = 0; i < predsJsonArray.length(); i++) {
	            resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
	            referenceList.add(predsJsonArray.getJSONObject(i).getString("reference"));
	        }
	    } catch (JSONException e) {
	        Log.e(LOG_TAG, "Cannot process JSON results", e);
	    }

	    return resultList;
	}
    
    	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
		
		// access options menu from fragment
		setHasOptionsMenu(true); 
		
		mDbHelper = new NotesDbAdapter(getActivity());
		mDbHelper.open();
		
		
		mRowId = (savedInstanceState == null) ? null : 
			(Long) savedInstanceState.getSerializable(NotesDbAdapter.KEY_ROWID);
   
		
		return inflater.inflate(R.layout.note_edit, container, false);
        
	}
       
	
	@Override 
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	    menu.findItem(R.id.action_done).setVisible(true);
	    menu.findItem(R.id.action_new).setVisible(false);
	    super.onCreateOptionsMenu(menu, inflater);
	}  
	
	
    
    
    private void populateFields() {
    	if (mRowId != null) {
    		int settingsResult = mDbHelper.fetchSetting();
    		if (settingsResult == mRowId) {
    			mCheckBox.setChecked(true);
    		} else mCheckBox.setChecked(false);
    		Cursor note = mDbHelper.fetchNote(mRowId);
    		getActivity().startManagingCursor(note);
    		mTitleText.setText(note.getString(
    					note.getColumnIndexOrThrow(NotesDbAdapter.KEY_TITLE)));
    		mBodyText.setText(note.getString(
    					note.getColumnIndexOrThrow(NotesDbAdapter.KEY_BODY)));
    		autoCompView.setAdapter(null);
    		autoCompView.setText(note.getString(note.getColumnIndexOrThrow(NotesDbAdapter.KEY_LOCATION)));
    		autoCompView.setAdapter(new PlacesAutoCompleteAdapter(getActivity(), R.layout.list_item));
    		location = note.getString(note.getColumnIndexOrThrow(NotesDbAdapter.KEY_LOCATION));
    		longitude = note.getDouble(note.getColumnIndexOrThrow(NotesDbAdapter.KEY_LNG));
    		latitude = note.getDouble(note.getColumnIndexOrThrow(NotesDbAdapter.KEY_LAT));
    	} else {
    		
    		autoCompView.requestFocus();
    		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    		imm.showSoftInput(autoCompView,InputMethodManager.SHOW_IMPLICIT);
    	}
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	if (this.getView() == null) {
    		return;
    	} else {
    	saveState();
    	outState.putSerializable(NotesDbAdapter.KEY_ROWID, mRowId);
    	}
    }
    
    @Override
	public void onPause() {
    	super.onPause();
    	if (this.isHidden()) {
    		return;
    	} else {
    		//locationManager.removeUpdates(this);
    		saveState();
    	}
    }
    
    @Override
	public void onResume() {
    	super.onResume();
    	mCallback.setEditItems();
		if (mRowId == null) {
			Bundle extras = getArguments();
			if (!extras.containsKey(NotesDbAdapter.KEY_ROWID)) {
				mTitleText.setText("");
			}
			mRowId = extras.containsKey(NotesDbAdapter.KEY_ROWID) ? extras.getLong(NotesDbAdapter.KEY_ROWID)
     								: null;
		}    
    	populateFields();
    }
    
    public void saveState() {
    		String title = mTitleText.getText().toString();
    		String body = mBodyText.getText().toString();
    		if (title.isEmpty()) {
    			title = body.substring(0, Math.min(body.length(), 7));
    		}
    		    	
    		if (mRowId == null) {
    			long id = mDbHelper.createNote(title, body, latitude, longitude, location);
    			if (id > 0) {
    				mRowId = id;
    				if (mCheckBox.isChecked()) {
    					 Log.e("db",String.valueOf(mRowId));
    					 mDbHelper.updateSetting(mRowId);
    				}          
   		         	
    				}
    				
    			 
    		} else {
    			mDbHelper.updateNote(mRowId, title, body, latitude, longitude, location);
    		}
    	
    	
    }
    
    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();
        
        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.checkbox_on_top:
                if (checked)
                    mDbHelper.updateSetting(mRowId);
                else
                    mDbHelper.removeSetting();
                break;
        }
    }

	  @Override
	  public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		  mBodyText.requestFocus();
		  autoCompView.setSelection(0);
		  tempPosition = position;
		  Log.e(LOG_TAG, String.valueOf(tempPosition));
	       location = (String) adapterView.getItemAtPosition(position);
	       Log.e(LOG_TAG, location);
	       Geocoder find = new Geocoder(getActivity());
	       try {
	    	   List<Address> AddressList = find.getFromLocationName(location, 10);
	    	   if (AddressList.size() > 0) {
	    		   longitude = AddressList.get(0).getLongitude();
		    	   	latitude = AddressList.get(0).getLatitude();
		    	   	Log.e(LOG_TAG, String.valueOf(longitude));
	    		   	//if 1st provider does not have data, loop through other providers to find it.
	    		   	int count = 0;
	    		   	while (longitude == 0 && count < AddressList.size()) {
	    		   		longitude = AddressList.get(count).getLongitude();
	    		   		latitude = AddressList.get(count).getLatitude();
	    		   		count++;
	    		   	}
	    		} else {
	   			        StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_DETAILS + OUT_JSON);
	   			        sb.append("?sensor=true&key=" + API_KEY);
	   			        sb.append("&reference=" + String.valueOf(referenceList.get(tempPosition)));
	   			        Log.e(LOG_TAG, sb.toString());
	   			        
	   			        new NetworkTask().execute(sb.toString());
	    			}
	      		} catch (IOException e) {
	      			// TODO Auto-generated catch block
	      			Log.e(LOG_TAG, "Couldnt received coordinates");
	      			e.printStackTrace();
	      		}     
	    	}
		}
