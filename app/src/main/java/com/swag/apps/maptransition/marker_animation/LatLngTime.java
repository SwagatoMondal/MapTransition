package com.swag.apps.maptransition.marker_animation;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by swagato.mondal on 3/21/16.
 */
public class LatLngTime {
    private LatLng mLatLng;
    private long mDelay;
    private float mAccuracy;

    public LatLngTime(LatLng latLng, long delay, float accuracy) {
        mLatLng = latLng;
        mDelay = delay;
        mAccuracy = accuracy;
    }

    public LatLng getLatLng() {
        return mLatLng;
    }

    public long getDelay() {
        return mDelay;
    }

    public float getAccuracy() {
        return mAccuracy;
    }
}
