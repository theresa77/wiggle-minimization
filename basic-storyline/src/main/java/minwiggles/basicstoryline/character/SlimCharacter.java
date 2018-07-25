package minwiggles.basicstoryline.character;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SlimCharacter {

    private int id;
    private String name;
    private int startTimePoint;
    private int endTimePoint;
    private int compressedStartTimePoint;
    private int compressedEndTimePoint;

    public SlimCharacter(Character character){
        this.id = character.getId();
        this.name = character.getName();
        this.startTimePoint = character.getStartTimePoint();
        this.endTimePoint = character.getEndTimePoint();
        Set<Integer> compressedTimePointsSet = character.getCompressedTimePointsSet();
        List<Integer> compressedTimePointsList = new ArrayList<>(compressedTimePointsSet);
        Collections.sort(compressedTimePointsList);
        this.compressedStartTimePoint = compressedTimePointsList.get(0);
        this.compressedEndTimePoint = compressedTimePointsList.get(compressedTimePointsList.size()-1);
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
}
