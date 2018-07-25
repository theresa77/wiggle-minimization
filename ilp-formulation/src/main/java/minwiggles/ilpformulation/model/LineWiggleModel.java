package minwiggles.ilpformulation.model;

import minwiggles.basicstoryline.character.*;
import minwiggles.basicstoryline.character.Character;
import minwiggles.basicstoryline.crossing.CharacterCrossings;
import minwiggles.basicstoryline.crossing.CrossingsInformation;
import minwiggles.basicstoryline.meeting.InteractionSession;
import minwiggles.basicstoryline.meeting.MeetingComparison;
import minwiggles.basicstoryline.meeting.MeetingVariables;
import minwiggles.basicstoryline.meeting.MeetingsInformation;
import minwiggles.basicstoryline.moviedata.MovieData;
import minwiggles.basicstoryline.storyline.*;
import minwiggles.basicstoryline.wiggle.CompressedStorylineWiggles;
import minwiggles.basicstoryline.wiggle.CompressedStorylineWigglesOfCharacter;
import gurobi.*;

import java.io.*;
import java.lang.*;
import java.util.*;

public class LineWiggleModel {

    private GRBEnv env;
    private GRBModel model;
    private boolean minMaxWiggleH;
    private boolean binary;
    private boolean mindCrossings;
    private boolean sameWeighting = true;
    private boolean prioritizeWiggles = false;
    private boolean prioritizeCrossings = false;

    private GRBVar[][][] characterPositionMatrix;
    private GRBVar[][] wiggleMatrix;
    private GRBVar[][] wiggleMatrixBinary;
    private List<GRBVar[][]> meetingCharSlotList;
    private GRBVar[] maxSlots;
    private GRBVar[] minSlots;
    private Map<Integer, GRBVar[]> meetingsComparisonList; // key sind die compressed timePoints
    private Map<Integer, Map<CharacterPair, GRBVar>> characterComparisonMapList; // 1.key-set sind die compressed timePoints, zweites key-set sind die characterIds, die Liste in der zweiten Map beinhalten den vergleich des key characters mit allen anderen characteren
    private Map<Integer, GRBVar[]> characterCrossingsList; // key sind die compressed timePoints, die liste sind die vergleiche der characterPairs von einem zeitpunkt zum nächsten
    private GRBVar maxWiggleHeight; // variable holding the value of the height of the highest wiggle

    private MovieData movieData;
    private int slotCount;

    public LineWiggleModel(){}

    public GRBModel createGRBModel(boolean binary, MovieData movieData, boolean mindCrossings, boolean sameWeighting, boolean prioritizeWiggles, boolean prioritizeCrossings,
                                   boolean minMaxWiggleH, CompressedStoryline comprInitialStoryline){
        this.binary = binary;
        this.mindCrossings = mindCrossings;
        this.sameWeighting = sameWeighting;
        this.prioritizeWiggles = prioritizeWiggles;
        this.prioritizeCrossings = prioritizeCrossings;
        this.minMaxWiggleH = minMaxWiggleH;
        this.movieData = movieData;
        this.slotCount = movieData.getMinSlotCount();

        Properties prop = new Properties();
        InputStream input = null;

        try {
            File propertiesPath = new File("basic-storyline/src/main/resources/application.properties");
            input = new FileInputStream(propertiesPath.getAbsolutePath());
            prop.load(input);

//            String basePath = prop.getProperty("basePath");
//            String initialStorylinePathString = prop.getProperty("initialStoryline");
//            if(initialStorylinePathString != null){
//                String[] initialStorylinePaths = initialStorylinePathString.split(",");
//            }

            AllCompressedStorylinesPerCharacter allCompressedStorylinesPerCharacter = null;
            CompressedStorylineWiggles compressedStorylineWiggles = null;
            MeetingsInformation meetingsInformation = null;
            MeetingComparison meetingComparison = null;
            CrossingsInformation crossingsInformation = null;
            CharacterComparisonInformation characterComparisonInformation = null;

            //use comprInitialStoryline variable for initializing variables for character position, wiggle heights and meeting positions
            if(comprInitialStoryline != null){
                List<Character> characters = movieData.getCharacters();
                CompressedStorylineOfCharacter[] storylinesPerCharacter = new CompressedStorylineOfCharacter[characters.size()];
                CompressedStorylineOfCharacter compressedStorylineOfCharacter;
                String[][] initialStoryline = comprInitialStoryline.getStoryline();
                this.slotCount = initialStoryline.length;
                String[][] storyline;
                CompressedStorylineWigglesOfCharacter[] wigglesOfCharacters = new CompressedStorylineWigglesOfCharacter[characters.size()];
                CompressedStorylineWigglesOfCharacter compressedStorylineWigglesOfCharacter;
                double[] wiggleWeights;

                characters.sort(Comparator.comparingInt(Character::getId));

                // create separated storylines for each character
                for(int c=0; c<characters.size(); c++){
                    storyline = new String[initialStoryline.length][initialStoryline[0].length];
                    wiggleWeights = new double[initialStoryline[0].length];

                    for(int s=0; s<initialStoryline.length; s++){
                        for(int t=0; t<initialStoryline[0].length; t++){
                            if(initialStoryline[s][t].equals(String.valueOf(characters.get(c).getId()))){
                                storyline[s][t] = "1.0";
                                if((t+1) < initialStoryline[0].length){
                                    for(int nextS=0; nextS<initialStoryline.length; nextS++){
                                        if(initialStoryline[nextS][t+1].equals(String.valueOf(characters.get(c).getId())) && s!=nextS){
                                            wiggleWeights[t] = Math.abs(s-nextS);
                                        }
                                    }
                                }
                            } else {
                                storyline[s][t] = "0.0";
                            }
                        }
                    }
                    compressedStorylineOfCharacter = new CompressedStorylineOfCharacter(characters.get(c).getId(), storyline);
                    storylinesPerCharacter[c] = compressedStorylineOfCharacter;
                    compressedStorylineWigglesOfCharacter = new CompressedStorylineWigglesOfCharacter(characters.get(c).getId(), wiggleWeights);
                    wigglesOfCharacters[c] = compressedStorylineWigglesOfCharacter;
                }


                MeetingVariables[] allMeetingVariables = new MeetingVariables[movieData.getInteractionSessionCount()];
                MeetingVariables meetingVariables;
                InteractionSession[] interactionSessions = movieData.getInteractionSessions();
                double minSlot = 0;
                double maxSlot = 0;
                List<double[]> meetingComparisons = new LinkedList<>();
                Integer[] compressedTimePoints = movieData.getCompressedTimePoints();
                Integer[] meetingsForTimePoint;
                double[] meetingsComparisonArray;
                int comparisonCount;
                int meeting1Id;
                int meeting2Id;

                for(int i=0; i<interactionSessions.length; i++){
                    minSlot = -1;
                    maxSlot = -1;

                    for(int s=0; s<initialStoryline.length; s++){
                        for(int charId: interactionSessions[i].getCharacterIds()){
                            if((initialStoryline[s][interactionSessions[i].getCompressedStartTimePoint()]).equals(String.valueOf(charId))){
                                if(minSlot == -1 || s < minSlot) minSlot = s;
                                if(maxSlot == -1 || s > maxSlot) maxSlot = s;
                            }
                        }
                    }

                    meetingVariables = new MeetingVariables(interactionSessions[i].getId(), interactionSessions[i].getName(),
                            minSlot, maxSlot, interactionSessions[i].getCompressedStartTimePoint(), interactionSessions[i].getCompressedEndTimePoint());
                    allMeetingVariables[i] = meetingVariables;

                }

                //create meeting comparison lists
                for(int t=0; t<compressedTimePoints.length; t++){
                    meetingsForTimePoint = movieData.getMeetingsForCompressedTimePoint(compressedTimePoints[t]);
                    if(meetingsForTimePoint.length > 1){
                        meetingsComparisonArray = new double[meetingsForTimePoint.length * (meetingsForTimePoint.length-1)];

                        comparisonCount = 0;
                        for(int k=0; k<meetingsForTimePoint.length; k++){
                            meeting1Id = meetingsForTimePoint[k];
                            for(int l=0; l<meetingsForTimePoint.length; l++) {
                                if (k != l) {
                                    meeting2Id = meetingsForTimePoint[l];

                                    for(MeetingVariables mvs: allMeetingVariables){
                                        if(mvs.getId() == meeting1Id) maxSlot = mvs.getMaxSlot();
                                        else if(mvs.getId() == meeting2Id) minSlot = mvs.getMinSlot();
                                    }
                                    if(minSlot > maxSlot){
                                        meetingsComparisonArray[comparisonCount] = 1.0;
                                    }

                                    comparisonCount++;
                                }
                            }
                        }

                    } else {
                        meetingsComparisonArray = new double[0];
                    }

                    meetingComparisons.add(meetingsComparisonArray);
                }


                allCompressedStorylinesPerCharacter = new AllCompressedStorylinesPerCharacter(storylinesPerCharacter);
                compressedStorylineWiggles = new CompressedStorylineWiggles(wigglesOfCharacters);
                meetingsInformation = new MeetingsInformation(allMeetingVariables);
                meetingComparison = new MeetingComparison(meetingComparisons);

            }

            env   = new GRBEnv("ilp-formulation/log/lineWiggles.log");
            model = new GRBModel(env);

            // Create variables, set objective and add constraints
            initVariables();
            createVariables(allCompressedStorylinesPerCharacter, compressedStorylineWiggles, meetingsInformation, meetingComparison, crossingsInformation, characterComparisonInformation);
            setObjective();
            addConstraints();

        } catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
            e.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return model;
    }

    private void initVariables(){
        // variables for the matrix of a storyline
        Integer[] compressedTimePoints = movieData.getCompressedTimePoints();
        characterPositionMatrix = new GRBVar[slotCount][compressedTimePoints.length][movieData.getNodeCount()];
        wiggleMatrixBinary = new GRBVar[movieData.getNodeCount()][compressedTimePoints.length-1];
        wiggleMatrix = new GRBVar[movieData.getNodeCount()][compressedTimePoints.length-1];
        meetingCharSlotList = new LinkedList<GRBVar[][]>();
        maxSlots = new GRBVar[movieData.getInteractionSessionCount()];
        minSlots = new GRBVar[movieData.getInteractionSessionCount()];

        InteractionSession[] meetings = movieData.getInteractionSessions();
        for(InteractionSession meeting: meetings){
            GRBVar[][] meetingCharSlotVars = new GRBVar[meeting.numberOfCharacters()][slotCount];
            meetingCharSlotList.add(meetingCharSlotVars);
        }

        int size;

        // meetings comparison variables
        meetingsComparisonList = new HashMap<Integer, GRBVar[]>();
        for(int compressedTimePoint: compressedTimePoints){
            Integer[] meetingsArray = movieData.getMeetingsForCompressedTimePoint(compressedTimePoint);
            size = 0;
            if(meetingsArray.length>1){
                size = meetingsArray.length * (meetingsArray.length-1);
            }
            meetingsComparisonList.put(compressedTimePoint, new GRBVar[size]);
        }

        //comparison variables for character positions
        if(mindCrossings){
            characterComparisonMapList = new HashMap<Integer, Map<CharacterPair, GRBVar>>();//key it a compressed time point
            characterCrossingsList = new HashMap<Integer, GRBVar[]>();

            List<Integer> characterIds;
            Map<CharacterPair, GRBVar> characterComparisonList;

            for(int i=0; i<compressedTimePoints.length-1; i++){
                characterIds = movieData.getCharacterIdsPresentForTimePoints(compressedTimePoints[i]);
                characterComparisonList = new HashMap<CharacterPair, GRBVar>();

                for(Integer characterId1: characterIds){
                    for(Integer characterId2: characterIds){
                        if(!characterId1.equals(characterId2)){
                            characterComparisonList.put(new CharacterPair(characterId1, characterId2), null);
                        }
                    }
                }
                characterComparisonMapList.put(compressedTimePoints[i], characterComparisonList);

                characterCrossingsList.put(compressedTimePoints[i], new GRBVar[characterComparisonList.keySet().size()]);
            }
        }

    }

    private void createVariables(AllCompressedStorylinesPerCharacter allCompressedStorylinesPerCharacter,
                                 CompressedStorylineWiggles compressedStorylineWiggles,
                                 MeetingsInformation meetingsInformation,
                                 MeetingComparison meetingComparison,
                                 CrossingsInformation crossingsInformation,
                                 CharacterComparisonInformation characterComparisonInformation) throws GRBException {

        InteractionSession[] meetings = movieData.getInteractionSessions();
        Integer[] compressedTimePoints = movieData.getCompressedTimePoints();
        String st;
        CompressedStorylineOfCharacter[] compressedStorylinesPerCharacters = null;
        if(allCompressedStorylinesPerCharacter!=null){
            compressedStorylinesPerCharacters = allCompressedStorylinesPerCharacter.getStorylinesPerCharacter();
        }
        CompressedStorylineWigglesOfCharacter[] compressedStorylineWigglesOfCharacters = null;
        if(compressedStorylineWiggles!=null){
            compressedStorylineWigglesOfCharacters = compressedStorylineWiggles.getWigglesOfCharacters();
        }

        for (int j = 0; j < slotCount; j++) {
            for (int t = 0; t < compressedTimePoints.length; t++) {
                for (int i = 0; i < movieData.getNodeCount(); i++) {
                    st = "v-j_"+j+"-_t_"+t+"-_i_"+i;
                    characterPositionMatrix[j][t][i] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
                    if(allCompressedStorylinesPerCharacter!=null && compressedStorylinesPerCharacters!=null){
                        characterPositionMatrix[j][t][i].set(GRB.DoubleAttr.Start, Double.valueOf(compressedStorylinesPerCharacters[i].getStoryline()[j][t]));
                    }

                }
            }
        }

        // z variables for wiggles
        for (int i = 0; i < movieData.getNodeCount(); i++) {
            for (int t = 0; t < (compressedTimePoints.length-1); t++) {
                if(binary){
                    st = "v-binary-i_"+i+"-_transition_from_t_"+t+"_to_t_"+String.valueOf(t+1);
                    wiggleMatrix[i][t] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
                } else {
                    st = "v-binary-i_"+i+"-_transition_from_t_"+t+"_to_t_"+String.valueOf(t+1);
                    wiggleMatrixBinary[i][t] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
                    st = "v-integer-i_"+i+"-_transition_from_t_"+t+"_to_t_"+String.valueOf(t+1);
                    wiggleMatrix[i][t] = model.addVar(0.0, slotCount-1, 0.0, GRB.INTEGER, st);
                    if(compressedStorylineWiggles!=null && compressedStorylineWigglesOfCharacters!=null){
                        wiggleMatrix[i][t].set(GRB.DoubleAttr.Start, compressedStorylineWigglesOfCharacters[i].getWiggleWeights()[t]);
                    }
                }
            }
        }

        // variables for all meetings
        for(int x=0; x<meetings.length; x++){
            GRBVar[][] meetingCharSlot = meetingCharSlotList.get(x);

            st = "v-meeting_"+x+"-max_slot_for_t_"+String.valueOf(meetings[x].getCompressedStartTimePoint());
            maxSlots[x] = model.addVar(0.0, slotCount-1, 0.0, GRB.INTEGER, st);
            st = "v-meeting_"+x+"-min_slot_for_t_"+String.valueOf(meetings[x].getCompressedStartTimePoint());
            minSlots[x] = model.addVar(0.0, slotCount-1, 0.0, GRB.INTEGER, st);

            if (meetingsInformation != null) {
                for(MeetingVariables meetingVariables: meetingsInformation.getMeetingVariables()){
                    if(meetings[x].getId()==meetingVariables.getId()){
                        maxSlots[x].set(GRB.DoubleAttr.Start, meetingVariables.getMaxSlot());
                        minSlots[x].set(GRB.DoubleAttr.Start, meetingVariables.getMinSlot());
                    }
                }
            }

            for (int i = 0; i < meetings[x].numberOfCharacters(); i++) {
                for (int j = 0; j < slotCount; j++) {
                    st = "v-meeting_"+x+"-t_"+String.valueOf(meetings[x].getCompressedStartTimePoint())+"-_i_";
                    if(i<meetings[x].numberOfCharacters()) st += String.valueOf(meetings[x].getCharacterIds()[i])+"-j_"+j;
                    else st += String.valueOf(movieData.getNodeCount())+"-j_"+j;
                    meetingCharSlot[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
                }
            }
        }


        List<double[]> meetingComparisonsInitalList = null;
        if(meetingComparison!=null){
            meetingComparisonsInitalList = meetingComparison.getMeetingComparisons();
        }
        double[] meetingComparisonsInital = null;
        int t;
        // meetings comparison variables
        for(int i = 0; i< compressedTimePoints.length; i++){
            t = compressedTimePoints[i];
            GRBVar[] meetingsComparison = meetingsComparisonList.get(t);
            if(meetingComparison!=null && meetingComparisonsInitalList!=null){
                meetingComparisonsInital = meetingComparisonsInitalList.get(i);
            }

            if(meetingsComparison.length > 1){
                int comparisonCount = 0;
                Integer[] meetingsForTimePoint = movieData.getMeetingsForCompressedTimePoint(t);
                for(int k=0; k<meetingsForTimePoint.length; k++){
                    int meeting1Id = meetingsForTimePoint[k];
                    for(int l=0; l<meetingsForTimePoint.length; l++) {
                        if (k != l) {
                            int meeting2Id = meetingsForTimePoint[l];
                            st = "v-meeting-comparison-binary-for-t_"+t+"-and-meeting-pair_"+comparisonCount+"-for-m1Id_"+meeting1Id+"-m2Id_"+meeting2Id;
                            meetingsComparison[comparisonCount] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
                            if(meetingComparison!=null && meetingComparisonsInitalList!=null && meetingComparisonsInital!=null){
                                meetingsComparison[comparisonCount].set(GRB.DoubleAttr.Start, meetingComparisonsInital[comparisonCount]);
                            }
                            comparisonCount++;
                        }
                    }
                }
            }
        }

        // character position comparison variables
        if(mindCrossings){
            List<CharacterCrossings> characterCrossingsInitialList = null;
            if(crossingsInformation!=null){
                characterCrossingsInitialList = crossingsInformation.getCharacterCrossings();
            }

            List<Integer> characterIds;
            Map<CharacterPair, GRBVar> characterComparisonMap;
            GRBVar characterComparisonVar;
            GRBVar[] characterCrossingsVars;
            CharacterCrossings initialCharacterCrossings;
            double[] crossings = null;
            List<CharacterComparison> initialCharacterComparisons = null;

            for(int i=0; i<compressedTimePoints.length-1; i++){
                final int currTimePoint = compressedTimePoints[i];
                if(crossingsInformation!=null && characterCrossingsInitialList!=null){
                    initialCharacterCrossings = characterCrossingsInitialList.stream().filter(cc -> cc.getTimePoint()==currTimePoint).findFirst().get();
                    crossings = initialCharacterCrossings.getCrossingVariables();
                }

                if(characterComparisonInformation!=null){
                    initialCharacterComparisons = characterComparisonInformation.getCharacterComparisons().get(i);
                }

                characterComparisonMap = characterComparisonMapList.get(compressedTimePoints[i]);
                Set<CharacterPair> characterPairSet = characterComparisonMap.keySet();
                List<CharacterPair> characterPairList = new LinkedList<CharacterPair>(characterPairSet);
                characterPairList.sort(
                        Comparator.comparingInt(CharacterPair::getCharacterId1).
                                thenComparingInt(CharacterPair::getCharacterId2));

                for(CharacterPair characterPair: characterPairList){
                    st = "v-character-position-comparison-for-t_"+compressedTimePoints[i]+"-and-character-pair_-for-c1Id_"+characterPair.getCharacterId1()+"-above-c2Id_"+characterPair.getCharacterId2();
                    characterComparisonVar = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
                    if(characterComparisonInformation!=null && initialCharacterComparisons!=null){
                        characterComparisonVar.set(GRB.DoubleAttr.Start, (initialCharacterComparisons.stream()
                                .filter(cc -> cc.getCharacterPair().getCharacterId1()==characterPair.getCharacterId1() &&
                                        cc.getCharacterPair().getCharacterId2()==characterPair.getCharacterId2())
                                .findFirst().get()).getValue());
                    }
                    characterComparisonMap.put(characterPair, characterComparisonVar);
                }

                characterCrossingsVars = characterCrossingsList.get(compressedTimePoints[i]);

                for(int c=0; c<characterPairList.size(); c++){
                    st = "v-character-crossing-for-t_"+compressedTimePoints[i]+"to-timePoint-"+compressedTimePoints[i+1]+"-and-c1Id_"+characterPairList.get(c).getCharacterId1()+"-and-c2Id_"+characterPairList.get(c).getCharacterId2();
                    characterCrossingsVars[c] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
                    if(crossingsInformation!=null && characterCrossingsInitialList!=null && crossings!=null){
                        characterCrossingsVars[c].set(GRB.DoubleAttr.Start, crossings[c]);
                    }
                }

            }
        }

    }

    private void setObjective() throws GRBException {
        Integer[] compressedTimePoints = movieData.getCompressedTimePoints();

        GRBLinExpr objExprWiggle = new GRBLinExpr();

        if(minMaxWiggleH){
            maxWiggleHeight = model.addVar(0, slotCount, 0.0, GRB.INTEGER, "maxWiggleHeight-variable");

            GRBLinExpr exprWiggle;
            GRBLinExpr wiggleCounter = new GRBLinExpr();

            for (int i = 0; i < movieData.getNodeCount(); i++) {
                for (int t = 0; t < compressedTimePoints.length-1; t++) {
                    exprWiggle = new GRBLinExpr();
                    exprWiggle.addTerm(1.0, wiggleMatrix[i][t]);
                    model.addConstr(maxWiggleHeight, GRB.GREATER_EQUAL, exprWiggle, "objWiggle-constraint-for-wiggle-"+i+"-"+t);
                    wiggleCounter.addTerm(1, wiggleMatrixBinary[i][t]);
                }
            }

            objExprWiggle.addTerm(1.0, maxWiggleHeight);

        } else {
            // Set objective: minimize SUM_{i=0}^{n} SUM_{t=0}^{p-1} SUM_{j=1}^{m} z_{i,j}^t
            for (int i = 0; i < movieData.getNodeCount(); i++) {
                for (int t = 0; t < compressedTimePoints.length-1; t++) {
                    objExprWiggle.addTerm(1.0, wiggleMatrix[i][t]);
                }
            }
        }

        if(mindCrossings){
            /*
            * sameWeighting, prioritizeWiggles, prioritizeCrossings
            * */

            GRBLinExpr objExprCrossing = new GRBLinExpr();
            GRBVar[] characterCrossingVars;

            for(int t=0; t<(compressedTimePoints.length-1); t++){
                characterCrossingVars = characterCrossingsList.get(compressedTimePoints[t]);
                for(GRBVar crossingVar: characterCrossingVars){
                    objExprCrossing.addTerm(0.5, crossingVar);
                }
            }

            GRBLinExpr expr1 = new GRBLinExpr();
            GRBLinExpr expr2 = new GRBLinExpr();

            if(sameWeighting){
                expr1.multAdd(1.0, objExprWiggle); //weight for maxWiggle obj
                expr2.multAdd(1.0, objExprCrossing); //weight worst case for crossing

            } else {
                int weight = 1;

                if(prioritizeWiggles){
                    if(binary){
                        weight = movieData.calcWorstCaseWiggleCount();
                    } else {
                        weight = movieData.calcWorstCaseTotalWiggleHeight();
                    }
                    expr1.multAdd(weight, objExprWiggle); //weight for maxWiggle obj
                    expr2.multAdd(1.0, objExprCrossing); //weight worst case for crossing

                } else if (prioritizeCrossings){
                    weight = movieData.calcWorstCaseWiggleCount();
                    expr1.multAdd(1.0, objExprWiggle); //weight for maxWiggle obj
                    expr2.multAdd(weight, objExprCrossing); //weight worst case for crossing

                } else {
                    expr1.multAdd(weight, objExprWiggle); //weight for maxWiggle obj
                    expr2.multAdd(weight, objExprCrossing); //weight worst case for crossing
                }
            }

            GRBLinExpr objExpr = new GRBLinExpr();
            objExpr.add(expr1);
            objExpr.add(expr2);

            model.setObjective(objExpr, GRB.MINIMIZE);

        } else {
            if(minMaxWiggleH){
                model.setObjective(objExprWiggle, GRB.MINIMIZE);
            } else {
                model.addConstr(objExprWiggle, GRB.GREATER_EQUAL, 0, "objWiggle-constraint");
                model.setObjective(objExprWiggle, GRB.MINIMIZE);
            }
        }

    }

    private void addConstraints() throws GRBException {
        String st;
        InteractionSession[] meetings = movieData.getInteractionSessions();
        Integer[] compressedTimePoints = movieData.getCompressedTimePoints();

        GRBLinExpr expr;
        GRBLinExpr expr1;
        GRBLinExpr expr2;

        // each slot can only be occupied by at most one character at one timePoint
        for (int j = 0; j < slotCount; j++) {
            for (int t = 0; t < compressedTimePoints.length; t++) {
                expr = new GRBLinExpr();
                for (int i = 0; i < movieData.getNodeCount(); i++) {
                    expr.addTerm(1.0, characterPositionMatrix[j][t][i]);
                }
                st = "c-slot_"+j+"-_t_"+t;
                model.addConstr(expr, GRB.LESS_EQUAL, 1.0, st);
            }
        }

        // calculate wiggles and add constraints
        int[] characterIds = movieData.getCharacterIds();

        for (int i = 0; i < movieData.getNodeCount(); i++) {
            Character currentCharacter = movieData.getCharacterForId(characterIds[i]);
            Integer[] compressedTimePointsArray = new Integer[currentCharacter.getCompressedTimePointsSet().size()];
            currentCharacter.getCompressedTimePointsSet().toArray(compressedTimePointsArray);
            Arrays.sort(compressedTimePointsArray);

            //only add constraints for the timePoints the character is present in the storyline
            for(int t=0; t<compressedTimePointsArray.length-1; t++){
                if(compressedTimePointsArray[t+1] == (compressedTimePointsArray[t]+1)){

                    expr1 = new GRBLinExpr();
                    for (int j = 0; j < slotCount; j++) {
                        expr1.addTerm((j+1), characterPositionMatrix[j][compressedTimePointsArray[t]][i]);
                        expr1.addTerm((-j-1), characterPositionMatrix[j][compressedTimePointsArray[t+1]][i]);
                    }

                    if(binary){
                        st = "c1-wiggle-binary-for-i_"+i+"-_transition_from_t_"+String.valueOf(compressedTimePointsArray[t])+"_to_t_"+String.valueOf(compressedTimePointsArray[t+1]);
                        model.addGenConstrIndicator(wiggleMatrix[i][compressedTimePointsArray[t]], 0, expr1, GRB.EQUAL, 0.0, st);
                    } else {
                        st = "c1-wiggle-binary-for-i_"+i+"-_transition_from_t_"+String.valueOf(compressedTimePointsArray[t])+"_to_t_"+String.valueOf(compressedTimePointsArray[t+1]);
                        model.addGenConstrIndicator(wiggleMatrixBinary[i][compressedTimePointsArray[t]], 0, expr1, GRB.EQUAL, 0.0, st);
                        // the higher the wiggle the higher the costs for it
                        st = "c1-wiggle-for-i_"+i+"-_transition_from_t_"+String.valueOf(compressedTimePointsArray[t])+"_to_t_"+String.valueOf(compressedTimePointsArray[t+1]);
                        model.addConstr(expr1, GRB.LESS_EQUAL, wiggleMatrix[i][compressedTimePointsArray[t]], st);

                        expr2 = new GRBLinExpr();
                        expr2.multAdd(-1.0, expr1);
                        st = "c2-wiggle-for-i_"+i+"-_transition_from_t_"+String.valueOf(compressedTimePointsArray[t])+"_to_t_"+String.valueOf(compressedTimePointsArray[t+1]);
                        model.addConstr(expr2, GRB.LESS_EQUAL, wiggleMatrix[i][compressedTimePointsArray[t]], st);
                    }
                }
            }
        }

        // constraints for interaction sessions

        for(int x=0; x<meetings.length; x++) {
            InteractionSession currentMeeting = meetings[x];

            //iteration über alle timePoints des meetings
            for(int t=currentMeeting.getCompressedStartTimePoint(); t<currentMeeting.getCompressedEndTimePoint(); t++){

                GRBVar[][] meetingCharSlot = meetingCharSlotList.get(x);
                GRBVar maxSlot = maxSlots[x];
                GRBVar minSlot = minSlots[x];

                for (int i = 0; i < currentMeeting.numberOfCharacters(); i++) {
                    expr = new GRBLinExpr();
                    for (int j = 0; j < slotCount; j++) {
                        expr.addTerm(1.0, meetingCharSlot[i][j]);
                    }
                    st = "c-meeting_"+x+"-slot-for-i_"+currentMeeting.getCharacterIds()[i]+"-_t_"+String.valueOf(t);
                    model.addConstr(expr, GRB.EQUAL, 1.0, st);
                }

                for (int j = 0; j < slotCount; j++) {

                    for (int i = 0; i < currentMeeting.numberOfCharacters(); i++) {
                        // connection of positionMatrix of all characters and slotMatrix of interaction session
                        expr1 = new GRBLinExpr();
                        expr1.addTerm(1.0, characterPositionMatrix[j][t][currentMeeting.getCharacterIds()[i]]);
                        expr2 = new GRBLinExpr();
                        expr2.addTerm(1.0, meetingCharSlot[i][j]);
                        st = "c-meeting_"+x+"-slot_same-value-for-t_"+String.valueOf(t)+"-_i_"+String.valueOf(currentMeeting.getCharacterIds()[i])+"-slot_"+j;
                        model.addConstr(expr1, GRB.EQUAL, expr2, st);
                    }

                    // if expr == 1 is true:
                    // j <= maxSlot
                    // j >= minSlot
                    for (int i = 0; i < currentMeeting.numberOfCharacters(); i++) {
                            expr = new GRBLinExpr();
                            expr.addTerm(1.0,  maxSlot);
                            st = "c-meeting_"+x+"-slot-smaller-than-maxSlot-for-t_"+String.valueOf(t)+"-_j_"+j+"-_i_"+(currentMeeting.getCharacterIds())[i];
                            model.addGenConstrIndicator(meetingCharSlot[i][j], 1, expr, GRB.GREATER_EQUAL, j, st);

                            expr = new GRBLinExpr();
                            expr.addTerm(1.0,  minSlot);
                            st = "c-meeting_"+x+"-slot-greater-than-minSlot-for-t_"+String.valueOf(t)+"-_j_"+j+"-_i_"+(currentMeeting.getCharacterIds())[i];
                            model.addGenConstrIndicator(meetingCharSlot[i][j], 1, expr, GRB.LESS_EQUAL, j, st);
                    }
                }

                expr = new GRBLinExpr();
                expr.addTerm(1.0, maxSlot); expr.addTerm(-1.0, minSlot);

                st = "c-meeting_"+x+"_distance-from_max_to_min_slot_for_t_"+String.valueOf(t)+"-";
                model.addConstr(expr, GRB.EQUAL,(currentMeeting.numberOfCharacters()-1), st);

            }
        }

        // constraint for meetings comparison variables
        for(int t: compressedTimePoints){
            GRBVar[] meetingsComparison = meetingsComparisonList.get(t);
            if(meetingsComparison.length > 1){
                int comparisonCount = 0;
                Integer[] meetingsForCTP = movieData.getMeetingsForCompressedTimePoint(t);
                for(int k=0; k<meetingsForCTP.length; k++){
                    int meeting1Id = meetingsForCTP[k];
                    for(int l=0; l<meetingsForCTP.length; l++) {
                        if (k != l) {
                            int meeting2Id = meetingsForCTP[l];

                            expr = new GRBLinExpr();
                            expr.addTerm(1.0, minSlots[meeting2Id]);
                            expr.addTerm(-1.0, maxSlots[meeting1Id]);
                            st = "c-meetings-comparison-for-t_"+t+"_"+comparisonCount+"-for-m1Id_"+meeting1Id+"-m2Id_"+meeting2Id;
                            if(model==null){
                                System.out.println("model is null");
                            }
                            if(meetingsComparison==null){
                                System.out.println("meetingsComparison is null");
                            }
                            if(meetingsComparison[comparisonCount]==null){
                                System.out.println("meetingsComparison[comparisonCount] is null");
                            }
                            if(expr==null){
                                System.out.println("expr is null");
                            }
                            model.addGenConstrIndicator(meetingsComparison[comparisonCount], 1, expr, GRB.GREATER_EQUAL, 2.0, st);

                            comparisonCount++;
                        }
                    }
                }

                int gapCount = meetingsForCTP.length - 1;
                int compCounter ;
                int lhsCounter;
                int rhsCounter;

                for(int i=0; i<gapCount; i++){
                    lhsCounter = i * meetingsForCTP.length;
                    rhsCounter = lhsCounter + (meetingsForCTP.length - 1);
                    compCounter = (meetingsForCTP.length - 1) - i;
                    for(int j=0; j<compCounter; j++){
                        expr = new GRBLinExpr();
                        expr.addTerm(1.0, meetingsComparison[lhsCounter]);
                        expr.addTerm(1.0, meetingsComparison[rhsCounter]);
                        st = "c-meeting-comparison-for-timePoint-t_"+t+"-m1_"+lhsCounter+"-m2_"+rhsCounter+"-less-equal-to-1";
                        model.addConstr(expr, GRB.EQUAL, 1.0, st);

                        lhsCounter += 1;
                        rhsCounter += (meetingsForCTP.length - 1);
                    }
                }
            }
        }

        // constraint for gab between meetings with character position comparison variables
        Map<CharacterPair, GRBVar> characterComparisonMap;

        //constraint for character position comparison variables
        if(mindCrossings){
            GRBLinExpr expr3;
            GRBVar characterComparisonVar;
            List<CharacterPair> characterPairList;
            int characterId1;
            int characterId2;
            GRBVar[] characterCrossingsVars;
            GRBVar currCharacterComparisonVar;
            GRBVar nextCharacterComparisonVar;
            Map<CharacterPair, GRBVar> nextCharacterComparisonMap;

            for(int i=0; i<compressedTimePoints.length-1; i++){
                characterComparisonMap = characterComparisonMapList.get(compressedTimePoints[i]);
                nextCharacterComparisonMap = characterComparisonMapList.get(compressedTimePoints[i+1]);

                characterPairList = new LinkedList<CharacterPair>(characterComparisonMap.keySet());
                characterPairList.sort(
                        Comparator.comparingInt(CharacterPair::getCharacterId1).
                                thenComparingInt(CharacterPair::getCharacterId2));

                for(CharacterPair characterPair: characterPairList){
                    characterId1 = characterPair.getCharacterId1();
                    characterId2 = characterPair.getCharacterId2();

                    expr1 = new GRBLinExpr();
                    for (int j = 0; j < slotCount; j++) {
                        expr1.addTerm((j+1), characterPositionMatrix[j][i][characterId1]);
                    }
                    expr3 = new GRBLinExpr();
                    expr3.multAdd(-1.0, expr1);

                    expr2 = new GRBLinExpr();
                    for (int j = 0; j < slotCount; j++) {
                        expr2.addTerm((j+1), characterPositionMatrix[j][i][characterId2]);
                    }

                    expr = new GRBLinExpr();
                    expr.add(expr2);
                    expr.add(expr3);
                    characterComparisonVar = characterComparisonMap.get(characterPair);
                    st = "c-character-position-comparison-for-timePoint-t_"+compressedTimePoints[i]+"-c1_"+characterId1+"-above-c2_"+characterId2;
                    model.addGenConstrIndicator(characterComparisonVar, 1, expr, GRB.GREATER_EQUAL, 1.0, st);
                }

                // y(a,b) + y(b,a) = 1
                CharacterPair reverseCharacterPair;
                for(CharacterPair characterPair: characterPairList){
                    reverseCharacterPair = new CharacterPair(characterPair.getCharacterId2(), characterPair.getCharacterId1());

                    expr = new GRBLinExpr();
                    expr.addTerm(1.0, characterComparisonMap.get(characterPair));
                    expr.addTerm(1.0, characterComparisonMap.get(reverseCharacterPair));
                    st = "c-character-pos-comparison-for-timePoint-t_"+compressedTimePoints[i]+"-c1_"+characterPair.getCharacterId1()+"-plus-c2_"+characterPair.getCharacterId2()+"-has-to-be-one";
                    model.addConstr(expr, GRB.EQUAL, 1.0, st);
                }


                characterCrossingsVars = characterCrossingsList.get(compressedTimePoints[i]);

                if(nextCharacterComparisonMap!=null){
                    for(int c=0; c<characterPairList.size(); c++){
                        if(nextCharacterComparisonMap.containsKey(characterPairList.get(c))){
                            currCharacterComparisonVar = characterComparisonMap.get(characterPairList.get(c));
                            nextCharacterComparisonVar = nextCharacterComparisonMap.get(characterPairList.get(c));

                            expr = new GRBLinExpr();
                            expr.addTerm(1.0, currCharacterComparisonVar);
                            expr.addTerm(-1.0, nextCharacterComparisonVar);

                            //there is no crossing if the characters do not change their relative position to each other
                            //if there is a crossing the expr should evaluate to -1 or 1 and thus the characterCrossingVar should get the value 1
                            st = "c-character-crossing-for-timePoint-t_"+compressedTimePoints[i]+"-to-timePoint"+compressedTimePoints[i+1]+"-c1_"+characterPairList.get(c).getCharacterId1()+"-and-c2_"+characterPairList.get(c).getCharacterId2();
                            model.addGenConstrIndicator(characterCrossingsVars[c], 0, expr, GRB.EQUAL, 0.0, st);
                        }
                    }

                }

            }
        }

    }

    public void disposeModel(){
        // Dispose of model and environment
        try {
            model.dispose();
            env.dispose();
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    public boolean isBinary() {
        return binary;
    }

    public GRBVar[][][] getCharacterPositionMatrix() {
        return characterPositionMatrix;
    }

    public void setCharacterPositionMatrix(GRBVar[][][] characterPositionMatrix) {
        this.characterPositionMatrix = characterPositionMatrix;
    }

    public GRBVar[][] getWiggleMatrix() {
        return wiggleMatrix;
    }

    public MovieData getMovieData() {
        return movieData;
    }

    public List<GRBVar[][]> getMeetingCharSlotList() {
        return meetingCharSlotList;
    }

    public GRBVar[] getMaxSlots() {
        return maxSlots;
    }

    public GRBVar[] getMinSlots() {
        return minSlots;
    }

    public GRBModel getModel() {
        return model;
    }

    public int getSlotCount() {
        return slotCount;
    }

    public Map<Integer, GRBVar[]> getMeetingsComparisonList() {
        return meetingsComparisonList;
    }

    public Map<Integer, Map<CharacterPair, GRBVar>> getCharacterComparisonMapList() {
        return characterComparisonMapList;
    }

    public Map<Integer, GRBVar[]> getCharacterCrossingsList() {
        return characterCrossingsList;
    }

    public boolean mindCrossings(){ return mindCrossings; }
}
