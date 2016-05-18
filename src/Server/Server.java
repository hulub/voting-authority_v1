package Server;
import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;

public class Server {

	private static int port = 7777;
	private static int counter = 0;
	private static ServerSocket serverSocket;
	
	public static void main(String[] args) {
		
		try
		{
			Election election = new Election();
			Thread electionManager = new ElectionThread(election);
			electionManager.start();
			serverSocket = new ServerSocket(port);
			System.out.println("Listening on:" + serverSocket.getInetAddress().getHostAddress() + " port:" + serverSocket.getLocalPort());
			while(true)
			{
				//Here you create connections with other clients
				new ServerThread(serverSocket.accept(), election).start();
				counter++;
				System.out.println("Number of clients: " + counter);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Problem initializing election");
		}
	}
}
