import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TicTacToeThread extends Thread {
    int numPlayers;
    private ArrayList<Client> players;
    private ArrayList<CommunicationChanelAction> chanels;
    private Server server;
    private volatile boolean gameOver = false;
    private ReentrantReadWriteLock lock;




    public TicTacToeThread(int numPlayers, Server server) {
        this.numPlayers = numPlayers;
        this.players = new ArrayList<>();
        this.chanels = new ArrayList<>();
        this.server = server;
        this.lock = new ReentrantReadWriteLock();
    }

    public void addPlayers(Client player) {
        this.players.add(player);
    }
    public void addCommuncationChannel(CommunicationChanelAction chanel) {
        this.chanels.add(chanel);
    }

    @Override
    public void run() {
        TicTacToe game = new TicTacToe(numPlayers, players, chanels, new ArrayList<>());
        game.start();

        gameOver = true;

        for (int i = 0; i < players.size(); i++) {
            final Client player = players.get(i);
            final CommunicationChanelAction chanel = chanels.get(i);
            Thread.ofVirtual().start(() -> handlePlayerResponse(player, chanel ));
        }

        server.onGameFinished(this);
    }

    private void handlePlayerResponse(Client player, CommunicationChanelAction chanel) {


        chanel.send(CommunicationChanelAction.Message.CHECK_RESULT);

        String answer = chanel.read();

        if (answer == "" || answer == null) {
            lock.writeLock().lock();
            server.database.logOut(player.getUsername());
            System.out.println(player.getUsername() + " disconnected from the game.");
            lock.writeLock().unlock();
            return;
        }

        while (answer.equals("WAIT") || answer.equals("CHECK")){
            answer = chanel.read();
            if (answer == "" || answer == null) {
                lock.writeLock().lock();
                server.database.logOut(player.getUsername());
                System.out.println(player.getUsername() + " disconnected from the game.");
                lock.writeLock().unlock();
                return;
            }
        }

        lock.writeLock().lock();
        String username = chanel.read();
        ClientData p = server.database.getClientInfo(username);

        if (answer.equals("WON")){
            p.increaseScore();
        }
        if (answer.equals("LOST")){
            p.decreaseScore();
        }
        if (!answer.equals("DISCONNECTED")) {
            lock.writeLock().unlock();
            chanel.send(CommunicationChanelAction.Message.LOGOUT_OR_NOT);

            answer = chanel.read();
            if (answer == null) {
                lock.writeLock().lock();
                server.database.logOut(player.getUsername());
                System.out.println(username + " disconnected from the game...");
                lock.writeLock().unlock();
                return;
            }
            while (answer.equals("WAIT")) {
                answer = chanel.read();
                if (answer == null) {
                    lock.writeLock().lock();
                    server.database.logOut(player.getUsername());
                    System.out.println(username + " disconnected from the game.");
                    lock.writeLock().unlock();
                    return;
                }
            }

            lock.writeLock().lock();
            if (answer.equals("PLAY_AGAIN")) {

                server.requeuePlayer(player, chanel);


            } else if (answer.equals("LOGOUT")) {
                server.database.logOut(player.getUsername());
                System.out.println(username + " logged out from the game.");
            }
        } else {
            chanel.send(CommunicationChanelAction.Message.CHECK);
            lock.writeLock().lock();
            server.requeuePlayer(player, chanel);
        }

        players.remove(player);
        chanels.remove(chanel);
        lock.writeLock().unlock();

    }
}
