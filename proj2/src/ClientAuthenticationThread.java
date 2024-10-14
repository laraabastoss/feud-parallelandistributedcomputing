import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientAuthenticationThread extends Thread {
    private Socket socket;
    private Server server;
    private ReentrantReadWriteLock lock;

    public ClientAuthenticationThread(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.lock = new ReentrantReadWriteLock();
    }

    // Independent thread for authenticating player
    public void run() {
        try {
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            Client client = new Client(socket);
            CommunicationChanelAction chanel = client.getCommunicationChanel();

            System.out.println("New player entered server.\n");

            boolean auth = false;

            while (!auth) {
                chanel.send(CommunicationChanelAction.Message.AUTH);
                String authType;
                String username;
                String password;
                String hashedPassword;


                // Collect input from player
                try {

                    authType = reader.readLine();
                    username = reader.readLine();
                    password = reader.readLine();
                    if ( authType == null ||   username==null ||    password ==null ){
                        System.out.println("Player left server without athenticating");
                        return;
                    }
                    hashedPassword = Hash.hashPassword(password);

                } finally {
                    // Release the read lock
                }


                ClientData clientInfo = server.database.getClientInfo(username);


                // Register player
                 if ("REGISTER".equals(authType)) {
                    // Locking access to database
                    lock.writeLock().lock();
                    try {

                        AuthResult registered = server.database.signUp(username, hashedPassword,0, server.databasePath);
                        if (registered.getSuccess()) {
                            chanel.send(CommunicationChanelAction.Message.APP_AUTH, registered.getMessage(), username);
                            auth = true;
                            System.out.println(registered.getMessage());
                            if (server.getGameMode() == 1){
                                server.addToWaitingQueue(client, chanel);
                            }
                            else{
                                int score = server.database.getClientInfo(username).score;
                                server.addToRankingWaitingQueue(client, chanel, score);
                            }
                            chanel.send(CommunicationChanelAction.Message.WAIT);
                            client.setUsername(username);
                            server.incrementClient(1);


                        } else {
                            chanel.send(CommunicationChanelAction.Message.REJ_AUTH, registered.getMessage());
                        }
                    } finally {
                        lock.writeLock().unlock();

                    }
                }

                // LogIn Player
                else if ("LOGIN".equals(authType)) {

                    try {
                        // Locking access to database
                        lock.writeLock().lock();
                        AuthResult authenticated = server.database.logIn(username, hashedPassword);

                        if (authenticated.getSuccess()) {
                            chanel.send(CommunicationChanelAction.Message.APP_AUTH, authenticated.getMessage(), username);
                            auth = true;
                            System.out.println(authenticated.getMessage());

                            if (server.getGameMode() == 1){
                                server.addToWaitingQueue(client, chanel);
                            }
                            else{
                                int score = server.database.getClientInfo(username).score;
                                server.addToRankingWaitingQueue(client, chanel, score);
                            }

                            chanel.send(CommunicationChanelAction.Message.WAIT);
                            client.setUsername(username);
                            server.incrementClient(1);


                        }
                        else if(clientInfo!=null && hashedPassword.equals(clientInfo.getPassword()) && clientInfo.status.equals(ClientData.Status.Online) && !(server.isOn(username,clientInfo.score )) ){

                            client.setUsername(username);

                            //find status player was in and send

                            chanel.send(CommunicationChanelAction.Message.RECONNECTED, username);
                            auth = true;

                            if (server.getGameMode() == 1){
                                server.addToWaitingQueue(client, chanel,username);
                            }
                            else{
                                int score = server.database.getClientInfo(username).score;
                                server.addToRankingWaitingQueue(client, chanel, score,username);
                            }

                            chanel.send(CommunicationChanelAction.Message.WAIT);

                            server.incrementClient(1);
                        }
                        else {
                            chanel.send(CommunicationChanelAction.Message.REJ_AUTH, authenticated.getMessage());
                        }
                    } finally {
                        lock.writeLock().unlock();

                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
