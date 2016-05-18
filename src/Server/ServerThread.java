package Server;

import java.io.*;
import java.net.*;

import Model.ElectionParameters;
import Model.ElectionResults;
import Model.Failure;
import Model.MyPublicKey;
import Model.MyVote;

public class ServerThread extends Thread {
	private Socket clientSocket = null;
	private Election election;

	public ServerThread(Socket socket, Election election) {
		super("Server");
		this.clientSocket = socket;
		this.election = election;
	}

	public void run() {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
			ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());

			Object received;
			System.out.println("server: thread started");
			while ((received = ois.readObject()) != null) {
				if (received instanceof MyPublicKey) { // received public key to check what pi is .. return L, M and pi
					MyPublicKey mpk = (MyPublicKey) received;
					ElectionParameters ep = election.getElectionParameters(mpk.bytes);
					
					System.out.println("Voter index " + ep.pi + " asked for election parameters");
					
					oos.writeObject(ep);
				} else if (received instanceof MyVote) {
					if (election.state.equals("voting")) {
						MyVote vote = (MyVote) received;
						System.out.println("server: received vote from : " + bytesToHex(vote.getYtilda()));
						boolean casted = election.CastVote(vote);
						oos.writeObject(casted);
					} else {
						oos.writeObject(new Failure("Election not in voting state. Can't vote now."));
					}
				} else if (received instanceof String) {
					String request = (String) received;
					switch (request) {
					case "results":
						if (election.state.equals("stopped")) {
							System.out.println("server: asked for results");
							String results = election.getResults();
							oos.writeObject(results);
						} else {
							oos.writeObject(new Failure("Election not finished. Can't get results now."));
						}
						break;
					case "results raw":
						if (election.state.equals("stopped")) {
							System.out.println("server: asked for results raw");
							ElectionResults er = new ElectionResults(election.getRawVotes());
							oos.writeObject(er);
						} else {
							oos.writeObject(new Failure("Election not finished. Can't get results now."));
						}
						break;
					default:
						oos.writeObject(new Failure("No such request supported."));
						break;
					}
				} else {
					oos.writeObject(new Failure("No such communication supported."));
				}
			}
			System.out.println("server: thread stopped");
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("connection lost");
		}
	}
	final protected static char[] hexArray = "0123456789abcdef".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 3];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 3] = hexArray[v >>> 4];
			hexChars[j * 3 + 1] = hexArray[v & 0x0F];
			hexChars[j * 3 + 2] = ' ';
		}
		return new String(hexChars);
	}

}
