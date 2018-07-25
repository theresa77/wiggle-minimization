package minwiggles.basicstoryline.wiggle;

public class CompressedStorylineWigglesOfCharacter {

    private int character;
    private double[] wiggleWeights;

    public CompressedStorylineWigglesOfCharacter(int character, double[] wiggleWeights){
        this.character = character;
        this.wiggleWeights = wiggleWeights;
    }

    public int getCharacter() {
        return character;
    }

    public void setCharacter(int character) {
        this.character = character;
    }

    public double[] getWiggleWeights() {
        return wiggleWeights;
    }

    public void setWiggleWeights(double[] wiggleWeights) {
        this.wiggleWeights = wiggleWeights;
    }
}
