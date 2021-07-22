package com.ds.main.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This sends a heartbeat message to notify
 * peers the node is alive
 */
public class Heartbeat extends Thread
{

	public static Node node;
	public static DatagramSocket ds;
	public static DatagramSocket socket;

	public static int threadStartDelay = 1000;
	public static int refreshPeriod = 10000;

	public Heartbeat( Node receivingNode )
	{
		node = receivingNode;
		ds = node.ds;
		socket = node.socket;
	}

	@Override
	public void run()
	{
		sendHeartbeat();
	}

	private static void sendHeartbeat()
	{
		Timer timer = new Timer();
		TimerTask task = new TimerTask()
		{
			@Override
			public void run()
			{
				forwardHeartbeatToNeighbours();
			}
		};
		timer.schedule( task, threadStartDelay, refreshPeriod );
	}

	private static void forwardHeartbeatToNeighbours()
	{
		if ( !node.addedNeighbours.isEmpty() )
		{
			ArrayList<Node> allNeighbours = new ArrayList<>( node.addedNeighbours);

			for ( Node node : allNeighbours )
			{
				sendHeartbeatToNeighbours( node );
			}

		}

	}

	private static void sendHeartbeatToNeighbours( Node nodeToBeSent )
	{
		try
		{
			ds = new DatagramSocket();
			InetAddress nodeIp = InetAddress.getByName( nodeToBeSent.getIp() );
			int senderPort = nodeToBeSent.getPort();
			String requestStr = "IS_ALIVE " + node.ip + " " + node.port;
			String requestSize = String.valueOf( requestStr.length() + 5 );
			requestSize = String.format( "%4s", requestSize ).replace( ' ', '0' );
			requestStr = requestSize + " " + requestStr;
			byte[] msg = requestStr.getBytes();
			DatagramPacket packet = new DatagramPacket( msg, msg.length, nodeIp, senderPort );
			ds.send( packet );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

}
