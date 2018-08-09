package minwiggles.basicstoryline.moviedata;

import minwiggles.basicstoryline.character.Character;
import minwiggles.basicstoryline.location.Location;
import minwiggles.basicstoryline.meeting.InteractionSession;

import java.util.*;
import java.util.stream.IntStream;

public class MovieData {

    private String movieName;
    private int timeCount; // number of time steps
    private int nodeCount; // number of characters
    private int interactionSessionCount; // number of interaction sessions
    private int locationCount; // number of unique locations

    private int lowerBoundWiggles = -1;
    private int lowerBoundCrossings = -1;
    private int minSlotCount = -1;

    private HashMap<Integer, Character> charactersMap = new HashMap<Integer, Character>();
    private HashMap<Integer, Location> locationsMap = new HashMap<Integer, Location>();
    private Map<Integer, int[]> characterActiveTimePoints = new HashMap<>();

    private List<Character> characters = new LinkedList<Character>();
    private List<Location> locations = new LinkedList<Location>();
    private List<InteractionSession> interactionSessions = new LinkedList<InteractionSession>();

    private Integer[] compressedTimePoints;
    private int compressedTPCount;
    private Set<Integer> compressedTimePointsSet;
    private Map<Integer, List<Integer>> meetingsAtCompressedTimePointsMap;
    private Map<Integer, List<InteractionSession>> completeMeetingsAtCompressedTimePointsMap;

    public MovieData(String movieName, int timeCount, int nodeCount, int interactionSessionCount, int locationCount){
        this.movieName = movieName;
        this.timeCount = timeCount;
        this.nodeCount = nodeCount;
        this.interactionSessionCount = interactionSessionCount;
        this.locationCount = locationCount;
        this.compressedTimePoints = new Integer[interactionSessionCount+1];
        this.compressedTimePointsSet = new HashSet<>();
        this.meetingsAtCompressedTimePointsMap = new HashMap<>();
        this.completeMeetingsAtCompressedTimePointsMap = new HashMap<>();
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

    public List<Character> getCharacters() {
        return characters;
    }

    public void setCharacters(List<Character> characters) {
        this.characters = characters;
    }

    public void addCharacter(Character character){
        this.characters.add(character);
        this.charactersMap.put(character.getId(), character);
    }

    public List<Location> getLocations() {
        return locations;
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }

    public void addLocation(Location location){
        this.locations.add(location);
        this.locationsMap.put(location.getId(), location);
    }

    public InteractionSession[] getInteractionSessions() {
        InteractionSession[] array = new InteractionSession[interactionSessions.size()];
        for(int i=0; i<interactionSessions.size(); i++){
            array[i] = interactionSessions.get(i);
        }
        return array;
    }

    public void setInteractionSessions(List<InteractionSession> interactionSessions) {
        this.interactionSessions = interactionSessions;
    }

    public void addInteractionSession(InteractionSession interactionSession){
        this.compressedTimePointsSet.add(interactionSession.getStartTimePoint());
        this.compressedTimePointsSet.add(interactionSession.getEndTimePoint());

        for(int characterId: interactionSession.getCharacterIds()){
            Character character = this.charactersMap.get(characterId);
            if(character.getStartTimePoint()==-1){
                character.setStartTimePoint(interactionSession.getStartTimePoint());
            }
            character.setEndTimePoint(interactionSession.getEndTimePoint());
        }

        this.interactionSessions.add(interactionSession);
    }

    public Integer[] getCompressedTimePoints() {
        return this.compressedTimePoints;
    }

    public void setCompressedTPCount(int compressedTPCount){
        this.compressedTPCount = compressedTPCount;
    }

    public int getCompressedTPCount() {
        if(compressedTimePoints != null ){
            this.compressedTPCount = this.compressedTimePoints.length;
        }
        return compressedTPCount;
    }

    public void compressTimePoints(){
        List<Integer> sortedList = new ArrayList<Integer>(this.compressedTimePointsSet);
        Collections.sort(sortedList);
        this.compressedTimePoints = new Integer[sortedList.size()];
        sortedList.toArray(this.compressedTimePoints);

        List<Integer> meetingIdsList;
        List<InteractionSession> meetingsList;

        for(int i=0; i<this.compressedTimePoints.length; i++){
            for(InteractionSession interactionSession: this.interactionSessions){
                if(interactionSession.getStartTimePoint() == this.compressedTimePoints[i]){
                    interactionSession.setCompressedStartTimePoint(i);
                }

                if(interactionSession.getEndTimePoint() == this.compressedTimePoints[i]){
                    interactionSession.setCompressedEndTimePoint(i);
//                    for(int characterId: interactionSession.getCharacterIds()){
//                        Character character = this.charactersMap.get(characterId);
//                        character.addCompressedTimePoint(i);
//                    }
                }

                if(this.compressedTimePoints[i]>=interactionSession.getStartTimePoint() && this.compressedTimePoints[i]<interactionSession.getEndTimePoint()){
                    if(this.meetingsAtCompressedTimePointsMap.containsKey(this.compressedTimePoints[i])){
                        meetingIdsList = this.meetingsAtCompressedTimePointsMap.get(this.compressedTimePoints[i]);
                        meetingsList = this.completeMeetingsAtCompressedTimePointsMap.get(this.compressedTimePoints[i]);
                    } else {
                        meetingIdsList = new LinkedList<>();
                        meetingsList = new LinkedList<>();
                    }
                    if(!meetingIdsList.contains(interactionSession.getId())){
                        meetingIdsList.add(interactionSession.getId());
                        meetingsList.add(interactionSession);
                    }
                    this.meetingsAtCompressedTimePointsMap.put(this.compressedTimePoints[i], meetingIdsList);
                    this.completeMeetingsAtCompressedTimePointsMap.put(this.compressedTimePoints[i], meetingsList);

                    for(int characterId: interactionSession.getCharacterIds()){
                        Character character = this.charactersMap.get(characterId);
                        character.addCompressedTimePoint(i);
                    }
                }
            }
        }
    }

    public Integer[] getMeetingsForCompressedTimePoint(int compressedTimePoint){
        List<Integer> meetings = meetingsAtCompressedTimePointsMap.get(compressedTimePoint);
        if(meetings == null){
            return new Integer[]{};
        } else {
            Integer[] result = new Integer[meetings.size()];
            meetingsAtCompressedTimePointsMap.get(compressedTimePoint).toArray(result);
            return result;
        }
    }

    public List<InteractionSession> getCompleteMeetingsForCompressedTimePoint(int compressedTimePoint){
        return completeMeetingsAtCompressedTimePointsMap.get(compressedTimePoint);
    }

    public List<InteractionSession> getOtherInteractionSessionsForTimePoint(int meetingId, int timePoint){
        List<InteractionSession> result = new LinkedList<InteractionSession>();
        for(InteractionSession interactionSession: this.interactionSessions){
            if(interactionSession.getId()!=meetingId &&
                    interactionSession.getCompressedStartTimePoint()<=timePoint && interactionSession.getCompressedEndTimePoint()>timePoint){
                result.add(interactionSession);
            }
        }
        return result;
    }

    public InteractionSession getInteractionSessionForTimePointAndCharacter(int timePoint, int character){
        for(InteractionSession interactionSession: this.interactionSessions){
            if(interactionSession.getStartTimePoint()<=timePoint && interactionSession.getEndTimePoint()>timePoint){
                int[] characters = interactionSession.getCharacterIds();
                for(int i=0; i<characters.length; i++){
                    if(characters[i] == character) return interactionSession;
                }
            }
        }
        return null;
    }

    public InteractionSession getInteractionSessionForId(int id){
        for(InteractionSession interactionSession: this.interactionSessions){
            if(interactionSession.getId() == id) return interactionSession;
        }
        return null;
    }


    /**
     * get a list of all meetings at a spectific timePoint. the timePoint must be the actual one, not the compressed one
     * @param timePoint not compressed timePoint
     * @return
     */
    public List<InteractionSession> getInteractionSessionsForTimePoint(int timePoint){
        List<InteractionSession> result = new LinkedList<InteractionSession>();
        for(InteractionSession interactionSession: this.interactionSessions){
            if(interactionSession.getStartTimePoint()<=timePoint && interactionSession.getEndTimePoint()>timePoint){
                result.add(interactionSession);
            }
        }
        return result;
    }

    public Character getCharacterForId(int id){
        for(Character character: this.characters){
            if(character.getId()==id) return character;
        }
        return null;
    }

    public int[] getCharacterIds(){
        this.characters.sort(Comparator.comparingInt(Character::getId));
        int[] characterIds = new int[this.characters.size()];
        for(int i=0; i<this.characters.size(); i++){
            characterIds[i] = this.characters.get(i).getId();
        }
        return characterIds;
    }

    public int getMinSlotCount(){
        if(minSlotCount == -1 && completeMeetingsAtCompressedTimePointsMap!=null){
            minSlotCount = this.nodeCount;
            int currentCount;
            List<InteractionSession> meetings;

            for(int timePoint: this.compressedTimePoints){
                currentCount = -1;
                meetings = completeMeetingsAtCompressedTimePointsMap.get(timePoint);
                if(meetings != null){
                    for(InteractionSession interactionSession: meetings){
                        currentCount += interactionSession.getCharacterIds().length + 1;
                    }
                    if(currentCount > minSlotCount){
                        minSlotCount = currentCount;
                    }
                }
            }
        }
        return minSlotCount;
    }

    /*
    Abfrage ob sich 2 Charactere eventuell kreuzen können vom angegebenen Zeitpunkt zum nächsten Zeitpunkt.
    Dies ist dann der Fall, wenn beide im aktuellen Zeitpunkt und im nächsten Zeitpunkt präsent sind
    und wenigstens einer der beiden sein Meeting wechselt, also seine Position ändern kann.
     */
    public boolean possibleCrossingOfCharactersForTimePoints(int characterId1, int characterId2, int currComprTimePoint, int nextComprTimePoint){
        boolean possiblePosChangeCharacter1 = possiblePositionChangeOfCharacterForTimePoints(characterId1, currComprTimePoint, nextComprTimePoint);
        boolean possiblePosChangeCharacter2 = possiblePositionChangeOfCharacterForTimePoints(characterId2, currComprTimePoint, nextComprTimePoint);
        return (possiblePosChangeCharacter1 || possiblePosChangeCharacter2);
    }

    /*
    Abfrage, ob ein character sein Meeting ändert von angegebenen Zeitpunkt zum nächsten
    es wird nur true zurück gegeben, wenn der character im aktuellen und nächsten zeitpunkt auch in einem Meeting vorkommt
     */
    public boolean possiblePositionChangeOfCharacterForTimePoints(int characterId, int currComprTimePoint, int nextComprTimePoint){
        int meetingCurrTimePoint = meetingOfCharacterForTimePoint(characterId, currComprTimePoint);
        int meetingNextTimePoint = meetingOfCharacterForTimePoint(characterId, nextComprTimePoint);
        return (meetingCurrTimePoint!=-1 && meetingNextTimePoint!=-1 && meetingCurrTimePoint!=meetingNextTimePoint);
    }

    /*
    Abfrage, ob character im angegeben Zeitpunkt auch in einem Meeting vorkommt
    zurückgegeben wir die MeetingsId in dem der character vorkommt
    kommt der character in keinem meeting vor, wird -1 zurückgegeben
     */
    public int meetingOfCharacterForTimePoint(int characterId, int comprTimePoint){
        List<InteractionSession> meetings = completeMeetingsAtCompressedTimePointsMap.get(comprTimePoint);
        if(meetings != null && meetings.size()>0){
            for(InteractionSession meeting: meetings){
                if(IntStream.of(meeting.getCharacterIds()).anyMatch(c -> c == characterId)){
                    return meeting.getId();
                }
            }
        }
        return -1;
    }

    public List<Integer> getCharacterIdsForTimePoint(int comprTimePoint){
        List<InteractionSession> meetings = completeMeetingsAtCompressedTimePointsMap.get(comprTimePoint);
        List<Integer> characterIds = new LinkedList<>();

        for(InteractionSession interactionSession: meetings){
            for(int i=0; i<interactionSession.getCharacterIds().length; i++){
                characterIds.add(interactionSession.getCharacterIds()[i]);
            }
        }
        return characterIds;
    }

    public List<Integer> getCharacterIdsPresentForTimePoints(int comprTimePoint){
        List<Integer> characterIds = getCharacterIdsForTimePoint(comprTimePoint);
        List<Integer> resultCharacters = new LinkedList<>();
        for(Integer characterId: characterIds){
            if(meetingOfCharacterForTimePoint(characterId, comprTimePoint)!=-1){
                resultCharacters.add(characterId);
            }
        }
        return resultCharacters;
    }

    public List<Integer> getCharacterIdsOfPossibleCrossingsForTimePoints(int currComprTimePoint, int nextComprTimePoint){
        List<Integer> characterIds = getCharacterIdsForTimePoint(currComprTimePoint);
        List<Integer> resultCharacters = new LinkedList<>();
        for(Integer characterId: characterIds){
            if(meetingOfCharacterForTimePoint(characterId, currComprTimePoint)!=-1 &&
                    meetingOfCharacterForTimePoint(characterId, nextComprTimePoint)!=-1){
                resultCharacters.add(characterId);
            }
        }
        return resultCharacters;
    }

    public int getLowerBoundForWiggleObj() {
        if(lowerBoundWiggles == -1){
            lowerBoundWiggles = 0;
            for(InteractionSession interactionSession: interactionSessions){
                lowerBoundWiggles += interactionSession.getCharacterIds().length-1;
            }
        }
        return lowerBoundWiggles;
    }

    public int getLowerBoundForCrossingsObj() {
        if(lowerBoundCrossings == -1){
            lowerBoundCrossings = 0;
            Set<Integer> otherCharacters;
            List<InteractionSession> interactionSessionsOfCharacter;
            for(Character character: characters){
                otherCharacters = new HashSet<>();
                interactionSessionsOfCharacter = getInteractionSessionsForCharacter(character.getId());
                for(InteractionSession interactionSession: interactionSessionsOfCharacter){
                    for(int c: interactionSession.getCharacterIds()){
                        if(c!=character.getId()) otherCharacters.add(c);
                    }
                }
                if(otherCharacters.size() > 2){
                    lowerBoundCrossings += (otherCharacters.size() - 2);
                }
            }
        }
        return lowerBoundCrossings;
    }

    public List<InteractionSession> getInteractionSessionsForCharacter(int characterId){
        List<InteractionSession> result = new LinkedList<>();
        for(InteractionSession interactionSession: interactionSessions){
            if(IntStream.of(interactionSession.getCharacterIds()).anyMatch(c -> c==characterId)){
                result.add(interactionSession);
            }
        }
        return result;
    }

    public void createActiveTimePointsPerCharacter(){
        int[] timePoints;
        List<Integer> sortedTimePointsList;

        for(Character character: this.characters){
            sortedTimePointsList = new ArrayList<>(character.getCompressedTimePointsSet());
            Collections.sort(sortedTimePointsList);

            timePoints = new int[sortedTimePointsList.size()];
            for(int t=0; t<timePoints.length; t++){
                timePoints[t] = sortedTimePointsList.get(t);
            }
            characterActiveTimePoints.put(character.getId(), timePoints);
        }
    }

    public int[] getActiveTimePointsPerCharacter(int characterId){
        return characterActiveTimePoints.get(characterId);
    }


    public int calcWorstCaseTotalWiggleHeight(){
        int result = 0;
        int jumpsCounter;
        List<InteractionSession> currentMeetings;

        for(Integer timePoint: this.completeMeetingsAtCompressedTimePointsMap.keySet()){
            currentMeetings = completeMeetingsAtCompressedTimePointsMap.get(timePoint);
            for(InteractionSession meeting: currentMeetings){
                if(meeting.getStartTimePoint() == timePoint){
                    jumpsCounter = this.minSlotCount-1;
                    for(int i=0; i<meeting.numberOfCharacters(); i++){
                        result += jumpsCounter;
                        jumpsCounter = jumpsCounter-1;
                    }
                }
            }
        }
        return result;
    }

    public int calcWorstCaseWiggleCount(){
        int result = 0;
        List<InteractionSession> currentMeetings;

        for(Integer timePoint: this.completeMeetingsAtCompressedTimePointsMap.keySet()){
            currentMeetings = completeMeetingsAtCompressedTimePointsMap.get(timePoint);
            for(InteractionSession meeting: currentMeetings){
                if(meeting.getStartTimePoint() == timePoint){
                    result += meeting.numberOfCharacters();
                }
            }
        }
        return result;
    }

    public String toString(){
        return "MovieData = {movieName="+movieName
                +", timeCount="+timeCount
                +", nodeCount="+nodeCount
                +", interactionSessionCount="+interactionSessionCount
                +", locationCount="+locationCount+"}";
    }

}

