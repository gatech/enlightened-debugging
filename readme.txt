==================================================================================

I. Installation


0. Recommended compiler and runtime environment

	Compiler: Oracle JDK version 1.8.0_121
	Runtime: Oracle JRE version 1.8.0_141 on Windows 10


1. Compile project annotation-utils


2. Compile project jpf-core

	This is a customized version of the Java PathFinder (JPF).
	Visit https://babelfish.arc.nasa.gov/trac/jpf for more information.

	The project is dependent on annotation-utils.


3. Compile project jpf-nhandler

	This is a customized version of the JPF Native Method Handler.
	Visit https://bitbucket.org/nastaran/jpf-nhandler for more information.

	The project is dependent on jpf-core.


4. Compile project enlighten

	The project is dependent on annotation-utils and jpf-core.

5. Create site.properties file for JPF

	JPF requires a site.properties file to be located at ~/.jpf/site.properties.
	(~ stands for the home directory on your local machine.)
	A template file has been provided at jpf-core/site.properties.
	Replace "<Absolute path on your local machine>"  with the actual path.

	For more information about the site properties file, 
	visit https://babelfish.arc.nasa.gov/trac/jpf.


6. Create jar files used for instrumentation

	Run enlighten/gen-iagent.pl.
	This will create iagent.jar and callback.jar

7. Set a few paths in the source code

	Set
		anonymous.domain.enlighten.exec.InstrumentationJars.instrumenterJarPath
		anonymous.domain.enlighten.exec.InstrumentationJars.callbackJarPath
	to corresponding values on your local system.
	You can use either absolute paths or paths relative to the working directory.

At this point, Enlighten should be ready to go!


==================================================================================

II. Running Experiment with Simulated User (Automated Oracle)


1. Running on specified benchmark program

	Main class: anonymous.domain.enlighten.SimulatedFeedbackDirectedFL
	Parameters: either a list of names of benchmark programs, or 
		empty in which case Enlighten will run on all benchmark programs
		found in the benchmark directory.

	Before running Enlighten, you may want to set the benchmark directory
	in which the experiment driver will search for benchmark programs by names.
	Set anonymous.domain.enlighten.ExperimentDataLayout.SUBJECT_ROOT to corresponding
	value on your local machine.
	You can use either the absolute path or path relative to the working directory.

	You may also want to set the output directory where the tool stores the intermediate
	data and the experiment results.
	Set anonymous.domain.enlighten.ExperimentDataLayout.DATA_ROOT to the corresponding
	value on your local machine.
	You can use either the absolute path or path relative to the working directory.

	You can create benchmark programs yourself.
	The root directory of the benchmark program should contain two sub-folders -- 
	"faulty", and "golden", containing the faulty and reference implementations, respectively.
	Both sub-folders should also contain a subject_program.info file, containing information
	about the application package, application source folder, test source folder, and library
	paths. The subject_program.info file of the faulty version should also specify the 
	location of the fault.
	Refer to commons-math-v1-C_AK_1/faulty/subject_program.info we provided in our benchmark
	for an example.

	Some examples of the command line to run Enlighten are listed below:
		java anonymous.domain.enlighten.SimulatedFeedbackDirectedFL
		java anonymous.domain.enlighten.SimulatedFeedbackDirectedFL commons-math-v1-C_AK_1
		java anonymous.domain.enlighten.SimulatedFeedbackDirectedFL commons-math-v1-C_AK_1 commons-math-v1-F_AK_1


2. Running on mutation faults

	We provided a wrapper class to run Enlighten on mutation faults conveniently.
	Use 
		java anonymous.domain.enlighten.mutation.MutationIterativeFL
	to run Enlighten on all mutants we used in our benchmark.
	Before invoking the command, you may need to change 
	anonymous.domain.enlighten.mutation.MutationIterativeFL.benchmarkRoot to the directory
	containing the mutation faults on your local machine.


3. Running Enlighten with customized SFL formula and/or amplifying factor disabled

	To disable the customized SFL formula, 
	set anonymous.domain.enlighten.FeedbackDirectedFLCore.INITIAL_PASSING_TESTS_FL_WEIGHT to 1.0.

	To disable the amplifying factor, 
	set anonymous.domain.enlighten.FeedbackDirectedFLCore.ENABLE_AMPLIFYING_FACTOR to false.


4. Running Enlighten with automated oracle that occasionally provides erroneous answers

	To make the automated oracle return erroneous answers occasionally, we made some changes to the
	source code of the oracle. In future versions, we will factor our these changes and make it
	controlled by a single option. But for now, to run this experiment, please override the 
	source code of Enlighten using the the files provided in enlighten-error-sensitivity.zip.

	After re-compiling Enlighten, execute
		java anonymous.domain.enlighten.wrongfeedback.WrongFeedbackStochastic
	to run the experiment on specific benchmark faulty program.
	It tasks the same parameters as the SimulatedFeedbackDirectedFL class.

	Execute
		java anonymous.domain.enlighten.mutation.WrongFeedbackExpr
	to execute the experiment on all mutation faults conveniently.
	You may need to change anonymous.domain.enlighten.mutation.WrongFeedbackExpr.baseDir to point
	to the directory containing the mutation faults.


==================================================================================
