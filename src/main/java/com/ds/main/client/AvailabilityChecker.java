package com.ds.main.client;

import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class AvailabilityChecker extends Thread {

    public static final int START_DELAY = 30000;
    public static final int PERIOD = 30000;
    public static final int MAX_CHECKS = 5;

    public static Node node;
    public static DatagramSocket ds;
    public static DatagramSocket socket;
    public static int checksCounter;


    public AvailabilityChecker(Node receiverNode) {
        node = receiverNode;
        ds = node.ds;
        socket = node.socket;
    }

    @Override
    public void run() {
        checkAvailability();
    }


    public static void checkAvailability() {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                checkNeighboursAvailability();
            }
        };
        timer.schedule(task, START_DELAY, PERIOD);
    }

    public static void checkNeighboursAvailability() {
        checksCounter++;

        if (node.availableNeighbours.size() > 0) {
            ArrayList<String> nodeKeys = new ArrayList<>();
            int removingIndex = -1;
            for (Node node : node.addedNeighbours) {
                nodeKeys.add(node.ip + ":" + node.port);
            }

            for (String key : nodeKeys) {
                if (!node.availableNeighbours.containsKey(key)) {
                    for (int i = 0; i < node.addedNeighbours.size(); i++) {
                        if (node.addedNeighbours.get(i).getKey().equals(key)) {
                            removingIndex = i;
                        }
                    }
                    if (removingIndex >= 0) {
                        System.out.println("Node " + node.addedNeighbours.get(removingIndex).getIp() + ":"
                                + node.addedNeighbours.get(removingIndex).getPort() + " was disconnected. Removed from routing table");
                        node.addedNeighbours.remove(removingIndex);
                        node.blacklist.add(key);

                    }
                }
            }
            node.availableNeighbours = new HashMap<>();

        }

        if (checksCounter == MAX_CHECKS) {
            node.blacklist = new ArrayList<>();
            checksCounter = 0;
        }
    }


}
