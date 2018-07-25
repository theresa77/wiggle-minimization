package minwiggles.basicstoryline.crossing;


import java.util.List;

public class CrossingsInformation {

    private List<CharacterCrossings> characterCrossings;
    private int crossingsCount;

    public CrossingsInformation(List<CharacterCrossings> characterCrossings, int crossingsCount){
        this.characterCrossings = characterCrossings;
        this.crossingsCount = crossingsCount;
    }

    public List<CharacterCrossings> getCharacterCrossings() {
        return characterCrossings;
    }

    public void setCharacterCrossings(List<CharacterCrossings> characterCrossings) {
        this.characterCrossings = characterCrossings;
    }

    public int getCrossingsCount() {
        return crossingsCount;
    }

    public void setCrossingsCount(int crossingsCount) {
        this.crossingsCount = crossingsCount;
    }
}
