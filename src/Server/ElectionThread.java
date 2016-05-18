package Server;

import java.util.InputMismatchException;
import java.util.Scanner;

public class ElectionThread extends Thread {
	private Election election;
	private Scanner scanner;

	public ElectionThread(Election election) {
		this.election = election;
	}

	public void run() {
		scanner = new Scanner(System.in);
		while (true) {
			System.out.println("Press 0 to refresh");
			System.out.println("Press 1 for election info");

			if (election.state.equals("initial"))
				System.out.println("Press 2 to start election");

			if (election.state.equals("voting"))
				System.out.println("Press 3 to stop election");

			if (election.state.equals("stopped"))
				System.out.println("Press 4 to print results");
			
			System.out.println("Press 5 to close application");

			try {
				int option = scanner.nextInt();
				switch (option) {
				case 0:
					System.out.println();
					break;
				case 1:
					System.out.println();
					System.out.println(election.getElectionInfo());
					System.out.println();
					break;
				case 2:
					if (election.state.equals("voting")) {
						System.out.println("Election already started");
						break;
					}
					if (election.state.equals("stopped")) {
						System.out.println("Can't start election again. Restart application ");
						break;
					}
					// if in initial state
					election.startElection();
					break;
				case 3:
					if (election.state.equals("initial")) {
						System.out.println("Election not started yet. Start election first");
						break;
					}
					if (election.state.equals("stopped")) {
						System.out.println("Election stopped already");
						break;
					}
					// if in voting state
					election.stopElection();
					break;
				case 4:
					if (!election.state.equals("stopped")) {
						System.out.println("Election not finished. Can't get results yet");
						break;
					}
					// if election stopped
					System.out.println();
					System.out.println(election.getResults());
					System.out.println();
					break;
				case 5 :
					System.exit(0);
					break;
				default:
					throw new InputMismatchException();
				}
			} catch (InputMismatchException e) {
				System.out.println("please type in an integer between 0 and 5");
			}
		}
	}
}
