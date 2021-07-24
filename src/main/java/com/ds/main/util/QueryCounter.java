package com.ds.main.util;

public class QueryCounter {
    private static int counter = 0;

    public static int getCounter() {
        return counter;
    }

    public static void increase() {
        counter++;
    }

    public static void reset() {
        counter = 0;
    }
}
