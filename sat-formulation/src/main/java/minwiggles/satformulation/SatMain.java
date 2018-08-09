package minwiggles.satformulation;

import minwiggles.basicstoryline.moviedata.MovieData;
import minwiggles.basicstoryline.moviedata.MovieDataParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class SatMain {

    public static void main(String[] args) throws IOException {

        System.out.println("[INFO] ");
        System.out.println("[INFO] Start SatMain");

        boolean movieToWCNF = false;
        boolean createSolution = false;
        boolean binary = false;
        String movieName = null;
        String movieDataFile = null;
        String variablesFilePath = null;
        String maxSatSolverName = null;

        try {
            for (String arg : args) {
                switch (arg) {
                    case "movieToWCNF":
                        movieToWCNF = true;
                        break;
                    case "createSolution":
                        createSolution = true;
                        break;
                    default:
                        String[] argArray = arg.split("=");
                        switch (argArray[0]) {
                            case "movieName":
                                movieName = argArray[1];
                                break;
                            case "movieDataFile":
                                movieDataFile = argArray[1];
                                break;
                            case "variablesFile":
                                variablesFilePath = argArray[1];
                                break;
                            case "maxSatSolverName":
                                maxSatSolverName = argArray[1];
                                break;
                            case "binary":
                                if (argArray[1].equals("true")) {
                                    binary = true;
                                } else if (argArray[1].equals("false")) {
                                    binary = false;
                                } else {
                                    throw new IllegalArgumentException("Invalid parameter: " + arg);
                                }
                                break;
                            default:
                                throw new IllegalArgumentException("Invalid parameter: " + arg);
                        }
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("ArrayIndexOutOfBoundsException caught");
        }

        if (movieName == null) {
            System.out.println();
            System.out.println("--------------------------------------------------");
            System.out.println("[ERROR] Missing value for parameter 'movieName'!");
            System.out.println("--------------------------------------------------");
            System.out.println();
        } else if (movieDataFile == null) {
            System.out.println();
            System.out.println("-------------------------------------------------------");
            System.out.println("[ERROR] Missing value for parameter 'movieDataFile'!");
            System.out.println("-------------------------------------------------------");
            System.out.println();
        } else if (movieToWCNF && createSolution || !movieToWCNF && !createSolution) {
            System.out.println();
            System.out.println("------------------------------------------------------------------------------------------------------------------------");
            System.out.println("[ERROR] Not clear which feature to use for Max-SAT Formulation: 'movieToWCNF="+movieToWCNF+"', 'createSolution="+createSolution+"'");
            System.out.println("------------------------------------------------------------------------------------------------------------------------");
            System.out.println();
        } else if (createSolution && variablesFilePath == null) {
            System.out.println();
            System.out.println("-------------------------------------------------------");
            System.out.println("[ERROR] Missing value for parameter 'variablesFilePath'!");
            System.out.println("-------------------------------------------------------");
            System.out.println();
        } else {

            MovieDataParser parser = new MovieDataParser();
            File movieDataPath = new File(movieDataFile);
            MovieData movieData = parser.parse(movieDataPath.getAbsolutePath(), movieName);
            SatFormulation satFormulation = new SatFormulation(movieName, movieData, binary);

            System.out.println("[INFO] ");
            System.out.println("[INFO] ------------------------------------------------------------------------");
            System.out.println("[INFO] Finished parsing movieData!");
            System.out.println("[INFO] movieName: "+movieData.getMovieName());
            System.out.println("[INFO] nodeCount: "+movieData.getNodeCount());
            System.out.println("[INFO] timeCount: "+movieData.getTimeCount());
            System.out.println("[INFO] compressedTimepoints: "+movieData.getCompressedTimePoints().length);
            System.out.println("[INFO] interactionSessionCount: "+movieData.getInteractionSessionCount());
            System.out.println("[INFO] binary: "+binary);
            System.out.println("[INFO] movieToWCNF: "+movieToWCNF);
            System.out.println("[INFO] createSolution: "+createSolution);
            if(createSolution) System.out.println("[INFO] variablesFile: "+variablesFilePath);
            if(maxSatSolverName!=null) System.out.println("[INFO] maxSatSolverName: "+maxSatSolverName);
            System.out.println("[INFO] ------------------------------------------------------------------------");
            System.out.println("[INFO] ");

            if (movieToWCNF) {
                createFileForSatFormulation(satFormulation, movieName);
            } else {

                File variablesFile = new File(variablesFilePath);
                FileReader dataInput = new FileReader(variablesFile);
                BufferedReader bufRead = new BufferedReader(dataInput);
                String variablesString = bufRead.readLine();

                parseResultFromMaxSatSolver(satFormulation, maxSatSolverName, variablesString);
            }

        }

        System.out.println("[INFO] End SatMain");
        System.out.println("[INFO] ");

    }

    private static void createFileForSatFormulation(SatFormulation satFormulation, String fileName){
        satFormulation.writeSatFormulationToFile(fileName);
    }

    private static void parseResultFromMaxSatSolver(SatFormulation satFormulation, String maxSatSolverName, String variablesString){
        try {
            satFormulation.parseResult(maxSatSolverName, variablesString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
