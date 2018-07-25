# Wiggle Minimization

**This project contains the implementation for my master thesis about minimizing wiggles in storyline visualizations.**

The maven project is constructed of four subprojects containing different parts:

- **basic-storyline**: contains the basic code base required in all other three subprojects
- **ilp-formulation**: implementation of the ILP formulation for wiggle minimization in storyline visualizations
- **sat-formulation**: implementation of the Max-SAT formulation for wiggle minimization in storyline visualizations
- **web-frontend**: contains a web interface for viewing resulting storyline visualization images from the subprojects ilp-formulation and sat-formulation

The project can be compiled and packaged into separated executable JAR files with the following command:

`mvn clean install`

The subprojects can also be compiled and packaged separately, but if you want to do that it is important that you compile the subproject *basic-storyline* first since it is incorporated into the other subprojects as dependency. 

The movie data files are taken from Tanahashi and Ma [10], which are provided at [9]. To use other movie data files for this project it is important to keep the format of the movie data files given by the movie data files from [9].

## Basic storyline

The subproject can be build separately with the following command in the main directory of the subproject *basic-storyline*:

`mvn clean install`

## ILP Formulation

The ILP optimizations can be started by executing the created JAR file *ilp-wiggle-minimisation-exec.jar*. The JAR takes several input parameter, which are necessary for initializing a concrete GRBModel and concrete objective function.

The following parameters can be added to the run command:

- `-Djava.library.path=/absolut/path/to/gurobi/lib`: [required] JVM command to use your native JNI gurobi library, the concrete path has to be replaced by the absolute path to the location of gurobi.jar at your local system
- `movieName`: [required] name of the movie (used for naming output files containing the results storyline visualizations)
- `movieDataFile`: [required] path to movie data file
- `binary`: boolean parameter to indicate if you want to minimize the number of wiggles or another wiggle objective. Concrete parameter settings are `binary=true` and `binary=false (default)`
- `minMaxWiggleH`: boolean parameter to indicate if you want the minimize the maximum wiggle height or another wiggle objective. Concrete parameter settings are `minMaxWiggleH=true` and `minMaxWiggleH=false (default)`
- `withInitialSolution`: boolean parameter to indicate if an initial solution for the corresponding movie data should to be used. concrete parameter settings are `withInitialSolution=true` and `withInitialSolution=false (default)`
- `initialSolutionFile`: gets only considered with `withInitialSolution=true`. Holds path to the JSON file containing the initial solution shown in the command line output of the python script of the genetic algorithm from [10]. The python script can be found at [9]. 
- `mindCrossings`: boolean parameter to indicate of a multi-objective for combining the concrete wiggle objective with a second objective for minimizing the number of crossings is desired. Concrete parameter settings are `mindCrossings=true` and `mindCrossings=false (default)`
- `sameWeighting`: boolean parameter which gets only considered if it is used next to `mindCrossings=true`. It indicates if a uniform weighting between the two objectives should be used. Concrete parameter settings are `sameWeighting=true` and `sameWeighting=false (default)`
- `prioritizeWiggles`: boolean parameter which gets only considered together with `mindCrossings=true`. It indicates if the wiggle objective receives a higher weighting than the crossings objective. Concrete parameter settings are `prioritizeWiggles=true` and `prioritizeWiggles=false (default)`
- `prioritizeCrossings`: boolean parameter which gets only considered together with `mindCrossings=true`. It indicates if the crossings objective receives a higher weighting than the wiggle objective. Concrete parameter settings are `prioritizeCrossings=true` and `prioritizeCrossings=false (default)`

The order of the parameters is not of importance.

The following table gives an overview of the supported wiggle objectives and the corresponding parameter combinations for `binary` and `minMaxWiggleH`:

| wiggle objective               | `binary`  | `minMaxWiggleH` |
| :----------------------------- |:---------:| :-------------:|
| minimize total wiggle height   | false     | false          |
| minimize maximum wiggle height | false     | true           |
| minimize number of wiggles     | true      | false          |

The next table gives an overview of the different weighting strategies and the required parameter combinations for `mindCrossings`, `sameWeighting`, `prioritizeWiggles`, `prioritizeCrossings`:

| weighting strategy                                       | `mindCrossings` | `sameWeighting` | `prioritizeWiggles` | `prioritizeCrossings`|
| :------------------------------------------------------- |:---------------:| :--------------:| :------------------:| :-------------------:|
| uniform weighting between wiggle and crossings objective | true            | true            | false               | false                |
| higher weighting for wiggle objective                    | true            | false           | true                | false                |
| heigher weighting for crossings objective                | true            | false           | false               | true                 |

As initial solution a JSON file is expected containing the command line output produced by the python script from [9] for generating storyline visualizations.

The following Gurobi parameters are used in the implementation of the RGBModel:

- *OutputFlag* = 0
- *MIPFocus* = 3
- *Method* = 3
- *Presolve* = 2

For all other RGBModel parameters the default values were used. The parameters can be adjusted at the beginning of method `run(...)` in `minwiggles.ilpformulation.model.LineWigglesMain.java`.


#### Commands for subproject *ilp-formulation*

The subproject can be build separately with the following command in the main directory of the subproject *ilp-formulation*:

`mvn clean install`

Example run command for *ilp-formulation* from the main directory of the project:

`java -jar -Djava.library.path=/absolut/path/to/gurobi/lib movieName=Inception movieDataFile=path/to/movie/data/file/Inception_interaction_sessions.txt ilp-formulation/target/ilp-wiggle-minimisation-exec.jar binary=false minMaxWiggleH=false withInitialSolution=false sameWeighting=false prioritizeWiggles=false prioritizeCrossings=false mindCrossings=false`

#### Commands you might need in advance to use Gurobi

install gurobi.jar into local maven repository:

`mvn install:install-file -Dfile=/absolut/path/to/gurobi/lib/gurobi.jar -DgroupId=com.gurobi -DartifactId=gurobi -Dversion=<gurobi version number> -Dpackaging=jar`

command to be able to use the native JNI gurobi library:

`export MAVEN_OPTS=-Djava.library.path=/absolut/path/to/gurobi/lib`


## Max-SAT Formulation

The subproject *sat-formulation* includes the implemented SAT formulas for minimizing wiggles in storyline visualizations and serves as kind of adapter between the Max-SAT formulation of this master thesis and Max-SAT solvers.
 
The following two features are supported:

- create input file for Max-SAT solvers in standard WCNF format [1] from a movie data file from [9,10]
- read resulting variable assignments from a Max-SAT solver and create the storyline visualization files which can be used as input for the subproject *web-frontend*

It does not support running a Max-SAT solver with the created WCNF files. This has to be done individually. 

The following Max-SAT solvers from [2] where used for the experiments of this master thesis:

- LMHS [7,8]
- Loandra [5]
- MaxHS [4,6]
- Maxino [3]

#### Commands for subproject *sat-formulation*

The following parameters can be added to the run command:

- `movieToWCNF`: indicates that you want to create a WCNF file containing all clauses for the Max-SAT formulation
- `createSolution`: indicates  that you have a solution for a storyline visualization from a Max-SAT solver in form of the variable assignments and you want to create the input files for the web-frontend subproject
- `movieName`: [required] name of the movie (used for naming output files containing the results storyline visualizations)
- `movieDataFile`: [required] path to movie data file
- `binary`: boolean parameter which indicates which wiggle objective should be chosen for the SAT formulation. With `binary=false` you choose the minimization of the total wiggle height and with `binary=false` you choose the minimization of the number of wiggles as objective. It is important that you use for feature `createSolution` the same parameter setting for `binary` as you used before during creating the WCNF file. concrete paramter settings are `binary=true` and `binary=false (default)` 
- `variablesFile`: mandatory parameter if `createSolution` is added to the run command as well. Holds path to simple txt file with the resulting variable assignments from the Max-SAT solver. The file should not contain any text formatting or line breaks, only the list of variables with or without an negation, separated by one space.
- `maxSatSolverName`: only usable in combination with `createSolution`. Holds name of the used Max-SAT solver.

The subproject can be build separately with the following command in the main directory of the subproject *sat-formulation*:

`mvn clean install`

Run command to create a WCNF file for a movie data file from the main directory of the project:

`java -jar sat-formulation/target/sat-wiggle-minimisation-jar-with-dependencies.jar movieToWCNF movieName=Inception movieDataFile=path/to/movie/data/Inception_interaction_sessions.txt binary=true`

Run command to create a WCNF file for a movie data file from the main directory of the project:

`java -jar sat-formulation/target/sat-wiggle-minimisation-jar-with-dependencies.jar createSolution movieName=Inception movieDataFile=path/to/movie/data/file/Inception_interaction_sessions.txt binary=true variablesFile=path/to/assigned/variables/Inception_variables.txt maxSatSolverName=<solverName>`


## Web Frontend

All storyline visualization results that should appear in the drop-down of the web interface have to be added to folder `web-frontend/results`. 
Every storyline visualization has *three JSON files* which are required for the web frontend (the subproject *ilp-formulation* might produce more JSON files): one for the *compressed* storyline visualization, one for the *uncompressed* storyline visualization and one for the *meetings information* of the storyline visualization.

Do not add additional nesting folders to the result folders produced by the subprojects *ilp-formulation* or *sat-formulation*.
Just move the folders named with the timestamp of the solution to `web-frontend/results` so that all solution folders are on the same directory level. 

The subproject can be build separately with the following command in the main directory of subproject *web-frontend*:

`mvn clean install`

Run `web-frontend` with the following command in the main directory of subproject *web-frontend*:

`mvn spring-boot:run`

The web interface can be accessed with the following url:
    
    http://localhost:8080/#/

Credentials for web socket:

    - Username: user
    - Password: password

The frontend supports storyline visualizations with up to 14 characters.

---

## References

[1]  MaxSAT Evaluation 2017. Input format (accessed 2018-07-24). http://mse17.cs.helsinki.fi/rules.html#input .

[2]  MaxSAT Evaluation 2017. Participating solvers (accessed 2018-07-24). http://mse17.cs.helsinki.fi/descriptions.html .

[3]  Mario Alviano. Maxino. MaxSAT Evaluation 2017, page 10.

[4]  Fahiem Bacchus. Maxhs v3. 0 in the 2017 maxsat evaluation. MaxSAT Evaluation 2017, page 8.

[5]  Jeremias Berg, Tuukka Korhonen, and Matti Järvisalo. Loandra: Pmres extended with preprocessing entering maxsat evaluation 2017. MaxSAT Evaluation 2017, page 13.

[6]  Jessica Davies and Fahiem Bacchus. Maxhs: A fast and robust maxsat solver (accessed 2018-07-24). http://www.maxhs.org .

[7]  Paul Saikko, Jeremias Berg, and Matti Järvisalo. Lmhs: a sat-ip hybrid maxsat solver. In International Conference on Theory and Applications of Satisfiability Testing, pages 539–546. Springer, 2016.

[8]  Paul Saikko, Tuukka Korhonen, Jeremias Berg, and Matti Järvisalo. Lmhs in maxsat evaluation 2017. MaxSAT Evaluation 2017, page 16.

[9]  Yuzuru Tanahashi and Kwan-Liu Ma. Design considerations for optimizing storyline visualizations: Data and resources (accessed 2018-07-24). https://old.datahub.io/dataset/vis-storyline-visualizations .

[10]  Yuzuru Tanahashi and Kwan-Liu Ma. Design considerations for optimizing story- line visualizations. IEEE Transactions on Visualization and Computer Graphics, 18(12):2679–2688, 2012.
