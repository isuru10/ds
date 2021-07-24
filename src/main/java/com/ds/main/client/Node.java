package com.ds.main.client;

import com.ds.main.util.QueryCounter;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContext;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.*;

public class Node implements Runnable {

    private static int HOP_LIMIT = 4;
    public String ip;
    public int port;
    public String username;
    public ArrayList<Node> addedNeighbours = new ArrayList<>();
    public HashMap<String, Node> availableNeighbours = new HashMap<>();
    public ArrayList<String> blacklist = new ArrayList<>();
    private ArrayList<String> resources = new ArrayList<>();
    DatagramSocket ds;
    public DatagramSocket socket = null;

    @Autowired
    ServletContext context;


    public Node(String ip, int port, String username) {
        this.ip = ip;
        this.port = port;
        this.username = username;
    }

    public Node(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void addResource(String name) {
        this.resources.add(name);
    }

    private boolean equals(String ip, int port) {
        if (this.port == port && this.ip.equals(ip)) {
            return true;
        }
        return false;
    }

    public String getKey() {
        return ip + ":" + port;
    }

    private ArrayList<String> removeEmpty(String[] arr) {
        ArrayList<String> cleanedList = new ArrayList<>();
        for (String i : arr) {
            if (!i.trim().equals(""))
                cleanedList.add(i.trim());
        }

        return cleanedList;
    }

    private Boolean isNeighbourOf(String ip, int port) {
        boolean found = false;
        for (Node i : addedNeighbours) {
            if (i.getIp().equals(ip) && i.getPort() == port) {
                found = true;
                break;
            }
        }
        return found;
    }

    @Override
    public String toString() {
        return "Node{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(this.port);

            while (true) {
                byte[] buffer = new byte[65536];
                DatagramPacket input = new DatagramPacket(buffer, buffer.length);

                try {
                    socket.receive(input);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                byte[] data = input.getData();
                String receivedMessage = new String(data, 0, input.getLength());
                StringTokenizer tokens = new StringTokenizer(receivedMessage, " ");
                String encodeLength = tokens.nextToken();

                switch (tokens.nextToken()) {

                    //handle join message
                    case "JOIN":
                        String nodeIP = receivedMessage.split(" ")[2];
                        int nodePort = Integer.parseInt(receivedMessage.split(" ")[3]);

                        if (!isNeighbourOf(nodeIP, nodePort)) {
                            addToRoutingTable(new Node(nodeIP, nodePort));
                        }
                        break;

                    //handle search message
                    case "SER":
                        QueryCounter.increase();
                        System.out.println("Node: " + getUsername() + " Queries: " + QueryCounter.getCounter());
                        String[] commandArgs = receivedMessage.split("\"");
                        String command = commandArgs[0];
                        String fileName = commandArgs[1];
                        int newHops = Integer.parseInt(commandArgs[2].trim()) + 1;

                        ArrayList<String> filesFound = new ArrayList<>();

                        for (String name : this.resources) {
                            for (String word : name.split(" ")) {
                                if (word.equalsIgnoreCase(fileName)) {
                                    filesFound.add(name);
                                    break;
                                }
                            }
                            if (name.equalsIgnoreCase(fileName)) {
                                filesFound.add(name);
                                break;
                            }
                        }

                        Set<String> searchResults = new HashSet<>(filesFound);

                        if (filesFound.isEmpty()) {
                            try {
                                if (newHops <= HOP_LIMIT) {
                                    this.searchWithNeighbours(fileName, command.split(" ")[2], command.split(" ")[3], String.valueOf(newHops));
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        else {
                            try {
                                sendFilePathToOrigin(searchResults, command.split(" ")[2], command.split(" ")[3], String.valueOf(newHops));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;

                    //handle search OK message
                    case "SEROK":
                        String[] result = receivedMessage.split("\"");
                        ArrayList<String> cleanedList = removeEmpty(result);
                        String resultCommand = cleanedList.get(0);
                        int resultsCount = Integer.parseInt(resultCommand.split(" ")[2]);
                        String sourceIP = resultCommand.split(" ")[3];
                        String sourcePort = resultCommand.split(" ")[4];
                        String totalHops = resultCommand.split(" ")[5];
                        System.out.println(resultsCount + " results found from " + sourceIP + ":" + sourcePort + " in " + totalHops + " hops");
                        System.out.print("File Names:");
                        for (int i = 1; i < cleanedList.size(); i++) {
                            System.out.print(cleanedList.get(i) + " ");
                        }
                        System.out.println("\n-----------------------------------------");

                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        System.out.println("Search end time: " + timestamp);

                        break;

                    //handle leave message
                    case "LEAVE":
                        String ip = receivedMessage.split(" ")[2];
                        int port = Integer.parseInt(receivedMessage.split(" ")[3]);
                        int nodeIndex = -1;

                        for (int i = 0; i < addedNeighbours.size(); i++) {
                            if (addedNeighbours.get(i).getIp().equals(ip) && port == addedNeighbours.get(i).getPort()) {
                                nodeIndex = i;
                            }
                        }

                        if (nodeIndex >= 0) {
                            addedNeighbours.remove(nodeIndex);
                            System.out.println("Removed node " + ip + ":" + port);
                            String request = "LEAVEOK 0";
                            String length = String.valueOf(request.length() + 5);
                            length = String.format("%4s", length).replace(' ', '0');
                            request = length + " " + request;
                            byte[] msg = request.getBytes();

                            InetAddress receiverIP;
                            try {
                                receiverIP = InetAddress.getByName(ip);

                                DatagramPacket packet = new DatagramPacket(msg, msg.length, receiverIP, port);
                                ds.send(packet);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println(ip + ":" + port + " not found to remove");
                        }
                        break;

                    //handle leave OK message
                    case "LEAVEOK":
                        if (receivedMessage.split(" ")[2].equals("0"))
                            System.out.println(this.ip + ":" + this.port + " left successfully");
                        else
                            System.out.println("Leave failed!");
                        break;

                    //handle gossip message
                    case "GOSSIP":
                        sendNeighbours(tokens);
                        break;

                    //handle gossip ok message
                    case "GOSSIPOK":
                        handleGossip(tokens);
                        break;

                    //handle is_alive message from availability checker
                    case "IS_ALIVE":
                        checkHeartbeat(tokens);
                        break;

                    //handle active message
                    case "ACTIVE":
                        addHeartBeatNeighbours(tokens);
                        break;

                    default:
                        System.out.println("Invalid Command");
                        break;
                }
            }
        } catch (BindException ex) {
            System.out.println("Already registered! Try a different one or un-register first");
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /**
     * node registering method
     */
    public void register() throws IOException {
        ds = new DatagramSocket();
        String msg = "REG " + this.ip + " " + this.port + " " + this.username;
        String length = String.valueOf(msg.length() + 5);
        length = String.format("%4s", length).replace(' ', '0');
        msg = length + " " + msg;
        byte b[] = msg.getBytes();

        InetAddress ip = InetAddress.getByName("192.168.8.101");
        int port = 55555;

        DatagramPacket packet = new DatagramPacket(b, b.length, ip, port);
        ds.send(packet);

        addNeighboursAfterRegister();
        printRoutingTable();
        join();

    }

    /**
     * node unregister
     */
    public void unregister() throws IOException {
        ds = new DatagramSocket();
        String msg = "UNREG " + this.ip + " " + this.port + " " + this.username;
        String length = String.valueOf(msg.length() + 5);
        length = String.format("%4s", length).replace(' ', '0');
        msg = length + " " + msg;
        byte b[] = msg.getBytes();

        InetAddress ip = InetAddress.getByName("192.168.8.101");
        int port = 55555;

        DatagramPacket packet = new DatagramPacket(b, b.length, ip, port);
        ds.send(packet);
        byte[] buffer = new byte[512];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        ds.receive(response);
        String responseMsg = new String(buffer, 0, response.getLength());
        String responseMsgArr[] = responseMsg.split(" ");

        if (responseMsgArr[1].equals("UNROK")) {
            if (responseMsgArr[2].equals("0"))
                System.out.println(this.ip + ":" + this.port + " UNREGISTER successful");
            else
                System.out.println("UNREGISTER successful!");
        }
    }

    /**
     * search a given file
     */
    public void search(String name) throws IOException {
        String msg = "SER " + this.ip + " " + this.port + " \"" + name + "\" 0";
        String length = String.valueOf(msg.length() + 5);
        length = String.format("%4s", length).replace(' ', '0');
        msg = length + " " + msg;
        byte b[] = msg.getBytes();

        for (Node n : addedNeighbours) {
            int port = n.getPort();
            InetAddress ip = InetAddress.getByName(n.getIp());
            DatagramPacket packet = new DatagramPacket(b, b.length, ip, port);
            ds.send(packet);
        }
    }

    /**
     * request neighbours to search with flooding
     */
    public void searchWithNeighbours(String fileName, String searcherIp, String searcherPort, String hops) throws IOException {
        String request = "SER " + searcherIp + " " + searcherPort + " \"" + fileName + "\" " + hops;
        String length = String.valueOf(request.length() + 5);
        length = String.format("%4s", length).replace(' ', '0');
        request = length + " " + request;
        byte b[] = request.getBytes();
        ds = new DatagramSocket();


        for (Node n : addedNeighbours) {
            int port = n.getPort();
            InetAddress ip = InetAddress.getByName(n.getIp());
            if (port != Integer.parseInt(searcherPort) || !n.getIp().equals(searcherIp)) {
                DatagramPacket packet = new DatagramPacket(b, b.length, ip, port);
                ds.send(packet);
            }
        }
    }

    /**
     * send the found file to requester
     */
    public void sendFilePathToOrigin(Set<String> fileName, String receiverIP, String receiverPort, String hops) throws IOException {
        StringBuilder filesStr = new StringBuilder();

        for (String result : fileName) {
            filesStr.append("\"").append(result).append("\" ");
        }

        String msg = "SEROK " + fileName.size() + " " + ip + " " + port + " " + hops + " " + filesStr;
        String length = String.valueOf(msg.length() + 5);
        length = String.format("%4s", length).replace(' ', '0');
        msg = length + " " + msg;
        byte b[] = msg.getBytes();
        ds = new DatagramSocket();

        InetAddress ip = InetAddress.getByName(receiverIP);
        int port = Integer.parseInt(receiverPort);

        DatagramPacket packet = new DatagramPacket(b, b.length, ip, port);
        ds.send(packet);
    }

    /**
     * adding the found nodes from server as neighbours
     */
    public void addNeighboursAfterRegister() throws IOException {
        byte[] buffer = new byte[512];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        ds.receive(response);      //get the server response
        String responseMsg = new String(buffer, 0, response.getLength());
        String[] responseMsgArgs = responseMsg.split(" ");

        if (responseMsgArgs[1].equals("REGOK")) {
            int availableNodes = Integer.parseInt(responseMsgArgs[2]);
            if (availableNodes == 9998) {
                System.out.println("Already registered. Please unregister first");
            } else if (availableNodes == 9999) {
                System.out.println("Failed. Error in the command");
            } else if (availableNodes == 9997) {
                System.out.println("Failed, registered to another user. Try a different Node");
            } else if (availableNodes == 9996) {
                System.out.println("Failed, can’t register. Bootstrap Server is full");
            } else if (availableNodes != 0) {
                for (int i = 3; i < responseMsgArgs.length; i += 2) {
                    String nodeIp = responseMsgArgs[i];
                    int nodePort = Integer.parseInt(responseMsgArgs[i + 1]);
                    if (!isNeighbourOf(nodeIp, nodePort)) {
                        addToRoutingTable(new Node(nodeIp, nodePort));
                    }
                }
                for (Node i : addedNeighbours)
                    System.out.println(this.port + ": Neighbours" + i.toString());
            } else {
                System.out.println(this.port + ": No neighbours yet");
            }
        }

    }

    /**
     * join the network
     */
    public void join() throws IOException {
        String request = "JOIN " + this.ip + " " + this.port;
        String length = String.valueOf(request.length() + 5);
        length = String.format("%4s", length).replace(' ', '0');
        request = length + " " + request;
        byte[] msg = request.getBytes();
        for (Node node : addedNeighbours) {
            InetAddress ip = InetAddress.getByName(node.getIp());
            int port = node.getPort();

            DatagramPacket packet = new DatagramPacket(msg, msg.length, ip, port);
            ds.send(packet);
        }
        printRoutingTable();
    }

    /**
     * display the routing table
     */
    public void printRoutingTable() {
        System.out.println("Routing table of " + ip + ":" + port);
        System.out.println("--------------------------------------");
        for (Node i : addedNeighbours) {
            System.out.println(i.getIp() + ":" + i.getPort());
        }
        System.out.println("--------------------------------------");
    }

    /**
     * print the serving files in current node
     */
    public void showResources() {
        System.out.println("Stored files at " + ip + ":" + port);
        System.out.println("---------------------------------------");
        resources.forEach(System.out::println);
    }

    /**
     * leave the network
     */
    public void leave() throws IOException {
        String request = "LEAVE " + this.ip + " " + this.port;
        String length = String.valueOf(request.length() + 5);
        length = String.format("%4s", length).replace(' ', '0');
        request = length + " " + request;
        byte[] msg = request.getBytes();
        for (Node node : addedNeighbours) {
            InetAddress ip = InetAddress.getByName(node.getIp());
            int port = node.getPort();

            DatagramPacket packet = new DatagramPacket(msg, msg.length, ip, port);
            ds.send(packet);
        }
        addedNeighbours.clear();
        unregister();
        System.exit(1);
    }

    /**
     * downloadFile file
     */
    public void downloadFile(String ip, String port, String name) throws IOException, NoSuchAlgorithmException {
        try {
            System.out.println("Started downloading...");
            URL url = new URL("http://" + ip + ":" + port + "/files/downloadFile?name=" + name);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(15000);
            con.setReadTimeout(15000);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            if (content.toString().length() > 0) {
                String workingDirectory = System.getProperty("user.dir");
                String path = workingDirectory + "/src/main/resources/static/downloaded/" + name.replace("%20", " ") + ".txt";
                FileOutputStream fileOutputStream = new FileOutputStream(path);
                fileOutputStream.write(content.toString().getBytes());

                Scanner scanner = new Scanner(new FileReader(path));
                StringBuilder sb = new StringBuilder();
                String outString;
                while (scanner.hasNext()) {
                    sb.append(scanner.next());
                }
                scanner.close();
                System.out.println("Download completed!");
                outString = sb.toString();

                //calculate the hash
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(outString.getBytes(StandardCharsets.UTF_8));
                String encoded = Base64.getEncoder().encodeToString(hash);
                System.out.println("Downloaded file hash:" + encoded);
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                System.out.println("Download end time: " + timestamp);
            } else {
                System.out.println("No data retrieved! File not may exist at node");
            }

        } catch (java.net.SocketTimeoutException e) {
            System.out.println("Connection timeout! Node may be down. Try another");
            removeNeighbour(ip, Integer.parseInt(port));
        } catch (ConnectException e) {
            System.out.println("Node is down. Try another!");
            removeNeighbour(ip, Integer.parseInt(port));
        } catch (MalformedURLException ex) {
            System.out.println("Error in the command");
        } catch (SocketException ex) {
            System.out.println("Connection lost! Node may be down");
        } catch (UnknownHostException ex) {
            System.out.println("Error in IP or PORT");
        }
    }

    /**
     * remove a existing neighbour
     */
    private void removeNeighbour(String ip, int port) {
        int removingNodeIndex = -1;
        for (int i = 0; i < addedNeighbours.size(); i++) {
            if (addedNeighbours.get(i).getIp().equals(ip) && addedNeighbours.get(i).getPort() == port) {
                removingNodeIndex = i;
            }
        }

        if (removingNodeIndex >= 0) {
            addedNeighbours.remove(removingNodeIndex);
        }
    }

    /**
     *
     * send relevant ips to requesting node
     */
    private void sendNeighbours(StringTokenizer st) {
        String senderIP = st.nextToken();
        int senderPort = Integer.parseInt(st.nextToken());
        Node senderNode = new Node(senderIP, senderPort, "");
        StringBuilder neighboursToBeSent = new StringBuilder();
        ArrayList<String> nodeKeys = new ArrayList<>();
        int count = 0;

        for (Node node : this.addedNeighbours) {
            nodeKeys.add(node.ip + ":" + node.port);
        }

        if (!nodeKeys.contains(senderNode.getKey())) {
            addToRoutingTable(senderNode);
            System.out.println("Node IP " + senderNode.ip + " Port " + senderNode.port + " added by Request");
        }


        if (this.addedNeighbours.size() > 1) {
            for (Node n : this.addedNeighbours) {
                if (!senderNode.equals(n.getIp(), n.getPort())) {
                    neighboursToBeSent.append(n.getIp()).append(" ").append(n.getPort()).append(" ");
                    count++;
                } else {
                    continue;
                }
            }
            neighboursToBeSent.substring(0, neighboursToBeSent.length() - 1);
            sendNeighboursToNeighbourMessage(senderNode, count, neighboursToBeSent.toString());
        }
    }


    /**
     *
     * encode gossipok message
     */
    public void sendNeighboursToNeighbourMessage(Node nodeToBeSent, int neighbourCount, String neighboursDetails) {
        InetAddress myIP;
        try {

            myIP = InetAddress.getByName(nodeToBeSent.getIp());
            int port = nodeToBeSent.getPort();
            String request = "GOSSIPOK " + this.ip + " " + this.port + " " + neighbourCount + " " + neighboursDetails;
            String length = String.valueOf(request.length() + 5);
            length = String.format("%4s", length).replace(' ', '0');
            request = length + " " + request;
            byte[] msg = request.getBytes();
            DatagramPacket packet = new DatagramPacket(msg, msg.length, myIP, port);
            ds.send(packet);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     *handle gossip response
     */
    private void handleGossip(StringTokenizer st) {

        if (this.addedNeighbours.size() < 3) {
            String senderIP = st.nextToken();
            int senderPort = Integer.parseInt(st.nextToken());
            int receivedNodesCount = Integer.parseInt(st.nextToken());
            ArrayList<String> nodeKeys = new ArrayList<>();

            Node senderNode = new Node(senderIP, senderPort, "");

            for (Node node : this.addedNeighbours) {
                nodeKeys.add(node.ip + ":" + node.port);
            }

            if (!nodeKeys.contains(senderNode.getKey())) {
                System.out.println("Node " + senderNode.ip + ":" + senderNode.port + " was added by Gossip");
                addToRoutingTable(senderNode);
            }

            for (int i = 0; i < receivedNodesCount; i++) {
                Node node = new Node(st.nextToken(), Integer.parseInt(st.nextToken()), "");

                if (nodeKeys.contains(node.getKey())) {
                    continue;
                } else {
                    if (!this.blacklist.contains(node.getKey())) {
                        addToRoutingTable(node);
                        System.out.println("Node " + node.ip + ":" + node.port + " was added by Gossip");
                    }
                }

            }

        }
    }

    /**
     *
     * process is active message from neighbour nodes
     */
    private void checkHeartbeat(StringTokenizer st) {

        String ip_of_sender = st.nextToken();
        int port_of_sender = Integer.parseInt(st.nextToken());
        Node senderNode = new Node(ip_of_sender, port_of_sender, "");
        sendNeighboursToHeartBeatMessage(senderNode);

    }


    /**
     *
     * response to is active request
     */
    private void sendNeighboursToHeartBeatMessage(Node nodeToBeSent) {
        InetAddress myIP;
        try {
            myIP = InetAddress.getByName(nodeToBeSent.getIp());
            int port = nodeToBeSent.getPort();
            String request = "ACTIVE " + this.ip + " " + this.port;
            String length = String.valueOf(request.length() + 5);
            length = String.format("%4s", length).replace(' ', '0');
            request = length + " " + request;
            byte[] msg = request.getBytes();
            DatagramPacket packet = new DatagramPacket(msg, msg.length, myIP, port);
            ds.send(packet);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * add active responses to hash map
     */
    public void addHeartBeatNeighbours(StringTokenizer st) {
        String senderIP = st.nextToken();
        int senderPort = Integer.parseInt(st.nextToken());
        Node senderNode = new Node(senderIP, senderPort, "");
        availableNeighbours.put(senderNode.ip + ":" + senderNode.port, senderNode);
    }


    private void addToRoutingTable(Node node) {
        ArrayList<String> nodeKeys = new ArrayList<>();
        for (Node nodeVal : this.addedNeighbours) {
            nodeKeys.add(nodeVal.ip + ":" + nodeVal.port);
        }
        if (nodeKeys.contains(node.getKey())) {
            return;
        } else {
            this.addedNeighbours.add(node);
        }
    }


    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }


}

