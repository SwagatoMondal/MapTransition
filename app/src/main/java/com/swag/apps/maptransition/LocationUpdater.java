package com.swag.apps.maptransition;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class LocationUpdater implements LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private WeakReference<Context> mContext;
    private LocationUpdateCallBack mCallback;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    public LocationUpdater(Context context, LocationUpdateCallBack callBack) {
        mContext = new WeakReference<>(context);
        mCallback = callBack;

        buildGoogleApiClient();
        createLocationRequest();
    }

    private void buildGoogleApiClient() {
        if(mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(mContext.get())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void startLocationUpdates() {
        mGoogleApiClient.connect();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
    }

    public void stopLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    public Location getLatestLocation() {
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        startLocationUpdates();
        return lastLocation;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        mCallback.onLocationUpdated(lastLocation);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mCallback.onLocationUpdated(location);
    }

    /**
     * Method to return the latitude and longitude from a given address
     * @param address Represents the address whose latitude and longitude is to be found
     * @return A LatLng object if address found otherwise returns null
     */
    public LatLng getLocationFromAddress(String address) {
        if(address == null) {
            return null;
        } else {
            Geocoder geocoder = new Geocoder(mContext.get());
            List<Address> addresses;

            try {
                addresses = geocoder.getFromLocationName(address, 3);
                if (addresses == null || addresses.size() == 0) {
                    return null;
                } else {
                    Address bestAddress = addresses.get(0);
                    return new LatLng(bestAddress.getLatitude(), bestAddress.getLongitude());
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Method to get the address of the given latitude and longitude
     * @param latitude Represents the latitude provided
     * @param longitude Represents the longitude provided
     * @return The address of the given latitude and longitude if found otherwise null
     */
    public String getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(mContext.get());
        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses == null || addresses.size() == 0) {
                return null;
            } else {
                Address firstAddress = addresses.get(0);
                String address = firstAddress.getAddressLine(0) + "," +
                        firstAddress.getAddressLine(1) + "," + firstAddress.getLocality() + "," +
                        firstAddress.getAdminArea();
                return address;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public interface LocationUpdateCallBack {
        void onLocationUpdated(Location location);
        //void onAddressFound(String address);
    }
}
