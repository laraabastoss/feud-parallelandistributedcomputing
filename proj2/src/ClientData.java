import java.net.*;

public class ClientData {

    public String username;
    public int score;
    private String password;
    private String token = null;
    protected Status status; // Info about wether player is online or logged out
    enum Status {
        Online,
        Offline
    }

    public ClientData(String username, String password, int score){
        this.username = username;
        this.password = password;
        this.score = score;
        this.status = Status.Offline;
    }

    public void connect(){
        this.status = Status.Online;
    }
    public void disconnect(){
        this.status = Status.Offline;
    }
    public boolean isOnline(){
        return this.status == Status.Online;
    }

    public String getPassword(){
        return password;
    }
    public void increaseScore(){
        this.score += 1;
    }
    public void decreaseScore(){
        this.score -= 1;
    }

}
