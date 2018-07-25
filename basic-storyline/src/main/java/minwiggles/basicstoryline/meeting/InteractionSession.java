package minwiggles.basicstoryline.meeting;

import minwiggles.basicstoryline.location.Location;
import java.util.Arrays;
import java.util.List;

public class InteractionSession {

    private String name;
    private int id;
    private int[] characterIds;
    private int locationId;
    private int startTimePoint;
    private int endTimePoint;
    private int compressedStartTimePoint;
    private int compressedEndTimePoint;
//    private List<Character> members;
//    private Location location;

    public InteractionSession(int id, String name, int startTimePoint, int endTimePoint, int[] characterIds, int locationId){
        this.id = id;
        this.name = name;
        this.characterIds = characterIds;
        this.locationId = locationId;
        this.startTimePoint = startTimePoint;
        this.endTimePoint = endTimePoint;
        this.compressedStartTimePoint = -1;
        this.compressedEndTimePoint = -1;
    }

    public InteractionSession(int id, String name, int compressedStartTimePoint, int compressedEndTimePoint, int startTimePoint, int endTimePoint, int[] characterIds, int locationId){
        this.id = id;
        this.name = name;
        this.characterIds = characterIds;
        this.locationId = locationId;
        this.startTimePoint = startTimePoint;
        this.endTimePoint = endTimePoint;
        this.compressedStartTimePoint = compressedStartTimePoint;
        this.compressedEndTimePoint = compressedEndTimePoint;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int[] getCharacterIds() {
        return characterIds;
    }

    public void setCharacterIds(int[] characterIds) {
        this.characterIds = characterIds;
        Arrays.sort(this.characterIds);
    }

    public int getLocationId() {
        return locationId;
    }

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

//    public List<Character> getMembers() {
//        return members;
//    }
//
//    public int getMembersCount() { return this.characterIds.length; }
//
//    public void setMembers(List<Character> members) {
//        this.members = members;
//    }
//
//    public Location getLocation() {
//        return location;
//    }
//
//    public void setLocation(Location location) {
//        this.location = location;
//    }

    public int numberOfCharacters(){
        return this.characterIds.length;
    }

    public int numberOfTimePoints(){
        return this.endTimePoint - this.startTimePoint;
    }


    public int getStartTimePoint() {
        return startTimePoint;
    }

    public void setStartTimePoint(int startTimePoint) {
        this.startTimePoint = startTimePoint;
    }

    public int getEndTimePoint() {
        return endTimePoint;
    }

    public void setEndTimePoint(int endTimePoint) {
        this.endTimePoint = endTimePoint;
    }

    public int getCompressedStartTimePoint() {
        return compressedStartTimePoint;
    }

    public void setCompressedStartTimePoint(int compressedStartTimePoint) {
        this.compressedStartTimePoint = compressedStartTimePoint;
    }

    public int getCompressedEndTimePoint() {
        return compressedEndTimePoint;
    }

    public void setCompressedEndTimePoint(int compressedEndTimePoint) {
        this.compressedEndTimePoint = compressedEndTimePoint;
    }

    public int getCompressedTimePointsCount(){
        return this.compressedEndTimePoint-this.compressedStartTimePoint;
    }
}

