package minwiggles.basicstoryline.moviedata;

import minwiggles.basicstoryline.location.Location;
import minwiggles.basicstoryline.character.Character;
import minwiggles.basicstoryline.meeting.InteractionSession;
import minwiggles.basicstoryline.storyline.CompressedStoryline;
import minwiggles.basicstoryline.storyline.UncompressedStoryline;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


public class MovieDataParser {

    public MovieDataParser(){}

    public MovieData parse(String movieDataFileName, String movieName) throws IOException {
        MovieData movieData = null;
        File movieDataFile = new File(movieDataFileName);
        FileReader movieDataInput = new FileReader(movieDataFile);
        BufferedReader movieDataBufRead = new BufferedReader(movieDataInput);
        String[] movieDataLineArray = null;

        int timeCount = -1;
        int nodeCount = -1;
        int interactionSessionCount = -1;
        int locationCount = -1;

        // read movie data file
        // read count lines
        for(int i=0; i<5; i++){
            movieDataLineArray = readNextLine(movieDataBufRead, "=");
            if(movieDataLineArray != null){
                if(movieDataLineArray[0].equals("TIME_COUNT")){
                    timeCount = Integer.parseInt(movieDataLineArray[1]);
                } else if(movieDataLineArray[0].equals("NODE_COUNT")){
                    nodeCount = Integer.parseInt(movieDataLineArray[1]);
                } else if(movieDataLineArray[0].equals("INTERACTION_SESSION_COUNT")){
                    interactionSessionCount = Integer.parseInt(movieDataLineArray[1]);
                } else if(movieDataLineArray[0].equals("LOCATION_COUNT")){
                    locationCount = Integer.parseInt(movieDataLineArray[1]);
                } else if(movieDataLineArray[0].equals("WEIGHTS")){
                    // currently ignored
                }
            }
        }

        if(timeCount>-1 && nodeCount>-1 && interactionSessionCount>-1 && locationCount>-1){
            movieData = new MovieData(movieName, timeCount, nodeCount, interactionSessionCount, locationCount);
            readCharactersAndLocations(movieDataBufRead, movieData);
            readInteractionSessions(movieDataBufRead, movieData);
        }

        movieDataInput.close();
        movieDataBufRead.close();
        return  movieData;
    }

    public UncompressedStoryline parseInitVisualization(String initStoryVisFileName, int nodeCount, int timeCount) throws IOException {
//        if(initStoryVisFileName == null) return null;
        //read initial visualization file
        JsonObject initStoryVisObj = new JsonParser()
                .parse(new String(Files.readAllBytes(Paths.get(initStoryVisFileName)), StandardCharsets.UTF_8))
                .getAsJsonObject();

        int maxSlot = 0;
        boolean maxSlotSet = false;
        Set<Integer> necessarySlots = new LinkedHashSet<>();
        int slot;
        JsonObject characterObj;

        for(int c=0; c<nodeCount; c++){
            if(initStoryVisObj.has(String.valueOf(c))){
                characterObj = initStoryVisObj.getAsJsonObject(String.valueOf(c));

                for(int t=0; t<timeCount; t++){
                    if(characterObj.has(String.valueOf(t))){
                        slot = characterObj.getAsJsonPrimitive(String.valueOf(t)).getAsInt();
                        necessarySlots.add(slot);
                        necessarySlots.add(slot+1);

                        if(!maxSlotSet){
                            maxSlot = slot;
                            maxSlotSet = true;
                        } else if(slot > maxSlot) maxSlot = slot;
                    }
                }
            }
        }

        if(necessarySlots.contains(maxSlot+1)){
            necessarySlots.remove(maxSlot+1);
        }

        List<Integer> sortedSlots = new LinkedList<>(necessarySlots);
        Collections.sort(sortedSlots);

        //pre-initialize the storyline array
        String[][] storyline = new String[sortedSlots.size()][timeCount];
        for(int s=0; s<sortedSlots.size(); s++){
            for(int t=0; t<timeCount; t++){
                storyline[s][t] = " ";
            }
        }

        //add character lines to storyline
        for(int c=0; c<nodeCount; c++) {
            if (initStoryVisObj.has(String.valueOf(c))) {
                characterObj = initStoryVisObj.getAsJsonObject(String.valueOf(c));
                for(int t=0; t<timeCount; t++){
                    if(characterObj.has(String.valueOf(t))){
                        slot = characterObj.getAsJsonPrimitive(String.valueOf(t)).getAsInt();
                        storyline[sortedSlots.indexOf(slot)][t] = String.valueOf(c);
                    }
                }
            }
        }

        return new UncompressedStoryline(storyline);
    }

    public CompressedStoryline compressStoryline(UncompressedStoryline uncompressedStorylineObj, Integer[] compressedTimePoints){
        String[][] uncompressedStoryline = uncompressedStorylineObj.getStoryline();
        String[][] compressedStoryline = new String[uncompressedStoryline.length][compressedTimePoints.length];

        for(int s=0; s<uncompressedStoryline.length; s++){
            for(int t=0; t<compressedTimePoints.length; t++){
                if(t+1 == compressedTimePoints.length)
                    compressedStoryline[s][t] = uncompressedStoryline[s][compressedTimePoints[t]-1];
                else
                    compressedStoryline[s][t] = uncompressedStoryline[s][compressedTimePoints[t]];
            }
        }

        //TODO: should I have to calculate the objective here??
        return new CompressedStoryline(compressedStoryline, 0);
    }


    private String[] readNextLine(BufferedReader bufRead, String regex) throws IOException {
        String currLine =  bufRead.readLine();
        if(currLine != null) {
            String[] lineArray = currLine.split(regex);
            for (int i=0; i<lineArray.length; i++){
                lineArray[i] =  lineArray[i].trim();
            }
            return lineArray;
        } else {
            return null;
        }
    }

    private void readCharactersAndLocations(BufferedReader bufRead, MovieData movieData) throws IOException {
        String currLine =  bufRead.readLine().replace("{", "").replace("}", "").replace("'", "").trim();
        String[] lineArray = currLine.split(",");
        for(String line: lineArray){
            String[] characterArray = line.trim().split(":");
            Character character = new Character(Integer.parseInt(characterArray[1].trim()), characterArray[0].trim());
            movieData.addCharacter(character);
        }

        currLine =  bufRead.readLine().replace("{", "").replace("}", "").replace("'", "").trim();
        lineArray = currLine.split(",");
        for(String line: lineArray){
            String[] locationArray = line.trim().split(":");
            movieData.addLocation(new Location(Integer.parseInt(locationArray[1].trim()), locationArray[0].trim()));
        }
    }

    private void readInteractionSessions(BufferedReader bufRead, MovieData movieData) throws IOException {
        String name = null;
        int id = -1;
        int start = -1;
        int end = -1;
        int[] members = null;
        int location = -1;

        String[] lineArray = readNextLine(bufRead, ":");
        while(lineArray != null){
            if(lineArray[0].equals("Name")){
                name = lineArray[1];
            } else if(lineArray[0].equals("Id")){
                id = Integer.parseInt(lineArray[1]);
            } else if(lineArray[0].equals("Start")){
                start = Integer.parseInt(lineArray[1]);
            } else if(lineArray[0].equals("End")){
                end = Integer.parseInt(lineArray[1]);
            } else if(lineArray[0].equals("Members")){
                String[] membersArray = lineArray[1].trim().replace("[", "").replace("]", "").split(",");
                members = new int[membersArray.length];
                for(int i=0; i<membersArray.length; i++){
                    members[i] = Integer.parseInt(membersArray[i].trim());
                }
            } else if(lineArray[0].equals("Location")){
                location = Integer.parseInt(lineArray[1]);
            }

            if(name!=null && id>-1 && start>-1 && end>-1 && members!=null & location>-1){
                movieData.addInteractionSession(new InteractionSession(id, name, start, end, members, location));
                name = null;
                id = -1;
                start = -1;
                end = -1;
                members = null;
                location = -1;
            }

            lineArray = readNextLine(bufRead, ":");
        }

        movieData.compressTimePoints();
        movieData.createActiveTimePointsPerCharacter();
    }
}
