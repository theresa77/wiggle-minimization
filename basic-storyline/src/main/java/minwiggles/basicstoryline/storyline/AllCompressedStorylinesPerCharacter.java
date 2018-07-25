package minwiggles.basicstoryline.storyline;

public class AllCompressedStorylinesPerCharacter {

    CompressedStorylineOfCharacter[] storylinesPerCharacter;

    public AllCompressedStorylinesPerCharacter(CompressedStorylineOfCharacter[] storylinesPerCharacter){
        this.storylinesPerCharacter = storylinesPerCharacter;
    }

    public CompressedStorylineOfCharacter[] getStorylinesPerCharacter() {
        return storylinesPerCharacter;
    }

    public void setStorylinesPerCharacter(CompressedStorylineOfCharacter[] storylinesPerCharacter) {
        this.storylinesPerCharacter = storylinesPerCharacter;
    }
}
