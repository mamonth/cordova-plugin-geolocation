package org.apache.cordova.geolocation;

import android.content.IntentSender;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import org.apache.cordova.CallbackContext;
import android.location.Location;

import java.util.*;

/**
 * @author Andrew Tereshko <andrew.tereshko@gmail.com>
 */
public class NewLocationListener implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener,
        LocationListenerInterface
    {
        private GeoBroker owner;

        private Timer timer = null;

        private LocationClient mLocationClient;
        private LocationRequest mLocationRequest;

        protected boolean running = false;

        private String TAG = "[Cordova New Location Listener]";

        public HashMap<String, CallbackContext> watches = new HashMap<String, CallbackContext>();
        private List<CallbackContext> callbacks = new ArrayList<CallbackContext>();

        public NewLocationListener( GeoBroker broker ) {
            this.owner = broker;

            // Create the LocationRequest object
            mLocationRequest = LocationRequest.create();
            // Use high accuracy
            mLocationRequest.setPriority( LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY );
            // Set the update interval to 30 seconds
            mLocationRequest.setInterval( 30 );
            // Set the fastest update interval to 10 second
            mLocationRequest.setFastestInterval( 10 );
        }

        private void win(Location loc) {
            this.cancelTimer();

            for (CallbackContext callbackContext: this.callbacks)
            {
                this.owner.win(loc, callbackContext, false);
            }

            if(this.watches.size() == 0)
            {
                Log.d(TAG, "Stopping global listener");
                this.stop();
            }

            this.callbacks.clear();

            Iterator<CallbackContext> it = this.watches.values().iterator();
            while (it.hasNext()) {
                this.owner.win(loc, it.next(), true);
            }
        }

        public void addCallback(CallbackContext callbackContext, int timeout) {

            if(this.timer == null) {
                this.timer = new Timer();
            }

            this.timer.schedule(new LocationTimeoutTask(callbackContext, this), timeout);

            this.callbacks.add(callbackContext);
            if (this.size() == 1) {
                this.start();
            }
        }

        /**
         * Start requesting location updates.
         */
        protected void start() {

            if( mLocationClient == null ) mLocationClient = new LocationClient( this.owner.cordova.getActivity(), this, this);

            if( !mLocationClient.isConnected() ) mLocationClient.connect();
        }

        /**
         * Stop receiving location updates.
         */
        public void stop() {

            this.cancelTimer();

            if ( mLocationClient.isConnected() ) {

                mLocationClient.removeLocationUpdates( this );

                mLocationClient.disconnect();
            }

            this.running = false;
        }


        public int size() {
            return this.watches.size() + this.callbacks.size();
        }

        private void cancelTimer() {
            if(this.timer != null) {
                this.timer.cancel();
                this.timer.purge();
                this.timer = null;
            }
        }

        public void addWatch(String timerId, CallbackContext callbackContext) {
            this.watches.put(timerId, callbackContext);
            if (this.size() == 1) {
                this.start();
            }
        }

        public void clearWatch(String timerId) {
            if (this.watches.containsKey(timerId)) {
                this.watches.remove(timerId);
            }
            if (this.size() == 0) {
                this.stop();
            }
        }

        // Define the callback method that receives location updates
        public void onLocationChanged(Location location) {
            // Report to the UI that the location was updated
            String msg = "Updated Location: " +
                    Double.toString(location.getLatitude()) + "," +
                    Double.toString(location.getLongitude());

            Log.d( "NewLocationListener", msg );

            this.win(location);
        }

        /*
        * Called by Location Services when the request to connect the
        * client finishes successfully. At this point, you can
        * request the current location or start periodic updates
        */
        @Override
        public void onConnected( Bundle dataBundle) {

//            try{
//                this.onLocationChanged( mLocationClient.getLastLocation() );
//            } catch( Exception e ){
//                Log.e( "NewLocationListener::onConnected", "LocationClient.getLastLocation() failed: " + e.getMessage() );
//            }

            try{
                mLocationClient.requestLocationUpdates(mLocationRequest, this);
            } catch( Exception e ){
                Log.e( "NewLocationListener::onConnected", "LocationClient.requestLocationUpdates() failed: " + e.getMessage() );
            }
        }
        /*
         * Called by Location Services if the connection to the
         * location client drops because of an error.
         */
        @Override
        public void onDisconnected() {
        }
        /*
         * Called by Location Services if the attempt to
         * Location Services fails.
         */
        @Override
        public void onConnectionFailed( ConnectionResult connectionResult) {
            /*
             * Google Play services can resolve some errors it detects.
             * If the error has a resolution, try sending an Intent to
             * start a Google Play services activity that can resolve
             * error.
             */
            if (connectionResult.hasResolution()) {
                try {
                    // Start an Activity that tries to resolve the error
                    connectionResult.startResolutionForResult( this.owner.cordova.getActivity(), 9000);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
                } catch (IntentSender.SendIntentException e) {
                    // Log the error
                    e.printStackTrace();
                }
            } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            }
        }

        private class LocationTimeoutTask extends TimerTask {

            private CallbackContext callbackContext = null;
            private NewLocationListener listener = null;

            public LocationTimeoutTask(CallbackContext callbackContext, NewLocationListener listener) {
                this.callbackContext = callbackContext;
                this.listener = listener;
            }

            @Override
            public void run() {
                for (CallbackContext callbackContext: listener.callbacks) {
                    if(this.callbackContext == callbackContext) {
                        listener.callbacks.remove(callbackContext);
                        break;
                    }
                }

                if(listener.size() == 0) {
                    listener.stop();
                }
            }
        }
}
