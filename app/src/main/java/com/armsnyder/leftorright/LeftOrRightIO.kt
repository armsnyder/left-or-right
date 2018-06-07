package com.armsnyder.leftorright

import android.content.Context
import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.experimental.async
import org.w3c.dom.Document
import java.util.*
import kotlin.coroutines.experimental.suspendCoroutine

class LeftOrRightIO internal constructor(context: Context) {

    private val requestQueue: RequestQueue = Volley.newRequestQueue(context)

    suspend fun getAdvice(): Advice {
        Log.d(TAG, "Getting advice")
        val futureArrivalTimesRight = async { getArrivals(RIGHT_STOP_ID, RIGHT_ROUTES) }
        val futureArrivalTimesLeft = async { getArrivals(LEFT_STOP_ID, LEFT_ROUTES) }
        val now = System.currentTimeMillis()
        val earliestRight = futureArrivalTimesRight.await()
                .filter { it.predictedArrivalTime > now }
                .minBy { it.predictedArrivalTime } ?: NO_BUS
        val earliestLeft = futureArrivalTimesLeft.await()
                .filter { it.predictedArrivalTime > now }
                .minBy { it.predictedArrivalTime } ?: NO_BUS
        val direction =
                if (earliestLeft.predictedArrivalTime < earliestRight.predictedArrivalTime)
                    Direction.LEFT
                else Direction.RIGHT
        val (predictedArrivalTime, route) =
                if (earliestRight.predictedArrivalTime < earliestLeft.predictedArrivalTime)
                    earliestRight
                else earliestLeft
        Log.d(TAG, "Predicted direction: $direction")
        val etaMillis = predictedArrivalTime - now
        val etaMins = (etaMillis / 1000 / 60).toInt()
        Log.d(TAG, "Predicted ETA: $etaMins")
        Log.d(TAG, "Route: $route")
        return Advice(direction, etaMins, route)
    }

    private suspend fun getArrivals(stopId: String, routeIds: Set<String>): List<Arrival> {
        val url = String.format(GET_ARRIVALS_FORMAT, stopId)
        val document = getDocument(url)
        return parseDocument(document, routeIds)
    }

    private suspend fun getDocument(url: String): Document = suspendCoroutine { cont ->
        Log.d(TAG, "Requesting $url")
        val request = XMLRequest(url, Response.Listener({ cont.resume(it) }),
                Response.ErrorListener({ cont.resumeWithException(it) }))
        requestQueue.add(request)
    }

    private fun parseDocument(document: Document, routeIds: Set<String>): List<Arrival> {
        Log.d(TAG, "Parsing document")
        val arrivalsParsed = ArrayList<Arrival>()
        val arrivals = document.getElementsByTagName("arrivalAndDeparture")
        Log.d(TAG, "${arrivals.length} arrivals")
        for (i in 0 until arrivals.length) {
            val arrival = arrivals.item(i)
            val arrivalChildNodes = arrival.childNodes
            var routeId: String? = null
            var predicted = false
            var shortRouteName: String? = null
            var predictedArrivalTime: Long = 0
            for (j in 0 until arrivalChildNodes.length) {
                val arrivalChild = arrivalChildNodes.item(j)
                when (arrivalChild.nodeName) {
                    "predictedArrivalTime" ->
                        predictedArrivalTime = java.lang.Long.parseLong(arrivalChild.textContent)
                    "predicted" ->
                        predicted = java.lang.Boolean.parseBoolean(arrivalChild.textContent)
                    "routeId" -> routeId = arrivalChild.textContent
                    "routeShortName" -> shortRouteName = arrivalChild.textContent
                }
            }
            Log.d(TAG, "routeId=$routeId, predicted=$predicted, " +
                    "arrivalTime=$predictedArrivalTime, shortRouteId=$shortRouteName")
            if (predicted && routeId in routeIds) {
                arrivalsParsed.add(Arrival(predictedArrivalTime, shortRouteName!!))
            }
        }
        return arrivalsParsed
    }

    companion object {
        private const val TAG = "LeftOrRightIO"
        private const val RIGHT_STOP_ID = "1_590"
        private const val LEFT_STOP_ID = "1_578"
        private val RIGHT_ROUTES = setOf("1_100229",
                "1_100062", "1_100071", "1_100081", "1_100132", "1_100151", "1_100169", "1_100194",
                "1_102574", "1_100252", "1_100014", "1_100016", "1_100017", "1_100023")
        private val LEFT_ROUTES = setOf("1_100044", "1_102581", "1_102615")
        private const val GET_ARRIVALS_FORMAT = "http://api.pugetsound.onebusaway.org/api" +
                "/where/arrivals-and-departures-for-stop/%s.xml?key=TEST&minutesBefore=0"
        private val NO_BUS = Arrival(java.lang.Long.MAX_VALUE, "No bus")
    }
}
