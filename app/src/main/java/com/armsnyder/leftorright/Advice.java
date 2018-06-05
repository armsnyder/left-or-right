package com.armsnyder.leftorright;

public class Advice {
    private final Direction direction;
    private final int minutesUntilArrival;
    private final String route;

    public Advice(Direction direction, int minutesUntilArrival, String route) {
        this.direction = direction;
        this.minutesUntilArrival = minutesUntilArrival;
        this.route = route;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getMinutesUntilArrival() {
        return minutesUntilArrival;
    }

    public String getRoute() {
        return route;
    }
}
