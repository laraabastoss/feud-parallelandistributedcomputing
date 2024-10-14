import java.util.ArrayList;

public class Question {
    private String question;
    private int correctAnswer;
    private ArrayList<String> possibleAnswers;

    public Question(String question, ArrayList<String> possibleAnswers, int correctAnswer) {
        this.question = question;
        this.possibleAnswers = possibleAnswers;
        this.correctAnswer = correctAnswer;
    }

    // Getters and setters
    public String getQuestion() {
        return question;
    }

    public int getCorrectAnswer() {
        return correctAnswer;
    }

    public ArrayList<String>  getOptions() {
        return this.possibleAnswers;
    }
    public String getFormattedOptions() {
        StringBuilder formattedOptions = new StringBuilder();
        for (int i = 0; i < possibleAnswers.size(); i++) {
            formattedOptions.append(i + 1).append(" - ").append(possibleAnswers.get(i));
            if (i < possibleAnswers.size() - 1) {
                formattedOptions.append("\n");
            }
        }
        return formattedOptions.toString();
    }
}
