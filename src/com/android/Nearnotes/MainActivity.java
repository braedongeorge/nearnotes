package com.android.Nearnotes;





import android.app.ActionBar.LayoutParams;
import android.content.res.Configuration;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;


public class MainActivity extends FragmentActivity implements LocationListener, Notepadv3.OnNoteSelectedListener, NoteEdit.noteEditListener {
	  private NotesDbAdapter mDbHelper;
	  private static final int ACTIVITY_EDIT=1;
	  private int mFragType=0;
	  private DrawerLayout mDrawerLayout;
	  private ListView mDrawerList;
	  private ActionBarDrawerToggle mDrawerToggle;
	  private Menu mMenu;
	  private NoteLocation mLoc;
	  private Location mLocation;
	  private double mLatitude;
	  private double mLongitude;

	  private CharSequence mDrawerTitle;
	  private CharSequence mTitle;
	  private String[] mMenuTitles;

	 @Override
	  public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_main);
	   
	    mDbHelper = new NotesDbAdapter(this);  // Create new custom database class for sqlite and pass the current context as a variable
        mDbHelper.open(); // Gets the writable database
        mLoc = new NoteLocation(this);
        double[] locations = mLoc.getLocation();
        mLatitude = locations[0];
        mLongitude = locations[1];
       
        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        // getActionBar().setIcon(android.R.color.transparent);
    
        
             
        getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_gradient));
        
        
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);

        View customNav = LayoutInflater.from(this).inflate(R.layout.edit_title, null); // layout which contains your button.
        getActionBar().setCustomView(customNav, lp);
        getActionBar().setDisplayShowCustomEnabled(true);
        
	 // START NAV DRAWER
        mTitle = mDrawerTitle = getTitle();
        mMenuTitles = getResources().getStringArray(R.array.menu_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mMenuTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

       

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
                ) {
            @Override
			public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            @Override
			public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            selectItem(0);
        }
	    
	    
	    if (findViewById(R.id.fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }
            
            Cursor notesCursor = mDbHelper.fetchAllNotes(this, mLongitude, mLatitude);	
            notesCursor.moveToFirst();
            // Create a new Fragment to be placed in the activity layout
            NoteEdit firstFragment = new NoteEdit();
            Bundle args = new Bundle();
            args.putLong(NotesDbAdapter.KEY_ROWID, notesCursor.getLong(0));
            firstFragment.setArguments(args);
            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            
            // firstFragment.setArguments(getIntent().getExtras());
            
            // Add the fragment to the 'fragment_container' FrameLayout
             getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, firstFragment).commit();
        }

	 }
		
	 public void setActionItems() {
		 getActionBar().setDisplayShowCustomEnabled(false);
	   //  mDrawerList.setItemChecked(1, true);
		mFragType = 2;
     	invalidateOptionsMenu();
		 
	    }
	 
	 public void setEditItems() {
		 mFragType = 1; 
		 getActionBar().setDisplayShowCustomEnabled(true);
		//  mDrawerList.setItemChecked(0, true);
		 invalidateOptionsMenu();
		 
	 }
	 
	 
	 public void onNoteSelected(long id) {
	        // The user selected the headline of an article from the HeadlinesFragment
	        // Do something here to display that article
		 NoteEdit newFragment = new NoteEdit();
 		
		 mFragType = 0;
 		Bundle args = new Bundle();
 		args.putLong("_id", id);
 		newFragment.setArguments(args);

 		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

 		// Replace whatever is in the fragment_container view with this fragment,
 		// and add the transaction to the back stack so the user can navigate back
 		transaction.replace(R.id.fragment_container, newFragment);
 		transaction.addToBackStack(null);

 		// Commit the transaction
 		transaction.commit();;
		 
		 
	    }
	 
	 
	    /* The click listener for ListView in the navigation drawer */
	    private class DrawerItemClickListener implements ListView.OnItemClickListener {
	        @Override
	        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	        	Log.e("testers","getting to onItemClick");
	            selectItem(position);
	        }
	    }

	    private void selectItem(int position) {
	    	Log.e("drawer",String.valueOf(position));
	    	if (position == 1) {
	    		mFragType = 2;
	    	
	    		Notepadv3 newFragment = new Notepadv3();
	    		
	    		Bundle args = new Bundle();
	    		args.putDouble("latitude", mLatitude);
	    		args.putDouble("longitude", mLongitude);
	    		newFragment.setArguments(args);

	    		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
	    		
	    		transaction.replace(R.id.fragment_container, newFragment);
	    		transaction.addToBackStack(null);
	    		transaction.commit();;
	    	} else if (position == 0) {
	    		mFragType = 1;
			
	    		NoteEdit newFragment1 = new NoteEdit();
	
	    		Cursor notesCursor = mDbHelper.fetchAllNotes(this, mLongitude, mLatitude);	
	            notesCursor.moveToFirst();
	           	              
	            Bundle args = new Bundle();
	            args.putLong(NotesDbAdapter.KEY_ROWID, notesCursor.getLong(0));
	            newFragment1.setArguments(args);    		

	    		FragmentTransaction transaction1 = getSupportFragmentManager().beginTransaction();

	    		transaction1.replace(R.id.fragment_container, newFragment1);
	    		transaction1.addToBackStack(null);
	    		transaction1.commit();;
	    	}
	       
			
	        // update selected item and title, then close the drawer
	        mDrawerList.setItemChecked(position, false);
	        setTitle(mMenuTitles[position]);
	        mDrawerLayout.closeDrawer(mDrawerList);
	    }
	 
	    @Override
	    public boolean onPrepareOptionsMenu(Menu menu) {
	        // If the nav drawer is open, hide action items related to the content view
	        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
	        if (mFragType == 2) {
	        	menu.findItem(R.id.action_new).setVisible(true);
	        	menu.findItem(R.id.action_done).setVisible(false);
	        	menu.findItem(R.id.action_location).setVisible(true);
	        	// menu.findItem(R.id.title_edit).setVisible(false);
	        } else if (mFragType == 1) {
	        	menu.findItem(R.id.action_new).setVisible(false);
	        	menu.findItem(R.id.action_done).setVisible(true);
	        	menu.findItem(R.id.action_location).setVisible(false);
	        	// menu.findItem(R.id.title_edit).setVisible(true);
	        }
	       
	        
	        return super.onPrepareOptionsMenu(menu);
	    }
	    
	    @Override
	    public void setTitle(CharSequence title) {
	        mTitle = title;
	        getActionBar().setTitle(mTitle);
	    }


	    @Override
	    protected void onPostCreate(Bundle savedInstanceState) {
	        super.onPostCreate(savedInstanceState);
	        // Sync the toggle state after onRestoreInstanceState has occurred.
	        mDrawerToggle.syncState();
	    }

	    @Override
	    public void onConfigurationChanged(Configuration newConfig) {
	        super.onConfigurationChanged(newConfig);
	        // Pass any configuration change to the drawer toggls
	        mDrawerToggle.onConfigurationChanged(newConfig);
	    }
	    
	    
	    @Override
	    public boolean onCreateOptionsMenu(Menu menu) {
	    	// Inflate the menu items for use in the action bar
	        MenuInflater inflater = getMenuInflater();
	        inflater.inflate(R.menu.main_activity_actions, menu);
	        
	       
	        
	        return true;	
	    	
	    }
	    
	    @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	             // The action bar home/up action should open or close the drawer.
	             // ActionBarDrawerToggle will take care of this.
	            if (mDrawerToggle.onOptionsItemSelected(item)) {
	                return true;
	            }

	       // Handle presses on the action bar items
	        switch (item.getItemId()) {
	             case R.id.action_new:
	            	 mFragType = 1;
	            	 invalidateOptionsMenu();
	            	
	 	    		NoteEdit newFragment1 = new NoteEdit();
	 	    		
	 	    		Bundle args2 = new Bundle();
	 	    		args2.putDouble("latitude", mLatitude);
	 	    		args2.putDouble("longitude", mLongitude);
	 	    		newFragment1.setArguments(args2);

	 	    		FragmentTransaction transaction1 = getSupportFragmentManager().beginTransaction();

	 	    		transaction1.replace(R.id.fragment_container, newFragment1);
	 	    		transaction1.addToBackStack(null);
	 	    		
	 	    		// Commit the transaction
	 	    		transaction1.commit();;
	                return true;
	             case R.id.action_done:   
	            	 mFragType = 2;
	            	 
	            	 NoteEdit noteFrag = (NoteEdit)
	            		     getSupportFragmentManager().findFragmentById(R.id.fragment_container);
	            	 noteFrag.saveState(); 
	            	 invalidateOptionsMenu();
	            	 
	            	 Notepadv3 newFragment = new Notepadv3();
	 	    		
	 	    		Bundle args1 = new Bundle();
	 	    		args1.putDouble("latitude", mLatitude);
	 	    		args1.putDouble("longitude", mLongitude);
	 	    		newFragment.setArguments(args1);

	 	    		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

	 	    		// Replace whatever is in the fragment_container view with this fragment,
	 	    		// and add the transaction to the back stack so the user can navigate back
	 	    		transaction.hide(getSupportFragmentManager().findFragmentById(R.id.fragment_container));
	 	    		transaction.replace(R.id.fragment_container, newFragment);
	 	    		transaction.addToBackStack(null);
	 	    		

	 	    		// Commit the transaction
	 	    		transaction.commit();;
	               return true;
	                
	             case R.id.action_location:
	            	
	            	 invalidateOptionsMenu();
	            	// Create fragment and give it an argument specifying the article it should show
	 	    		
	            	 //NoteLocation nLoc = new NoteLocation(this);
	         		 double[] locations = mLoc.getLocation();
	         		 mLatitude = locations[0];
	         		 mLongitude = locations[1];
	         		 String accuracy = String.valueOf(Math.round(locations[2]));
	         		 Toast.makeText(this, "Location accurate to " + accuracy + " metres.", Toast.LENGTH_SHORT).show();  
	         		 Notepadv3 articleFrag = (Notepadv3) getSupportFragmentManager().findFragmentById(R.id.fragment_container);

	                 if (articleFrag != null) {
	                	 Log.e("filling","filling data");
	                     articleFrag.fillData(mLongitude, mLatitude);
	                 } 

	            	 
	                return true;   
	            default:
	                return super.onOptionsItemSelected(item);
	        }
	      
	           
	    }
	    
	    
	    @Override
		  public void onLocationChanged(Location location) {
		 
		  }

		  @Override
		  public void onStatusChanged(String provider, int status, Bundle extras) {
		    // TODO Auto-generated method stub

		  }

		  @Override
		  public void onProviderEnabled(String provider) {
		    
			 Toast.makeText(this, "Enabled new provider " + provider,
		        Toast.LENGTH_SHORT).show();

		  }

		  @Override
		  public void onProviderDisabled(String provider) {
		    Toast.makeText(this, "Disabled provider " + provider,
		        Toast.LENGTH_SHORT).show();
		  }
		  
		  
		  @Override
			public void onPause() {
		    	super.onPause();
		    	
		    	mLoc.locationManager.removeUpdates(this);
		    	
		    }
		    
		    @Override
			public void onResume() {
		    	super.onResume();
		    	
		    	
		    }
	    
}
