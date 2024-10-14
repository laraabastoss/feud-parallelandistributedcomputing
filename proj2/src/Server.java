import java.util.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * This program demonstrates a simple TCP/IP socket server.
 *
 * @author www.codejava.net
 */

/*
COMPILE: javac *.java
RUN: java -Server 8000
java Client localhost 8000

 */

public class Server {

    static int clients = 0;
    public static String databasePath = "database/db.txt";

    protected static DataBase database = new DataBase(databasePath);

    static Queue<Client> waiting_queue = new ArrayDeque<>() ;
    static Queue<Client> highWaitingQueue = new ArrayDeque<>();
    static Queue<Client> mediumWaitingQueue = new ArrayDeque<>();
    static Queue<Client> lowWaitingQueue = new ArrayDeque<>();
    static Queue<Client> beginnerWaitingQueue = new ArrayDeque<>();
    static Queue<CommunicationChanelAction> communicationChanel = new ArrayDeque<>() ;
    static Queue<CommunicationChanelAction> highCommunicationChanel = new ArrayDeque<>() ;
    static Queue<CommunicationChanelAction> mediumCommunicationChanel = new ArrayDeque<>() ;
    static Queue<CommunicationChanelAction> lowCommunicationChanel = new ArrayDeque<>() ;
    static Queue<CommunicationChanelAction> beginnerCommunicationChanel = new ArrayDeque<>() ;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();



    protected static List<TicTacToeThread> activeGameThreads = new ArrayList<>();
    static int numPlayers;
    static int gameMode;
    static int relaxed = 0;

    public static void main(String[] args) {
        if (args.length < 1) return;

        int port = Integer.parseInt(args[0]);
        Server server = new Server();
        server.start(port);

    }


    public void start(int port) {


        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server is listening on port " + port);

            System.out.println("Choose a number of players per game ( >= 2 ) :" );

            Scanner scanner = new Scanner(System.in);

            this.numPlayers = scanner.nextInt();

            while (numPlayers < 2)  {
                System.out.println("Invalid game mode.\nChoose a number between 1 and 2.");
                numPlayers = scanner.nextInt();
            }

            System.out.println("Choose a game mode:" );
            System.out.println("1 - Simple" );
            System.out.println("2 - Ranking" );



            gameMode = scanner.nextInt();

            while (gameMode > 2 || gameMode < 0) {
                System.out.println("Invalid game mode.\nChoose a number between 1 and 2.");
                gameMode = scanner.nextInt();
            }

            System.out.println("-------------------------------------------------------------------------------" );
            System.out.println("                     Started the FEUP Trivia Server                            " );
            System.out.println("-------------------------------------------------------------------------------" );

            AuthenticationThread authenticationThread = new AuthenticationThread(this, serverSocket);
            Thread.ofVirtual().start(authenticationThread);

            long start = System.nanoTime();
            int cycleCounter = 1;


            while (true) {

                long elapsedTimeMinutes = (System.nanoTime() - start) / (60 * 1000000000L);

                if (elapsedTimeMinutes >= cycleCounter) {
                    relaxed = (relaxed + 1) % 4;
                    incrementClient(0);
                    cycleCounter++;
                }

            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }


    }

    public synchronized void onGameFinished(TicTacToeThread gameThread) {
        System.out.println("");
        System.out.println("Game Finished!");
        System.out.println("");
        System.out.println("Updates scores:");
        database.displayPlayers();

        lock.writeLock().lock();
        activeGameThreads.remove(gameThread);
        database.updateDatabase(databasePath);
        lock.writeLock().unlock();
    }

    public  void requeuePlayer(Client client,  CommunicationChanelAction chanel) {
        lock.readLock().lock();
            if (gameMode == 1){
                waiting_queue.add(client);
                communicationChanel.add(chanel);
            }
            else{

                addToRankingWaitingQueue(client,chanel, database.getClientInfo(client.getUsername()).score);

            }
        lock.readLock().unlock();
            chanel.send(CommunicationChanelAction.Message.WAIT);
            incrementClient(1);

    }

    public  void incrementClient(int increment){

        clients += increment;
        if (gameMode == 1) {


            if (clients == numPlayers && allPlayersOn("normal",numPlayers)) {


                List<Client> players = removeWaitingQueue(waiting_queue, numPlayers);
                List<CommunicationChanelAction> chanels = removeWaitingChanel(communicationChanel, numPlayers);
                startGameThread(players, chanels);

                clients -= numPlayers;

            }

        }
        else{

            if (clients >= numPlayers){

                if (highWaitingQueue.size() >= numPlayers && allPlayersOn("high", numPlayers)){

                    List<Client> players = removeWaitingQueue(highWaitingQueue, numPlayers);
                    List<CommunicationChanelAction> chanels = removeWaitingChanel(highCommunicationChanel, numPlayers);
                    startGameThread(players, chanels);
                    clients -= numPlayers;
                } else if (mediumWaitingQueue.size() >= numPlayers && allPlayersOn("medium", numPlayers)) {
                    List<Client> players = removeWaitingQueue(mediumWaitingQueue, numPlayers);
                    List<CommunicationChanelAction> chanels = removeWaitingChanel(mediumCommunicationChanel, numPlayers);
                    startGameThread(players, chanels);
                    clients -= numPlayers;
                } else if (lowWaitingQueue.size() >= numPlayers && allPlayersOn("low", numPlayers)) {
                    List<Client> players = removeWaitingQueue(lowWaitingQueue, numPlayers);
                    List<CommunicationChanelAction> chanels = removeWaitingChanel(lowCommunicationChanel, numPlayers);
                    startGameThread(players, chanels);
                    clients -= numPlayers;
                } else if (beginnerWaitingQueue.size() >= numPlayers && allPlayersOn("beginner", numPlayers)){
                    List<Client> players = removeWaitingQueue(beginnerWaitingQueue, numPlayers);
                    List<CommunicationChanelAction> chanels = removeWaitingChanel(beginnerCommunicationChanel, numPlayers);
                    startGameThread(players, chanels);
                    clients -= numPlayers;
                } else if (highWaitingQueue.size() + mediumWaitingQueue.size() >= numPlayers && relaxed > 0 && allPlayersOn("high", highWaitingQueue.size()) && allPlayersOn("medium", numPlayers -highWaitingQueue.size())) {
                    int highPlayers = highWaitingQueue.size();
                    int mediumPlayers = numPlayers - highPlayers;
                    List<Client> players = removeWaitingQueue(highWaitingQueue, highPlayers);
                    List<CommunicationChanelAction> chanels = removeWaitingChanel(highCommunicationChanel, highPlayers);
                    players.addAll(removeWaitingQueue(mediumWaitingQueue, mediumPlayers));
                    chanels.addAll(removeWaitingChanel(mediumCommunicationChanel, mediumPlayers));
                    startGameThread(players, chanels);
                    clients -= numPlayers;
                } else if (mediumWaitingQueue.size() + lowWaitingQueue.size() >= numPlayers && relaxed > 0 && allPlayersOn("medium", mediumWaitingQueue.size()) && allPlayersOn("low", numPlayers -mediumWaitingQueue.size())) {
                    int mediumPlayers = mediumWaitingQueue.size();
                    int lowPlayers = numPlayers - mediumPlayers;
                    List<Client> players = removeWaitingQueue(mediumWaitingQueue, mediumPlayers);
                    List<CommunicationChanelAction> chanels = removeWaitingChanel(mediumCommunicationChanel, mediumPlayers);
                    players.addAll(removeWaitingQueue(lowWaitingQueue, lowPlayers));
                    chanels.addAll(removeWaitingChanel(lowCommunicationChanel, lowPlayers));
                    startGameThread(players, chanels);
                    clients -= numPlayers;
                } else if (lowWaitingQueue.size() + beginnerWaitingQueue.size() >= numPlayers && relaxed > 0 && allPlayersOn("low", lowWaitingQueue.size()) && allPlayersOn("beginner", numPlayers -lowWaitingQueue.size())) {
                    int lowPlayers = lowWaitingQueue.size();
                    int beginnerPlayers = numPlayers - lowPlayers;
                    List<Client> players = removeWaitingQueue(lowWaitingQueue, lowPlayers);
                    List<CommunicationChanelAction> chanels = removeWaitingChanel(lowCommunicationChanel, lowPlayers);
                    players.addAll(removeWaitingQueue(beginnerWaitingQueue, beginnerPlayers));
                    chanels.addAll(removeWaitingChanel(beginnerCommunicationChanel, beginnerPlayers));
                    startGameThread(players, chanels);
                    clients -= numPlayers;
                } else if (highWaitingQueue.size() + mediumWaitingQueue.size() + lowWaitingQueue.size() >= numPlayers && relaxed > 1  && allPlayersOn("high", highWaitingQueue.size()) && allPlayersOn("medium", mediumWaitingQueue.size()) && allPlayersOn("low", numPlayers -highWaitingQueue.size() - mediumWaitingQueue.size())) {
                    int highPlayers = highWaitingQueue.size();
                    int mediumPlayers = mediumWaitingQueue.size();
                    int lowPlayers = numPlayers - highPlayers - mediumPlayers;
                    List<Client> players = removeWaitingQueue(highWaitingQueue, highPlayers);
                    List<CommunicationChanelAction> chanels = removeWaitingChanel(highCommunicationChanel, highPlayers);
                    players.addAll(removeWaitingQueue(mediumWaitingQueue, mediumPlayers));
                    chanels.addAll(removeWaitingChanel(mediumCommunicationChanel, mediumPlayers));
                    players.addAll(removeWaitingQueue(lowWaitingQueue, lowPlayers));
                    chanels.addAll(removeWaitingChanel(lowCommunicationChanel, lowPlayers));
                    startGameThread(players, chanels);
                    clients -= numPlayers;
                } else if (beginnerWaitingQueue.size() + mediumWaitingQueue.size() + lowWaitingQueue.size() >= numPlayers && relaxed > 1 && allPlayersOn("low", lowWaitingQueue.size()) && allPlayersOn("medium", mediumWaitingQueue.size()) && allPlayersOn("begginer", numPlayers -lowWaitingQueue.size() - mediumWaitingQueue.size())) {
                    int lowPlayers = lowWaitingQueue.size();
                    int mediumPlayers = mediumWaitingQueue.size();
                    int beginnerPlayers = numPlayers - lowPlayers - mediumPlayers;
                    List<Client> players = removeWaitingQueue(beginnerWaitingQueue, beginnerPlayers);
                    List<CommunicationChanelAction> chanels = removeWaitingChanel(beginnerCommunicationChanel, beginnerPlayers);
                    players.addAll(removeWaitingQueue(mediumWaitingQueue, mediumPlayers));
                    chanels.addAll(removeWaitingChanel(mediumCommunicationChanel, mediumPlayers));
                    players.addAll(removeWaitingQueue(lowWaitingQueue, lowPlayers));
                    chanels.addAll(removeWaitingChanel(lowCommunicationChanel, lowPlayers));
                    startGameThread(players, chanels);
                    clients -= numPlayers;
                } else if (relaxed > 2 && allPlayersOn("low", lowWaitingQueue.size()) && allPlayersOn("medium", mediumWaitingQueue.size()) && allPlayersOn("high", highWaitingQueue.size())&& allPlayersOn("low", numPlayers -highWaitingQueue.size() - mediumWaitingQueue.size() - lowWaitingQueue.size())) {
                    int highPlayers = highWaitingQueue.size();
                    int lowPlayers = lowWaitingQueue.size();
                    int mediumPlayers = mediumWaitingQueue.size();
                    int beginnerPlayers = numPlayers - lowPlayers - mediumPlayers - highPlayers;
                    List<Client> players = removeWaitingQueue(highWaitingQueue, highPlayers);
                    List<CommunicationChanelAction> chanels = removeWaitingChanel(highCommunicationChanel, highPlayers);
                    players.addAll(removeWaitingQueue(beginnerWaitingQueue, beginnerPlayers));
                    chanels.addAll(removeWaitingChanel(beginnerCommunicationChanel, beginnerPlayers));
                    players.addAll(removeWaitingQueue(mediumWaitingQueue, mediumPlayers));
                    chanels.addAll(removeWaitingChanel(mediumCommunicationChanel, mediumPlayers));
                    players.addAll(removeWaitingQueue(lowWaitingQueue, lowPlayers));
                    chanels.addAll(removeWaitingChanel(lowCommunicationChanel, lowPlayers));
                    startGameThread(players, chanels);
                    clients -= numPlayers;
                }

            }

        }

    }

    public List<Client> removeWaitingQueue(Queue<Client> players, int removePlayers){

        List<Client> gamePlayers = new ArrayList<>();

        for (int i = 0; i < removePlayers; i++) {

            if (players.size()>0) {
                Client player = players.remove();
                gamePlayers.add(player);
            }


        }

        return gamePlayers;
    }

    public List<CommunicationChanelAction> removeWaitingChanel(Queue<CommunicationChanelAction> chanels, int removePlayers){

        List<CommunicationChanelAction> gameChanels = new ArrayList<>();

        for (int i = 0; i < removePlayers; i++) {

            CommunicationChanelAction communicationChanelVar = chanels.remove();
            gameChanels.add(communicationChanelVar);

        }

        return gameChanels;

    }

    public void startGameThread(List<Client> players, List<CommunicationChanelAction> chanels){

        TicTacToeThread gameThread = (new TicTacToeThread(numPlayers, this));

        for (int i = 0; i < numPlayers; i++){
            gameThread.addPlayers(players.get(i));
            gameThread.addCommuncationChannel(chanels.get(i));
        }

        System.out.println("New game thread created.\n");
        activeGameThreads.add(gameThread);
        Thread.ofVirtual().start(gameThread);

    }

    public void addToWaitingQueue(Client client, CommunicationChanelAction chanel){
        this.waiting_queue.add(client);
        this.communicationChanel.add(chanel);

    }

    public void addToWaitingQueue(Client client, CommunicationChanelAction chanel, String username){

        Queue<Client> tempClients = new ArrayDeque<Client>();
        Queue<CommunicationChanelAction> tempChanel = new ArrayDeque<CommunicationChanelAction>();

        for (Client currClient : this.waiting_queue){
            if (username.equals(currClient.getUsername())) {
                tempClients.add(client);
                tempChanel.add(chanel);
            }
            else{
                tempClients.add(currClient);
                tempChanel.add(currClient.getCommunicationChanel());
            }
        }
        this.waiting_queue.clear();this.waiting_queue.addAll(tempClients);
        this.communicationChanel.clear();this.communicationChanel.addAll(tempChanel);

        this.clients--;
    }

    public void addToBegginerWaitingQueue(Client client, CommunicationChanelAction chanel, String username){

        Queue<Client> tempClients = new ArrayDeque<Client>();
        Queue<CommunicationChanelAction> tempChanel = new ArrayDeque<CommunicationChanelAction>();

        for (Client currClient : this.beginnerWaitingQueue){
            if (username.equals(currClient.getUsername())) {
                tempClients.add(client);
                tempChanel.add(chanel);
            }
            else{
                tempClients.add(currClient);
                tempChanel.add(currClient.getCommunicationChanel());
            }
        }
        this.beginnerWaitingQueue.clear();this.beginnerWaitingQueue.addAll(tempClients);
        this.beginnerCommunicationChanel.clear();this.beginnerCommunicationChanel.addAll(tempChanel);

        this.clients--;
    }


    public void addToLowWaitingQueue(Client client, CommunicationChanelAction chanel, String username){

        Queue<Client> tempClients = new ArrayDeque<Client>();
        Queue<CommunicationChanelAction> tempChanel = new ArrayDeque<CommunicationChanelAction>();

        for (Client currClient : this.lowWaitingQueue){
            if (username.equals(currClient.getUsername())) {
                tempClients.add(client);
                tempChanel.add(chanel);
            }
            else{
                tempClients.add(currClient);
                tempChanel.add(currClient.getCommunicationChanel());
            }
        }
        this.lowWaitingQueue.clear();this.lowWaitingQueue.addAll(tempClients);
        this.lowCommunicationChanel.clear();this.lowCommunicationChanel.addAll(tempChanel);

        this.clients--;
    }
    public void addToMediumWaitingQueue(Client client, CommunicationChanelAction chanel, String username){

        Queue<Client> tempClients = new ArrayDeque<Client>();
        Queue<CommunicationChanelAction> tempChanel = new ArrayDeque<CommunicationChanelAction>();

        for (Client currClient : this.mediumWaitingQueue){
            if (username.equals(currClient.getUsername())) {
                tempClients.add(client);
                tempChanel.add(chanel);
            }
            else{
                tempClients.add(currClient);
                tempChanel.add(currClient.getCommunicationChanel());
            }
        }
        this.mediumWaitingQueue.clear();this.mediumWaitingQueue.addAll(tempClients);
        this.mediumCommunicationChanel.clear();this.mediumCommunicationChanel.addAll(tempChanel);

        this.clients--;
    }
    public void addToHighWaitingQueue(Client client, CommunicationChanelAction chanel, String username){

        Queue<Client> tempClients = new ArrayDeque<Client>();
        Queue<CommunicationChanelAction> tempChanel = new ArrayDeque<CommunicationChanelAction>();

        for (Client currClient : this.highWaitingQueue){
            if (username.equals(currClient.getUsername())) {
                tempClients.add(client);
                tempChanel.add(chanel);
            }
            else{
                tempClients.add(currClient);
                tempChanel.add(currClient.getCommunicationChanel());
            }
        }
        this.highWaitingQueue.clear();this.highWaitingQueue.addAll(tempClients);
        this.highCommunicationChanel.clear();this.highCommunicationChanel.addAll(tempChanel);

        this.clients--;
    }

    public void addToRankingWaitingQueue(Client client, CommunicationChanelAction chanel, int score){

        if (score <= 20){
            this.beginnerWaitingQueue.add(client);
            this.beginnerCommunicationChanel.add(chanel);
        } else if (score <= 50) {
            this.lowWaitingQueue.add(client);
            this.lowCommunicationChanel.add(chanel);
        } else if (score <= 100) {
            this.mediumWaitingQueue.add(client);
            this.mediumCommunicationChanel.add(chanel);
        }
        else{
            this.highWaitingQueue.add(client);
            this.highCommunicationChanel.add(chanel);
        }

    }

    public void addToRankingWaitingQueue(Client client, CommunicationChanelAction chanel, int score,String username){

        if (score <= 20){
            this.addToBegginerWaitingQueue(client, chanel,username);

        } else if (score <= 50) {
            this.addToLowWaitingQueue(client, chanel,username);
        } else if (score <= 100) {
            this.addToMediumWaitingQueue(client, chanel,username);
        }
        else{
            this.addToHighWaitingQueue(client, chanel,username);
        }

    }

    public boolean isOn(String username, int score){


        Queue<Client> tempClients;
        Queue<CommunicationChanelAction> tempChanel;

        if (gameMode == 1){
            tempClients = new ArrayDeque<Client>(this.waiting_queue);
            tempChanel = new ArrayDeque<CommunicationChanelAction>(this.communicationChanel);
        }
        else if  (score <= 20){
            tempClients = new ArrayDeque<Client>(this.beginnerWaitingQueue);
            tempChanel = new ArrayDeque<CommunicationChanelAction>(this.beginnerCommunicationChanel);

        } else if (score <= 50) {
            tempClients = new ArrayDeque<Client>(this.lowWaitingQueue);
            tempChanel = new ArrayDeque<CommunicationChanelAction>(this.lowCommunicationChanel);
        } else if (score <= 100) {
            tempClients = new ArrayDeque<Client>(this.mediumWaitingQueue);
            tempChanel = new ArrayDeque<CommunicationChanelAction>(this.mediumCommunicationChanel);
        }
        else{
            tempClients = new ArrayDeque<Client>(this.highWaitingQueue);
            tempChanel = new ArrayDeque<CommunicationChanelAction>(this.highCommunicationChanel);
        }


        for (Client currClient : tempClients){
            Client client = tempClients.remove();
            CommunicationChanelAction chanel = tempChanel.remove();
            if (username.equals(currClient.getUsername())) {

                chanel.send(CommunicationChanelAction.Message.CHECK);
                String line =chanel.read();

                if (line==null){
                    return false;
                }
                return true;

            }

        }
        return false;
    }
    public boolean allPlayersOn(String queue, int num){

        Queue<Client> tempClients;
        Queue<CommunicationChanelAction> tempChanel;

        if (gameMode == 1){
            tempClients = new ArrayDeque<Client>(this.waiting_queue);
            tempChanel = new ArrayDeque<CommunicationChanelAction>(this.communicationChanel);
        }
        else if  (queue.equals("beginner")){
            tempClients = new ArrayDeque<Client>(this.beginnerWaitingQueue);
            tempChanel = new ArrayDeque<CommunicationChanelAction>(this.beginnerCommunicationChanel);

        } else if (queue.equals("low")) {
            tempClients = new ArrayDeque<Client>(this.lowWaitingQueue);
            tempChanel = new ArrayDeque<CommunicationChanelAction>(this.lowCommunicationChanel);
        } else if (queue.equals("medium")) {
            tempClients = new ArrayDeque<Client>(this.mediumWaitingQueue);
            tempChanel = new ArrayDeque<CommunicationChanelAction>(this.mediumCommunicationChanel);
        }
        else{
            tempClients = new ArrayDeque<Client>(this.highWaitingQueue);
            tempChanel = new ArrayDeque<CommunicationChanelAction>(this.highCommunicationChanel);
        }

        int i = 1;

        for (Client currClient : tempClients){
            if (i <= num){
                Client client = tempClients.remove();
                CommunicationChanelAction chanel = tempChanel.remove();
                lock.readLock().lock();

                int score = database.getClientInfo(currClient.getUsername()).score;
                lock.readLock().unlock();

                if (!isOn(currClient.getUsername(),score)){
                    removeClient(currClient.getUsername(), queue);
                    System.out.println("" );
                    System.out.println( currClient.getUsername() + " was removed from the server as she wasn't online for her turn." );
                    System.out.println("" );
                    return false;
                }
                i++;
            }
            else return true;
        }
        return true;
    }

    public void removeClient(String username, String queue){
        Queue<Client> tempClients;
        Queue<CommunicationChanelAction> tempChanel;

        if (gameMode == 1){
            tempClients = new ArrayDeque<Client>(this.waiting_queue);
            tempChanel = new ArrayDeque<CommunicationChanelAction>(this.communicationChanel);
        }
        else if  (queue.equals("beginner")){
            tempClients = new ArrayDeque<Client>(this.beginnerWaitingQueue);
            tempChanel = new ArrayDeque<CommunicationChanelAction>(this.beginnerCommunicationChanel);

        } else if (queue.equals("low")) {
            tempClients = new ArrayDeque<Client>(this.lowWaitingQueue);
            tempChanel = new ArrayDeque<CommunicationChanelAction>(this.lowCommunicationChanel);
        } else if (queue.equals("medium")) {
            tempClients = new ArrayDeque<Client>(this.mediumWaitingQueue);
            tempChanel = new ArrayDeque<CommunicationChanelAction>(this.mediumCommunicationChanel);
        }
        else {
            tempClients = new ArrayDeque<Client>(this.highWaitingQueue);
            tempChanel = new ArrayDeque<CommunicationChanelAction>(this.highCommunicationChanel);
        }

        int i = 0;
        for (Client currClient : tempClients){
            Client client = tempClients.remove();
            CommunicationChanelAction chanel = tempChanel.remove();
            if (username.equals(currClient.getUsername())) {
                database.logOut(username);
            }
            else{
                tempClients.add(currClient);
                tempChanel.add(chanel);
            }

        }
        if (gameMode == 1){
            this.waiting_queue.clear();this.waiting_queue.addAll(tempClients);
            this.communicationChanel.clear();this.communicationChanel.addAll(tempChanel);
        }
        else if  (queue.equals("beginner")){
            this.beginnerWaitingQueue.clear();this.beginnerWaitingQueue.addAll(tempClients);
            this.beginnerCommunicationChanel.clear();this.beginnerCommunicationChanel.addAll(tempChanel);

        } else if (queue.equals("low")) {

            this.lowWaitingQueue.clear();this.lowWaitingQueue.addAll(tempClients);
            this.lowCommunicationChanel.clear();this.lowCommunicationChanel.addAll(tempChanel);
        } else if (queue.equals("medium")) {
            this.mediumWaitingQueue.clear();this.mediumWaitingQueue.addAll(tempClients);
            this.mediumCommunicationChanel.clear();this.mediumCommunicationChanel.addAll(tempChanel);
        }
        else {
            this.highWaitingQueue.clear();this.highWaitingQueue.addAll(tempClients);
            this.highCommunicationChanel.clear();this.highCommunicationChanel.addAll(tempChanel);

        }



        this.clients--;

    }


    public int getGameMode(){
        return this.gameMode;
    }

}



