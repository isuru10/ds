package com.ds.main.client;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Gossip extends Thread {

    public static   int gossipPeriod =10000; //10s
    public static   int threadStartingDelay=1000; //10s
    public static Node node;
    public static DatagramSocket ds;
    public static DatagramSocket socket;

    public Gossip(Node nodeRecieve){
        node = nodeRecieve;
        ds = node.ds;
        socket = node.socket;
    }

    @Override
    public void run(){
            sendGossip();
    }

    public static void sendGossip(){
        //The scheduler to schedule gossip sending interval
        Timer timer=new Timer();
        TimerTask task=new TimerTask() {
            @Override
            public void run() {
                sendGossipsToNeighbours();
            }
        };
        timer.schedule(task,threadStartingDelay, gossipPeriod);
    }

    public static void sendGossipsToNeighbours(){
        //send gossip to neighbours asking for IPs
        if (node.addedNeighbours.size() <3 ) {

            ArrayList<Node> neighbours = new ArrayList<>();
            neighbours.addAll(node.addedNeighbours);

            for (Node node : neighbours) {
                sendNeighboursToNeighbourMessage(node);
            }
        }
    }

    public static void sendNeighboursToNeighbourMessage(Node nodeToBeSent){
        //gossip request create
        InetAddress ipaddr = null; //myip
        try {
            ds = new DatagramSocket();
            ipaddr = InetAddress.getByName(nodeToBeSent.getIp());
            int port = nodeToBeSent.getPort();
            String request="GOSSIP "+node.ip+" "+node.port;
            String length = String.valueOf(request.length()+5);
            length = String.format("%4s", length).replace(' ', '0');
            request = length + " " + request;
            byte[] msg = request.getBytes();
            DatagramPacket packet = new DatagramPacket(msg, msg.length, ipaddr, port);
            ds.send(packet);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
