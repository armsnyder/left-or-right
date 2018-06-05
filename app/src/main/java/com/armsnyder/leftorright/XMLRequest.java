package com.armsnyder.leftorright;

import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XMLRequest extends Request<Document> {

    private static final String TAG = "XMLRequest";
    private final Object lock = new Object();
    private Response.Listener<Document> listener;

    public XMLRequest(String url, Response.Listener<Document> listener,
                      Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        this.listener = listener;
    }

    @Override
    protected Response<Document> parseNetworkResponse(NetworkResponse response) {
        Log.d(TAG, "Parsing response");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document document = dBuilder.parse(new ByteArrayInputStream(response.data));
            document.normalizeDocument();
            return Response.success(document,
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void deliverResponse(Document response) {
        Log.d(TAG, "Delivering response");
        Response.Listener<Document> listenerLocal;
        synchronized (lock) {
            listenerLocal = listener;
        }
        if (listenerLocal != null) {
            listenerLocal.onResponse(response);
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        synchronized (lock) {
            listener = null;
        }
    }
}
