package minwiggles.webfrontend.controller;

import com.google.gson.Gson;
import minwiggles.basicstoryline.meeting.MeetingsInformation;
import minwiggles.basicstoryline.moviedata.MovieData;
import minwiggles.basicstoryline.storyline.CompressedStoryline;
import minwiggles.basicstoryline.storyline.UncompressedStoryline;
import minwiggles.webfrontend.dto.StorylineDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/")
public class MessageController {

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private SimpMessagingTemplate template;

    @MessageMapping("/initialize")
    @SendTo("/all-storylines")
    public List<StorylineDTO> init(){
        return this.readAllSavedStorylines();
    }

    @SendTo("/movie-data")
    public MovieData sendMovieData(MovieData movieData){
        return movieData;
    }

    private List<StorylineDTO> readAllSavedStorylines(){
        Properties prop = new Properties();
        InputStream input = null;
        List<StorylineDTO> storylines = new ArrayList<>();

        try {

            Map<String, Map<String, List<String>>> directoriesMap = new HashMap<>();//key: folder name of run
            Map<String, List<String>> filesMap; //key: solCount_XX, value: file name of storyline visualization
            Map<String, String> movieDataMap = new HashMap<>();//key: folder name of run, value: movie data file name
            List<String> files;

            List<Path> paths = Files.walk(Paths.get("results")).collect(Collectors.toList());
            for(Path path: paths){
                String[] pathArray = path.toString().split("/");

                if(pathArray.length>=3){
                    if(pathArray[2].contains("solCount") || pathArray[2].contains("-obj_")){
                        if(directoriesMap.containsKey(pathArray[1])){
                            filesMap = directoriesMap.get(pathArray[1]);
                        } else {
                            filesMap = new HashMap<>();
                        }

                        String key = pathArray[2].split("-")[0];
                        if(filesMap.containsKey(key)){
                            files = filesMap.get(key);
                        } else {
                            files = new LinkedList<>();
                        }

                        if(pathArray[2].contains("-uncompressedStoryline.json")
                                || pathArray[2].contains("-compressedStoryline.json")
                                || pathArray[2].contains("-meetingsInformation.json")){
                            files.add(pathArray[2]);
                        }

                        filesMap.put(key, files);
                        directoriesMap.put(pathArray[1], filesMap);

                    } else {
                        movieDataMap.put(pathArray[1], pathArray[2]);
                    }
                }
            }

            for(String runFolderName: directoriesMap.keySet()){
                filesMap = directoriesMap.get(runFolderName);
                if(filesMap != null && filesMap.size() > 0){
                    for(String keySolCount: filesMap.keySet()){
                        StorylineDTO storylineDTO = readStorylinesFiles(movieDataMap.get(runFolderName), runFolderName, filesMap.get(keySolCount));
                        if(storylineDTO!=null){
                            storylines.add(storylineDTO);
                        }
                    }
                }
            }

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

        return storylines;
    }

    private StorylineDTO readStorylinesFiles(String movieDataFile, String runFolderName, List<String> files) throws FileNotFoundException {
        Gson gson = new Gson();
        CompressedStoryline compressedStoryline = null;
        UncompressedStoryline storylineUnCompressed = null;
        MeetingsInformation meetingsInformation = null;
        StorylineDTO storylineDTO;
        String name;

        MovieData movieData = gson.fromJson(readFile("results/"+runFolderName+"/"+movieDataFile), MovieData.class);

        for(String fileName: files){
            if(fileName.contains("-uncompressedStoryline.json")){
                storylineUnCompressed = gson.fromJson(readFile("results/"+runFolderName+"/"+fileName), UncompressedStoryline.class);
            } else if(fileName.contains("-compressedStoryline.json")){
                compressedStoryline = gson.fromJson(readFile("results/"+runFolderName+"/"+fileName), CompressedStoryline.class);
            } else if(fileName.contains("-meetingsInformation.json")){
                meetingsInformation = gson.fromJson(readFile("results/"+runFolderName+"/"+fileName), MeetingsInformation.class);
            }
        }

        if(runFolderName.trim().substring(0,3).equals("SAT")){
            name = runFolderName.trim() + '/';
        } else {
            name = runFolderName.trim() + '/' + files.get(0).trim().split("-")[0] + '-';
        }

        try{
            storylineDTO = new StorylineDTO(name, compressedStoryline.getStoryline(),
                    storylineUnCompressed.getStoryline(), meetingsInformation, movieData);
        } catch(NullPointerException nex){
            return null;
        }

        return storylineDTO;
    }

    private FileReader readFile(String path) throws FileNotFoundException {
        return new FileReader(new File(path).getAbsolutePath());
    }
}
