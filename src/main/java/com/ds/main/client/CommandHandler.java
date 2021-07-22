package com.ds.main.client;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class CommandHandler {
    private Node node;

    public CommandHandler(Node node) {
        this.node = node;
    }

    public void handleCommand(String command) throws IOException, NoSuchAlgorithmException {
        switch (command.split(" ")[0]) {
            case "routes":
                node.showRoutingTable();
                break;
            case "unregister":
                try {
                    node.unregister();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "register":
                try {
                    node.register();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "join":
                try {
                    node.join();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "files":
                node.showResources();
                break;
            case "search":
                try {
                    String[] commandArr = command.split(" ");
                    String fileName = "";
                    for (int i = 1; i < commandArr.length; i++)
                        fileName += " " + commandArr[i];
                    System.out.println("Searching:" + fileName.trim());
                    node.search(fileName.trim());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ArrayIndexOutOfBoundsException ex) {
                    System.out.println("Illegal command");
                }
                break;
            case "leave":
                node.leave();
                break;
            case "downloadFile":
                try {
                    String[] commandArgs = command.split(" ");
                    String ip = commandArgs[1];
                    String port = commandArgs[2];
                    StringBuilder fileName = new StringBuilder();

                    for (int i = 3; i < commandArgs.length; i++)
                        fileName.append(commandArgs[i]).append("%20");

                    fileName = new StringBuilder(fileName.substring(0, fileName.length() - 3));
                    System.out.println("File name: " + fileName);
                    node.download(ip, port, fileName.toString());
                } catch (ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException ex) {
                    System.out.println("Illegal command");
                }
                break;
            default:
                System.out.println("Command: " + command.split(" ")[0] + " not found!");
        }
    }
}
