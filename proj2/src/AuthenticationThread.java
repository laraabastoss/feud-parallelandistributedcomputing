import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class AuthenticationThread extends Thread {
    private Server server;
    private ServerSocket serverSocket;

    public AuthenticationThread(Server server, ServerSocket serverSocket) {
        this.server = server;
        this.serverSocket = serverSocket;

    }

    public void run() {

        while (true) {
            try {
                Socket socket = serverSocket.accept();


                ClientAuthenticationThread clientAuthenticationThread = new ClientAuthenticationThread(socket, server);
                Thread.ofVirtual().start(clientAuthenticationThread);

            } catch (IOException e) {

                e.printStackTrace();
            }
        }
    }
}

