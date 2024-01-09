package com.zoho_Inc.Quiz_App_Client;

import java.io.*;
import java.net.*;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Client {
	String plname;
	String password;
	private static Person p;

	public Client(String plName, String password) {
		this.plname = plName;
		this.password = password;
	}

	public static void main(String[] args) {
		String url = "localhost";
		int port = 1230;
		Scanner sc = new Scanner(System.in);
		System.out.println("\n  Enter your username to play the game: \n");
		String userName = sc.nextLine();
		System.out.println("\n  Enter your password to play the game: \n");
		String password = sc.nextLine();

		try (Socket socket = new Socket(url, port);ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
			Client client = new Client(userName, password);
			oos.writeObject(userName);
			oos.flush();

			oos.writeObject(password);
			oos.flush();

			// Receive the JSON string from the server using your preferred method (e.g.
			// ObjectInputStream)
			String receivedJson = (String) ois.readObject();

			// Deserialize the JSON string into a Person object
			ObjectMapper objectMapper = new ObjectMapper();
			p = objectMapper.readValue(receivedJson, Person.class);

			// Now, you have the received Person object
			System.out.println("Received Person: " + p);
			client.startGameHelper(sc, socket, oos, ois);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("An exception occurred.." + e.getMessage());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void startGameHelper(Scanner sc, Socket socket, ObjectOutputStream oos, ObjectInputStream ois) {
		try {
			PlayerManager pM = new PlayerManager();
			System.out.println("Connection there");
			while (true) {
				System.out.println("\nC: Enter your choice: ");
				int choice = PlayerManager.validChoices(sc);
				oos.writeInt(choice);
				oos.flush();

				switch (choice) {
				case 1: {
					// Play Quiz
					pM.playGameHelper(p, sc, socket, oos, ois, false, true, null, new ArrayList<Question>());
				}
					break;
				case 2: {
					// Play Timed Quiz
					pM.playGameHelper(p, sc, socket, oos, ois, true, true, null, new ArrayList<Question>());
				}
					break;
				case 3: {
					// Get All Categories
					pM.getAllCategories(oos, ois, new ArrayList<>(), new HashMap<>());
				}
					break;
				case 4: {
					// Get All Questions
					pM.getAllQuestions(oos, ois);
				}
					break;
				case 5: {
					// Get All Previous Quiz Log
					pM.getLog(p, sc, socket, oos, ois);
				}
					break;
				case 6: {
					// Get Top Players
					pM.getTopPlayersPercentWise(oos, ois);
				}
					break;
				case 7: {
					// Reattempt an Quiz which you have previously taken
					pM.attemptQuiz(p, sc, socket, oos, ois);
				}
					break;
				case 8: {
					// Exiting the Application
					System.out.println("Exiting Quiz Application. Goodbye!");
					System.exit(0);
				}
					break;
				default:
					System.out.println("Invalid choice. Please try again.");
					break;
				}

			}
		} catch (SocketException e) {
			// Handle the SocketException (Connection reset) gracefully
			e.printStackTrace();
			System.out.println("Client disconnected unexpectedly.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Person getPerson() {
		return p;
	}
	
//	public static void closeEverything(Closeable... closable) {
//		for(Closeable c : closable) {
//			try {
//				c.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//	}

//	public static void closeEverything(Socket socket, ObjectOutputStream objectOutputStream,
//			ObjectInputStream objectInputStream) {
//		try {
//			if (objectInputStream != null) {
//				objectInputStream.close();
//			}
//			if (objectOutputStream != null) {
//				objectOutputStream.close();
//			}
//			if (socket != null) {
//				socket.close();
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
}
