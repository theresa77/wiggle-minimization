package minwiggles.basicstoryline.storyline;

public class CompressedStorylineOfCharacter {

    private int character;
    private String[][] storyline;

    public CompressedStorylineOfCharacter(int character, String[][] storyline){
        this.character = character;
        this.storyline = storyline;
    }

    public int getCharacter() {
        return character;
    }

    public void setCharacter(int character) {
        this.character = character;
    }

    public String[][] getStoryline() {
        return storyline;
    }

    public void setStoryline(String[][] storyline) {
        this.storyline = storyline;
    }

}

