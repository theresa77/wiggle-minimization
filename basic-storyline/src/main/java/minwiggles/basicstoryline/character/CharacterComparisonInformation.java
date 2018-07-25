package minwiggles.basicstoryline.character;

import java.util.List;

public class CharacterComparisonInformation {

    private List<List<CharacterComparison>> characterComparisons; // the first index of the array are the compressed timePoints, the second the intex of the characterComparisons for this timePoint

    public CharacterComparisonInformation(List<List<CharacterComparison>> characterComparisons){
        this.characterComparisons = characterComparisons;
    }

    public List<List<CharacterComparison>> getCharacterComparisons() {
        return characterComparisons;
    }

    public void setCharacterComparisons(List<List<CharacterComparison>> characterComparisons) {
        this.characterComparisons = characterComparisons;
    }
}
