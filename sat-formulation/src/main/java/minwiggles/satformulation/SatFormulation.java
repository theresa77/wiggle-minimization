package minwiggles.satformulation;

import minwiggles.basicstoryline.meeting.InteractionSession;
import minwiggles.basicstoryline.meeting.MeetingVariables;
import minwiggles.basicstoryline.meeting.MeetingsInformation;
import minwiggles.basicstoryline.moviedata.MovieData;
import minwiggles.basicstoryline.storyline.CompressedStoryline;
import minwiggles.basicstoryline.storyline.UncompressedStoryline;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.IntStream;

public class SatFormulation {

    private String movieName;
    private MovieData movieData;
    private String dateString;
    private String currentResourceDir;
    private String currentResultDir;
    private int variableCounter;
    private boolean minNumWiggle;

    private Map<Integer, String[][]> xVariablesPerCharacter = new HashMap<>();//key i of map for character, first index t of array for time point, second index j for slot
    private Map<Integer, String[][]> zVariablesPerCharacter = new HashMap<>();//key i of map for character, first index t of array for time point, second index j for slot
    private Map<Integer, String[]> zVariablesPerCharacterBinary = new HashMap<>();//key i of map for character, first index t of array for time point (slot not needed, because we are only interested in the number of wiggles)
    private Map<Integer, String[][]> sVariablesPerMeeting = new HashMap<>();// key m is the identifier of the meeting, first index t of array for time point, second index for counter of the s-var
    private Map<Integer, int[]> sVarSlotSet = new HashMap<>();// key s is the sVar as Integer and the value is the array of the slots belonging to the s-variable

    private Map<Integer, List<Clause>> wiggleHeightClausesPerCharacter = new HashMap<>();
    private Map<Integer, List<Clause>> wiggleCountClausesPerCharacter = new HashMap<>();
    private List<Clause> charPositionConstraintClauses = new ArrayList<>();
    private List<Clause> onlyOneCharPerSlotClauses = new ArrayList<>();
    private List<Clause> optionalZVarClauses = new ArrayList<>();
    private Map<Integer, List<Clause>> meetingPositionClausesMap = new HashMap<>();
    private List<Clause> meetingContinuePositionClauses = new ArrayList<>();
    private List<Clause> meetingConstraintClauses = new ArrayList<>();

    public SatFormulation(String movieName, MovieData movieData, boolean minNumWiggle){
        this.minNumWiggle = minNumWiggle;
        this.movieName = movieName;
        this.movieData = movieData;
        this.variableCounter = 1;
        ZonedDateTime date = ZonedDateTime.now(ZoneId.systemDefault());
        this.dateString = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm").format(date);

        initVariables();
        createClauses();
    }

    private void initVariables(){
        int[] activeTimePoints;
        String[][] xVars;

        for(int i=0; i<movieData.getNodeCount(); i++){
            activeTimePoints = movieData.getActiveTimePointsPerCharacter(i);
            xVars = new String[activeTimePoints.length][movieData.getMinSlotCount()];

            for(int t=0; t<activeTimePoints.length; t++){
                for(int j=0; j<movieData.getMinSlotCount(); j++){
                    xVars[t][j] = String.valueOf(variableCounter);
                    variableCounter++;
                }
            }

            for(int j=0; j<movieData.getMinSlotCount(); j++){
                for(int t=0; t<movieData.getCompressedTimePoints().length; t++){
                    final int[] activeTP = movieData.getActiveTimePointsPerCharacter(i);
                    final int currT = t;
                    OptionalInt tpIndex = IntStream.range(0, activeTP.length)
                            .filter(tIndex -> activeTP[tIndex]==currT)
                            .findFirst();
                }
            }

            xVariablesPerCharacter.put(i, xVars);
        }

        if(minNumWiggle){
            String[] zVars;

            for(int i=0; i<movieData.getNodeCount(); i++){
                activeTimePoints = movieData.getActiveTimePointsPerCharacter(i);
                zVars = new String[activeTimePoints.length-1];

                for(int t=0; t<zVars.length; t++){
                    zVars[t] = String.valueOf(variableCounter);
                    variableCounter++;
                }

                zVariablesPerCharacterBinary.put(i, zVars);
            }

        } else {

            String[][] zVars;

            for(int i=0; i<movieData.getNodeCount(); i++){
                activeTimePoints = movieData.getActiveTimePointsPerCharacter(i);
                zVars = new String[activeTimePoints.length-1][movieData.getMinSlotCount()];

                for(int t=0; t<zVars.length; t++){
                    for(int j=0; j<movieData.getMinSlotCount(); j++){
                        zVars[t][j] = String.valueOf(variableCounter);
                        variableCounter++;
                    }
                }

                zVariablesPerCharacter.put(i, zVars);


                for(int j=0; j<movieData.getMinSlotCount(); j++){
                    for(int t=0; t<movieData.getCompressedTimePoints().length; t++){
                        final int[] activeTP = movieData.getActiveTimePointsPerCharacter(i);
                        final int currT = t;
                        OptionalInt tpIndex = IntStream.range(0, activeTP.length)
                                .filter(tIndex -> activeTP[tIndex]==currT)
                                .findFirst();
                    }

                }
            }

        }

        int[] slotsOfsVar;

        for(InteractionSession meeting: movieData.getInteractionSessions()){
            //for every time point of the meeting (exclusive the end time point) there has to be a set of s-vars
            String[][] sVars = new String[meeting.getCompressedEndTimePoint()-meeting.getCompressedStartTimePoint()]
                    [movieData.getMinSlotCount()-(meeting.numberOfCharacters()-1)];

            for(int t=0; t<(meeting.getCompressedEndTimePoint()-meeting.getCompressedStartTimePoint()); t++){
                for(int s=0; s<(movieData.getMinSlotCount()-(meeting.numberOfCharacters()-1)); s++){
                    sVars[t][s] = String.valueOf(variableCounter);
                    slotsOfsVar = new int[meeting.numberOfCharacters()];

                    for(int j=0; j<meeting.numberOfCharacters(); j++) {
                        slotsOfsVar[j] = j + s;
                    }
                    sVarSlotSet.put(variableCounter, slotsOfsVar);
                    variableCounter++;
                }
            }
            sVariablesPerMeeting.put(meeting.getId(), sVars);
        }

    }

    private void createClauses(){
        for(int i=0; i<movieData.getNodeCount(); i++){
            createWiggleHeightClausesForCharacter(i);
            createCharPositionConstraintClauses(i);
        }
        createOnlyOneCharacterPerSlotClauses();

        for(InteractionSession m: movieData.getInteractionSessions()){
            createMeetingPositionVariables(m);
            createContinueMeetingPositionClauses(m);
            createMeetingConstraintClauses(m);
        }

        createOptionalZVarClauses();
    }

    public void createWiggleHeightClausesForCharacter(int characterId){
        List<Clause> clauses = new ArrayList<>();
        int[] activeTimePoints = movieData.getActiveTimePointsPerCharacter(characterId);
        String[][] xVars = xVariablesPerCharacter.get(characterId); // first index t for time point, second index j for slot

        if(minNumWiggle){
            String[] zVars = zVariablesPerCharacterBinary.get(characterId); // index t for time point

            for(int t=0; t<zVars.length; t++){

                for(int j=0; j<movieData.getMinSlotCount(); j++){
                    for(int h=0; h<movieData.getMinSlotCount(); h++){
                        if(h!=j){
                            clauses.add(createWiggleHeightClause(xVars[t][j], xVars[t+1][j], xVars[t+1][h], zVars[t]));
                        }
                    }
                }
            }

            wiggleCountClausesPerCharacter.put(characterId, clauses);

        } else {
            String[][] zVars = zVariablesPerCharacter.get(characterId); // first index t for time point, second index j for slot

            for(int t=0; t<zVars.length; t++){

                for(int j=1; j<movieData.getMinSlotCount(); j++){
                    for(int h=0; h<j; h++){
                        for(int f=0; f<(j-h); f++){
                            clauses.add(createWiggleHeightClause(xVars[t][j], xVars[t + 1][j], xVars[t + 1][h], zVars[t][f]));
                        }
                    }
                }

                for(int j=0; j<movieData.getMinSlotCount()-1; j++){
                    for(int h=j+1; h<movieData.getMinSlotCount(); h++){
                        for(int f=0; f<(h-j); f++){
                            clauses.add(createWiggleHeightClause(xVars[t][j], xVars[t+1][j], xVars[t+1][h], zVars[t][f]));
                        }
                    }
                }
            }

            wiggleHeightClausesPerCharacter.put(characterId, clauses);
        }
    }

    private Clause createBinaryWiggleClause(Literal xLitT1, Literal xLitT2, Literal zLit){
        Clause clause = new Clause();
        clause.addLiteral(xLitT1);
        clause.addLiteral(xLitT2);
        clause.addLiteral(zLit);
        return clause;
    }

    private Clause createWiggleHeightClause(String negXVarT1, String posXVarT2, String negXVarT2, String zVar){
        Clause clause = new Clause();
        clause.addLiteral(new Literal(false, negXVarT1));
        clause.addLiteral(new Literal(true, posXVarT2));
        clause.addLiteral(new Literal(false, negXVarT2));
        clause.addLiteral(new Literal(true, zVar));
        return clause;
    }

    public void createCharPositionConstraintClauses(int characterId){
        String[][] xVars = xVariablesPerCharacter.get(characterId); // first index t for time point, second index j for slot
        Clause clause;

        for(int t=0; t<xVars.length; t++){
            if(xVars.length>0){
                clause = new Clause();
                for(int j=0; j<xVars[0].length; j++){
                    clause.addLiteral(new Literal(true, xVars[t][j]));
                }
                charPositionConstraintClauses.add(clause);

                for(int j1=0; j1<xVars[0].length-1; j1++){
                    for(int j2=j1; j2<xVars[0].length; j2++){
                        if(j1!=j2){
                            clause = new Clause();
                            clause.addLiteral(new Literal(false, xVars[t][j1]));
                            clause.addLiteral(new Literal(false, xVars[t][j2]));
                            charPositionConstraintClauses.add(clause);
                        }
                    }
                }
            }
        }
    }

    public void createOnlyOneCharacterPerSlotClauses(){
        Clause clause;
        String[][] xVars1; // first index t for time point, second index j for slot
        String[][] xVars2;
        OptionalInt i1TPIndex;
        OptionalInt i2TPIndex;
        int i1Index;
        int i2Index;

        for(int t=0; t<movieData.getCompressedTPCount(); t++){
            for(int i1=0; i1<movieData.getNodeCount()-1; i1++){
                xVars1 = xVariablesPerCharacter.get(i1);
                final int[]activeTPi1 = movieData.getActiveTimePointsPerCharacter(i1);
                final int currT = t;
                i1TPIndex = IntStream.range(0, activeTPi1.length)
                        .filter(tIndex -> activeTPi1[tIndex]==currT)
                        .findFirst();

                if(i1TPIndex.isPresent()){
                    for(int i2=i1+1; i2<movieData.getNodeCount(); i2++){
                        final int[] activeTPi2 = movieData.getActiveTimePointsPerCharacter(i2);
                        i2TPIndex = IntStream.range(0, activeTPi2.length)
                                .filter(tIndex -> activeTPi2[tIndex]==currT)
                                .findFirst();

                        if(i1!=i2 & i2TPIndex.isPresent()){ //if both characters are active at the current time point t
                            xVars2 = xVariablesPerCharacter.get(i2);
                            i1Index = i1TPIndex.getAsInt();
                            i2Index = i2TPIndex.getAsInt();

                            for(int j=0; j<movieData.getMinSlotCount(); j++){
                                clause = new Clause();
                                clause.addLiteral(new Literal(false, xVars1[i1Index][j]));
                                clause.addLiteral(new Literal(false, xVars2[i2Index][j]));
                                onlyOneCharPerSlotClauses.add(clause);
                            }

                        }
                    }
                }
            }
        }

    }

    // create clauses for the formula describing a s-variable (position variable of a meeting)
    // and clauses which make sure the variable and the formula have to have the same truth value
    // (NOT s OR s-Formula) AND (s OR NOT s-Formula) --> with this formula s and the s-Formula have to have the same truth value
    public void createMeetingPositionVariables(InteractionSession meeting){
        Clause clause;
        String[][] xVars;
        String[][] sVars = sVariablesPerMeeting.get(meeting.getId());
        int[] counter;
        int[] slots;
        Literal notMemberPosLiteral1;
        Literal notMemberPosLiteral2;
        Literal notMemberNegLiteral1;
        Literal notMemberNegLiteral2;
        List<Clause> clauses = new ArrayList<>();
        List<Integer> notMembers = getAllNotMembersOfMeeting(meeting.getCharacterIds());
        List<Integer> notMembersAtTimePoint;
        OptionalInt notMemTPIndex;

        for(int t=0; t<meeting.getCompressedTimePointsCount(); t++){
            for(int s=0; s<sVars[t].length; s++){
                slots = sVarSlotSet.get(Integer.valueOf(sVars[t][s]));

                for(int j: slots){
                    clause = createPositivMeetingClause(sVars[t][s], meeting.getCharacterIds(), meeting.getCompressedStartTimePoint()+t, j);
                    clauses.add(clause);
                }

                if(notMembers.size() > 0){
                    //true, if there is a slot above the meeting
                    if(slots[0] > 0){
                        for(Integer notMember: notMembers){
                            if(isCharacterActiveAtTimePoint(notMember, meeting.getCompressedStartTimePoint()+t)){
                                final int[] notMemActiveTP = movieData.getActiveTimePointsPerCharacter(notMember);
                                final int currT = meeting.getCompressedStartTimePoint()+t;
                                notMemTPIndex = IntStream.range(0, notMemActiveTP.length)
                                        .filter(tIndex -> notMemActiveTP[tIndex]==currT)
                                        .findFirst();

                                if(notMemTPIndex.isPresent()){
                                    xVars = xVariablesPerCharacter.get(notMember);
                                    notMemberPosLiteral1 = new Literal(true, xVars[notMemTPIndex.getAsInt()][slots[0]-1]);
                                    notMemberNegLiteral1 = new Literal(false, xVars[notMemTPIndex.getAsInt()][slots[0]-1]);

                                    clause = new Clause();
                                    clause.addLiteral(notMemberNegLiteral1);
                                    clause.addLiteral(new Literal(false, sVars[t][s]));
                                    clauses.add(clause);

                                    counter = new int[meeting.numberOfCharacters()];
                                    for(int i=0; i<meeting.numberOfCharacters(); i++){
                                        counter[i] = meeting.getCharacterIds()[0];
                                    }

                                    clause = createNegatedMeetingClause(counter, sVars[t][s], meeting.getCompressedStartTimePoint()+t, slots);
                                    clause.addLiteral(notMemberPosLiteral1);
                                    clauses.add(clause);

                                    while(incrementCounterArray(counter, meeting.numberOfCharacters()-1, meeting.getCharacterIds())){
                                        clause = createNegatedMeetingClause(counter, sVars[t][s], meeting.getCompressedStartTimePoint()+t, slots);
                                        clause.addLiteral(notMemberPosLiteral1);
                                        clauses.add(clause);
                                    }
                                }
                            }
                        }
                    }

                    //true, if there is still a slot below the meeting
                    if(slots[slots.length-1] < (movieData.getMinSlotCount()-1)){
                        for(Integer notMember: notMembers){
                            if(isCharacterActiveAtTimePoint(notMember, meeting.getCompressedStartTimePoint()+t)){
                                final int[] notMemActiveTP = movieData.getActiveTimePointsPerCharacter(notMember);
                                final int currT = meeting.getCompressedStartTimePoint()+t;
                                notMemTPIndex = IntStream.range(0, notMemActiveTP.length)
                                        .filter(tIndex -> notMemActiveTP[tIndex]==currT)
                                        .findFirst();

                                if(notMemTPIndex.isPresent()){
                                    xVars = xVariablesPerCharacter.get(notMember);
                                    notMemberPosLiteral2 = new Literal(true, xVars[notMemTPIndex.getAsInt()][slots[slots.length-1]+1]);
                                    notMemberNegLiteral2 = new Literal(false, xVars[notMemTPIndex.getAsInt()][slots[slots.length-1]+1]);

                                    clause = new Clause();
                                    clause.addLiteral(notMemberNegLiteral2);
                                    clause.addLiteral(new Literal(false, sVars[t][s]));
                                    clauses.add(clause);

                                    counter = new int[meeting.numberOfCharacters()];
                                    for(int i=0; i<meeting.numberOfCharacters(); i++){
                                        counter[i] = meeting.getCharacterIds()[0];
                                    }

                                    clause = createNegatedMeetingClause(counter, sVars[t][s], meeting.getCompressedStartTimePoint()+t, slots);
                                    clause.addLiteral(notMemberPosLiteral2);
                                    clauses.add(clause);

                                    while(incrementCounterArray(counter, meeting.numberOfCharacters()-1, meeting.getCharacterIds())){
                                        clause = createNegatedMeetingClause(counter, sVars[t][s], meeting.getCompressedStartTimePoint()+t, slots);
                                        clause.addLiteral(notMemberPosLiteral2);
                                        clauses.add(clause);
                                    }
                                }
                            }
                        }
                    }

                    notMembersAtTimePoint = getAllNotMembersOfMeetingForTimepoint(meeting.getCharacterIds(), meeting.getCompressedStartTimePoint()+t);

                    if(slots[0] == 0 || slots[slots.length-1] == (movieData.getMinSlotCount()-1) || notMembersAtTimePoint.size()==0){
                        clauses.addAll(createNegatedMeetingClausesWithoutNotMembers(meeting, slots, sVars[t][s], meeting.getCompressedStartTimePoint()+t));
                    }

                } else {
                    clauses.addAll(createNegatedMeetingClausesWithoutNotMembers(meeting, slots, sVars[t][s], meeting.getCompressedStartTimePoint()+t));
                }
            }
        }

        meetingPositionClausesMap.put(meeting.getId(), clauses);

    }

    //NOTE: for the increment of the counter array to work correctly it is crucial that the meetingMember array is sorted in ascending order regarding the ID of the members
    private boolean incrementCounterArray(int[] counter, int currIndex, int[] meetingMembers){
        if(incrementAllowed(counter, meetingMembers[meetingMembers.length-1])){
            final int currMember = counter[currIndex];
            //first get the index of the member at the current position in the counter array
            OptionalInt mIndex = IntStream.range(0, counter.length)
                    .filter(m -> meetingMembers[m]==currMember)
                    .findFirst();
            if(mIndex.isPresent()) {
                if (mIndex.getAsInt() < (meetingMembers.length - 1)) {
                    //increment the counter array be replacing the member at the current position in the counter array
                    //by the next higher member
                    counter[currIndex] = meetingMembers[mIndex.getAsInt() + 1];
                    return true;

                } else if (mIndex.getAsInt() == (meetingMembers.length - 1)) {
                    if (currIndex > 0) {
                        counter[currIndex] = meetingMembers[0];
                        incrementCounterArray(counter, currIndex-1, meetingMembers);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        } else return false;

    }

    private boolean incrementAllowed(int[] counter, int lastMember){
        for(int i: counter){
            if(i < lastMember){
                return true;
            }
        }
        return false;
    }

    private Clause createPositivMeetingClause(String sVar, int[] meetingMembers, int timePoint,  int slot){
        String[][] xVars;
        Clause clause = new Clause();
        clause.addLiteral(new Literal(false, sVar));
        OptionalInt tIndex;

        for(int i: meetingMembers){
            xVars = xVariablesPerCharacter.get(i);
            final int[] notMemActiveTP = movieData.getActiveTimePointsPerCharacter(i);
            final int currT = timePoint;
            tIndex = IntStream.range(0, notMemActiveTP.length)
                    .filter(index -> notMemActiveTP[index]==currT)
                    .findFirst();
            if(tIndex.isPresent()){
                clause.addLiteral(new Literal(true, xVars[tIndex.getAsInt()][slot]));
            }
        }

        return clause;
    }

    //create negated clause without adding literal for row above or below
    private Clause createNegatedMeetingClause(int[] counter, String sVar, int timePoint, int[] slots){
        String[][] xVars;
        Clause clause = new Clause();
        clause.addLiteral(new Literal(true, sVar));
        OptionalInt tIndex;

        for(int j=0; j<slots.length; j++){
            xVars = xVariablesPerCharacter.get(counter[j]);
            final int[] activeTP = movieData.getActiveTimePointsPerCharacter(counter[j]);
            final int currT = timePoint;
            tIndex = IntStream.range(0, activeTP.length)
                    .filter(index -> activeTP[index]==currT)
                    .findFirst();
            if(tIndex.isPresent()){
                clause.addLiteral(new Literal(false, xVars[tIndex.getAsInt()][slots[j]]));
            }
        }
        return clause;
    }


    private List<Clause> createNegatedMeetingClausesWithoutNotMembers(InteractionSession meeting, int[] slots, String sVar, int timePoint){
        List<Clause> clauses = new ArrayList<>();
        Clause clause;
        int[] counter = new int[meeting.numberOfCharacters()];

        for(int i=0; i<meeting.numberOfCharacters(); i++){
            counter[i] = meeting.getCharacterIds()[0];
        }

        clause = createNegatedMeetingClause(counter, sVar, timePoint, slots);
        clauses.add(clause);

        while(incrementCounterArray(counter, meeting.numberOfCharacters()-1, meeting.getCharacterIds())){
            clause = createNegatedMeetingClause(counter, sVar, timePoint, slots);
            clauses.add(clause);
        }

        return clauses;
    }

    // (NOT s_sTP OR s_t') AND (s_sTP OR NOT s_t')
    public void createContinueMeetingPositionClauses(InteractionSession meeting){
        String[][] sVars = sVariablesPerMeeting.get(meeting.getId());
        if(sVars.length > 1){
            Clause clause;

            for(int t=1; t<sVars.length; t++){
                for(int s=0; s<sVars[t].length; s++){
                    clause = new Clause();
                    clause.addLiteral(new Literal(false, sVars[0][s]));
                    clause.addLiteral(new Literal(true, sVars[t][s]));
                    meetingContinuePositionClauses.add(clause);

                    clause = new Clause();
                    clause.addLiteral(new Literal(true, sVars[0][s]));
                    clause.addLiteral(new Literal(false, sVars[t][s]));
                    meetingContinuePositionClauses.add(clause);
                }
            }
        }
    }

    public void createMeetingConstraintClauses(InteractionSession meeting){
        String[][] sVars = sVariablesPerMeeting.get(meeting.getId());

        Clause clause = new Clause();
        for(int s=0; s<sVars[0].length; s++){
            clause.addLiteral(new Literal(true, sVars[0][s]));
        }
        meetingConstraintClauses.add(clause);

        if(sVars[0].length > 1){
            for(int s1=0; s1<sVars[0].length-1; s1++){
                for(int s2=(s1+1); s2<sVars[0].length; s2++){
                    clause = new Clause();
                    clause.addLiteral(new Literal(false, sVars[0][s1]));
                    clause.addLiteral(new Literal(false, sVars[0][s2]));
                    meetingConstraintClauses.add(clause);
                }
            }
        }
    }

    public void createOptionalZVarClauses(){
        Clause clause;

        if(minNumWiggle){
            String[] zVars; // index t for time point

            for(int i=0; i<movieData.getNodeCount(); i++){
                zVars = zVariablesPerCharacterBinary.get(i);
                for(int t=0; t<zVars.length; t++){
                    clause = new Clause();
                    clause.addLiteral(new Literal(false, zVars[t]));
                    optionalZVarClauses.add(clause);
                }
            }

        } else {

            String[][] zVars; // first index t for time point, second index j for slot

            for(int i=0; i<movieData.getNodeCount(); i++){
                zVars = zVariablesPerCharacter.get(i);
                for(int t=0; t<zVars.length; t++){
                    for(int j=0; j<movieData.getMinSlotCount(); j++){
                        clause = new Clause();
                        clause.addLiteral(new Literal(false, zVars[t][j]));
                        optionalZVarClauses.add(clause);
                    }
                }
            }
        }
    }

    public List<Integer> getAllNotMembersOfMeeting(int[] members){
        List<Integer> notMembers = new ArrayList<>();

        for(int i=0; i<movieData.getNodeCount(); i++){
            final int c = i;
            OptionalInt character = Arrays.stream(members)
                    .filter(m -> m==c)
                    .findFirst();
            if(!character.isPresent()){
                notMembers.add(c);
            }
        }

        return notMembers;
    }

    public List<Integer> getAllNotMembersOfMeetingForTimepoint(int[] members, int timePoint){
        List<Integer> notMembers = new ArrayList<>();

        for(int i=0; i<movieData.getNodeCount(); i++){
            final int c = i;
            OptionalInt character = Arrays.stream(members)
                    .filter(m -> m==c)
                    .findFirst();
            if(!character.isPresent()){
                if(isCharacterActiveAtTimePoint(c, timePoint)){
                    notMembers.add(c);
                }
            }
        }

        return notMembers;
    }

    private boolean isCharacterActiveAtTimePoint(int characterId, int timePoint){
        int[] activeTimePoints = this.movieData.getActiveTimePointsPerCharacter(characterId);
        boolean active = false;
        for(int aTP: activeTimePoints){
            if(aTP == timePoint){
                active = true;
                break;
            }
        }
        return active;
    }

    private boolean isCharacterActiveAtAnyTimePoint(int characterId, int[] timePoints){
        boolean active = false;
        for(int t: timePoints){
            if(isCharacterActiveAtTimePoint(characterId, t)){
                active = true;
                break;
            }
        }
        return active;
    }

    public void writeSatFormulationToFile(String fileName){
        ZonedDateTime date = ZonedDateTime.now(ZoneId.systemDefault());
        this.dateString = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm").format(date);
        this.currentResourceDir = "sat-formulation/wcnf-files/"+movieName+"_"+dateString;
        boolean success = (new File(currentResourceDir)).mkdirs();

        if(success){
            try {
                PrintWriter writer;
                if(minNumWiggle){
                    writer = new PrintWriter(currentResourceDir+"/"+fileName+"_binary_clauses.wcnf", "UTF-8");
                } else {
                    writer = new PrintWriter(currentResourceDir+"/"+fileName+"_clauses.wcnf", "UTF-8");
                }

                writer.println("c MaxSat for "+fileName+" instance");
                writer.println("c");
                writer.println("p wcnf "+(variableCounter-1)+" "+getNumberOfClauses()+" "+(optionalZVarClauses.size()+1));

                if(minNumWiggle){
                    for(List<Clause> clauseList: wiggleCountClausesPerCharacter.values()){
                        for(Clause clause: clauseList){
                            writer.println((optionalZVarClauses.size()+1)+" "+clause.toString());
                        }
                    }
                } else {
                    for(List<Clause> clauseList: wiggleHeightClausesPerCharacter.values()){
                        for(Clause clause: clauseList){
                            writer.println((optionalZVarClauses.size()+1)+" "+clause.toString());
                        }
                    }
                }

                for(Clause clause: charPositionConstraintClauses){
                    writer.println((optionalZVarClauses.size()+1)+" "+clause.toString());
                }
                for(Clause clause: onlyOneCharPerSlotClauses){
                    writer.println((optionalZVarClauses.size()+1)+" "+clause.toString());
                }
                for(List<Clause> clauseList: meetingPositionClausesMap.values()){
                    for(Clause clause: clauseList){
                        writer.println((optionalZVarClauses.size()+1)+" "+clause.toString());
                    }
                }
                for(Clause clause: meetingContinuePositionClauses){
                    writer.println((optionalZVarClauses.size()+1)+" "+clause.toString());
                }
                for(Clause clause: meetingConstraintClauses){
                    writer.println((optionalZVarClauses.size()+1)+" "+clause.toString());
                }
                for(Clause clause: optionalZVarClauses){
                    writer.println("1 "+clause.toString());
                }

                writer.close();

                System.out.println("[INFO] ------------------------------------------------------------------------ ");
                System.out.println("[INFO] ");
                if(minNumWiggle){
                    System.out.println("[INFO] Clauses for SAT formulation written to file: "+currentResourceDir+"/"+fileName+"_binary_clauses.wcnf");
                } else {
                    System.out.println("[INFO] Clauses for SAT formulation written to file: "+currentResourceDir+"/"+fileName+"_clauses.wcnf");
                }
                System.out.println("[INFO] ");
                System.out.println("[INFO] ------------------------------------------------------------------------ ");
                System.out.println("[INFO] ");
            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
            } catch (UnsupportedEncodingException unse) {
                unse.printStackTrace();
            }
        } else {
            System.out.println("[INFO] ");
            System.out.println("[FAILURE] Not so fast!");
            System.out.println("[FAILURE] Because we use the current timestamp as name of the result folder we can only create one new result per minute");
            System.out.println("[FAILURE] Just wait for a new minute to start and then try again");
            System.out.println("[INFO] ");
        }
    }

    private int getNumberOfClauses(){
        int counter = 0;

        for(int i=0; i<movieData.getNodeCount(); i++){
            List<Clause> clauses;
            if(minNumWiggle){
                clauses = wiggleCountClausesPerCharacter.get(i);
            } else {
                clauses = wiggleHeightClausesPerCharacter.get(i);
            }
            counter += clauses.size();
        }

        for(int i=0; i<movieData.getInteractionSessionCount(); i++){
            List<Clause> clauses = meetingPositionClausesMap.get(i);
            counter += clauses.size();
        }

        counter += charPositionConstraintClauses.size();
        counter += onlyOneCharPerSlotClauses.size();
        counter += optionalZVarClauses.size();
        counter += meetingContinuePositionClauses.size();
        counter += meetingConstraintClauses.size();

        return counter;
    }

    public void parseResult(String maxSatSolverName, String variablesString) throws Exception {
        Map<String, Boolean> variableValues = getVariableValues(variablesString);
        int wiggleHeightCounter = 0;
        String[][] xVars;
        int[] activeTimePoints;
        String[][] storyline = new String[movieData.getMinSlotCount()][movieData.getCompressedTPCount()];

        for(int j=0; j<movieData.getMinSlotCount(); j++){
            for(int t=0; t<movieData.getCompressedTPCount(); t++){
                storyline[j][t] = " ";
            }
        }

        for(int i=0; i<movieData.getNodeCount(); i++){
            activeTimePoints = movieData.getActiveTimePointsPerCharacter(i);
            xVars = xVariablesPerCharacter.get(i);

            for(int t=0; t<activeTimePoints.length; t++){
                for(int j=0; j<movieData.getMinSlotCount(); j++){
                    if(variableValues.get(xVars[t][j])){
                        if(storyline[j][activeTimePoints[t]].equals(" ")){
                            storyline[j][activeTimePoints[t]] = String.valueOf(i);
                        } else {
                            throw new Exception("Slot occupied by two characters at once: "+storyline[j][activeTimePoints[t]]+" and "+i);
                        }
                    }
                }
            }


            if(minNumWiggle){
                Map<Integer, Integer[]> zVarValuesBinaryPerCharacter = new HashMap<>();
                String[] zVars;

                zVars = zVariablesPerCharacterBinary.get(i);
                Integer[] zVarValues = new Integer[zVars.length];

                for(int tz=0; tz<zVars.length; tz++){
                    if(variableValues.get(zVars[tz])){
                        wiggleHeightCounter = wiggleHeightCounter+1;
                        zVarValues[tz] = 1;
                    } else {
                        zVarValues[tz] = 0;
                    }
                }

                zVarValuesBinaryPerCharacter.put(i, zVarValues);

            } else {
                Map<Integer, Integer[][]> zVarValuesPerCharacter = new HashMap<>();
                String[][] zVars;

                zVars = zVariablesPerCharacter.get(i);
                Integer[][] zVarValues = new Integer[zVars.length][zVars[0].length];

                for(int tz=0; tz<zVars.length; tz++){
                    for(int jz=0; jz<zVars[tz].length; jz++){
                        if(variableValues.get(zVars[tz][jz])){
                            wiggleHeightCounter = wiggleHeightCounter+1;
                            zVarValues[tz][jz] = 1;
                        } else {
                            zVarValues[tz][jz] = 0;
                        }
                    }
                }

                zVarValuesPerCharacter.put(i, zVarValues);
            }

        }

        ZonedDateTime date = ZonedDateTime.now(ZoneId.systemDefault());
        this.dateString = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm").format(date);
        this.currentResultDir = "sat-formulation/results/SAT_"+maxSatSolverName+"_"+movieName+"_"+dateString;
        boolean success = (new File(currentResultDir)).mkdirs();
        if (success) {
            String baseFileName = currentResultDir+"/"+movieName;
            writeParsedMovieDataToFile(baseFileName+".json", this.movieData);
            System.out.println("[INFO] ------------------------------------------------------------------------ ");
            System.out.println("[INFO] ");
            System.out.println("[INFO] Movie data written to file:             "+baseFileName+".json");

            baseFileName = baseFileName+"-obj_"+wiggleHeightCounter;
            writeCompressedStorylineToFile(baseFileName+"-compressedStoryline.json", storyline, wiggleHeightCounter);
            System.out.println("[INFO] Compressed storyline written to file:   "+baseFileName+"-compressedStoryline.json");
            writeUncompressedStorylineToFile(baseFileName+"-uncompressedStoryline.json", storyline, wiggleHeightCounter);
            System.out.println("[INFO] Uncompressed storyline written to file: "+baseFileName+"-uncompressedStoryline.json");
            writeMeetingInformationToFile(baseFileName+"-meetingsInformation.json", variableValues, wiggleHeightCounter);
            System.out.println("[INFO] Meetings information written to file:   "+baseFileName+"-meetingsInformation.json");
            System.out.println("[INFO] ");
            System.out.println("[INFO] ------------------------------------------------------------------------ ");
        } else {
            System.out.println("[INFO] ");
            System.out.println("[FAILURE] Not so fast!");
            System.out.println("[FAILURE] Because we use the current timestamp as name of the result folder we can only create one new result per minute");
            System.out.println("[FAILURE] Just wait for a new minute to start and then try again");
            System.out.println("[INFO] ");
        }
    }

    private void writeParsedMovieDataToFile(String fileName, MovieData movieData){
        try {
            File movieDataFile = new File(fileName);
            movieDataFile.createNewFile();
            FileOutputStream oFile = new FileOutputStream(movieDataFile, false);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(movieDataFile, movieData);
            oFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeCompressedStorylineToFile(String fileName, String[][] storyline, int wiggleHeightCounter){
        try {
            File compressedVisFile = new File(fileName);
            compressedVisFile.createNewFile();
            FileOutputStream oFile = new FileOutputStream(compressedVisFile, false);
            ObjectMapper mapper = new ObjectMapper();
            CompressedStoryline compressedStoryline = new CompressedStoryline(storyline, wiggleHeightCounter);
            mapper.writeValue(compressedVisFile, compressedStoryline);
            oFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeUncompressedStorylineToFile(String fileName, String[][] compressedStoryline, int wiggleHeightCounter){
        String[][] storyline = new String[movieData.getMinSlotCount()][movieData.getTimeCount()];
        int columnCount;

        for (int j = 0; j < movieData.getMinSlotCount(); j++) {
            columnCount = 0;
            for (int t=0; t < (movieData.getCompressedTimePoints().length-1); t++) {
                for (int u=0; u < (movieData.getCompressedTimePoints()[t+1]-movieData.getCompressedTimePoints()[t]); u++) {
                    storyline[j][columnCount] = compressedStoryline[j][t];
                    columnCount++;
                }
            }
        }

        try {
            File uncompressedVisFile = new File(fileName);
            uncompressedVisFile.createNewFile();
            FileOutputStream oFile = new FileOutputStream(uncompressedVisFile, false);
            ObjectMapper mapper = new ObjectMapper();
            UncompressedStoryline uncompressedStoryline = new UncompressedStoryline(storyline);
            mapper.writeValue(uncompressedVisFile, uncompressedStoryline);
            oFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeMeetingInformationToFile(String fileName, Map<String, Boolean> variableValues, int wiggleHeightCounter){
        MeetingVariables[] meetingVariables = new MeetingVariables[movieData.getInteractionSessionCount()];
        InteractionSession[] interactionSessions = movieData.getInteractionSessions();
        InteractionSession interactionSession;
        MeetingVariables meetingVars;
        String[][] sVars;
        int[] slots;

        for(int m=0; m<interactionSessions.length; m++){
            interactionSession = interactionSessions[m];
            sVars = sVariablesPerMeeting.get(interactionSession.getId());
            for(String[] sVarsForTP: sVars){
                for(String s: sVarsForTP){
                    if(variableValues.get(s)){
                        slots = sVarSlotSet.get(Integer.valueOf(s));
                        meetingVars = new MeetingVariables(interactionSession.getId(), interactionSession.getName(), slots[0], slots[slots.length-1]);
                        meetingVars.setCompressedStartT(interactionSession.getCompressedStartTimePoint());
                        meetingVars.setCompressedEndT(interactionSession.getCompressedEndTimePoint());
                        meetingVars.setStartT(interactionSession.getStartTimePoint());
                        meetingVars.setEndT(interactionSession.getEndTimePoint());
                        meetingVariables[m] = meetingVars;
                    }
                }
            }
        }

        MeetingsInformation meetingsInformation = new MeetingsInformation(meetingVariables);

        try {
            File meetingsInformationFile = new File(fileName);
            meetingsInformationFile.createNewFile();
            FileOutputStream oFile = new FileOutputStream(meetingsInformationFile, false);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(meetingsInformationFile, meetingsInformation);
            oFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, Boolean> getVariableValues(String variablesString){
        Map<String, Boolean> variableValues = new HashMap<>();
        String[] variables = variablesString.split(" ");

        for(String var: variables){
            if(var.startsWith("-")){
                variableValues.put(var.substring(1), false);
            } else {
                variableValues.put(var, true);
            }
        }

        return variableValues;
    }

}

