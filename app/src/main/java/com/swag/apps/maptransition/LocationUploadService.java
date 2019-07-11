package com.swag.apps.maptransition;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;

public class LocationUploadService extends Service implements LocationUpdater.LocationUpdateCallBack {
    public static final String START_LOCATION_UPLOAD_EXTRA = "START_LOCATION_UPLOAD_EXTRA";
    public static final String STOP_LOCATION_UPLOAD_EXTRA = "STOP_LOCATION_UPLOAD_EXTRA";
    public static final String TRIP_ID_EXTRA = "TRIP_ID_EXTRA";
    public static final String LOCATION_BROADCAST = "LOCATION_BROADCAST";

    private LocationUpdater mLocationUpdater;
    //private double mLatitude = 0, mLongitude = 0;
    private Location mCurrent;
    private LocationUploadThread mThread;

    public LocationUploadService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLocationUpdater = new LocationUpdater(this, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("Service started");
        if (intent != null) {
            boolean startUpload = intent.getBooleanExtra(START_LOCATION_UPLOAD_EXTRA, false);
            if (startUpload) {
                setLatestLocationValues();
                mThread = new LocationUploadThread();
                mThread.start();
            } else {
                boolean stopUpload = intent.getBooleanExtra(STOP_LOCATION_UPLOAD_EXTRA, false);
                if (stopUpload) {
                    stopSelf();
                }
            }
        } else {
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mLocationUpdater != null) {
            mLocationUpdater.stopLocationUpdates();
        }

        if (mThread != null) {
            mThread.finishThread();
        }
    }

    @Override
    public void onLocationUpdated(Location location) {
        if (location != null) {
            //mLatitude = location.getLatitude();
            //mLongitude = location.getLongitude();
            mCurrent = location;
        }
    }

    private void setLatestLocationValues() {
        Location location = mLocationUpdater.getLatestLocation();
        if (location != null) {
            //mLatitude = location.getLatitude();
            //mLongitude = location.getLongitude();
            mCurrent = location;
        }
    }

    private class LocationUploadThread extends Thread {
        private boolean finish = false;
        private final Object mLock = new Object();

        private void finishThread() {
            finish = true;
        }

        @Override
        public void run() {
            System.out.println("Service thread started");
            Handler mainHandler = new Handler(LocationUploadService.this.getMainLooper());

            Runnable getLatestLocThread = new Runnable() {
                @Override
                public void run() {
                    setLatestLocationValues();
                }
            };

            while (!finish) {
                mainHandler.post(getLatestLocThread);

                //broadcastLocation(mLatitude, mLongitude);
                broadcastLocation();

                synchronized (mLock) {
                    try {
                        mLock.wait(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /*private void broadcastLocation(double latitude, double longitude) {
        Intent broadcast = new Intent(LOCATION_BROADCAST);
        broadcast.putExtra("LATITUDE", latitude);
        broadcast.putExtra("LONGITUDE", longitude);
        sendBroadcast(broadcast);
    }*/

    private void broadcastLocation() {
        if (mCurrent != null) {
            Intent broadcast = new Intent(LOCATION_BROADCAST);
            broadcast.putExtra("LATITUDE", mCurrent.getLatitude());
            broadcast.putExtra("LONGITUDE", mCurrent.getLongitude());
            broadcast.putExtra("ACCURACY", mCurrent.getAccuracy());
            sendBroadcast(broadcast);
        }
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
}
