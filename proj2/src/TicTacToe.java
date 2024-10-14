import java.net.Socket;
import java.util.*;


public class TicTacToe {

    enum gameStatus{
        ONGOING,
        P1WON,
        P2WON,
        DRAW
    }


    private final String[][] gameBoard = {{"" , "",""},{"" , "",""},{"" , "",""}};
    private final Question[] questions = {new Question("In what year was the new FEUP building inaugurated?", new ArrayList<>(Arrays.asList("1985", "2001", "2008", "1997")), 2),
            new Question("How many Higher Education courses are there at FEUP?", new ArrayList<>(Arrays.asList("10", "14", "11", "12")), 2),
            new Question("What is the most recent course at FEUP?", new ArrayList<>(Arrays.asList("Industrial Management Engineering", "Bioengineering", "Chemical Engineering", "Informatics and Computing Engineering")), 4),
            new Question("What is the name of the main building of FEUP?", new ArrayList<>(Arrays.asList("Building A", "Building B", "Building C", "Building D")), 2),
            new Question("Where was the old FEUP building located?", new ArrayList<>(Arrays.asList("Rua dos Bragas", "Rua do Campo Alegre", "Rua Jorge de Viterbo Ferreira", "Avenida de Rodrigues de Freitas")), 1),
            new Question("Which faculty currently occupies the old FEUP facilities?", new ArrayList<>(Arrays.asList("FCUP", "FLUP", "FFUP", "FDUP")), 4),
            new Question("In what year did FEUP receive that same designation?", new ArrayList<>(Arrays.asList("1945", "1967", "1926", "1953")), 2),
            new Question("Who is the current Director of FEUP?", new ArrayList<>(Arrays.asList("Ana Paula Rocha", "Jaime Cardoso", "Ana Sofia Teixeira", "Rui Calçada")), 4),
            new Question("FEUP is currently located between which other two faculties?", new ArrayList<>(Arrays.asList("FMUP and FADEUP", "ESE and FEP", "ESEP and FEP", "ESEP and FMUP")), 2),
            new Question("How old is AEFEUP?", new ArrayList<>(Arrays.asList("40", "27", "51", "32")), 1),
            new Question("How many departments does AEFEUP have?", new ArrayList<>(Arrays.asList("6", "8", "10", "12")), 4),
            new Question("What year was the AEFEUP building inaugurated?", new ArrayList<>(Arrays.asList("2003", "2006", "2007", "2010")), 4),
            new Question("Who is the current president of AEFEUP?", new ArrayList<>(Arrays.asList("Francisco Portela", "Carlos Alves", "Rui Guerreiro", "José Araújo")), 1),
            new Question("For how many sports does AEFEUP have a team?", new ArrayList<>(Arrays.asList("5", "4", "3", "6")), 1),
            new Question("What is the current beer brand that AEFEUP has a partnership with?", new ArrayList<>(Arrays.asList("Nortada", "Sagres", "Super Bock", "Coral")), 3)
    };


    private List<Socket> userSockets;
    ArrayList<Client> players;
    ArrayList<CommunicationChanelAction> chanels;

    private gameStatus status = gameStatus.ONGOING;

    private boolean gameFinished = false;
    int numPlayers;
    int questionRound = 1;
    boolean even = false ;
    private ArrayList<Integer> oddPlays = new ArrayList<Integer>();

    private ArrayList<Integer> evenPlays = new ArrayList<Integer>();



    public TicTacToe( int numPlayers,  ArrayList<Client>  players, ArrayList<CommunicationChanelAction> chanels, List<Socket> userSockets) {
        this.numPlayers = numPlayers;
        this.userSockets = userSockets;
        this.players = players;
        this.chanels = chanels;

        for (int i = 1; i <= numPlayers; i++) {
            if (i % 2 == 0) {
                evenPlays.add(i);
            } else {
                oddPlays.add(i);
            }
        }

    }



    public void start() {

        try {
            System.out.println("Starting game with " + this.players.size() + " players");

            for (int i = 0; i<this.players.size(); i++){

                chanels.get(i).send(CommunicationChanelAction.Message.GAME);
            }

            run();

        } catch (Exception exception) {
            System.out.println("Exception ocurred during game. Connection close. : " + exception.getMessage());
        }
        
    }

    public void run() {

        Random random = new Random();

        while (this.status.equals(gameStatus.ONGOING)){


            int randomIndex = random.nextInt(this.questions.length);
            ArrayList<String> answers = new ArrayList<>();

            for (int i = 1; i <= this.players.size() ; i++){

                if (chanels.get(i-1).read() == ""){
                    warnDisconnected(i - 1);
                    return;
                }

                if (this.questionRound%2 != i%2) {
                    chanels.get(i-1).send(CommunicationChanelAction.Message.OTHERPLAYER, displayBoard(), this.questionRound);
                }
                else{

                    askQuestion(players.get(i-1),chanels.get(i-1),randomIndex);
                }

            }

            for (int i = 1; i <= this.players.size() ; i++){

                if (this.questionRound%2==i%2) {

                    String answer = getAnswer(i-1, chanels.get(i-1));
                    if (answer == "") return;
                    answers.add(answer);

                }

            }

            String finalAnswer = findMostCommon(answers);

            if(validateAnswer(finalAnswer,randomIndex)){

                int[] position = getPosition(players.get(this.questionRound-1),  chanels.get(this.questionRound-1));
                placePiece(position, this.questionRound);

                gameFinished();


                if (!this.status.equals(gameStatus.ONGOING)) {

                    for (int i = 1; i <= this.players.size() ; i++){

                        if (this.status.equals(gameStatus.DRAW)) chanels.get(i -1).send( CommunicationChanelAction.Message.DRAW,displayBoard());

                        else if (this.questionRound%2!=i%2) {

                            chanels.get(i -1).send( CommunicationChanelAction.Message.LOST,displayBoard());
                        }
                        else {

                            chanels.get(i -1).send( CommunicationChanelAction.Message.WON,displayBoard());
                        }

                    }

                    break;
                }

            }

            nextRound();

        }

    }


    public String displayBoard() {
        StringBuilder board = new StringBuilder();

        for (int i = 0; i < gameBoard.length; i++) {
            StringBuilder line = new StringBuilder();

            for (int j = 0; j < gameBoard[i].length; j++) {
                if (j < gameBoard[i].length - 1) {
                    line.append(" ").append(gameBoard[i][j]).append(" | ");
                } else {
                    line.append(" ").append(gameBoard[i][j]).append(" ");
                }
            }

            board.append(line.toString()).append("\n");

            if (i < gameBoard.length - 1) {
                board.append("----------------\n");
            }
        }

        return board.toString();
    }





    public void gameFinished() {

        for (int i = 0; i < 3; i++) {
            if (this.gameBoard[i][0] != "" &&
                    this.gameBoard[i][0] == this.gameBoard[i][1] &&
                    this.gameBoard[i][1] == this.gameBoard[i][2]) {

                this.status = gameBoard[0][i].equals("x")?gameStatus.P1WON:gameStatus.P2WON;
                return;
            }
        }

        for (int j = 0; j < 3; j++) {
            if (this.gameBoard[0][j] != "" &&
                    this.gameBoard[0][j] == this.gameBoard[1][j] &&
                    this.gameBoard[1][j] == this.gameBoard[2][j]) {

                this.status = gameBoard[0][j].equals("x")?gameStatus.P1WON:gameStatus.P2WON;
                return;
            }
        }

        if (this.gameBoard[0][0] != "" &&
                this.gameBoard[0][0] == this.gameBoard[1][1] &&
                this.gameBoard[1][1] == this.gameBoard[2][2]) {

            this.status = gameBoard[0][0].equals("x")?gameStatus.P1WON:gameStatus.P2WON;
            return;
        }

        if (this.gameBoard[0][2] != "" &&
                this.gameBoard[0][2] == this.gameBoard[1][1] &&
                this.gameBoard[1][1] == this.gameBoard[2][0]) {

            this.status = gameBoard[0][2].equals("x")?gameStatus.P1WON:gameStatus.P2WON;
            return;

        }

       boolean isDraw = true;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (this.gameBoard[i][j] == "") {
                    isDraw = false;
                    break;
                }
            }
        }

        if (isDraw) {
            this.status = gameStatus.DRAW;
        }

    }

    public void askQuestion(Client player,CommunicationChanelAction chanel, int questionIndex) {

        chanel.send(CommunicationChanelAction.Message.QUESTION, displayBoard(), questions[questionIndex].getQuestion(), questions[questionIndex].getFormattedOptions(), numPlayers);

    }

    public void warnDisconnected(int player){

        String disconnectedUser = players.get(player).getUsername();
        for (int i = 0; i < this.players.size(); i++){
            if (i != player) chanels.get(i).send(CommunicationChanelAction.Message.DISCONNECTED, disconnectedUser);
        }
    }

    public String getAnswer(int player,CommunicationChanelAction chanel) {

        String answer = chanel.read();

        while (!answer.equals("ANSWER")) {

            if (answer == ""){
                warnDisconnected(player);
                return answer;
            }

            answer = chanel.read();
        }

        answer = chanel.read();
        return answer;

    }

    public String findMostCommon(List<String> list) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("List cannot be empty");
        }

        Map<String, Integer> countMap = new HashMap<>();
        String mostCommonElement = list.get(0);
        int maxCount = 1;

        for (String str : list) {
            int count = countMap.getOrDefault(str, 0) + 1;
            countMap.put(str, count);

            if (count > maxCount || (count == maxCount && str.compareTo(mostCommonElement) < 0)) {
                mostCommonElement = str;
                maxCount = count;
            }
        }

        return mostCommonElement;
    }

    public boolean validateAnswer(String answer, int questionIndex) {

        if (answer.equals(String.valueOf(questions[questionIndex].getCorrectAnswer()))){

            for (int i = 1; i <= this.players.size() ; i++){
                if (i == this.questionRound) {
                    this.chanels.get(i-1).send(CommunicationChanelAction.Message.WRITE_PLAY);
                }
                else if (questionRound%2==i%2){
                    this.chanels.get(i-1).send(CommunicationChanelAction.Message.TEAMCORRECT);

                }

            }

            return true;

        }

        else{
            for (int i = 1; i <= this.players.size() ; i++){

                if (questionRound%2==i%2){

                    this.chanels.get(i-1).send(CommunicationChanelAction.Message.WRONG);

                }

            }

            return false;

        }

    }

    public int[] getPosition(Client player, CommunicationChanelAction chanel){

        while (true){

            while (!chanel.read().equals("ROW")){

            }

            int row = Integer.parseInt(chanel.read()) ;

            while (!chanel.read().equals("COLUMN")){

            }

            int column =  Integer.parseInt(chanel.read());

            if (this.gameBoard[row - 1][column - 1].equals("")) {

                return new int[]{row - 1,column - 1};

            }

            chanel.send(CommunicationChanelAction.Message.OCCUPIED);

        }


    }
    public void placePiece(int[] position, int player){

        this.gameBoard[position[0]][position[1]] = (player % 2) == 0? "x":"o";

    }


    public void nextRound() {

        if (even) evenPlays.add(evenPlays.remove(0));
        else  oddPlays.add(oddPlays.remove(0));
        even = !even;

        if (even){
            int player = evenPlays.get(0);
            questionRound = player;
        }
        else{
            int player = oddPlays.get(0);
            questionRound = player;
        }

    }

}