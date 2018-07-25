package minwiggles.ilpformulation.model;

import minwiggles.basicstoryline.character.*;
import minwiggles.basicstoryline.crossing.CharacterCrossings;
import minwiggles.basicstoryline.crossing.CrossingsInformation;
import minwiggles.basicstoryline.meeting.InteractionSession;
import minwiggles.basicstoryline.meeting.MeetingComparison;
import minwiggles.basicstoryline.meeting.MeetingVariables;
import minwiggles.basicstoryline.meeting.MeetingsInformation;
import minwiggles.basicstoryline.moviedata.MovieData;
import minwiggles.basicstoryline.moviedata.MovieDataParser;
import minwiggles.basicstoryline.storyline.*;
import minwiggles.basicstoryline.wiggle.CompressedStorylineWiggles;
import minwiggles.basicstoryline.wiggle.CompressedStorylineWigglesOfCharacter;
import minwiggles.basicstoryline.wiggle.WigglesInformation;
import com.fasterxml.jackson.databind.ObjectMapper;
import gurobi.*;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Configurable(preConstruction = true)
public class LineWigglesMain {

    private ConfigurableApplicationContext applicationContext;
    private String currentResultDir;
    private boolean binary = false;
    private boolean mindCrossings = false;
    private boolean sameWeighting = false;
    private boolean prioritizeWiggles = false;
    private boolean prioritizeCrossings = false;
    private boolean minMaxWiggleH = false;
    private boolean withInitialInstance = false;
    private String movieName = null;
    private String initialInstanceFilePath = null;
    private String movieDataFilePath = null;
    private CompressedStoryline comprInitialStoryline = null;

    public LineWigglesMain(ConfigurableApplicationContext applicationContext, boolean minMaxWiggleH, boolean binary,
                           boolean mindCrossings, boolean sameWeighting, boolean prioritizeWiggles, boolean prioritizeCrossings,
                           boolean withInitialInstance, String initialInstanceFilePath, String movieName, String movieDataFilePath){
        this.applicationContext = applicationContext;
        this.minMaxWiggleH = minMaxWiggleH;
        this.binary = binary;
        this.mindCrossings = mindCrossings;
        this.sameWeighting = sameWeighting;
        this.prioritizeWiggles = prioritizeWiggles;
        this.prioritizeCrossings = prioritizeCrossings;
        this.withInitialInstance = withInitialInstance;
        this.movieName = movieName;
        this.movieDataFilePath = movieDataFilePath;
        this.initialInstanceFilePath = initialInstanceFilePath;
    }

    public MovieData parseMovieData(){
        MovieData movieData = null;
        UncompressedStoryline initialStoryline = null;

        try {
            MovieDataParser parser = new MovieDataParser();
            File movieDataPath = new File(movieDataFilePath);
            movieData = parser.parse(movieDataPath.getAbsolutePath(), movieName);
            movieData.compressTimePoints();

            if(this.withInitialInstance){
                File initialStoryVisPath = new File(initialInstanceFilePath);
                initialStoryline =  parser.parseInitVisualization(initialStoryVisPath.getAbsolutePath(), movieData.getNodeCount(), movieData.getTimeCount());
                comprInitialStoryline = parser.compressStoryline(initialStoryline, movieData.getCompressedTimePoints());
            }

            System.out.println("-------------------------------------------------------------------------");
            System.out.println(" Finished parsing movieData!");
            System.out.println(" movieName: "+movieData.getMovieName());
            System.out.println(" nodeCount: "+movieData.getNodeCount());
            System.out.println(" timeCount: "+movieData.getTimeCount());
            System.out.println(" compressedTimepoints: "+movieData.getCompressedTimePoints().length);
            System.out.println(" interactionSessionCount: "+movieData.getInteractionSessionCount());
            System.out.println(" minimal slotCount: "+movieData.getMinSlotCount());
            System.out.println(" lowerBound for wiggleObj: "+movieData.getLowerBoundForWiggleObj());
            System.out.println(" lowerBound for crossingsObj: "+movieData.getLowerBoundForCrossingsObj());
            System.out.println(" binary: "+binary);
            System.out.println(" minMaxWiggleH: "+minMaxWiggleH);
            System.out.println(" mindCrossings: "+mindCrossings);
            System.out.println(" sameWeighting: "+sameWeighting);
            System.out.println(" prioritizeWiggles: "+prioritizeWiggles);
            System.out.println(" prioritizeCrossings: "+prioritizeCrossings);
            System.out.println(" withInitialInstance: "+withInitialInstance);
            System.out.println("-------------------------------------------------------------------------");

            // Writing parsed movie data to file
            ZonedDateTime date = ZonedDateTime.now(ZoneId.systemDefault());
            String dateString = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm").format(date);
            currentResultDir = "ilp-formulation/results/"+movieName+"_"+dateString;
            boolean success = (new File(currentResultDir)).mkdirs();
            if (!success) {
                // Directory creation failed
            } else {
                File movieDataFile = new File(currentResultDir+"/"+movieName+".json");
                movieDataFile.createNewFile(); // if file already exists will do nothing
                FileOutputStream oFile = new FileOutputStream(movieDataFile, false);
                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(movieDataFile, movieData);
                oFile.close();
            }
        } catch(IOException ioe){
            ioe.printStackTrace();
        }

        return movieData;
    }

    public void run(MovieData movieData) {

        try {
            LineWiggleModel wModel = new LineWiggleModel();
            GRBModel grbModel = wModel.createGRBModel(binary, movieData, mindCrossings, sameWeighting, prioritizeWiggles,
                                                      prioritizeCrossings, minMaxWiggleH, comprInitialStoryline);
            grbModel.set(GRB.StringAttr.ModelName, "wiggleMinimization");

            FileWriter logfile = null;

            try {
                /*
                 * set GRBModel parameters
                 */
                grbModel.set(GRB.IntParam.OutputFlag, 0);
                grbModel.set(GRB.IntParam.MIPFocus, 3);
                grbModel.set(GRB.IntParam.Method, 3);
                grbModel.set(GRB.IntParam.Presolve, 2);

                File logDirectory = new File("ilp-formulation/log");
                if (!logDirectory.exists()){
                    logDirectory.mkdir();
                    // create directory for log files
                }

                File file = new File((new File("ilp-formulation/log/lineWigglesCallback.log")).getAbsolutePath());
                logfile = new FileWriter(file);
                GRBVar[] vars = grbModel.getVars();
                LineWiggleCallback cb   = new LineWiggleCallback(vars, logfile, wModel, this);
                grbModel.setCallback(cb);

                grbModel.set(GRB.DoubleParam.TimeLimit, 36000);

                // Limit how many solutions to collect
                grbModel.set(GRB.IntParam.PoolSolutions, 1000);
                grbModel.set(GRB.IntParam.Threads, 5);

                // Optimize model
                grbModel.optimize();

                //check if model is infeasable, if yes call printIISoutput(grbModel)
                if(grbModel.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE){
                    printIISoutput(grbModel);
                }

                System.out.println("");
                System.out.println("Optimization complete");

                if (grbModel.get(GRB.IntAttr.SolCount) == 0) {
                    System.out.println("No solution found, optimization status = "
                            + grbModel.get(GRB.IntAttr.Status));

                } else {
                    int solCount = grbModel.get(GRB.IntAttr.SolCount);
                    double objVal = grbModel.get(GRB.DoubleAttr.ObjVal);
                    double runtime = grbModel.get(GRB.DoubleAttr.Runtime);
                    double objbound = /*grbModel.get(GRB.DoubleAttr.ObjBound)*/ 0;

                    System.out.println("Solution found, objective = " + objVal);

                    // Write model to file
                    File lpFile = new File((new File("ilp-formulation/log/lineWiggles.lp")).getAbsolutePath());
                    grbModel.write(lpFile.getAbsolutePath());

                    printCompressedSolutionMatrix(grbModel.get(GRB.DoubleAttr.X, wModel.getCharacterPositionMatrix()), wModel.getMovieData(),
                            wModel.getSlotCount(), solCount, objVal, objbound, runtime);

                    printCompressedWiggleMatrixBinary(grbModel.get(GRB.DoubleAttr.X, wModel.getWiggleMatrix()), wModel.getMovieData(),
                            wModel.getSlotCount(), solCount, objVal, objbound, runtime);


                    printUnCompressedSolutionMatrix(grbModel.get(GRB.DoubleAttr.X, wModel.getCharacterPositionMatrix()), wModel.getMovieData(),
                            wModel.getSlotCount(), solCount, objVal, objbound, runtime);

                    InteractionSession[] interactionSessions = movieData.getInteractionSessions();
                    List<GRBVar[][]> meetingCharSlotList = wModel.getMeetingCharSlotList();
                    double[] maxSlots = grbModel.get(GRB.DoubleAttr.X, wModel.getMaxSlots());
                    String[] maxSlotsNames = grbModel.get(GRB.StringAttr.VarName, wModel.getMaxSlots());
                    double[] minSlots = grbModel.get(GRB.DoubleAttr.X, wModel.getMinSlots());
                    String[] minSlotsNames = grbModel.get(GRB.StringAttr.VarName, wModel.getMinSlots());

                    MeetingVariables[] meetingVariables = new MeetingVariables[interactionSessions.length];

                    for(int i=0; i<interactionSessions.length; i++){
                        printInteractionSessionVariables(interactionSessions[i],
                                grbModel.get(GRB.DoubleAttr.X, meetingCharSlotList.get(i)),
                                grbModel.get(GRB.StringAttr.VarName, meetingCharSlotList.get(i)),
                                maxSlots[i],
                                maxSlotsNames[i],
                                minSlots[i],
                                minSlotsNames[i]);
                        meetingVariables[i] = new MeetingVariables(interactionSessions[i].getId(), interactionSessions[i].getName(), minSlots[i], maxSlots[i]);
                    }

                    MeetingsInformation meetingsInformation = new MeetingsInformation(meetingVariables);
                    writeMeetingsInformationtoJsonFile(meetingsInformation, solCount, objVal, runtime);

                    Map<Integer, GRBVar[]> meetingComparisonList = wModel.getMeetingsComparisonList();
                    System.out.println();
                    System.out.println("Meeting comparisons for every compressed timepoint:");

                    List<double[]> comparisonsList = new ArrayList<>();
                    for(int timepoint: movieData.getCompressedTimePoints()){
                        comparisonsList.add(grbModel.get(GRB.DoubleAttr.X, meetingComparisonList.get(timepoint)));
                    }
                    printCompleteMeetingComparisonList(comparisonsList, movieData, solCount, objVal, runtime);
                    System.out.println();

                    // print character position comparison variables and crossing variables
                    Map<Integer, Map<CharacterPair, GRBVar>> characterComparisonGRBVarMapList = wModel.getCharacterComparisonMapList();
                    Map<Integer, Map<CharacterPair, Double>> characterComparisonDoubleMapList = new HashMap<Integer, Map<CharacterPair, Double>>();

                    if(mindCrossings){
                        Map<CharacterPair, GRBVar> characterPairGRBVarMap;
                        Map<CharacterPair, Double> characterPairDouleMap;
                        Set<CharacterPair> characterPairSet;
                        Set<Integer> compressedTimepoints = characterComparisonGRBVarMapList.keySet();
                        Map<Integer, GRBVar[]> characterCrossingsGRBVarList = wModel.getCharacterCrossingsList();
                        Map<Integer, double[]> characterCrossingsDoubleList = new HashMap<Integer, double[]>();

                        for(Integer timepoint: compressedTimepoints){
                            characterPairGRBVarMap = characterComparisonGRBVarMapList.get(timepoint);
                            characterPairDouleMap = new HashMap<CharacterPair, Double>();
                            characterPairSet = characterPairGRBVarMap.keySet();
                            for(CharacterPair characterPair: characterPairSet){
                                characterPairDouleMap.put(characterPair, grbModel.get(GRB.DoubleAttr.X, new GRBVar[]{characterPairGRBVarMap.get(characterPair)})[0]);
                            }

                            characterComparisonDoubleMapList.put(timepoint, characterPairDouleMap);
                            characterCrossingsDoubleList.put(timepoint, grbModel.get(GRB.DoubleAttr.X, characterCrossingsGRBVarList.get(timepoint)));
                        }
                        printCharacterPositionComparisonVariables(characterComparisonDoubleMapList, solCount, objVal, runtime);
                        printCharacterCrossingsList(characterCrossingsDoubleList, solCount, objVal, runtime);
                    }

                }

            } catch (IOException e) {
                    e.printStackTrace();
                }

            // Dispose of model and environment
            wModel.disposeModel();
            applicationContext.close();
            System.exit(0);

        } catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
            e.printStackTrace();
        }
    }

    public void writeMeetingsInformationtoJsonFile(MeetingsInformation meetingsInformation, int solCount, double obj, double time){
        try {
            File meetingsInformationFile = new File(currentResultDir+"/solCount_"+solCount+"-obj_"+Math.round(obj)+"-runtime_"+Math.round(time)+"s-meetingsInformation.json");
            meetingsInformationFile.createNewFile();
            FileOutputStream oFile = new FileOutputStream(meetingsInformationFile, false);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(meetingsInformationFile, meetingsInformation);
            oFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printCompressedSolutionMatrix(double[][][] solutionMatrix, MovieData movieData, int slotCount, int solCount, double obj, double objbnd, double time){
        System.out.println();
        System.out.println("Compressed Solution Matrix:");
        System.out.println();

        for (int t = 0; t < movieData.getCompressedTimePoints().length; t++) {
            if(t<10) System.out.print(" ");
            System.out.print(t+" ");
        }
        System.out.println();

        String[][] storyline = new String[slotCount][movieData.getCompressedTimePoints().length-1];

        for (int j = 0; j < slotCount; j++) {
            for (int t = 0; t < movieData.getCompressedTimePoints().length; t++) {
                if(t<10) System.out.print(" ");
                else System.out.print(" ");
                String c = "_ ";
                for (int i = 0; i < movieData.getNodeCount(); i++) {
                    if (solutionMatrix[j][t][i] > 0.5) {
                        c = i+" ";
                    }
                }
                if(t<movieData.getCompressedTimePoints().length-1){
                    if(c.contains("_")){
                        storyline[j][t] = " ";
                    } else {
                        storyline[j][t] = String.valueOf(c.trim());
                    }
                }

                System.out.print(c);
            }
            System.out.println();
        }
        System.out.println();

        try {
            File compressedVisFile = new File(currentResultDir+"/solCount_"+solCount+"-obj_"+Math.round(obj)+"-objbnd_"+Math.round(objbnd)+"-runtime_"+Math.round(time)+"s-compressedStoryline.json");
            compressedVisFile.createNewFile();
            FileOutputStream oFile = new FileOutputStream(compressedVisFile, false);
            ObjectMapper mapper = new ObjectMapper();
            CompressedStoryline compressedStoryline = new CompressedStoryline(storyline, obj);
            mapper.writeValue(compressedVisFile, compressedStoryline);
            oFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println();
        System.out.println("Compressed Solution Matrix - all characters:");
        System.out.println();

        CompressedStorylineOfCharacter[] storylinesPerCharacter = new CompressedStorylineOfCharacter[movieData.getNodeCount()];

        for (int i = 0; i < movieData.getNodeCount(); i++) {
            storyline = new String[slotCount][movieData.getCompressedTimePoints().length];
            for (int j = 0; j < slotCount; j++) {
                for (int t = 0; t < movieData.getCompressedTimePoints().length; t++) {
                    storyline[j][t] = String.valueOf(solutionMatrix[j][t][i]);
                }
            }
            storylinesPerCharacter[i] = new CompressedStorylineOfCharacter(i, storyline);
        }

        AllCompressedStorylinesPerCharacter allCompressedStorylinesPerCharacter = new AllCompressedStorylinesPerCharacter(storylinesPerCharacter);

        try {
            File compressedVisFile = new File(currentResultDir+"/solCount_"+solCount+"-obj_"+Math.round(obj)+"-objbnd_"+Math.round(objbnd)+"-runtime_"+Math.round(time)+"s-compressedStorylinePerCharacter.json");
            compressedVisFile.createNewFile();
            FileOutputStream oFile = new FileOutputStream(compressedVisFile, false);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(compressedVisFile, allCompressedStorylinesPerCharacter);
            oFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int t = 0; t < movieData.getCompressedTimePoints().length; t++) {
            if(t<10) System.out.print(" ");
            System.out.print(t+" ");
        }
        System.out.println();

        for (int j = 0; j < slotCount; j++) {
            for (int t = 0; t < movieData.getCompressedTimePoints().length; t++) {
                System.out.print(" ");
                System.out.print(" ");
                String c = " [";
                for (int i = 0; i < movieData.getNodeCount(); i++) {
                        c += i+"="+solutionMatrix[j][t][i]+", ";
                }
                c += "] ";
                System.out.print(c);
            }
            System.out.println();
        }
        System.out.println();

    }

    public void printUnCompressedSolutionMatrix(double[][][] solutionMatrix, MovieData movieData, int slotCount, int solCount, double obj, double objbnd, double time){
        System.out.println();
        System.out.println("Uncompressed Solution Matrix:");
        System.out.println();
        Integer[] compressedTimepoints = movieData.getCompressedTimePoints();

        String[][] storyline = new String[slotCount][movieData.getTimeCount()];
        int columnCount;

        for (int t = 0; t < movieData.getTimeCount(); t++) {
            if(t<10) System.out.print(" ");
            System.out.print(t+" ");
        }
        System.out.println();

        for (int j = 0; j < slotCount; j++) {
            columnCount = 0;
            for (int t = 0; t < (compressedTimepoints.length-1); t++) {
                InteractionSession is = null;
                int c = -1;
                for (int i = 0; i < movieData.getNodeCount(); i++) {
                    if (solutionMatrix[j][t][i] > 0.5) {
                        is = movieData.getInteractionSessionForTimePointAndCharacter(compressedTimepoints[t], i);
                        if(is != null){
                            c = i;
                            break;
                        }
                    }
                }
                String output = "";
                for (int u = 0; u < (compressedTimepoints[t+1]-compressedTimepoints[t]); u++) {
                    if(is == null){
                        output += " _ ";
                        storyline[j][columnCount] = " ";
                    } else {
                        output += " "+c+" ";
                        storyline[j][columnCount] = String.valueOf(c);
                    }
                    columnCount++;
                }
                System.out.print(output);
            }
            System.out.println();
        }
        System.out.println();

        try {
            File uncompressedVisFile = new File(currentResultDir+"/solCount_"+solCount+"-obj_"+Math.round(obj)+"-objbnd_"+Math.round(objbnd)+"-runtime_"+Math.round(time)+"s-uncompressedStoryline.json");
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

    public void printCompressedWiggleMatrixBinary(double[][] solutionWiggleMatrix, MovieData movieData, int slotCount, int solCount, double obj, double objbnd, double time){
        System.out.println();
        System.out.println("Compressed Wiggle Matrix:");
        System.out.println();

        int wiggleCount = 0;
        CompressedStorylineWigglesOfCharacter[] wigglesOfCharacters = new CompressedStorylineWigglesOfCharacter[movieData.getNodeCount()];
        double[] wigglesOfCharacter;

        for(int i=0; i<movieData.getNodeCount(); i++){
            wigglesOfCharacter = new double[movieData.getCompressedTimePoints().length-1];
            System.out.print("character "+i+": ");
            for (int t = 0; t < movieData.getCompressedTimePoints().length-1; t++) {
                if(solutionWiggleMatrix[i][t]>0) wiggleCount++;
                System.out.print(solutionWiggleMatrix[i][t]+" ");
                wigglesOfCharacter[t] = solutionWiggleMatrix[i][t];
            }
            System.out.println();
            wigglesOfCharacters[i] = new CompressedStorylineWigglesOfCharacter(i, wigglesOfCharacter);
        }
        System.out.println();
        System.out.println("Wiggle Count: "+ wiggleCount);
        System.out.println();
        System.out.println();

        WigglesInformation wigglesInformation = new WigglesInformation(wiggleCount);
        CompressedStorylineWiggles compressedStorylineWiggles = new CompressedStorylineWiggles(wigglesOfCharacters);

        try {
            File wigglesPerCharacterFile = new File(currentResultDir+"/solCount_"+solCount+"-obj_"+Math.round(obj)+"-objbnd_"+Math.round(objbnd)+"-runtime_"+Math.round(time)+"s-compressedWigglesPerCharacter.json");
            wigglesPerCharacterFile.createNewFile();
            FileOutputStream oFile = new FileOutputStream(wigglesPerCharacterFile, false);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(wigglesPerCharacterFile, compressedStorylineWiggles);
            oFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            File wigglesInformationFile = new File(currentResultDir+"/solCount_"+solCount+"-obj_"+Math.round(obj)+"-objbnd_"+Math.round(objbnd)+"-runtime_"+Math.round(time)+"s-wigglesInformation.json");
            wigglesInformationFile.createNewFile();
            FileOutputStream oFile = new FileOutputStream(wigglesInformationFile, false);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(wigglesInformationFile, wigglesInformation);
            oFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printCompleteMeetingComparisonList(List<double[]> comparisonsList, MovieData movieData, int solCount, double obj, double time){
        for(int t = 0; t<movieData.getCompressedTimePoints().length; t++){
            System.out.print("timepoint "+t);
            double[] comparisons = comparisonsList.get(t);
            if(comparisons != null){
                for(int j=0; j<comparisons.length; j++){
                    System.out.print(" "+comparisons[j]+" ");
                }
            } else {
                System.out.print(" null ");
            }
            System.out.println();
        }

        MeetingComparison meetingComparison = new MeetingComparison(comparisonsList);
        try {
            File uncompressedVisFile = new File(currentResultDir+"/solCount_"+solCount+"-obj_"+Math.round(obj)+"-runtime_"+Math.round(time)+"s-meetingComparisons.json");
            uncompressedVisFile.createNewFile();
            FileOutputStream oFile = new FileOutputStream(uncompressedVisFile, false);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(uncompressedVisFile, meetingComparison);
            oFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printInteractionSessionVariables(InteractionSession meeting,
                                                         double[][] meetingCharSlots,
                                                         String[][] meetingCharSlotNames,
                                                         double maxSlot, String maxSlotName,
                                                         double minSlot, String minSlotName){
        System.out.println();
        System.out.println("meeting {id:"+meeting.getId()+", name:"+meeting.getName()+", numberOfCharacters:"+meeting.numberOfCharacters()+
                ", start:"+meeting.getStartTimePoint()+", end:"+meeting.getEndTimePoint()+", compr-start:"+meeting.getCompressedStartTimePoint()+
                ", compr-end:"+meeting.getCompressedEndTimePoint()+"}:");;
        for(int l=0; l<meetingCharSlots.length; l++){
            double[] charsSlots = meetingCharSlots[l];
            String[] charsSlotNames = null;
            if(meetingCharSlotNames != null){
                charsSlotNames = meetingCharSlotNames[l];
            }
            for(int k=0; k<charsSlots.length; k++){
                if(charsSlots[k] > 0.5){
                    System.out.print("["+((charsSlotNames==null) ? " " : charsSlotNames[k]+": ")+charsSlots[k]+"] ");
                }
            }
        }
        System.out.println();

        System.out.println();
        System.out.println("min slot: " + ((minSlotName==null) ? "" : minSlotName +", ") + minSlot);
        System.out.println("max slot: " + ((maxSlotName==null) ? "" : maxSlotName +", ") + maxSlot);
        System.out.println();
    }

    public void printCharacterPositionComparisonVariables(Map<Integer, Map<CharacterPair, Double>> characterComparisonMapList, int solCount, double obj, double time){
        System.out.println();
        System.out.println("character position comparison variables:");
        System.out.println();

        Map<CharacterPair, Double> characterPairMap;
        List<Integer> compressedTimepointsList = new LinkedList<Integer>(characterComparisonMapList.keySet());
        Collections.sort(compressedTimepointsList);
        List<List<CharacterComparison>> allCharacterComparisonsList = new LinkedList<>();
        List<CharacterComparison> characterComparisons = new LinkedList<>();

        for(Integer timepoint: compressedTimepointsList){
            characterComparisons = new LinkedList<>();
            System.out.println("timepoint "+timepoint+":");
            characterPairMap = characterComparisonMapList.get(timepoint);
            List<CharacterPair> characterPairList = new LinkedList<CharacterPair>(characterPairMap.keySet());
            characterPairList.sort(
                    Comparator.comparingInt(CharacterPair::getCharacterId1).
                            thenComparingInt(CharacterPair::getCharacterId2));
            for(CharacterPair characterPair: characterPairList){
                System.out.println("characterPair: c1="+characterPair.getCharacterId1()+" is above c2="+characterPair.getCharacterId2()+" -> "+characterPairMap.get(characterPair));
                characterComparisons.add(new CharacterComparison(characterPair, characterPairMap.get(characterPair)));
            }
            allCharacterComparisonsList.add(characterComparisons);
            System.out.println();
        }

        CharacterComparisonInformation characterComparisonInformation = new CharacterComparisonInformation(allCharacterComparisonsList);
        try {
            File characterComparisonInformationFile = new File(currentResultDir+"/solCount_"+solCount+"-obj_"+Math.round(obj)+"-runtime_"+Math.round(time)+"s-characterComparisonInformation.json");
            characterComparisonInformationFile.createNewFile();
            FileOutputStream oFile = new FileOutputStream(characterComparisonInformationFile, false);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(characterComparisonInformationFile, characterComparisonInformation);
            oFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printCharacterCrossingsList(Map<Integer, double[]> characterCrossingsList, int solCount, double obj, double time){
        System.out.println();
        System.out.println("character crossing variables:");
        System.out.println();

        int crossingsCount = 0;
        List<CharacterCrossings> characterCrossingsVarList = new ArrayList<>();
        List<Integer> compressedTimepointsList = new LinkedList<Integer>(characterCrossingsList.keySet());
        Collections.sort(compressedTimepointsList);
        double[] crossings;

        for(Integer timepoint: compressedTimepointsList){
            System.out.println("timepoint "+timepoint+":");
            crossings = characterCrossingsList.get(timepoint);
            if(crossings!=null){
                for(double crossingVar: crossings){
                    System.out.println(crossingVar);
                    if(crossingVar>0) crossingsCount++;
                }
            }
            System.out.println();

            CharacterCrossings characterCrossings = new CharacterCrossings(timepoint, crossings);
            characterCrossingsVarList.add(characterCrossings);
        }
        System.out.println("crossingsCount: "+crossingsCount/2);
        System.out.println();
        System.out.println();

        CrossingsInformation crossingsInformation = new CrossingsInformation(characterCrossingsVarList, (crossingsCount/2));
        try {
            File crossingsInformationFile = new File(currentResultDir+"/solCount_"+solCount+"-obj_"+Math.round(obj)+"-runtime_"+Math.round(time)+"s-crossingsInformation.json");
            crossingsInformationFile.createNewFile();
            FileOutputStream oFile = new FileOutputStream(crossingsInformationFile, false);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(crossingsInformationFile, crossingsInformation);
            oFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public  void printIISoutput(GRBModel grbModel) throws GRBException{
        grbModel.computeIIS();

        // Write model to file
        File ilpFile = new File((new File("ilp-formulation/log/lineWiggles.ilp")).getAbsolutePath());
        grbModel.write(ilpFile.getAbsolutePath());

        for(GRBConstr constr: grbModel.getConstrs()){
            if(constr.get(GRB.IntAttr.IISConstr) > 0){
                System.out.println(constr.get(GRB.StringAttr.ConstrName));
            }
        }
        for(GRBVar var: grbModel.getVars()){
            if(var.get(GRB.IntAttr.IISLB) > 0 || var.get(GRB.IntAttr.IISUB) > 0){
                System.out.println(var.get(GRB.StringAttr.VarName));
            }
        }
    }

}
