package com.zoho_Inc.Quiz_App_Client;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.json.*;

public class PlayerManager {
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private static ScheduledFuture<?> timerHandle;
	static int TIME_LIMIT_SECONDS = 5; // Set the time limit for each question

	// Get Valid Choices
	static int validChoices(Scanner sc) {
		int choice = 0;
		while (true) {
			try {
				System.out.println("\t\n*** QUIZ APPLICATION MENU ***\n");
				System.out.println("\t1. Play Game");
				System.out.println("\t2. Play timed Game");
				System.out.println("\t3. View All Question Categories");
				System.out.println("\t4. View All Questions");
				System.out.println("\t5. Get Log");
				System.out.println("\t6. View Top Players");
				System.out.println("\t7. Retake a Quizz");
//              System.out.println("\t5. Play With Opponent");
				System.out.println("\t8. EXIT\n");
				System.out.print("Enter your choice: ");
				choice = sc.nextInt();
				break;
			} catch (Exception e) {
				System.out.println("Please enter a valid integer choice.\n");
				sc.next();
			}
		}
		return choice;
	}

	// Display All Questions
	public static void displayQuestions(List<Question> questions) {
		System.out.println("Questions:");
		for (Question question : questions) {
			System.out.println("ID: " + question.getId());
			System.out.println("Question: " + question.getQuestionText());
			System.out.println("Category ID: " + question.getCategoryId());
			System.out.println("Answer ID: " + question.getAnswerId());
			System.out.println("-----------------------------");
		}
	}

	// Display All Options
	static void displayOptions(List<Option> options) {
		for (Option option : options) {
			System.out.println(option);
		}
	}

	// Get All Previous Quiz Log Of the Player
	public void getLog(Person p, Scanner sc, Socket socket, ObjectOutputStream oos, ObjectInputStream ois) {
		try {
			oos.writeInt(p.getPersonId());
			oos.flush();

			String json = "";
			json = (String) ois.readObject();
			JSONArray jsonArray = null;
			jsonArray = new JSONArray(json);
			List<QuizLog> quizLogList = new ArrayList<>();
			quizLogList.clear();
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject quizLogListObject = jsonArray.getJSONObject(i);
				int quizId = quizLogListObject.getInt("quizId");
				int isTimed = quizLogListObject.getInt("isTimed");
				int categoryId = quizLogListObject.getInt("categoryId");
				int correctlyAnswered = quizLogListObject.getInt("correctlyAnswered");
				int totalQuestions = quizLogListObject.getInt("totalQuestions");
				int attempts = quizLogListObject.getInt("attempts");
				double percent = quizLogListObject.getDouble("percent");
				quizLogList.add(
						new QuizLog(quizId, isTimed, categoryId, correctlyAnswered, totalQuestions, attempts, percent));
			}

			if (quizLogList.isEmpty()) {
				System.out.println("There are no logs available..");
				return;
			}

			for (QuizLog qz : quizLogList) {
				// Send the quizId
//            	oos.writeInt(qz.getQuizId());
//            	oos.flush();

				List<Question> questionList = new ArrayList();
				List<Option> chosenOptionList = new ArrayList();
				getQuestionList(questionList, socket, oos, ois);
				getChosenOptions(chosenOptionList, socket, oos, ois);
//            	System.out.println("S: qzId: "+qz.getQuizId()+", qnListSz: "+questionList.size()+", chosenOpListSz: "+chosenOptionList.size());
				displayLogOfAPlayer(qz, questionList, chosenOptionList);
				System.out.println();
			}
		} catch (Exception e) {
			System.out.println("C: An Exception occured.." + e.getMessage());
		}
	}

	// Get All Questions
	private void getQuestionList(List<Question> questionList, Socket socket, ObjectOutputStream oos,
			ObjectInputStream ois) {
		try {
			String json = "";
			json = (String) ois.readObject();
			JSONArray jsonArray = null;
			jsonArray = new JSONArray(json);
			questionList.clear();
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject questionListObject = jsonArray.getJSONObject(i);
				int questionId = questionListObject.getInt("id");
				String questionText = questionListObject.getString("questionText");
				int answerId = questionListObject.getInt("answerId");
				questionList.add(new Question(questionId, questionText, -1, answerId));
			}
		} catch (Exception e) {
			System.out.println("C: An Exception occured.." + e.getMessage());
		}
	}

	// Get Previously Chosen Options
	private void getChosenOptions(List<Option> chosenOptionList, Socket socket, ObjectOutputStream oos,
			ObjectInputStream ois) {
		try {
			String json = "";
			json = (String) ois.readObject();
			JSONArray jsonArray = null;
			jsonArray = new JSONArray(json);
			chosenOptionList.clear();
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject optionListObject = jsonArray.getJSONObject(i);
				int optionId = optionListObject.getInt("optionId");
				String optionText = optionListObject.getString("optionText");
				chosenOptionList.add(new Option(optionId, optionText));
			}
		} catch (Exception e) {
			System.out.println("C: An Exception occured.." + e.getMessage());
		}
	}

	// Re-attempt an Already taken Quiz
	public void attemptQuiz(Person p, Scanner sc, Socket socket, ObjectOutputStream oos, ObjectInputStream ois) {
		try {
			// Get all previously attended Quiz Id's of the player
			oos.writeInt(p.getPersonId());
			oos.flush();

			List<QuizLog> quizLogList = new ArrayList();
			String json = "";
			json = (String) ois.readObject();
			JSONArray jsonArray = null;
			jsonArray = new JSONArray(json);
			quizLogList.clear();
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject quizLogObject = jsonArray.getJSONObject(i);
				int quizId = quizLogObject.getInt("quizId");
				int isTimed = quizLogObject.getInt("isTimed");
				int categoryId = quizLogObject.getInt("categoryId");
				int correctlyAnswered = quizLogObject.getInt("correctlyAnswered");
				int totalQuestions = quizLogObject.getInt("totalQuestions");
				int attempts = quizLogObject.getInt("attempts");
				double percent = quizLogObject.getInt("percent");
				quizLogList.add(
						new QuizLog(quizId, isTimed, categoryId, correctlyAnswered, totalQuestions, attempts, percent));
			}

			// Choose a particular qzLogId from the quizLog
			viewLog(quizLogList);
			System.out.println("Please enter an valid quizLogId..");
			int qLogId = sc.nextInt();
			oos.writeInt(qLogId);
			oos.flush();

			// Get the corresponding QuizLog you chose
			QuizLog quizLog = null;
			for (QuizLog qLog : quizLogList) {
				if (qLog.getQuizId() == qLogId) {
					quizLog = qLog;
					break;
				}
			}
			// Now retrieve the questionList corresponding to that quiz Id
			json = "";
			json = (String) ois.readObject();
			jsonArray = null;
			jsonArray = new JSONArray(json);
			List<Question> questionList = new ArrayList<>();
			questionList.clear();
			int cId = -1;
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject questionObject = jsonArray.getJSONObject(i);
				int id = questionObject.getInt("id");
				String questionText = questionObject.getString("questionText");
				int categoryId1 = questionObject.getInt("categoryId");
				cId = questionObject.getInt("categoryId");
				int answerId = questionObject.getInt("answerId");
				questionList.add(new Question(id, questionText, categoryId1, answerId));
			}
			System.out.println("C: QnListSZ: " + questionList.size());

			playGameHelper(p, sc, socket, oos, ois, false, false, quizLog, questionList);

		} catch (Exception e) {

		}
	}

	// Play Game
	public void playGameHelper(Person p, Scanner sc, Socket socket, ObjectOutputStream oos, ObjectInputStream ois,
			boolean isTimed, boolean isfirstAttempt, QuizLog quizLog, List<Question> attemptedQuestionList) {
		try {
			oos.writeInt(p.getPersonId());
			oos.flush();

			List<Category> categoryList = new ArrayList<>();
			int categoryId1 = -1;
			int fetchNumber = 2;
			if (isfirstAttempt) {
				Map<Integer, Integer> categoryMap = new HashMap();
				getAllCategories(oos, ois, categoryList, categoryMap);

				System.out.println("\nEnter the category ID: (or)\nTo fetch qns randomly, enter -1");
				categoryId1 = getUserInputInt(sc); // Read the entire line
//            	while(categoryId1<-1 || categoryId1>=categoryList.size()) {
//            		categoryId1 = getUserInputInt(sc);
//            	}

				oos.writeInt(categoryId1);
				oos.flush();

				if (categoryId1 >= 3)
					PlayerManager.TIME_LIMIT_SECONDS = 10;
				System.out.println("Enter the number of questions you want to fetch:");
				fetchNumber = getUserInputInt(sc); // Read the entire line
				oos.writeInt(fetchNumber);
				oos.flush();

				// Check if the questions are available or not
				int isQnsAvailable = ois.readInt();
				// If questions are not available, then return
				if (isQnsAvailable == -1) {
					System.out.println("\n There are no questions available in this category..\n");
					return;
				}
			} else {
				categoryId1 = quizLog.getCategoryId();
				oos.writeInt(categoryId1);
				oos.flush();
				fetchNumber = quizLog.getTotalQuestions();
				oos.writeInt(fetchNumber);
				oos.flush();
				isTimed = (quizLog.getIsTimed() == 1);
			}

			if (isTimed != true) {
				PlayerManager.playGame(p, categoryId1, oos, ois, socket, fetchNumber, false, isfirstAttempt,
						attemptedQuestionList);
			} else {
				PlayerManager.playGame(p, categoryId1, oos, ois, socket, fetchNumber, true, isfirstAttempt,
						attemptedQuestionList);
			}
		} catch (SocketException e) {
			System.out.println("exception occured");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// Play Game (By category, Timed Quiz / Normal game)
	static void playGame(Person p, int categoryId, ObjectOutputStream oos, ObjectInputStream ois, Socket socket,
			int randomFetchNumber, boolean isTimed, boolean isfirstAttempt, List<Question> attemptQuestionList) {
		try {
			List<Question> questionList = new ArrayList<>();
			if (isfirstAttempt) {
				String json = "";
				json = (String) ois.readObject();
				JSONArray jsonArray = null;
				jsonArray = new JSONArray(json);
				questionList.clear();
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject questionObject = jsonArray.getJSONObject(i);
					int id = questionObject.getInt("id");
					String questionText = questionObject.getString("questionText");
					int categoryId1 = questionObject.getInt("categoryId");
					int answerId = questionObject.getInt("answerId");
					questionList.add(new Question(id, questionText, categoryId1, answerId));
				}
			} else {
				questionList.addAll(attemptQuestionList);
			}

			if (questionList.isEmpty()) {
				System.out.println("There are no questions available..");
				return;
			}

			if (categoryId != -1 && randomFetchNumber > questionList.size()) {
				System.out
						.println("There are only " + questionList.size() + " questions available for this category..");
			}
//            System.out.println("C: Before qns List SZ: "+questionList.size());
			displayQuestionOptions(questionList, ois, oos, isTimed, isfirstAttempt);
//            System.out.println("C: Finally qns List SZ: "+questionList.size());
			p.setTotalWins(0);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Display Questions and the corresponding options in the game
	private static void displayQuestionOptions(List<Question> questionList, ObjectInputStream ois,
			ObjectOutputStream oos, boolean isTimed, boolean isfirstAttempt) {
		int score = 0;
		int currentIndex = 0;
		int[] scoreArray = new int[questionList.size()];
		int[] answerChoiceArray = new int[questionList.size()];
		Scanner sc = new Scanner(System.in);

		List<List<Option>> allOptionsList = new ArrayList<>();
		getAllOptionsForAQuestion(oos, ois, questionList, allOptionsList);

		while (currentIndex < questionList.size()) {
			Question question = questionList.get(currentIndex);
			List<Option> optionList = allOptionsList.get(currentIndex);

			System.out.println("\n  Question " + (currentIndex + 1) + ")   " + question.getQuestionText() + "\n");

			int start = 0;
			int end = start + 4;
			char c = 'a';
			displayOptions(optionList, start, end, c);

			if (answerChoiceArray[currentIndex] != 0) {
				displayPreviousChoice(optionList, answerChoiceArray[currentIndex]);
			}

			System.out.print("\n  Enter your choice (a/b/c/d): ");
			try {
				if (isTimed) {
					startTimer();
				}

				char userChoice = sc.next().charAt(0);
				int userChoiceId = validateUserChoice(optionList, userChoice);
//                if(!isTimed || (isTimed && !timeExpired))
				answerChoiceArray[currentIndex] = userChoiceId;

				if (isTimed) {
					stopTimer();
					if (!timeExpired) {
						System.out.println("Not timer expired");
						evaluateUserChoice(optionList, scoreArray, currentIndex, userChoiceId, question.getAnswerId());
					}
					System.out.println("Time Expired.. " + currentIndex);
					currentIndex++;
					timeExpired = false;
					continue;
				} else {
					evaluateUserChoice(optionList, scoreArray, currentIndex, userChoiceId, question.getAnswerId());

					System.out.println("\nSelect an option:");
					System.out.println("1. Move to the next question");
					System.out.println("2. Move to the previous question");
					System.out.println("3. Review Question");
					System.out.println("4. Exit game");

					int option = getUserInputInt(sc);
					switch (option) {
					case 1:
						currentIndex++;
						break;
					case 2:
						if (currentIndex > 0) {
							currentIndex--;
						} else {
							System.out.println("You are at the first question.");
						}
						break;
					case 3:
						currentIndex = 0;
						break;
					case 4:
						return;
					default:
						System.out.println("Invalid option. Please try again.");
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (isTimed) {
					stopTimer();
				}
			}
		}
		calculateScore(scoreArray, score, questionList, answerChoiceArray, ois, oos);
	}

	// Display All Options
	private static void displayOptions(List<Option> optionList, int start, int end, char c) {
		Map<Character, Integer> choiceIdMap = new HashMap<>();
		String correctAnswer = "";
		String correctAnswerChoice = "";
		int correctAnswerId = optionList.get(0).getOptionId();

		for (int j = start; j < end; j++) {
			if (j < optionList.size()) {
				Option option = optionList.get(j);

				if (correctAnswerId == option.getOptionId()) {
					correctAnswerChoice = "" + c;
					correctAnswer = option.getOptionText();
				}

				choiceIdMap.put(c, option.getOptionId());
				System.out.print("  " + c + ". " + option.getOptionText() + "\t");
				c++;
			}
		}

		System.out.println();
	}

	// Display Previously Selected Choice If Any
	private static void displayPreviousChoice(List<Option> optionList, int answerChoice) {
		for (Option o : optionList) {
			if (o.getOptionId() == answerChoice) {
				System.out.println("The previously selected option is " + o.getOptionText());
				break;
			}
		}
	}

	// To Get a Valid user Choice
	private static int validateUserChoice(List<Option> optionList, char userChoice) {
		while (true) {
			if (userChoice >= 'a' && userChoice <= 'd') {
				int userChoiceId = optionList.get(userChoice - 'a').getOptionId();
				return userChoiceId;
			} else {
				System.out.println("Invalid choice. Please enter a valid option (a/b/c/d): ");
				userChoice = new Scanner(System.in).next().charAt(0);
			}
		}
	}

	// Evaluate the user's choice with the actual correct choice
	private static void evaluateUserChoice(List<Option> optionList, int[] scoreArray, int currentIndex,
			int userChoiceId, int correctAnswerId) {
		if (userChoiceId == correctAnswerId) {
			scoreArray[currentIndex] = 1;
			System.out.println("\n  Correct Answer! Your current score is: " + scoreArray[currentIndex]);
		} else {
			scoreArray[currentIndex] = 0;
			System.out.println(
					"\n  Wrong Answer! The correct answer is: " + getCorrectAnswer(optionList, correctAnswerId));
		}
	}

	// Get the correct answer option text by mapping with its ID
	private static String getCorrectAnswer(List<Option> optionList, int correctAnswerId) {
		for (Option option : optionList) {
			if (option.getOptionId() == correctAnswerId) {
				return option.getOptionText();
			}
		}
		return "";
	}

	// Calculate the score
	private static void calculateScore(int[] scoreArray, int score, List<Question> questionList,
			int[] answerChoiceArray, ObjectInputStream ois, ObjectOutputStream oos) {

		for (int i = 0; i < scoreArray.length; i++) {
			if (scoreArray[i] == 1) {
				score++;
			}
			Question q = questionList.get(i);
			int questionId = q.getId();
			int selectedOptionId = answerChoiceArray[i];
			boolean isCorrect = (selectedOptionId == q.getAnswerId());
			try {
				// Send the questionId
				oos.writeInt(questionId);
				oos.flush();

				// Send the selectedOptionId
				oos.writeInt(selectedOptionId);
				oos.flush();

				// Send the isCorrect boolean value
				oos.writeBoolean(isCorrect);
				oos.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		float fractionScore = 100.0f / questionList.size();
		float percentageScore = fractionScore * score;

		System.out.println("\n\t**GAME OVER**\n");
		System.out.printf("  YOUR SCORE IS: %d / %d (%.0f%%)\n", score, questionList.size(), percentageScore);
	}

	// In a Timed game, the timerExpired will be initially false, & when it exceed
	// the time limit, its set to true
	private static boolean timeExpired = false;

	// Starts the timer i.e. Starts running for the given time limit (In a Timed
	// Quiz)
	private static void startTimer() {
		timerHandle = scheduler.schedule(() -> {
			System.out.println("\nTime's up! Moving to the next question.");
			timeExpired = true;
		}, TIME_LIMIT_SECONDS, TimeUnit.SECONDS);
	}

	// Stop the timer
	private static void stopTimer() {
		timerHandle.cancel(true);
	}

	// Get All Options
	private static void getAllOptionsForAQuestion(ObjectOutputStream oos, ObjectInputStream ois,
			List<Question> questionList, List<List<Option>> allOptionsList) {
		for (Question question : questionList) {
			List<Option> optionList = new ArrayList<>();
			String json3 = "";

			try {
				oos.writeInt(question.getId());
				oos.flush();
				json3 = (String) ois.readObject();
				JSONArray jsonArray3 = null;
				jsonArray3 = new JSONArray(json3);

				for (int j = 0; j < jsonArray3.length(); j++) {
					JSONObject optionObject = jsonArray3.getJSONObject(j);
					int id = optionObject.getInt("optionId");
					String optionText = optionObject.getString("optionText");
					optionList.add(new Option(id, optionText));
				}
				allOptionsList.add(new ArrayList<>(optionList));
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Get All Qustions
	public void getAllQuestions(ObjectOutputStream oos, ObjectInputStream ois) {
		String json = "";
		try {
			json = (String) ois.readObject();
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Parse the JSON array
		JSONArray jsonArray = new JSONArray(json);

		// Iterate through the questions
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject questionObject = jsonArray.getJSONObject(i);

			// Extract question details
			String questionText = questionObject.getString("questionText");

			// Display the question details
			System.out.println("\n  Question " + (i + 1) + ")  " + questionText);
		}
	}

	// Get All Categories
	public void getAllCategories(ObjectOutputStream oos, ObjectInputStream ois, List<Category> categoryList,
			Map<Integer, Integer> categoryMap) {
		String json = "";
		try {
			json = (String) ois.readObject();
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JSONArray jsonArray = null;
		jsonArray = new JSONArray(json);
		categoryList.clear();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject catgeoryObject = jsonArray.getJSONObject(i);
			int id = catgeoryObject.getInt("id");
			String categoryText = catgeoryObject.getString("category");
			categoryList.add(new Category(id, categoryText));
		}

		if (categoryList.isEmpty()) {
			System.out.println("There are no categories available..");
			return;
		}

		int categoryCount = 0;
		for (int i = 0; i < categoryList.size(); i++) {
			categoryCount++;
			Category c = categoryList.get(i);
			System.out.println("\n " + categoryCount + ". " + c.category);
			categoryMap.put(categoryCount, c.id);
		}
	}

	// Get the Top Players % wise
	public void getTopPlayersPercentWise(ObjectOutputStream oos, ObjectInputStream ois) {
		System.out.println("Enter the number of top player you want to fetch..");
		Scanner sc = new Scanner(System.in);
		int fetchNo = getUserInputInt(sc);
		try {
			oos.writeInt(fetchNo);
			oos.flush();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		List<Person> topPlayers = new ArrayList();
		String json = "";
		try {
			json = (String) ois.readObject();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		JSONArray jsonArray = new JSONArray(json);
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject personObject = jsonArray.getJSONObject(i);
			String personName = personObject.getString("name");
			int winCount = personObject.getInt("totalWins");
			int totalMatchesPlayed = personObject.getInt("totalQuizTaken");
			double personPercent = personObject.getDouble("winPercentage");
			Person p = new Person(0, personName, "", totalMatchesPlayed, winCount, 0, personPercent);
			topPlayers.add(p);
//            System.out.println("\n  Person " + personName + ",  "+personPercent);
		}
		displayTopPlayers(topPlayers);
	}

	// View the previous LOG History of the player
	public void viewLog(List<QuizLog> quizLogList) {
		System.out.println("Quiz Log:");
		System.out.printf("%-8s %-8s %-20s %-18s %-10s %-10s%n", "Quiz ID", "Is Timed", "Correctly Answered",
				"Total Questions", "Attempts", "Percent");
//        System.out.println("-------------------------------------------------------------");
		System.out.println(createSeparatorLine(8, 8, 20, 18, 10, 10));
		for (QuizLog quizLog : quizLogList) {
			int quizId = quizLog.getQuizId();
			int isTimed = quizLog.getIsTimed();
			int correctlyAnswered = quizLog.getCorrectlyAnswered();
			int totalQuestions = quizLog.getTotalQuestions();
			int attempts = quizLog.getAttempts();
			double percent = quizLog.getPercent();

			System.out.printf("%-8d %-8d %-20d %-18d %-10d %-10.2f%n", quizId, isTimed, correctlyAnswered,
					totalQuestions, attempts, percent);
		}
	}

	private void displayLogOfAPlayer(QuizLog qzLog, List<Question> questionList, List<Option> chosenOptionList) {
		System.out.printf("%-20s %-15s %-20s %-20s %-15s %-15s%n", "Quiz ID", "Is Timed", "Correctly Answered",
				"Total Questions", "Attempts", "Percentage");
		System.out.printf("%-20d %-15s %-20d %-20d %-15d %-15.2f%%%n", qzLog.getQuizId(), qzLog.getIsTimed(),
				qzLog.getCorrectlyAnswered(), qzLog.getTotalQuestions(), qzLog.getAttempts(), qzLog.getPercent());
		System.out.println("\n");

		System.out.println(createSeparatorLine(20, 15, 20, 20, 15, 15));
		System.out.printf("%-55s %-35s %-15s%n", "Question", "Chosen Option", "Result");
		System.out.println(createSeparatorLine(55, 35, 15));

		for (int i = 0; i < questionList.size(); i++) {
			Question question = questionList.get(i);
			Option chosenOption = chosenOptionList.get(i);

			String questionText = question.getQuestionText();
			String chosenOptionText = chosenOption.getOptionText();
			boolean isCorrect = question.getAnswerId() == chosenOption.getOptionId();

			System.out.printf("%-55s %-35s %-15s%n", questionText, chosenOptionText,
					isCorrect ? "Correct" : "Incorrect");
		}
		System.out.println("\n");
	}

	// Display Top Players from the List (% wise)
	private void displayTopPlayers(List<Person> topPlayers) {
		System.out.printf("\n\n  %-4s %-15s %-15s %-20s%n", "S.No", "Player", "WinCount", "TotalMatchesPlayed");
		System.out.println("  ----------------------------------------------");

		int serialNumber = 1;

		for (Person player : topPlayers) {
			String playerName = player.getName();
			int winCount = player.getTotalWins();
			int totalMatchesPlayed = player.getTotalQuizTaken();

			System.out.printf("  %-4d %-15s %-15d %-20d%n", serialNumber++, playerName, winCount, totalMatchesPlayed);
		}
	}

	// Get an Valid Input String
	private static String getUserInputString(Scanner sc) {
		String input = "";
		while (input.isEmpty()) {
//            System.out.print("Input should not be empty. Enter again: ");
			input = sc.next();
		}
		return input;
	}

	// Get an valid Integer input
	private static int getUserInputInt(Scanner sc) {
		int input = 0;
		boolean validInput = false;

		while (!validInput) {
			try {
				input = sc.nextInt();
				validInput = true;
			} catch (Exception e) {
				System.out.println("Invalid input. Please enter a valid integer.");
				sc.next(); // Consume the invalid input
			}
		}
		return input;
	}

	// To display the data neatly by separating and aligning them
	private String createSeparatorLine(int... widths) {
		StringBuilder separator = new StringBuilder();

		for (int width : widths) {
			separator.append("+");
			for (int i = 0; i < width; i++) {
				separator.append("-");
			}
		}

		separator.append("+");
		return separator.toString();
	}

	class Category {
		private int id;
		private String category;

		public Category(int id, String category) {
			super();
			this.id = id;
			this.category = category;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return "Category [id=" + id + ", category=" + category + "]";
		}

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}

	}
}
