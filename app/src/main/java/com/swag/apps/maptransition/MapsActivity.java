package com.swag.apps.maptransition;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.swag.apps.maptransition.marker_animation.LatLngInterpolator;
import com.swag.apps.maptransition.marker_animation.LatLngTime;
import com.swag.apps.maptransition.marker_animation.MarkerAnimation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener,
        LocationUpdater.LocationUpdateCallBack, FilterLatLng.IFilterCallback {

    private GoogleMap mMap;
    private Marker mMarker;
    private LatLng mCurrent;

    private List<LatLng> mRecordedLatLng;
    private long mFirstAddedMarkerTime = 0;
    private long mLastAddedMarkerTime = 0;

    private boolean mDialogShown = false;
    private boolean mAutoLocationCollected = false;
    private LocationManager mManager;
    private final LocationListener mListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            System.out.println("Location changed");
            if (location != null) {
                mCurrent = new LatLng(location.getLatitude(), location.getLongitude());
                addLatLongToList(location.getAccuracy());
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private BroadcastReceiver mLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                double lat = intent.getDoubleExtra("LATITUDE", 0);
                double lon = intent.getDoubleExtra("LONGITUDE", 0);
                float accuracy = intent.getFloatExtra("ACCURACY", 0);

                if (lat != 0 && lon != 0) {
                    mCurrent = new LatLng(lat, lon);
                    addLatLongToList(accuracy);
                }

                Log.d("Home", "Broadcast received - Lat = " + lat + " , Long = " + lon);
            }
        }
    };

    private void updateMapMarker(LatLng latLng) {
        mCurrent = latLng;
        mMarker.setPosition(mCurrent);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrent, 15f));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        findViewById(R.id.record).setOnClickListener(this);
        findViewById(R.id.play).setOnClickListener(this);

        //mManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        animateMarker(findViewById(R.id.custom_marker));
        getCurrentLoc();
    }

    private void getCurrentLoc() {
        LocationUpdater updater = new LocationUpdater(this, this);
        Location location = updater.getLatestLocation();
        if (location != null) {
            mCurrent = new LatLng(location.getLatitude(), location.getLongitude());
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        System.out.println("Marker value = " + mCurrent);
        // Add a marker in Sydney and move the camera
        if (mCurrent == null) {
            mCurrent = new LatLng(-34, 151);
        }
        mMarker = mMap.addMarker(new MarkerOptions().position(mCurrent).title("Car"));
        //.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrent, 15f));
        // Add Camera change listener
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if (!mAutoLocationCollected) {
                    // Get the center of the Map.
                    LatLng centerOfMap = mMap.getCameraPosition().target;

                    // Update your Marker's position to the center of the Map.
                    //mMarker.setPosition(centerOfMap);
                    mCurrent = centerOfMap;
                }
            }
        });
    }

    private void addLatLongToList(float accuracy) {
        if (mRecordedLatLng == null) {
            mRecordedLatLng = new ArrayList<>();
        }

        long delay = System.currentTimeMillis();
        if (mLastAddedMarkerTime != 0) {
            delay -= mLastAddedMarkerTime;
        } else {
            delay = 2000;
        }
        mRecordedLatLng.add(mCurrent);

        if (mFirstAddedMarkerTime == 0) {
            mFirstAddedMarkerTime = System.currentTimeMillis();
        }
        mLastAddedMarkerTime = System.currentTimeMillis();

        System.out.println("New Size = " + mRecordedLatLng.size());
    }

    private void animateLatLongs() {
        if (mRecordedLatLng != null && mRecordedLatLng.size() > 1) {
            findViewById(R.id.custom_marker).setVisibility(View.GONE);

            final long perAnimationDuration = (mLastAddedMarkerTime - mFirstAddedMarkerTime)/
                    mRecordedLatLng.size();
            System.out.println("Animation duration - " + perAnimationDuration);

            LatLngInterpolator interpolator = new LatLngInterpolator.Spherical();
            Iterator<LatLng> iterator = mRecordedLatLng.iterator();

            LatLng firstLatLng = iterator.next();
            ObjectAnimator animator = MarkerAnimation.animateMarkerToICS(mMarker, firstLatLng,
                    interpolator, perAnimationDuration);
            animator.start();

            while (iterator.hasNext()) {
                LatLng next = iterator.next();
                final ObjectAnimator newAnimator = MarkerAnimation.animateMarkerToICS(mMarker, next,
                        interpolator, perAnimationDuration);
                animator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        newAnimator.start();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
                animator = newAnimator;
            }

            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {}

                @Override
                public void onAnimationEnd(Animator animation) {
                    findViewById(R.id.custom_marker).setVisibility(View.VISIBLE);
                    showReportAfterAnimation();
                    mRecordedLatLng.clear();
                }

                @Override
                public void onAnimationCancel(Animator animation) {}

                @Override
                public void onAnimationRepeat(Animator animation) {}
            });
        }
    }

    private void showGPSAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to track the locations automatically?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (canAccessLocation()) {
                    whenPermissionGranted();
                } else {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                }
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                addLatLongToList(100);
            }
        });
        builder.show();
        mDialogShown = true;
    }

    private void showFilterLatLngs() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to filter captured latitudes/longitudes?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getFilteredList();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                play();
            }
        });
        builder.show();
    }

    private void showReportAfterAnimation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Report");
        builder.setMessage(getReport());
        builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();
    }

    private void play() {
        if (mAutoLocationCollected) {
            stopLocationUpdates();
        }
        animateLatLongs();
        reset();
    }

    private void whenPermissionGranted() {
        /*mManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
                1, mListener);*/
        Intent intent = new Intent(this, LocationUploadService.class);
        intent.putExtra(LocationUploadService.START_LOCATION_UPLOAD_EXTRA, true);
        startService(intent);
        registerReceiver(mLocationReceiver, new IntentFilter(
                LocationUploadService.LOCATION_BROADCAST));
        mAutoLocationCollected = true;
        addLatLongToList(0);
    }

    private boolean canAccessLocation() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (PackageManager.PERMISSION_GRANTED
                == checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                PackageManager.PERMISSION_GRANTED == checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION));
    }

    private void reset() {
        mDialogShown = false;
        mAutoLocationCollected = false;
    }

    private void stopLocationUpdates() {
        //mManager.removeUpdates(mListener);
        Intent intent = new Intent(this, LocationUploadService.class);
        intent.putExtra(LocationUploadService.STOP_LOCATION_UPLOAD_EXTRA, true);
        startService(intent);

        unregisterReceiver(mLocationReceiver);
    }

    private String getReport() {
        String report = null;
        if (mRecordedLatLng != null) {
            report = new String("");
            for (LatLng latLng : mRecordedLatLng) {
                report = report.concat("Lat - " + latLng.latitude +  "\nLong - "
                        + latLng.longitude + "\nAccuracy - 0 " + "\n\n");
            }
        }
        return report;
    }

    private void getFilteredList() {
        new FilterLatLng(FilterLatLng.getFormattedLatLngList(mRecordedLatLng), this).execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            whenPermissionGranted();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.record:
                if (mDialogShown) {
                    if (!mAutoLocationCollected) {
                        addLatLongToList(100);
                    }
                } else {
                    showGPSAlert();
                }
                break;
            case R.id.play:
                showFilterLatLngs();
                break;
        }
    }

    @Override
    public void onLocationUpdated(Location location) {
        if (location != null) {
            System.out.println("Location updated with " + location.getLatitude() + "," + location.getLongitude());
            mCurrent = new LatLng(location.getLatitude(), location.getLongitude());
            mMarker.setPosition(mCurrent);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrent, 15f));
        }
    }

    @Override
    protected void onDestroy() {
        if (mDialogShown) {
            stopLocationUpdates();
        }
        super.onDestroy();
    }

    @Override
    public void onFiltered(List<LatLng> latLngs) {
        System.out.println("Received new lat longs");
        if (latLngs != null && latLngs.size() > 0) {
            mRecordedLatLng.clear();
            mRecordedLatLng.addAll(latLngs);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    play();
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MapsActivity.this, "Unable to filter", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void animateMarker(View view) {
        /*final RotateAnimation rotateAnimation = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f);
        rotateAnimation.setDuration(500);
        view.startAnimation(rotateAnimation);*/

        ObjectAnimator animation = ObjectAnimator.ofFloat(view, "rotationY", 0.0f, 360f);
        animation.setDuration(2000);
        animation.setRepeatCount(2);
        //animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.start();
    }
}
