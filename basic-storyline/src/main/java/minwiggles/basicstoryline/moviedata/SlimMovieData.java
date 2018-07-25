package minwiggles.basicstoryline.moviedata;

import minwiggles.basicstoryline.character.Character;
import minwiggles.basicstoryline.character.SlimCharacter;
import minwiggles.basicstoryline.location.Location;
import minwiggles.basicstoryline.meeting.InteractionSession;

import java.util.*;

public class SlimMovieData {

    private String movieName;
    private int timeCount; // number of time steps
    private int nodeCount; // number of characters
    private int interactionSessionCount; // number of interaction sessions
    private int locationCount; // number of unique locations

    private List<SlimCharacter> characters = new LinkedList<SlimCharacter>();
    private List<Location> locations = new LinkedList<Location>();
    private InteractionSession[] interactionSessions;

    private Integer[] compressedTimePoints;

    public SlimMovieData(MovieData movieData){
        this.movieName = movieData.getMovieName();
        this.timeCount = movieData.getTimeCount();
        this.nodeCount = movieData.getNodeCount();
        this.interactionSessionCount = movieData.getInteractionSessionCount();
        this.locationCount = movieData.getLocationCount();
        this.locations = movieData.getLocations();
        this.interactionSessions = movieData.getInteractionSessions();
        this.compressedTimePoints = movieData.getCompressedTimePoints();
        this.characters = new ArrayList<>();
        for(Character character: movieData.getCharacters()){
            this.characters.add(new SlimCharacter(character));
        }
    }

    public String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }

    public int getTimeCount() {
        return timeCount;
    }

    public void setTimeCount(int timeCount) {
        this.timeCount = timeCount;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public int getInteractionSessionCount() {
        return interactionSessionCount;
    }

    public void setInteractionSessionCount(int interactionSessionCount) {
        this.interactionSessionCount = interactionSessionCount;
    }

    public int getLocationCount() {
        return locationCount;
    }

    public void setLocationCount(int locationCount) {
        this.locationCount = locationCount;
    }

    public List<SlimCharacter> getCharacters() {
        return characters;
    }

    public void setCharacters(List<SlimCharacter> characters) {
        this.characters = characters;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }

    public InteractionSession[] getInteractionSessions() {
        return interactionSessions;
    }

    public void setInteractionSessions(InteractionSession[] interactionSessions) {
        this.interactionSessions = interactionSessions;
    }

    public Integer[] getCompressedTimePoints() {
        return compressedTimePoints;
    }

    public void setCompressedTimePoints(Integer[] compressedTimePoints) {
        this.compressedTimePoints = compressedTimePoints;
    }
}
