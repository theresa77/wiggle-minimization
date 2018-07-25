package minwiggles.ilpformulation.model;

import minwiggles.basicstoryline.character.CharacterPair;
import minwiggles.basicstoryline.meeting.InteractionSession;
import minwiggles.basicstoryline.meeting.MeetingVariables;
import minwiggles.basicstoryline.meeting.MeetingsInformation;
import minwiggles.basicstoryline.moviedata.MovieData;
import com.fasterxml.jackson.databind.ObjectMapper;
import gurobi.GRB;
import gurobi.GRBCallback;
import gurobi.GRBException;
import gurobi.GRBVar;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class LineWiggleCallback extends GRBCallback {

    private double lastiter;
    private double lastnode;
    private GRBVar[] vars;
    private FileWriter logfile;
    private LineWiggleModel wiggleModel;
    private LineWigglesMain lineWigglesMain;

    public LineWiggleCallback(GRBVar[] vars, FileWriter logfile, LineWiggleModel wiggleModel, LineWigglesMain lineWigglesMain) {
        this.lastiter = lastnode = -GRB.INFINITY;
        this.vars = vars;
        this.logfile = logfile;
        this.wiggleModel = wiggleModel;
        this.lineWigglesMain = lineWigglesMain;
    }

    protected void callback() {
        try {
//            logfile.write("\n");
            if (where == GRB.CB_POLLING) {
                // Ignore polling callback
            } else if (where == GRB.CB_PRESOLVE) {
                // Presolve callback
                int cdels = getIntInfo(GRB.CB_PRE_COLDEL);
                int rdels = getIntInfo(GRB.CB_PRE_ROWDEL);
                if (cdels != 0 || rdels != 0) {
                    String msg = cdels + " columns and " + rdels
                            + " rows are removed";
                    System.out.println(msg);
                    logfile.write("GRB.CB_PRESOLVE: "+msg+"\n");
                }
            } else if (where == GRB.CB_SIMPLEX) {
                // Simplex callback
                double itcnt = getDoubleInfo(GRB.CB_SPX_ITRCNT);
                if (itcnt - lastiter >= 100) {
                    lastiter = itcnt;
                    double obj = getDoubleInfo(GRB.CB_SPX_OBJVAL);
                    int ispert = getIntInfo(GRB.CB_SPX_ISPERT);
                    double pinf = getDoubleInfo(GRB.CB_SPX_PRIMINF);
                    double dinf = getDoubleInfo(GRB.CB_SPX_DUALINF);
                    char ch;
                    if (ispert == 0)      ch = ' ';
                    else if (ispert == 1) ch = 'S';
                    else                  ch = 'P';
                    String msg = itcnt + " " + obj + ch + " "
                            + pinf + " " + dinf;
                    System.out.println(msg);
                    logfile.write("GRB.CB_SIMPLEX: "+msg+"\n");
                }
            } else if (where == GRB.CB_MIP) {
                // General MIP callback
                double nodecnt = getDoubleInfo(GRB.CB_MIP_NODCNT);
                double objbst = getDoubleInfo(GRB.CB_MIP_OBJBST);
                double objbnd = getDoubleInfo(GRB.CB_MIP_OBJBND);
                int solcnt = getIntInfo(GRB.CB_MIP_SOLCNT);
                if (nodecnt - lastnode >= 100) {
                    lastnode = nodecnt;
                    int actnodes = (int) getDoubleInfo(GRB.CB_MIP_NODLFT);
                    int itcnt = (int) getDoubleInfo(GRB.CB_MIP_ITRCNT);
                    int cutcnt = getIntInfo(GRB.CB_MIP_CUTCNT);
                    String msg = nodecnt + " " + actnodes + " "
                            + itcnt + " " + objbst + " " + objbnd + " "
                            + solcnt + " " + cutcnt;
                    System.out.println(msg);
                    logfile.write("GRB.CB_MIP: "+msg+"\n");
                }
                /*if (Math.abs(objbst - objbnd) < 0.5 * (1.0 + Math.abs(objbst))) {
                    String msg = "Stop early - 50% gap achieved";
                    System.out.println(msg);
                    logfile.write(msg+"\n");
                    abort();
                }
                if (Math.abs(objbst - objbnd) < 0.1 * (1.0 + Math.abs(objbst))) {
                    String msg = "Stop early - 10% gap achieved";
                    System.out.println(msg);
                    logfile.write(msg+"\n");
                    abort();
                }*/
                /*if (solcnt >= 8) {
                    String msg = "Stop early - 10th solution found";
                    System.out.println(msg);
                    logfile.write(msg+"\n");
                    abort();
                }*/
                /*if (nodecnt >= 4000000 && solcnt > 0) {
                    String msg = "Stop early - 4000000 nodes explored";
                    System.out.println(msg);
                    logfile.write(msg+"\n");
                    abort();
                }*/
            } else if (where == GRB.CB_MIPSOL) {
                // MIP solution callback
                int nodecnt = (int) getDoubleInfo(GRB.CB_MIPSOL_NODCNT);
                double obj = getDoubleInfo(GRB.CB_MIPSOL_OBJ);
                int solcnt = getIntInfo(GRB.CB_MIPSOL_SOLCNT);
                double time = getDoubleInfo(GRB.CB_RUNTIME);
                double objbnd = getDoubleInfo(GRB.CB_MIPSOL_OBJBND);
                double[][][] solutionCharPosMatrix = new double[wiggleModel.getSlotCount()][wiggleModel.getMovieData().getCompressedTimePoints().length][wiggleModel.getMovieData().getNodeCount()];
                for(int j=0; j<solutionCharPosMatrix.length; j++){
                    solutionCharPosMatrix[j] = getSolution(wiggleModel.getCharacterPositionMatrix()[j]);
                }

                List<double[]> comparisonsList = new LinkedList<double[]>();
                for(int compressedTimepoint: wiggleModel.getMovieData().getCompressedTimePoints()){
                    double[] comparisons = getSolution(wiggleModel.getMeetingsComparisonList().get(compressedTimepoint));
                    comparisonsList.add(comparisons);
                }

                String msg = "**** New solution at node " + nodecnt
                        + ", obj " + obj + ", sol " + solcnt
                        /*+ ", x[0] = " + x[0]*/ + " ****";
                System.out.println(msg);
                logfile.write("GRB.CB_MIPSOL: "+msg+"\n");

                MovieData movieData = wiggleModel.getMovieData();

                this.lineWigglesMain.printCompressedSolutionMatrix(solutionCharPosMatrix, movieData, wiggleModel.getSlotCount(), solcnt, obj, objbnd, time);

                double[][] solutionWiggleMatrix = getSolution(wiggleModel.getWiggleMatrix());
                this.lineWigglesMain.printCompressedWiggleMatrixBinary(solutionWiggleMatrix, movieData, wiggleModel.getSlotCount(), solcnt, obj, objbnd, time);

                this.lineWigglesMain.printUnCompressedSolutionMatrix(solutionCharPosMatrix, movieData, wiggleModel.getSlotCount(), solcnt, obj, objbnd, time);
                this.lineWigglesMain.printCompleteMeetingComparisonList(comparisonsList,  movieData, solcnt, obj, time);

                InteractionSession[] interactionSessions = movieData.getInteractionSessions();
                List<GRBVar[][]> meetingCharSlotList = wiggleModel.getMeetingCharSlotList();
//                double[] maxSlots = getSolution(wiggleModel.getMaxSlots());
//                double[] minSlots = getSolution(wiggleModel.getMinSlots());

                MeetingVariables[] meetingVariables = new MeetingVariables[interactionSessions.length];

                for(int i=0; i<interactionSessions.length; i++){
                    double maxSlot = getSolution(wiggleModel.getMaxSlots()[i]);
                    double minSlot = getSolution(wiggleModel.getMinSlots()[i]);
                    this.lineWigglesMain.printInteractionSessionVariables(interactionSessions[i],
                            getSolution(meetingCharSlotList.get(i)),
                            null, maxSlot, null, minSlot, null);
                    meetingVariables[i] = new MeetingVariables(interactionSessions[i].getId(), interactionSessions[i].getName(), minSlot, maxSlot);
                }

                MeetingsInformation meetingsInformation = new MeetingsInformation(meetingVariables);
                this.lineWigglesMain.writeMeetingsInformationtoJsonFile(meetingsInformation, solcnt, obj, time);

                if(wiggleModel.mindCrossings()) {
                    //print character position comparison variables
                    Map<Integer, Map<CharacterPair, GRBVar>> characterComparisonGRBVarMapList = wiggleModel.getCharacterComparisonMapList();
                    Map<Integer, Map<CharacterPair, Double>> characterComparisonDoubleMapList = new HashMap<Integer, Map<CharacterPair, Double>>();

                    Map<CharacterPair, GRBVar> characterPairGRBVarMap;
                    Map<CharacterPair, Double> characterPairDouleMap;
                    Set<CharacterPair> characterPairSet;
                    List<Integer> compressedTimepoints = new ArrayList<Integer>(characterComparisonGRBVarMapList.keySet());
                    Collections.sort(compressedTimepoints);
                    Map<Integer, GRBVar[]> characterCrossingsGRBVarList = wiggleModel.getCharacterCrossingsList();
                    Map<Integer, double[]> characterCrossingsDoubleList = new HashMap<Integer, double[]>();

                    for (Integer timepoint : compressedTimepoints) {
                        characterPairGRBVarMap = characterComparisonGRBVarMapList.get(timepoint);
                        characterPairDouleMap = new HashMap<CharacterPair, Double>();
                        characterPairSet = characterPairGRBVarMap.keySet();
                        for (CharacterPair characterPair : characterPairSet) {
                            characterPairDouleMap.put(characterPair, getSolution(characterPairGRBVarMap.get(characterPair)));
                        }

                        characterComparisonDoubleMapList.put(timepoint, characterPairDouleMap);
                        characterCrossingsDoubleList.put(timepoint, getSolution(characterCrossingsGRBVarList.get(timepoint)));
                    }
                    this.lineWigglesMain.printCharacterPositionComparisonVariables(characterComparisonDoubleMapList, solcnt, obj, time);
                    this.lineWigglesMain.printCharacterCrossingsList(characterCrossingsDoubleList, solcnt, obj, time);
                }

            } else if (where == GRB.CB_MIPNODE) {
                // MIP node callback
//                System.out.println("**** New node ****");
                if (getIntInfo(GRB.CB_MIPNODE_STATUS) == GRB.OPTIMAL) {
                    double[] x = getNodeRel(vars);
                    setSolution(vars, x);
                }
            } else if (where == GRB.CB_BARRIER) {
                // Barrier callback
                int itcnt = getIntInfo(GRB.CB_BARRIER_ITRCNT);
                double primobj = getDoubleInfo(GRB.CB_BARRIER_PRIMOBJ);
                double dualobj = getDoubleInfo(GRB.CB_BARRIER_DUALOBJ);
                double priminf = getDoubleInfo(GRB.CB_BARRIER_PRIMINF);
                double dualinf = getDoubleInfo(GRB.CB_BARRIER_DUALINF);
                double cmpl = getDoubleInfo(GRB.CB_BARRIER_COMPL);
                String msg = itcnt + " " + primobj + " " + dualobj + " "
                        + priminf + " " + dualinf + " " + cmpl;
                System.out.println(msg);
                logfile.write("GRB.CB_BARRIER: "+msg+"\n");
            } else if (where == GRB.CB_MESSAGE) {
                // Message callback
                String msg = getStringInfo(GRB.CB_MSG_STRING);
                if (msg != null){
                    System.out.print(msg);
                    logfile.write("GRB.CB_MESSAGE: "+msg);
                }
            } else {
                System.out.print("OTHER CALLBACK! ");
                System.out.print(where);
            }
//            logfile.write("\n");
        } catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode());
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Error during callback");
            e.printStackTrace();
        }
    }
}
