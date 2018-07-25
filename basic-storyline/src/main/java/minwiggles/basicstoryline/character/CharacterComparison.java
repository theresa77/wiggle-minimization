package minwiggles.basicstoryline.character;

public class CharacterComparison {

    private CharacterPair characterPair;
    private double value;

    public CharacterComparison(CharacterPair characterPair, double value){
        this.characterPair = characterPair;
        this.value = value;
    }

    public CharacterPair getCharacterPair() {
        return characterPair;
    }

    public void setCharacterPair(CharacterPair characterPair) {
        this.characterPair = characterPair;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}