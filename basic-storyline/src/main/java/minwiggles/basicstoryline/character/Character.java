package minwiggles.basicstoryline.character;

import java.util.HashSet;
import java.util.Set;

public class Character {

    private int id;
    private String name;
    private int startTimePoint;
    private int endTimePoint;
    private int compressedStartTimePoint;
    private int compressedEndTimePoint;
    private Set<Integer> compressedTimePointsSet;

    public Character(int id, String name){
        this(id, name, -1, -1, -1, -1);
    }

    public Character(int id, String name, int compressedStartTimePoint, int compressedEndTimePoint, int startTimePoint, int endTimePoint){
        this.id = id;
        this.name = name;
        this.compressedStartTimePoint = compressedStartTimePoint;
        this.compressedEndTimePoint = compressedEndTimePoint;
        this.startTimePoint = startTimePoint;
        this.endTimePoint = endTimePoint;
        this.compressedTimePointsSet = new HashSet<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Set<Integer> getCompressedTimePointsSet() {
        return compressedTimePointsSet;
    }

    public void setCompressedTimePointsSet(Set<Integer> compressedTimePointsSet) {
        this.compressedTimePointsSet = compressedTimePointsSet;
    }

    public void addCompressedTimePoint(int timePoint){
        this.compressedTimePointsSet.add(timePoint);
    }
}
