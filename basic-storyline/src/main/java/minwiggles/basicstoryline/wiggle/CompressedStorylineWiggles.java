package minwiggles.basicstoryline.wiggle;

public class CompressedStorylineWiggles {

    private CompressedStorylineWigglesOfCharacter[] wigglesOfCharacters;

    public CompressedStorylineWiggles(CompressedStorylineWigglesOfCharacter[] wigglesOfCharacters){
        this.wigglesOfCharacters = wigglesOfCharacters;
    }

    public CompressedStorylineWigglesOfCharacter[] getWigglesOfCharacters() {
        return wigglesOfCharacters;
    }

    public void setWigglesOfCharacters(CompressedStorylineWigglesOfCharacter[] wigglesOfCharacters) {
        this.wigglesOfCharacters = wigglesOfCharacters;
    }

}