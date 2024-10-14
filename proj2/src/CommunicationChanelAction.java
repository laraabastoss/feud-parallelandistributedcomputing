import java.io.*;
import java.util.ArrayList;

public class CommunicationChanelAction {
    enum Message {
        REGISTER,
        LOGIN,
        AUTH,
        DISCONNECTED,
        APP_AUTH,
        REJ_AUTH,
        WAIT, // waiting for a new game
        GAME,
        OCCUPIED,
        QUESTION,
        WRITE_PLAY,
        WRONG,
        OTHERPLAYER,
        CHECK_RESULT,
        WON,
        LOST,
        DRAW,
        COLUMN,
        ROW,
        ANSWER,
        TEAMCORRECT,
        LOGOUT_OR_NOT,
        PLAY_AGAIN,
        LOGOUT,
        RECONNECTED,
        CHECK
    }



    Message message;
    String[][] board = null;
    String str = null;
    InputStream input;
    BufferedReader reader;
    OutputStream output;
    PrintWriter writer;

    int number;
    public CommunicationChanelAction(BufferedReader reader,  PrintWriter writer) {

        this.reader = reader;
        this.writer = writer;
    }

    public void send(Message message){

        writer.println(message.name());

    }

    public void send(Message message, String str){
        writer.println(message.name());
        writer.println(str);

    }
    public void send(Message message, String board, String str){
        writer.println(message.name());
        writer.println(board);
        writer.println(str);

    }
    public void send(Message message, String board, int integer){
        writer.println(message.name());
        writer.println(board);
        writer.println(integer);

    }
    public void send(Message message, int integer){

        writer.println(message);
        writer.println(String.valueOf(integer));


    }
    public void send(Message message, String board, String str, String options, int integer){

        writer.println(message);
        writer.println(board);
        writer.println(str);
        writer.println(options);
        writer.println(integer);

    }

    public String read(){

        try{
            String line = reader.readLine();
            return line;
        }
        catch (IOException e){

            return "";
        }

    }

    public String readBoard() {

        StringBuilder fullMessage = new StringBuilder();
        try {
            String line;

            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                fullMessage.append(line).append("\n");

            }

        } catch (IOException e) {
            System.out.println("Error reading message");
        }
        return fullMessage.toString();
    }
    public String readOptions() {

        StringBuilder fullMessage = new StringBuilder();
        try {
            String line;
            int i = 0;
            while (i<4 && (line = reader.readLine()) != null ) {
                fullMessage.append(line).append("\n");

                i++;
            }

        } catch (IOException e) {
            System.out.println("Error reading message");
        }
        return fullMessage.toString();
    }





}
