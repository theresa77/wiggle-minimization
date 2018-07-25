package minwiggles.webfrontend.dto;

import minwiggles.basicstoryline.meeting.InteractionSession;
import minwiggles.basicstoryline.meeting.MeetingVariables;
import minwiggles.basicstoryline.meeting.MeetingsInformation;
import minwiggles.basicstoryline.moviedata.MovieData;

import java.util.*;

public class StorylineDTO {

    private String name;
    private String[][] storyline;
    private MeetingsInformation meetingsInformation;
    private MovieData movieData;
    private Map<Integer, List<List<List<Integer>>>> coordinatesMap;
    private Map<Integer, List<List<List<Integer>>>> coordinatesMapUnCompressed;

    private int wiggleHeightSum;
    private int highestWiggleHeight;
    private int wiggleCount;
    private int crossingsCount;

    public StorylineDTO(String name, String[][] storyline, String[][] storylineUnCompressed, MeetingsInformation meetingsInformation, MovieData movieData){
        this.name = name;
        this.storyline = storyline;
        this.meetingsInformation = meetingsInformation;
        this.movieData = movieData;
        this.coordinatesMap = new HashMap<>();
        // set correct coordinatesMap for small version of storyline D3 graphic
        this.coordinatesMap = calculateCharacterCoordinates(storyline, movieData.getNodeCount(), 30, 15);
        this.coordinatesMapUnCompressed = calculateCharacterCoordinates(storylineUnCompressed, movieData.getNodeCount(), 8, 20);
        this.calculateMeetingCoordinates(meetingsInformation, movieData);

//        // set correct coordinatesMap for medium version of storyline D3 graphic
//        this.coordinatesMap = calculateCharacterCoordinates(storyline, movieData.getNodeCount(), 70, 25);
//        this.coordinatesMapUnCompressed = calculateCharacterCoordinates(storylineUnCompressed, movieData.getNodeCount(), 10, 25);
//        this.calculateMeetingCoordinates(meetingsInformation, movieData);

//        // set correct coordinatesMap for big version of storyline D3 graphic
//        this.coordinatesMap = calculateCharacterCoordinates(storyline, movieData.getNodeCount(), 80, 45); //change yWeight for increasing height of picture, change xWeight for increasing width
//        this.coordinatesMapUnCompressed = calculateCharacterCoordinates(storylineUnCompressed, movieData.getNodeCount(), 10, 45);
//        this.calculateMeetingCoordinates(meetingsInformation, movieData);

        this.calculateMetrics();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[][] getStoryline() {
        return storyline;
    }

    public void setStoryline(String[][] storyline) {
        this.storyline = storyline;
    }

    public MovieData getMovieData() {
        return movieData;
    }

    public void setMovieData(MovieData movieData) {
        this.movieData = movieData;
    }

    public Map<Integer, List<List<List<Integer>>>> getCoordinatesMap() {
        return coordinatesMap;
    }

    public void setCoordinatesMap(Map<Integer, List<List<List<Integer>>>> coordinatesMap) {
        this.coordinatesMap = coordinatesMap;
    }

    public MeetingsInformation getMeetingsInformation() {
        return meetingsInformation;
    }

    public void setMeetingsInformation(MeetingsInformation meetingsInformation) {
        this.meetingsInformation = meetingsInformation;
    }

    public Map<Integer, List<List<List<Integer>>>> getCoordinatesMapUnCompressed() {
        return coordinatesMapUnCompressed;
    }

    public void setCoordinatesMapUnCompressed(Map<Integer, List<List<List<Integer>>>> coordinatesMapUnCompressed) {
        this.coordinatesMapUnCompressed = coordinatesMapUnCompressed;
    }

    public int getWiggleHeightSum() {
        return wiggleHeightSum;
    }

    public void setWiggleHeightSum(int wiggleHeightSum) {
        this.wiggleHeightSum = wiggleHeightSum;
    }

    public int getHighestWiggleHeight() {
        return highestWiggleHeight;
    }

    public void setHighestWiggleHeight(int highestWiggleHeight) {
        this.highestWiggleHeight = highestWiggleHeight;
    }

    public int getWiggleCount() {
        return wiggleCount;
    }

    public void setWiggleCount(int wiggleCount) {
        this.wiggleCount = wiggleCount;
    }

    public int getCrossingsCount() {
        return crossingsCount;
    }

    public void setCrossingsCount(int crossingsCount) {
        this.crossingsCount = crossingsCount;
    }

    private Map<Integer, List<List<List<Integer>>>> calculateCharacterCoordinates(String[][] storyline, int nodeCount, int xWeight, int yWeight){
        Map<Integer, List<List<List<Integer>>>> coordinatesMap = new HashMap<Integer, List<List<List<Integer>>>>();
        List<List<List<Integer>>> coordinatesList;
        List<List<Integer>> coordinates;
        List<Integer> simpleCoord;
        int lastX;

        for(int n=0; n<nodeCount; n++){
            coordinatesList = new LinkedList<>();
            coordinates = new LinkedList<>();
            lastX = -1;
            for(int x=0; x<storyline[0].length; x++){
                for(int y=0; y<storyline.length; y++){
                    if(storyline[y][x].equals(String.valueOf(n))){
                        if(lastX != (x-1) && lastX > -1) {
                            coordinatesList.add(coordinates);
                            coordinates = new LinkedList<>();
                        }
                        lastX = x;
                        simpleCoord = new LinkedList<Integer>();
                        simpleCoord.add(0,(x+1)*xWeight);
                        simpleCoord.add(1,(y+1)*yWeight);
                        coordinates.add(simpleCoord);
                    }
                }
            }
            coordinatesList.add(coordinates);
            coordinatesMap.put(n, coordinatesList);
        }
        return coordinatesMap;
    }

    private void calculateMeetingCoordinates(MeetingsInformation meetingsInformation, MovieData movieData){
        for(MeetingVariables meetingVariables: meetingsInformation.getMeetingVariables()){
            for(InteractionSession interactionSession: movieData.getInteractionSessions()){
                if(meetingVariables.getId() == interactionSession.getId()){
                    //set correct coordinate values for small version of storyline D3 graphic
                    meetingVariables.setStartX((interactionSession.getCompressedStartTimePoint()+1)*30);
                    meetingVariables.setEndX(interactionSession.getCompressedEndTimePoint()*30);
                    Double minY = (meetingVariables.getMinSlot()+1)*15;
                    meetingVariables.setMinY(minY.intValue());
                    Double maxY = (meetingVariables.getMaxSlot()+1)*15;
                    meetingVariables.setMaxY(maxY.intValue());

                    meetingVariables.setUmcomprStartX((interactionSession.getStartTimePoint()+1)*8);
                    meetingVariables.setUmcomprEndX(interactionSession.getEndTimePoint()*8);
                    minY = (meetingVariables.getMinSlot()+1)*20;
                    meetingVariables.setUmcomprMinY(minY.intValue());
                    maxY = (meetingVariables.getMaxSlot()+1)*20;
                    meetingVariables.setUmcomprMaxY(maxY.intValue());

//                    //set correct coordinate values for medium version of storyline D3 graphic
//                    meetingVariables.setStartX((interactionSession.getCompressedStartTimePoint()+1)*30);//not different from small version
//                    meetingVariables.setEndX(interactionSession.getCompressedEndTimePoint()*30);//not different from small version
//                    Double minY = (meetingVariables.getMinSlot()+1)*25;
//                    meetingVariables.setMinY(minY.intValue());
//                    Double maxY = (meetingVariables.getMaxSlot()+1)*25;
//                    meetingVariables.setMaxY(maxY.intValue());

//                    //set correct coordinate values for big version of storyline D3 graphic
//                    meetingVariables.setStartX((interactionSession.getCompressedStartTimePoint()+1)*80);
//                    meetingVariables.setEndX(interactionSession.getCompressedEndTimePoint()*80);
//                    Double minY = (meetingVariables.getMinSlot()+1)*45;
//                    meetingVariables.setMinY(minY.intValue());
//                    Double maxY = (meetingVariables.getMaxSlot()+1)*45;
//                    meetingVariables.setMaxY(maxY.intValue());
                }
            }
        }
    }

    private void calculateMetrics(){
        //metrics calculation for wiggleHeightSum, highestWiggleHeight, wiggleCount, crossingsCount
        // für die wiggles:
        // für jeden character die storyline durchgehen und dann einen time point mit dem nächsten vergleichen und einfach wie bei dem ILP model vorgehen
        // für crossings halt immer 2 characters mit einander vergleichen
        this.wiggleHeightSum = 0;
        this.wiggleCount = 0;
        this.highestWiggleHeight = -1;
        this.crossingsCount = 0;

        int[] characterIds = movieData.getCharacterIds();
        int wiggleHeight = -1;

        for(int charId: characterIds){
            for(int j=0; j<storyline.length; j++){
                for(int t=0; t<storyline[0].length-1; t++){
                    if(storyline[j][t].trim().equals(String.valueOf(charId))){
                        if(!storyline[j][t+1].trim().equals(String.valueOf(charId))){
                            // wiggle found
                            wiggleHeight = 0;
                            for(int j2=0; j2<j; j2++){
                                if(storyline[j2][t+1].trim().equals(String.valueOf(charId))){
                                    // end of wiggle found
                                    wiggleHeight = j - j2;
                                }
                            }
                            if(wiggleHeight == 0){
                                for(int j2=j+1; j2<storyline.length; j2++){
                                    if(storyline[j2][t+1].trim().equals(String.valueOf(charId))){
                                        // end of wiggle found
                                        wiggleHeight = j2 - j;
                                    }
                                }
                            }

                            this.wiggleHeightSum += wiggleHeight;
                            if(wiggleHeight > 0) this.wiggleCount += 1;

                            if(this.highestWiggleHeight==-1 || wiggleHeight>this.highestWiggleHeight){
                                this.highestWiggleHeight = wiggleHeight;
                            }
                        }
                    }
                }
            }
        }

        int charId1;
        int charId2;
        int char1Slot1;
        int char1Slot2;
        int char2Slot1;
        int char2Slot2;

        for(int t=0; t<storyline[0].length-1; t++){
            for(int charIdIndex1=0; charIdIndex1<characterIds.length-1; charIdIndex1++){
                charId1 = characterIds[charIdIndex1];
                char1Slot1 = -1;
                char1Slot2 = -1;
                for(int j=0; j<storyline.length; j++){
                    if(storyline[j][t].trim().equals(String.valueOf(charId1))){
                        char1Slot1 = j;
                    }
                    if(storyline[j][t+1].trim().equals(String.valueOf(charId1))){
                        char1Slot2 = j;
                    }
                }

                if(char1Slot1 != -1 && char1Slot2 != -1){
                    //character 1 at time point t and t+1 present

                    for(int charIdIndex2=charIdIndex1+1; charIdIndex2<characterIds.length; charIdIndex2++){
                        charId2 = characterIds[charIdIndex2];
                        char2Slot1 = -1;
                        char2Slot2 = -1;
                        for(int j=0; j<storyline.length; j++){
                            if(storyline[j][t].trim().equals(String.valueOf(charId2))){
                                char2Slot1 = j;
                            }
                            if(storyline[j][t+1].trim().equals(String.valueOf(charId2))){
                                char2Slot2 = j;
                            }
                        }

                        if(char2Slot1 != -1 && char2Slot2 != -1){
                            //character 2 at time point t and t+1 present
                            //compare positions of both characters to each other

                            if((char1Slot1 < char2Slot1 && char1Slot2 > char2Slot2) ||
                               (char1Slot1 > char2Slot1 && char1Slot2 < char2Slot2)){
                                //crossing found
                                this.crossingsCount += 1;
                            }
                        }
                    }
                }
            }
        }

        this.name += "twh_" + this.wiggleHeightSum + "-mwh_" + this.highestWiggleHeight + "-wc_" + this.wiggleCount + "-cc_" + this.crossingsCount;

    }

}
