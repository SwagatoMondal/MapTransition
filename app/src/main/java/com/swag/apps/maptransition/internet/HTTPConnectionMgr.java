package com.swag.apps.maptransition.internet;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class HTTPConnectionMgr {
    public static final String CODE_SUCCESS = "Success";
    public static final int SUCCESS = 200;

    public static HttpResponse makePostRequest(String url) {
        HttpClient httpClient = new DefaultHttpClient();
        // Post
        HttpPost httpPost = new HttpPost(url);

        //httpPost.setHeader("Accept", "application/json");
        //httpPost.setHeader("Content-Type", "application/json");

        // Create entity
        try {
            // Post the request
            return httpClient.execute(httpPost);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static HttpResponse makePutRequest(String url, JSONObject jsonObject) {
        HttpClient httpClient = new DefaultHttpClient();
        // Post
        HttpPut httpPut = new HttpPut(url);
        httpPut.setHeader("Accept", "application/json");
        httpPut.setHeader("Content-Type", "application/json");

        // Create entity
        try {
            if (jsonObject != null) {
                HttpEntity httpEntity = new StringEntity(jsonObject.toString());

                // Set the body of the post, set entity
                httpPut.setEntity(httpEntity);
            }

            // Post the request
            return httpClient.execute(httpPut);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static HttpResponse makeGetRequest(String url) {
        HttpClient httpClient = new DefaultHttpClient();
        // Post
        HttpGet httpGet = new HttpGet(url);
        //httpGet.setHeader("Accept", "application/json");
        //httpGet.setHeader("Content-Type", "application/json");

        // Create entity
        try {
            // Post the request
            return httpClient.execute(httpGet);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
