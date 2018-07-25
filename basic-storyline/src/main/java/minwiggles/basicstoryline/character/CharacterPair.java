package minwiggles.basicstoryline.character;


public class CharacterPair {

    int characterId1;
    int characterId2;

    public CharacterPair( int characterId1, int characterId2){
        this.characterId1 = characterId1;
        this.characterId2 = characterId2;
    }

    public int getCharacterId1() {
        return characterId1;
    }

    public void setCharacterId1(int characterId1) {
        this.characterId1 = characterId1;
    }

    public int getCharacterId2() {
        return characterId2;
    }

    public void setCharacterId2(int characterId2) {
        this.characterId2 = characterId2;
    }


    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof CharacterPair)) {
            return false;
        }
        return (((CharacterPair)other).getCharacterId1()==this.characterId1 &&
                ((CharacterPair)other).getCharacterId2()==this.characterId2);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + this.characterId1;
        hash = 53 * hash + this.characterId2;
        return hash;
    }
}
