package com.ds.main;

import com.ds.main.client.*;
//import com.ds.main.service.FileService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
//import java.util.Random;
import java.util.Scanner;

@SpringBootApplication
public class SpringBootRestApplication {
    private static int port;
    public static String[] servingFiles;
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        Scanner scanner = new Scanner(System.in);
        //Random rNum = new Random();
       // FileService fileService = new FileService();

        String ip =  getMyIp();


        //get port
        System.out.print("Enter port:");
        port = scanner.nextInt();

        //start spring boot REST API
        SpringApplication.run(SpringBootRestApplication.class, args);

        scanner.nextLine();

        //get username
        System.out.print("Ënter username:");
        String username = scanner.nextLine();

        Node node1 = new Node(ip, port, username);

        //start listening thread
        new Thread(node1).start ();

        //add the serving resources to node
        for(int i=0; i<servingFiles.length; i++) {
            node1.addResource(servingFiles[i]);
        }
        node1.showResources();

        System.out.println(node1.getPort()+": registering");
        node1.register();

        CommandHandler commandHandler = new CommandHandler(node1);

        //start gossiping thread
        Gossip gossip = new Gossip(node1);
        gossip.run();

        //start the heartbeat thread
        Heartbeat heartbeat = new Heartbeat(node1);
        heartbeat.run();

        //start the active checker thread
        AvailabilityChecker availabilityChecker = new AvailabilityChecker(node1);
        availabilityChecker.run();

        //start listening to commands
        while (true){
            String command = scanner.nextLine();
            commandHandler.handleCommand(command);
        }

    }


    public static String getMyIp() {
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getPort(){
        return port;
    }
}
