package com.plumcreektechnology.geoapp;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationStatusCodes;
import com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;
import com.google.android.gms.location.LocationRequest;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class MainActivity extends FragmentActivity implements 
		ConnectionCallbacks,
		OnConnectionFailedListener,
		OnAddGeofencesResultListener {

	private LocationClient locClient;
	private LocationRequest locRequest;
    public enum REQUEST_TYPE {ADD};
    private REQUEST_TYPE request;
    private boolean inProgress;
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	
    List<Geofence> geoList;
    private SimpleGeofenceStore geoStore;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		if (servicesConnected()) {
			// Create new location client
			locClient = new LocationClient(this, this, this);
		} else return;
		inProgress = false;
		
		geoStore = new SimpleGeofenceStore(this);
		geoList = new ArrayList<Geofence>();
		createGeofences();
	}

	public void onStart() {
		super.onStart();
		addGeofences();
	}

	public void onStop(){
		super.onStop();
		locClient.disconnect();
	}
	
	public void addGeofences() {
		request = REQUEST_TYPE.ADD;
		if(!inProgress) {
			inProgress=true;
			locClient.connect();
		}
	}

	public void createGeofences() {
		
		// instantiate simpleGeofences
		SimpleGeofence geo1 = new SimpleGeofence("Giraffe", 41.294886, -82.216749,
				100, 180000, Geofence.GEOFENCE_TRANSITION_ENTER);
		SimpleGeofence geo2 = new SimpleGeofence("Neighbor", 41.294607, -82.216889,
				100, 180000, Geofence.GEOFENCE_TRANSITION_ENTER);
		SimpleGeofence geo3 = new SimpleGeofence("Church", 41.294393, -82.215591,
				100, 180000, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT);
		SimpleGeofence geo4 = new SimpleGeofence("Regina", 41.294714, -82.212654,
				100, 180000, Geofence.GEOFENCE_TRANSITION_ENTER);
		
		// store them all to shared preferences
		geoStore.setGeofence("Giraffe", geo1);
		geoStore.setGeofence("Neighbor", geo2);
		geoStore.setGeofence("Church", geo3);
		geoStore.setGeofence("Regina", geo4);
		
		//add them all to list
		geoList.add(geo1.toGeofence());
		geoList.add(geo2.toGeofence());
		geoList.add(geo3.toGeofence());
		geoList.add(geo4.toGeofence());
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * checks whether the device is connected to Google Play Services and
	 * displays an error message if not
	 * @return
	 */
	private boolean servicesConnected() {
		// check for Google Play
		int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		//it's available
		if(ConnectionResult.SUCCESS == result) {
			Log.d("Location Updates", "Google Play services is available.");
			return true;
		} else {
			//not available
			showErrorDialog(result);
			return false;
		}
	}
	
	/**
	 * Displays an error dialog
	 * @param errorCode
	 */
	private void showErrorDialog(int errorCode) {
		Dialog dialog = GooglePlayServicesUtil.getErrorDialog(errorCode, this, 0);
		if(dialog != null){
			ErrorDialogFragment errorFrag = new ErrorDialogFragment();
			errorFrag.setDialog(dialog);
			errorFrag.show(getFragmentManager(), "Location Updates");
		}
	}
	
	/**
	  * creates dialog fragment to display our error message.
	  * unlike other widgets, you can't instantiate DialogFragments on
	  * their own.
	  * @author norahayes
	  *
	  */
	@SuppressLint("NewApi")
	public static class ErrorDialogFragment extends DialogFragment{

		//global field containing actual error dialog
		private Dialog die;
		
		/**
		 * default constructor; sets dialog to null
		 */
		public ErrorDialogFragment(){
			super();
			die = null;
		}

		/**
		 * sets dialog
		 * @param dialog
		 */
		public void setDialog(Dialog dialog){
			die = dialog;
		}
		
		/**
		 * Return dialog to DialogFragment (tho not sure exactly why it works...)
		 * @param savedInstanceState
		 * @return dialog
		 */
		public Dialog onCreateDialog(Bundle savedInstanceState){
			return die;
		}
	}

	/**
	 * a single Geofence object
	 */
	public class SimpleGeofence {
		private final String id;
		private final double latitude;
		private final double longitude;
		private final float radius;
		private long expiration;
		private int transition;
		
		public SimpleGeofence(
				String id,
				double latitude,
				double longitude,
				float radius,
				long expiration,
				int transition) {
			this.id=id;
			this.latitude=latitude;
			this.longitude=longitude;
			this.radius=radius;
			this.expiration=expiration;
			this.transition=transition;
		}

		public String getId() {
			return id;
		}

		public double getLatitude() {
			return latitude;
		}

		public double getLongitude() {
			return longitude;
		}

		public float getRadius() {
			return radius;
		}

		public long getExpiration() {
			return expiration;
		}

		public int getTransition() {
			return transition;
		}
		
		public Geofence toGeofence() {
			//build a new Geofence object
			return new Geofence.Builder()
				.setRequestId(id)
				.setTransitionTypes(transition)
				.setCircularRegion(latitude, longitude, radius)
				.setExpirationDuration(expiration)
				.build();
		}
	}

	public class SimpleGeofenceStore {
		// Keys for flattened geofences stored in SharedPreferences
        public static final String KEY_LATITUDE = "com.example.android.geofence.KEY_LATITUDE";
        public static final String KEY_LONGITUDE = "com.example.android.geofence.KEY_LONGITUDE";
        public static final String KEY_RADIUS = "com.example.android.geofence.KEY_RADIUS";
        public static final String KEY_EXPIRATION_DURATION = "com.example.android.geofence.KEY_EXPIRATION_DURATION";
        public static final String KEY_TRANSITION_TYPE = "com.example.android.geofence.KEY_TRANSITION_TYPE";
        // The prefix for flattened geofence keys
        public static final String KEY_PREFIX = "com.example.android.geofence.KEY";
        
        /*
         * Invalid values, used to test geofence storage when
         * retrieving geofences
         */
        public static final long INVALID_LONG_VALUE = -999l;
        public static final float INVALID_FLOAT_VALUE = -999.0f;
        public static final int INVALID_INT_VALUE = -999;
        
        private final SharedPreferences prefs;
        private static final String SHARED_PREFERENCES = "SharedPreferences";
        
        // create the SharedPreferences storage with private access only
        public SimpleGeofenceStore(Context context) {
        	prefs = context.getSharedPreferences( SHARED_PREFERENCES, Context.MODE_PRIVATE);
        }
        
        public SimpleGeofence getGeofence(String id) {
            // lookup key-value pairs in SharedPreferences and save each geofence parameter
        	double lat = prefs.getFloat(getGeofenceFieldKey(id, KEY_LATITUDE),INVALID_FLOAT_VALUE);
            double lng = prefs.getFloat(getGeofenceFieldKey(id, KEY_LONGITUDE),INVALID_FLOAT_VALUE);
            float radius = prefs.getFloat(getGeofenceFieldKey(id, KEY_RADIUS),INVALID_FLOAT_VALUE);
            long expirationDuration = prefs.getLong(getGeofenceFieldKey(id, KEY_EXPIRATION_DURATION),INVALID_LONG_VALUE);
            int transitionType = prefs.getInt(getGeofenceFieldKey(id, KEY_TRANSITION_TYPE),INVALID_INT_VALUE);
			
            // If none of the values is incorrect, return the object
            if (lat != INVALID_FLOAT_VALUE &&
                lng != INVALID_FLOAT_VALUE &&
                radius != INVALID_FLOAT_VALUE &&
                expirationDuration != INVALID_LONG_VALUE &&
                transitionType != INVALID_INT_VALUE) {
                // Return a true Geofence object
                return new SimpleGeofence(id, lat, lng, radius, expirationDuration, transitionType);
            // Otherwise, return null.
            } else {
                return null;
            }
        }

		private String getGeofenceFieldKey(String id, String field) {
			 return KEY_PREFIX + "_" + id + "_" + field;
		}
		
		public void setGeofence(String id, SimpleGeofence geofence) {
			Editor editor = prefs.edit();
			editor.putFloat(getGeofenceFieldKey(id, KEY_LATITUDE),(float) geofence.getLatitude());
            editor.putFloat(getGeofenceFieldKey(id, KEY_LONGITUDE),(float) geofence.getLongitude());
            editor.putFloat(getGeofenceFieldKey(id, KEY_RADIUS),geofence.getRadius());
            editor.putLong(getGeofenceFieldKey(id, KEY_EXPIRATION_DURATION),geofence.getExpiration());
            editor.putInt(getGeofenceFieldKey(id, KEY_TRANSITION_TYPE),geofence.getTransition());
            editor.commit();
		}
		
		public void clearGeofence(String id) {
			Editor editor = prefs.edit();
			editor.remove(getGeofenceFieldKey(id, KEY_LATITUDE));
            editor.remove(getGeofenceFieldKey(id, KEY_LONGITUDE));
            editor.remove(getGeofenceFieldKey(id, KEY_RADIUS));
            editor.remove(getGeofenceFieldKey(id, KEY_EXPIRATION_DURATION));
            editor.remove(getGeofenceFieldKey(id, KEY_TRANSITION_TYPE));
            editor.commit();
		}
	}

	/**
	 * an intent service for responding to geofence transitions
	 * @author devinfrenze
	 *
	 */
	public class ReceiveIS extends IntentService {

		public ReceiveIS() {
			super("ReceiveIS");
		}

		@Override
		protected void onHandleIntent(Intent intent) {
			if(LocationClient.hasError(intent)) {
				int errorCode = LocationClient.getErrorCode(intent);
				Log.e("ReceiveIS", "Location Services error: "+ Integer.toString(errorCode));
			} else { // a valid transition was reported
				int transitionType = LocationClient.getGeofenceTransition(intent);
				// if it was an entrance
				if( transitionType==Geofence.GEOFENCE_TRANSITION_ENTER ) {
					List<Geofence> enterList = LocationClient.getTriggeringGeofences(intent);
					String enterIds = "";
					for(int i=0; i<enterList.size(); i++) {
						enterIds = enterIds + " " + enterList.get(i).getRequestId();
					}
					TextView enter = (TextView) findViewById(R.id.fenceenter);
					enter.setText(enterIds);
				} else if (transitionType==Geofence.GEOFENCE_TRANSITION_EXIT) {
					List<Geofence> exitList = LocationClient.getTriggeringGeofences(intent);
					String exitIds = "";
					for(int i=0; i<exitList.size(); i++) {
						exitIds = exitIds + " " + exitList.get(i).getRequestId();
					}
					TextView exit = (TextView) findViewById(R.id.fenceexit);
					exit.setText(exitIds);
				} else { // an invalid transition was reported
					Log.e("ReceiveIS", "Geofence transition error: " + Integer.toString(transitionType));
				}
			}
			
		}
		
	}

	private PendingIntent getTransitionPI() {
		Intent intent = new Intent(this, ReceiveIS.class);
		return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	@Override
	public void onAddGeofencesResult(int status, String[] geoIds) {
		//if adding was successful
		if(LocationStatusCodes.SUCCESS == status){
			TextView fencepost = (TextView) findViewById(R.id.fenceadd);
			fencepost.setText("fences added successfully");
		}else{
			//everything was a failure
			//report errors
			Log.e("Geofences addition", "Geo add error "+Integer.toString(status));
			Toast.makeText(this, "Failed to add geofences "+Integer.toString(status), Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectResult) {
		/*
		 * Google Play Services can resolve some connection errors
		 * If the error has a resolution, try to send an Intent to
		 * start a Google Play services activity that can fix error
		 */
		if(connectResult.hasResolution()) {
			try{
				connectResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
			} catch(IntentSender.SendIntentException e) {
				e.printStackTrace(); // log the error
			}
		} else {
			showErrorDialog(connectResult.getErrorCode()); // display a dialog
		}
	}

	@Override
	public void onConnected(Bundle bundle) {
		// Display connection status
		Toast.makeText(this, "Connected! Go you!", Toast.LENGTH_SHORT).show();

		switch (request) {
			case ADD: PendingIntent pending = getTransitionPI();
				locClient.addGeofences( geoList, pending, this);
				break;
		}
	}

	@Override
	public void onDisconnected() {
		// Display sad connection status
		Toast.makeText(this, "Disconnected. Don't beat yourself up about it.", Toast.LENGTH_SHORT).show();
		inProgress = false;
	}
}