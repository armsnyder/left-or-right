package com.armsnyder.leftorright

data class Advice(
        val direction: Direction,
        val minutesUntilArrival: Int,
        val route: String
)
