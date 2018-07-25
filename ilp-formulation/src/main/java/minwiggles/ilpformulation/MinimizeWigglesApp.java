package minwiggles.ilpformulation;

import minwiggles.basicstoryline.moviedata.MovieData;
import minwiggles.ilpformulation.model.LineWigglesMain;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class MinimizeWigglesApp {

	public static void main(String[] args) {
		ConfigurableApplicationContext applicationContext = SpringApplication.run(MinimizeWigglesApp.class, args);

        boolean minMaxWiggleH = false;
        boolean binary = false;
        boolean mindCrossings = false;
        boolean sameWeighting = false;
        boolean prioritizeWiggles = false;
        boolean prioritizeCrossings = false;
        boolean withInitialInstance = false;
        String movieName = null;
        String movieDataFilePath = null;
        String initialInstanceFilePath = null;

		try {
		    for(String arg : args){
                String[] argArray = arg.split("=");

                switch (argArray[0]) {
                    case "binary":
                        if(argArray[1].equals("true")){
                            binary = true;
                        } else if(argArray[1].equals("false")){
                            binary = false;
                        } else {
                            throw new IllegalArgumentException("Invalid parameter: " + arg);
                        }
                        break;
                    case "minMaxWiggleH":
                        if(argArray[1].equals("true")){
                            minMaxWiggleH = true;
                        } else if(argArray[1].equals("false")){
                            minMaxWiggleH = false;
                        } else {
                            throw new IllegalArgumentException("Invalid parameter: " + arg);
                        }
                        break;
                    case "mindCrossings":
                        if(argArray[1].equals("true")){
                            mindCrossings = true;
                        } else if(argArray[1].equals("false")){
                            mindCrossings = false;
                        } else {
                            throw new IllegalArgumentException("Invalid parameter: " + arg);
                        }
                        break;
                    case "sameWeighting":
                        if(argArray[1].equals("true")){
                            sameWeighting = true;
                        } else if(argArray[1].equals("false")){
                            sameWeighting = false;
                        } else {
                            throw new IllegalArgumentException("Invalid parameter: " + arg);
                        }
                        break;
                    case "prioritizeWiggles":
                        if(argArray[1].equals("true")){
                            prioritizeWiggles = true;
                        } else if(argArray[1].equals("false")){
                            prioritizeWiggles = false;
                        } else {
                            throw new IllegalArgumentException("Invalid parameter: " + arg);
                        }
                        break;
                    case "prioritizeCrossings":
                        if(argArray[1].equals("true")){
                            prioritizeCrossings = true;
                        } else if(argArray[1].equals("false")){
                            prioritizeCrossings = false;
                        } else {
                            throw new IllegalArgumentException("Invalid parameter: " + arg);
                        }
                        break;
                    case "withInitialSolution":
                        if(argArray[1].equals("true")){
                            withInitialInstance = true;
                        } else if(argArray[1].equals("false")){
                            withInitialInstance = false;
                        } else {
                            throw new IllegalArgumentException("Invalid parameter: " + arg);
                        }
                        break;
                    case "movieName":
                        movieName = argArray[1];
                        break;
                    case "movieDataFile":
                        movieDataFilePath = argArray[1];
                        break;
                    case "initialSolutionFile":
                        initialInstanceFilePath = argArray[1];
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid parameter: " + arg);
                }
            }
		} catch (ArrayIndexOutOfBoundsException e){
			System.out.println("ArrayIndexOutOfBoundsException caught");
		}

		if(movieName==null){
            System.out.println();
            System.out.println("--------------------------------------------------");
            System.out.println("[ERROR] Missing value for parameter 'movieName'!");
            System.out.println("--------------------------------------------------");
            System.out.println();
        } else if(movieDataFilePath==null){
            System.out.println();
            System.out.println("-------------------------------------------------------");
            System.out.println("[ERROR] Missing value for parameter 'movieDataFile'!");
            System.out.println("-------------------------------------------------------");
            System.out.println();
        } else if(withInitialInstance && initialInstanceFilePath==null){
            System.out.println();
            System.out.println("--------------------------------------------------------------");
            System.out.println("[ERROR] Missing value for parameter 'initialSolutionFile'!");
            System.out.println("--------------------------------------------------------------");
            System.out.println();
        } else if(mindCrossings && !sameWeighting && !prioritizeWiggles && !prioritizeCrossings) {
            System.out.println();
            System.out.println("------------------------------------------------------------------------------------------------------------------");
            System.out.println("[ERROR] Missing weighting strategy: 'sameWeighting=false', 'prioritizeWiggles=false', 'prioritizeCrossings=false'");
            System.out.println("------------------------------------------------------------------------------------------------------------------");
            System.out.println();
        } else if(mindCrossings && ((sameWeighting && prioritizeWiggles) || (sameWeighting && prioritizeCrossings) || (prioritizeWiggles && prioritizeCrossings))) {
            System.out.println();
            System.out.println("----------------------------------------------------------------------------------------------------------------------------------");
            System.out.println("[ERROR] Not clear which weighting strategy to choose: "+"'sameWeighting="+sameWeighting+"', 'prioritizeWiggles="+prioritizeWiggles+"', 'prioritizeCrossings="+prioritizeCrossings+"'");
            System.out.println("----------------------------------------------------------------------------------------------------------------------------------");
            System.out.println();
        } else {

            LineWigglesMain lineWigglesMain = new LineWigglesMain(applicationContext, minMaxWiggleH, binary, mindCrossings,
                    sameWeighting, prioritizeWiggles, prioritizeCrossings, withInitialInstance, initialInstanceFilePath, movieName, movieDataFilePath);
            MovieData movieData = lineWigglesMain.parseMovieData();
            lineWigglesMain.run(movieData);
        }

	}

}
