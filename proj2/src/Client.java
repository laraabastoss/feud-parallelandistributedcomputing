import java.net.*;
import java.io.*;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.UUID;

/**
 * This program demonstrates a simple TCP/IP socket client.
 *
 * @author www.codejava.net
 */
public class Client {
    enum State {
        START,
        LOGIN,
        REGISTER,
        WAIT,
        PLAYING

    }

    Socket socket;
    static BufferedReader reader;
    static PrintWriter writer;

    private CommunicationChanelAction chanel;
    static State currentState = State.START;
    public String username;
    static String token;


    public  Client(Socket socket)  {
        try {
            this.socket = socket;

            InputStream input = socket.getInputStream();
            this.reader = new BufferedReader(new InputStreamReader(input));

            OutputStream output = socket.getOutputStream();
            this.writer = new PrintWriter(output, true);

            this.chanel = new CommunicationChanelAction(reader, writer);
        } catch (IOException ex) {
            System.out.println("Error creating client: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {

        if (args.length < 2) return;

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        currentState = State.START;

        try (Socket socket = new Socket(hostname, port)) {

            Client client = new Client(socket);
            client.run();

        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        }
        catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    public void run() {

        try {

            System.out.println("-------------------------------------------------------------------------------" );
            System.out.println("                     Welcome to the FEUP Trivia Game                           " );
            System.out.println("-------------------------------------------------------------------------------" );

            while (true) {

                String line;
                line = chanel.read();
                if (line == null ){
                    System.out.println("Server disconected.");
                    return;
                }
                while (line.equals("CHECK"))  {

                    chanel.send(CommunicationChanelAction.Message.CHECK);
                    line = chanel.read();

                }


                if (line == "" ){
                    System.out.println("Server disconected.");
                    return;
                }

                switch (currentState) {

                    case START:


                        if (line.equals("AUTH")) {

                            System.out.println("Start with authentication");
                            System.out.println("\n1 - Register\n2 - Login");

                            Scanner scannerAnswer = new Scanner(System.in);

                            String choice = scannerAnswer.nextLine();

                            while (!(choice.equals("2")  || choice.equals("1"))) {
                                System.out.println("Invalid choice.\nChoose a number between 1 and 2.");
                                choice = scannerAnswer.nextLine();
                            }



                            System.out.print("\nUsername: ");
                            String temp_username = scannerAnswer.nextLine();

                            System.out.print("Password: ");
                            String password = scannerAnswer.nextLine();

                            if ( choice.equals("1")) {
                                chanel.send(CommunicationChanelAction.Message.REGISTER, temp_username, password);
                                currentState = State.REGISTER;
                            }
                            if ( choice.equals("2")) {
                                chanel.send(CommunicationChanelAction.Message.LOGIN, temp_username, password);
                                currentState = State.LOGIN;
                            }

                        }
                        break;

                    case REGISTER:

                        String message_register = reader.readLine();
                        if (line.equals("APP_AUTH")) {
                            System.out.println(message_register);
                            username = reader.readLine();
                            currentState = State.WAIT;
                        }
                        if (line.equals("REJ_AUTH")) {
                            System.out.println(message_register);
                            currentState = State.START;
                        }

                        break;

                    case LOGIN:
                        String message_login = reader.readLine();

                        if (line.equals("APP_AUTH")) {
                            System.out.println(message_login);
                            username = reader.readLine();
                            currentState = State.WAIT;
                        }

                        if (line.equals("REJ_AUTH")) {
                            System.out.println(message_login);
                            currentState = State.START;
                        }

                        if (line.equals("RECONNECTED")){
                            System.out.println("Reconnected!");
                            username = message_login;

                            currentState = State.WAIT;
                        }

                        break;

                    case WAIT:

                        System.out.println("Please wait for a new game to start!");

                        if (line.equals("GAME")) {
                            currentState = State.PLAYING;
                        } else break;

                    case PLAYING:

                        System.out.println("-------------------------------------------------------------------------------" );
                        System.out.println("                           New Game Starting                                   " );


                        int result = this.startGame();
                        String afterGameMessage = chanel.read();


                        if (afterGameMessage.equals("CHECK_RESULT")) {
                            if (result == 1) chanel.send(CommunicationChanelAction.Message.WON, username);
                            if (result == -1) chanel.send(CommunicationChanelAction.Message.LOST, username);
                            if (result == 0) chanel.send(CommunicationChanelAction.Message.DRAW, username);
                            if (result == -3) chanel.send(CommunicationChanelAction.Message.DISCONNECTED, username);
                            if (result == -4) return;
                            if (result == -2) {
                                System.out.println("Server Disconnected");
                                return;
                            }
                        }

                        String finalChoice = chanel.read();

                        if (finalChoice.equals("LOGOUT_OR_NOT")) {

                            System.out.println("-------------------------------------------------------------------------------" );
                            System.out.println("           Thank you for joining! What do you want to do now?                    ");
                            System.out.println("1 - Play Again");
                            System.out.println("2 - Logout");

                            Scanner scannerAnswer = new Scanner(System.in);
                            int answer = scannerAnswer.nextInt();

                            while (answer > 2 || answer < 1) {

                                System.out.println("Invalid option.\nChoose a number between 1 and 2.");
                                answer = scannerAnswer.nextInt();

                            }
                            if (answer == 1) chanel.send(CommunicationChanelAction.Message.PLAY_AGAIN);
                            else if (answer == 2){
                                chanel.send(CommunicationChanelAction.Message.LOGOUT);
                                System.out.println("Thank you for joining\nSee you next time.");
                                return;
                            }

                        }
                        currentState = State.WAIT;
                        break;

                }

            }
        } catch (IOException ex) {
            System.out.println("Error creating client: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    private int startGame() {

        while (true) {

            chanel.send(CommunicationChanelAction.Message.CHECK);

            String line = chanel.read();
            if (line.equals("")){
                return -2;
            }

            switch (line) {
                case "QUESTION":
                    String board = chanel.readBoard();
                    System.out.println("It's your team's turn!");
                    System.out.println(board);
                    line = chanel.read();
                    System.out.println(line);
                    String options = chanel.readOptions();
                    System.out.println(options);
                    System.out.println("Choose the correct answer:" );
                    Scanner scannerAnswer = new Scanner(System.in);
                    if (!scannerAnswer.hasNextLine()) return -4;
                    String answer = scannerAnswer.nextLine();
                    while (!(answer.equals("1") || answer.equals("2") || answer.equals("3") || answer.equals("4"))) {

                        System.out.println("Invalid answer.\nChoose a number between 1 and 4.");
                        answer = scannerAnswer.nextLine();

                    }
                    String numPlayer = chanel.read();
                    if ( !numPlayer.equals("2")) System.out.println("Wait for the entire team to decide..." );
                    chanel.send(CommunicationChanelAction.Message.ANSWER,answer);
                    break;

                case "OCCUPIED":
                    System.out.println("Occupied position. Please insert an available position." );
                    getRowAndColumns();
                    break;

                case "WRITE_PLAY":
                    System.out.println("Correct!\nYou can position piece now");
                    getRowAndColumns();

                    break;

                case "OTHERPLAYER":
                    board = chanel.readBoard();
                    System.out.println("It's you opponent's turn:\n");
                    System.out.println(board);
                    chanel.send(CommunicationChanelAction.Message.WAIT);
                    break;

                case "WRONG":
                    System.out.println("Wrong :(\nYour turn will be skiped." );
                    break;

                case "WON":
                    board = chanel.readBoard();
                    System.out.println(board);
                    System.out.println("                  Congratulations! You won the game                            " );
                    return 1;

                case "LOST":
                    board = chanel.readBoard();
                    System.out.println(board);
                    System.out.println("                Too bad! Your opponent won the game.                            " );
                    return -1;

                case "DRAW":
                    board = chanel.readBoard();
                    System.out.println(board);
                    System.out.println("It's a draw! Could be worse:/" );
                    return 0;

                case "DISCONNECTED":
                    String user = chanel.read();
                    System.out.println(user + " got disconnected :(\nYou will soon enter a new game!");
                    return -3;

                case "TEAMCORRECT":
                    System.out.println("Your team choose correct!\nAnother player from your team will now position the piece." );
                    break;

                default:
                    System.out.println("Waiting for other player to play..." );
                    break;
            }

        }

    }


    public void setUsername(String username){
        this.username = username;
    }



    public String getUsername(){
        return this.username;
    }

    public CommunicationChanelAction getCommunicationChanel(){
        return this.chanel;
    }
    public  void  getRowAndColumns(){

        System.out.println("Insert a Row:" );
        Scanner scannerRow = new Scanner(System.in);
        int row = scannerRow.nextInt();
        while (row > 3 || row < 1) {
            System.out.println("Invalid row.\nChoose a number between 1 and 3.");
            row = scannerRow.nextInt();
        }
        chanel.send(CommunicationChanelAction.Message.ROW,row);

        Scanner scannerColumn = new Scanner(System.in);
        System.out.println("Insert a Column:" );
        int column = scannerColumn.nextInt();
        while (column > 3 || column < 1) {
            System.out.println("Invalid column.\nChoose a number between 1 and 3.");
            column = scannerColumn.nextInt();
        }
        chanel.send(CommunicationChanelAction.Message.COLUMN, column);

    }


}