package me.devsaki.hentoid.services;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.util.AbstractMap;
import java.util.Map;

/**
 * Created by Robb_w on 2018/04
 * <p>
 * Specific Volley Request intended at transmitting :
 * - content as byte array
 * - raw HTTP response headers
 * <p>
 * to the download callback routine
 */
class InputStreamVolleyRequest extends Request<byte[]> {
    // Callback
    private final Response.Listener<Map.Entry<byte[], Map<String, String>>> mListener;
    // Temporary storage for HTTP response headers
    private Map<String, String> responseHeaders;

    InputStreamVolleyRequest(int method, String mUrl, Response.Listener<Map.Entry<byte[], Map<String, String>>> listener,
                             Response.ErrorListener errorListener) {
        super(method, mUrl, errorListener);
        // this request would never use cache.
        setShouldCache(false);
        mListener = listener;
    }

    @Override
    protected void deliverResponse(byte[] response) {
        mListener.onResponse(new AbstractMap.SimpleEntry<>(response, responseHeaders));
    }

    @Override
    protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {
        //Initialise local responseHeaders map with response headers received
        responseHeaders = response.headers;

        //Pass the response data here
        return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response));
    }
}