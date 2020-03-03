/* 
 * Copyright 2019 Georgia Institute of Technology
 * All rights reserved.
 *
 * Author(s): Xiangyu Li <xiangyu.li@cc.gatech.edu>
 *
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package anonymous.domain.enlighten;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {

	public static void main(String[] args) {
		RunParams runParams = getRunParams(args);
		FeedbackDirectedFLCore.setEnableAf(!runParams.disableAf);
		FeedbackDirectedFLCore.setEnableWt(!runParams.disableWt);
		if (runParams.runMode == RunMode.PRESET_DOCUMENTED 
				|| runParams.runMode == RunMode.SPECIFIED_BENCHMARK) {
			ExperimentDataLayout.SUBJECTS_ROOT = runParams.benchmarkRoot;
			ExperimentDataLayout.DATA_ROOT = runParams.outputRoot;
			SimulatedFeedbackDirectedFL.main(runParams.faultNames);
		}
	}
	
	private static RunParams getRunParams(String[] cmdArgs) {
		Options options = new Options();
		Option opPresetDocumentedFaults = Option.builder("d")
				.longOpt("documented")
				.desc("Run Enlighten on the previously-documented benchmark faults we provided.")
				.hasArg(false).required(false)
				.build();
		options.addOption(opPresetDocumentedFaults);
		Option opFaultName = Option.builder("n")
				.longOpt("fault-names")
				.type(String.class)
				.desc("Specify the names of the benchmark faults to run Enlighten on, separated by coma. "
						+ "This option is ignored unless --documented is specified")
				.hasArg(true).required(false)
				.valueSeparator(',')
				.build();
		options.addOption(opFaultName);
		Option opProvidedBenchmarkRoot = Option.builder("r")
				.longOpt("provided-benchmark-root")
				.type(String.class)
				.desc("Specify the path of the directory that contains the benchmark faults we provided.")
				.hasArg(true).required(false)
				.build();
		options.addOption(opProvidedBenchmarkRoot);
		Option opBenchmarkPath = Option.builder("p")
				.longOpt("benchmark-path")
				.type(String.class)
				.desc("Run Enlighten on a single benchmark fault")
				.hasArg(true).required(false)
				.build();
		options.addOption(opBenchmarkPath);
		Option opOutputPath = Option.builder("o")
				.longOpt("output-path")
				.type(String.class)
				.desc("Specify the output directory for Enlighten.")
				.hasArg(true).required(false)
				.build();
		options.addOption(opOutputPath);
		Option opModeDisableAf = Option.builder("noaf")
				.longOpt("disable-af")
				.desc("Run Enlighten with the amplifying factor disabled.")
				.hasArg(false).required(false)
				.build();
		options.addOption(opModeDisableAf);
		Option opModeDisableWt = Option.builder("nowt")
				.longOpt("disable-wt")
				.desc("Run Enlighten with the variable weight in the SFL component disabled.")
				.hasArg(false).required(false)
				.build();
		options.addOption(opModeDisableWt);
		CommandLineParser clParser = new DefaultParser();
		CommandLine cl = null;
		try {
			cl = clParser.parse(options, cmdArgs);
		} catch (ParseException ex) {
			printOptionsAndAbort(options);
		}
		RunParams runParams = new RunParams();
		if (cl.hasOption("documented")) {
			runParams.runMode = RunMode.PRESET_DOCUMENTED;
			runParams.benchmarkRoot = "../benchmark/documented";
		}
		if (cl.hasOption("benchmark-path")) {
			if (runParams.runMode != null) {
				System.err.println(
						"Only one of --documented, or --benchmark-path can be specified.");
				System.exit(1);
			}
			runParams.runMode = RunMode.SPECIFIED_BENCHMARK;
			Path benchmarkPath = Paths.get(cl.getOptionValue("benchmark-path")).toAbsolutePath();
			runParams.benchmarkRoot = benchmarkPath.getParent().toString();
			runParams.faultNames = new String[1];
			runParams.faultNames[0] = benchmarkPath.getFileName().toString();
		}
		if (runParams.runMode == null) {
			runParams.runMode = RunMode.PRESET_DOCUMENTED;
			runParams.benchmarkRoot = "../benchmark/documented";
		}
		if (runParams.runMode == RunMode.PRESET_DOCUMENTED) {
			if (cl.hasOption("fault-names")) {
				runParams.faultNames = cl.getOptionValues("fault-names");
			}
			if (cl.hasOption("provided-benchmark-root")) {
				runParams.benchmarkRoot = cl.getOptionValue("provided-benchmark-root");
			}
		}
		if (cl.hasOption("output-path")) {
			runParams.outputRoot = cl.getOptionValue("output-path");
		}
		if (cl.hasOption("disable-af")) {
			runParams.disableAf = true;
		}
		if (cl.hasOption("disable-wt")) {
			runParams.disableWt = true;
		}
		return runParams;
	}
	
	private static void printOptionsAndAbort(Options options) {
		HelpFormatter optionsPrinter = new HelpFormatter();
		optionsPrinter.printHelp("anonymous.domain.enlighten.Main", options);
		System.exit(1);
	}
	
	private static class RunParams {
		public RunMode runMode;
		public String[] faultNames = new String[0];
		public String benchmarkRoot;
		public String outputRoot = "../enlighten-output";
		public boolean disableAf = false;
		public boolean disableWt = false;
	}
	
	private static enum RunMode {
		PRESET_DOCUMENTED,
		SPECIFIED_BENCHMARK
	}
}
