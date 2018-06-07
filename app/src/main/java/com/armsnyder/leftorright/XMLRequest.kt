package com.armsnyder.leftorright

import android.util.Log
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.xml.parsers.DocumentBuilderFactory

class XMLRequest internal constructor(url: String,
                                      private val listener: Response.Listener<Document>,
                                      errorListener: Response.ErrorListener) :
        Request<Document>(Request.Method.GET, url, errorListener) {
    private val canceled = AtomicBoolean(false)

    override fun parseNetworkResponse(response: NetworkResponse): Response<Document> {
        Log.d(TAG, "Parsing response")
        val dbFactory = DocumentBuilderFactory.newInstance()
        try {
            val document = dbFactory.newDocumentBuilder().parse(ByteArrayInputStream(response.data))
            document.normalizeDocument()
            return Response.success(document, HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override fun deliverResponse(response: Document) {
        Log.d(TAG, "Delivering response")
        if (!canceled.get()) {
            listener.onResponse(response)
        }
    }

    override fun cancel() {
        super.cancel()
        canceled.set(true)
    }

    companion object {
        private const val TAG = "XMLRequest"
    }
}
