package com.armsnyder.leftorright;

import android.content.Context;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class LeftOrRightIO {
    private static final String TAG = "LeftOrRightIO";
    private static final String RIGHT_STOP_ID = "1_590";
    private static final String LEFT_STOP_ID = "1_578";
    private static final Set<String> RIGHT_ROUTES = new HashSet<>(Arrays.asList("1_100229",
            "1_100062", "1_100071", "1_100081", "1_100132", "1_100151", "1_100169", "1_100194",
            "1_102574", "1_100252", "1_100014", "1_100016", "1_100017", "1_100023"));
    private static final Set<String> LEFT_ROUTES = new HashSet<>(Arrays.asList("1_100044",
            "1_102581", "1_102615"));
    private static final String GET_ARRIVALS_FORMAT = "http://api.pugetsound.onebusaway.org/api" +
            "/where/arrivals-and-departures-for-stop/%s.xml?key=TEST&minutesBefore=0";
    private static final Arrival NO_BUS = new Arrival(Long.MAX_VALUE, "No bus");

    private final RequestQueue requestQueue;

    LeftOrRightIO(Context context) {
        requestQueue = Volley.newRequestQueue(context);
    }

    public CompletableFuture<Advice> getAdvice() {
        Log.d(TAG, "Getting advice");
        CompletableFuture<Advice> future = new CompletableFuture<>();
        CompletableFuture<List<Arrival>> futureArrivalTimesRight =
                getArrivals(RIGHT_STOP_ID, RIGHT_ROUTES);
        CompletableFuture<List<Arrival>> futureArrivalTimesLeft =
                getArrivals(LEFT_STOP_ID, LEFT_ROUTES);
        CompletableFuture.allOf(futureArrivalTimesRight, futureArrivalTimesLeft)
                .thenAccept(ignored -> {
                    Log.d(TAG, "I/O completed");
                    long now = System.currentTimeMillis();
                    Comparator<Arrival> arrivalComparator = (a, b) ->
                            Math.toIntExact(a.getPredictedArrivalTime()
                                    - b.getPredictedArrivalTime());
                    Arrival earliestRight = futureArrivalTimesRight.join().stream()
                            .filter(a -> a.getPredictedArrivalTime() > now)
                            .min(arrivalComparator).orElse(NO_BUS);
                    Arrival earliestLeft = futureArrivalTimesLeft.join().stream()
                            .filter(a -> a.getPredictedArrivalTime() > now)
                            .min(arrivalComparator).orElse(NO_BUS);
                    Direction direction = earliestLeft.getPredictedArrivalTime()
                            < earliestRight.getPredictedArrivalTime()
                            ? Direction.LEFT : Direction.RIGHT;
                    Arrival arrival = earliestRight.getPredictedArrivalTime()
                            < earliestLeft.getPredictedArrivalTime()
                            ? earliestRight : earliestLeft;
                    Log.d(TAG, "Predicted direction: " + direction);
                    long etaMillis = arrival.getPredictedArrivalTime() - now;
                    int etaMins = (int) (etaMillis / 1000 / 60);
                    Log.d(TAG, "Predicted ETA: " + etaMins);
                    String route = arrival.getRouteShortName();
                    Log.d(TAG, "Route: " + route);
                    future.complete(new Advice(direction, etaMins, route));
                }).exceptionally(t -> {
            future.completeExceptionally(t);
            return null;
        });
        return future;
    }

    private CompletableFuture<List<Arrival>> getArrivals(String stopId, Set<String> routeIds) {
        String url = String.format(GET_ARRIVALS_FORMAT, stopId);
        return getDocument(url).thenApply(document -> parseDocument(document, routeIds));
    }

    private CompletableFuture<Document> getDocument(String url) {
        Log.d(TAG, "Requesting " + url);
        CompletableFuture<Document> future = new CompletableFuture<>();
        XMLRequest request = new XMLRequest(url, future::complete, future::completeExceptionally);
        requestQueue.add(request);
        return future;
    }

    private List<Arrival> parseDocument(Document document, Set<String> routeIds) {
        Log.d(TAG, "Parsing document");
        List<Arrival> arrivalsParsed = new ArrayList<>();
        NodeList arrivals = document.getElementsByTagName("arrivalAndDeparture");
        Log.d(TAG, String.format("%d arrivals", arrivals.getLength()));
        for (int i = 0; i < arrivals.getLength(); i++) {
            Node arrival = arrivals.item(i);
            NodeList arrivalChildNodes = arrival.getChildNodes();
            String routeId = null;
            boolean predicted = false;
            String shortRouteName = null;
            long predictedArrivalTime = 0;
            for (int j = 0; j < arrivalChildNodes.getLength(); j++) {
                Node arrivalChild = arrivalChildNodes.item(j);
                if ("predictedArrivalTime".equals(arrivalChild.getNodeName())) {
                    predictedArrivalTime = Long.parseLong(arrivalChild.getTextContent());
                } else if ("predicted".equals(arrivalChild.getNodeName())) {
                    predicted = Boolean.parseBoolean(arrivalChild.getTextContent());
                } else if ("routeId".equals(arrivalChild.getNodeName())) {
                    routeId = arrivalChild.getTextContent();
                } else if ("routeShortName".equals(arrivalChild.getNodeName())) {
                    shortRouteName = arrivalChild.getTextContent();
                }
            }
            Log.d(TAG, String.format("routeId=%s, predicted=%s, arrivalTime=%d, shortRouteId=%s",
                    routeId, predicted, predictedArrivalTime, shortRouteName));
            if (predicted && routeIds.contains(routeId)) {
                arrivalsParsed.add(new Arrival(predictedArrivalTime, shortRouteName));
            }
        }
        return arrivalsParsed;
    }

    private long getMin(List<Long> list) {
        long min = Long.MAX_VALUE;
        for (int i = 0; i < list.size(); i++) {
            long value = list.get(i);
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

}
