package com.armsnyder.leftorright;

public class Arrival {
    private final long predictedArrivalTime;
    private final String routeShortName;

    public Arrival(long predictedArrivalTime, String routeShortName) {
        this.predictedArrivalTime = predictedArrivalTime;
        this.routeShortName = routeShortName;
    }

    public long getPredictedArrivalTime() {
        return predictedArrivalTime;
    }

    public String getRouteShortName() {
        return routeShortName;
    }
}
