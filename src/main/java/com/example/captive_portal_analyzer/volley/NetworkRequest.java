package com.example.captive_portal_analyzer.volley;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

// source https://github.com/m-i-k-e-e/WifiPortalAutoLog/blob/master/app/src/main/java/org/mike/autolog/volley/NetworkRequest.java, last checked, 29.01.2022

public class NetworkRequest extends Request<NetworkResponse> {

    private final Response.Listener<NetworkResponse> mListener;

    public NetworkRequest(int method, String url, Response.Listener<NetworkResponse> listener, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        mListener = listener;
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        mListener.onResponse(response);
    }
}
