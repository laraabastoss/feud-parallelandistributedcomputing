import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataBase {

    //list with the data for each already authenticated client
   public static List<ClientData> database;

    public DataBase(String path){
        database = new ArrayList<>();
        try {

            List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
            for (String player : lines) {
                String[] playerInfo = player.split(",");
                ClientData p = new ClientData(playerInfo[0], playerInfo[1], Integer.parseInt(playerInfo[2]));
                database.add(p);
            }

        } catch (IOException exception) {
            System.out.print(exception);
        }
    }

    // Siging up clients
    public AuthResult signUp(String username, String password, int score, String databasePath){
        if (username.equals("") || password.equals("")) {
            return new AuthResult(false, "Invalid username or password");
        }

        if (! (getClientInfo(username)==null)) {
            return new AuthResult(false, username + " is an already existant username.");
        }

        String info = "\n" + username + "," + password + ",0";
        try {
            Files.write(Paths.get(databasePath), info.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        } catch (IOException e){
            System.out.println(e);
            return new AuthResult(false, "Error writing to file");
        }

        ClientData player = new ClientData(username, password, score);
        player.connect();
        database.add(player);
        return new AuthResult(true, "Successfully registered!");
    }

    // Logging in clients
    public AuthResult logIn(String username, String password){
        ClientData player = getClientInfo(username);
        if (player == null){
            return new AuthResult(false, "Username not found");
        }
        if (!password.equals(player.getPassword())){
            return new AuthResult(false, "Wrong password");
        }
        if (player.isOnline()){
            return new AuthResult(false, username + " is already logged in");
        }
        player.connect();
        return new AuthResult(true, "Successfully logged in");
    }

    // Updating database with new info
    public void updateDatabase(String databasePath) {
        try {
            Files.write(Paths.get(databasePath), "".getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

            for (int i = 0; i < database.size(); i++) {

                ClientData player = database.get(i);
                String playerInfo = player.username + "," + player.getPassword() + "," + player.score;

                if (i > 0) {
                    playerInfo = "\n" + playerInfo;
                }

                Files.write(Paths.get(databasePath), playerInfo.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            }
        } catch (IOException exception) {
            System.out.println(exception);
        }
    }

    public AuthResult logOut(String username){
        for (ClientData player :database){
            if (player.username.equals(username)){
                player.disconnect();
                return  new AuthResult(true, username + " successfully logged out.");
            }
        }
        return new AuthResult(false, "No player with given username.");

    }

    // Logging out players
    public void displayPlayers(){
        System.out.println("                                                                               " );
        for (ClientData player :database){
            System.out.println("Player " + player.username + " : total of "  + player.score + " points");
            System.out.println("-------------------------------------------------------------------------------" );

        }
        System.out.println("                                                                               " );

    }

    //Find client data
    public static ClientData getClientInfo(String username){

        for (ClientData client : database ){

            if (username.equals(client.username)){
                return client;
            }
        }
        return null;
    }

}

