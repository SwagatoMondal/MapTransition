package com.swag.apps.maptransition;

import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;
import com.swag.apps.maptransition.internet.HTTPConnectionMgr;
import com.swag.apps.maptransition.marker_animation.LatLngTime;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by swagato.mondal on 3/21/16.
 */
public class FilterLatLng extends AsyncTask<Void, Void, Void> {
    private String mLatLngList;
    private final String mApiKey = "AIzaSyBU9pPtbVUptg2grlMa6AklY7hhLGD5_IE";
    private IFilterCallback mCallback;

    public FilterLatLng(String latLngList, IFilterCallback callback) {
        mLatLngList = latLngList;
        mCallback = callback;
    }

    @Override
    protected Void doInBackground(Void... params) {
        String url = "https://roads.googleapis.com/v1/snapToRoads?path=" + mLatLngList +
                "&interpolate=true&key=" + mApiKey;
        System.out.println("HTTP Request - " + url);
        HttpResponse response = HTTPConnectionMgr.makePostRequest(url);

        if (response != null) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HTTPConnectionMgr.SUCCESS) {
                try {
                    //System.out.println("HTTP CODE - SUCCESS" + EntityUtils.toString(response.getEntity()));
                    JSONObject reponseObject = new JSONObject(EntityUtils.toString(response.getEntity()));
                    mCallback.onFiltered(getLatLngsFromResponse(reponseObject));
                    System.out.println("HTTP CODE - SUCCESS" + reponseObject.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("HTTP ERROR - " + statusLine.getStatusCode());
            }
        } else {
            System.out.println("HTTP ERROR NULL RESPONSE");
        }
        return null;
    }

    public static String getFormattedLatLngList(List<LatLng> latLngs) {
        String latlngList = null;
        if (latLngs != null) {
            latlngList = new String("");

            Iterator<LatLng> iterator = latLngs.iterator();
            while (iterator.hasNext()) {
                LatLng latLng = iterator.next();
                latlngList = latlngList.concat(latLng.latitude + "," + latLng.longitude);

                if (iterator.hasNext()) {
                    try {
                        latlngList = latlngList.concat(URLEncoder.encode("|", "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }
            /*for (LatLngTime latLngTime : latLngTimes) {
                try {
                    latlngList = latlngList.concat(latLngTime.getLatLng().latitude + "," + latLngTime.getLatLng().longitude + URLEncoder.encode("|", "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            latlngList = latlngList.substring(0, latlngList.length()-1);*/
            /*try {
                latlngList = URLEncoder.encode(latlngList, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();return null;
            }*/
        }
        System.out.println("Prepared string for lat long list - " + latlngList);
        return latlngList;
    }

    private List<LatLng> getLatLngsFromResponse(JSONObject jsonObject) {
        List<LatLng> latLngs = new ArrayList<>();

        try {
            JSONArray points = jsonObject.getJSONArray("snappedPoints");

            int length = points.length();
            for (int i = 0; i < length; i++) {
                JSONObject location = points.getJSONObject(i).getJSONObject("location");

                latLngs.add(new LatLng(location.getDouble("latitude"), location.getDouble("longitude")));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return latLngs;
    }

    public interface IFilterCallback {
        void onFiltered(List<LatLng> latLngs);
    }
}
