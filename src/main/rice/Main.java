package main.rice;

// TODO: implement the Main class here

import main.rice.basegen.BaseSetGenerator;
import main.rice.concisegen.ConciseSetGenerator;
import main.rice.node.APyNode;
import main.rice.parse.*;
import main.rice.test.*;
import java.io.IOException;
import java.util.*;

/**
 * Main class for the FEAT test case auto-generator.
 */
public class Main {

    /**
     * Entry point for the test generation process. Reads the configuration file, generates a base test set,
     * runs tests, and uses a concise set generator to obtain an approximately minimal subset of tests.
     *
     * @param args Command line arguments, where args[0] is the path to the configuration file,
     *             args[1] is the path to the implementation directory, and args[2] is the path to the solution file.
     * @throws IOException           If there is an issue reading files.
     * @throws InterruptedException  If the execution is interrupted during the test generation process.
     * @throws InvalidConfigException If there is an issue with the configuration file.
     */
    public static void main(String[] args) throws IOException, InterruptedException, InvalidConfigException {
        System.out.println("Generating concise test set");
        System.out.println(generateTests(args));
    }

    /**
     * Generates a concise set of tests using a multi-step process. Parses the configuration file,
     * generates a base test set, runs tests on the implementation, and applies a concise set generator
     * to obtain an approximately minimal subset of tests.
     *
     * @param args Command line arguments, where args[0] is the path to the configuration file,
     *             args[1] is the path to the implementation directory, and args[2] is the path to the solution file.
     * @return A set of test cases representing an approximately minimal subset of tests.
     * @throws IOException           If there is an issue reading files.
     * @throws InterruptedException  If the execution is interrupted during the test generation process.
     * @throws InvalidConfigException If there is an issue with the configuration file.
     */
    public static Set<TestCase> generateTests(String[] args)
            throws IOException, InterruptedException, InvalidConfigException {
        // Extract the command line args
        String configFilePath = args[0];
        String implDirPath = args[1];
        String solutionPath = args[2];

        // Parse the config file
        ConfigFile config = parseConfigFile(configFilePath);
        String funcName = config.getFuncName();

        // Generate the base test set, if it doesn't already exist
        List<TestCase> baseTestSet = genBaseTestSet(config);

        // Use base test set to test all files, if the test results don't already exist
        TestResults testResults =
                runTests(funcName, baseTestSet, implDirPath, solutionPath);

        // Select an approximately minimal subset from the base test set
        return ConciseSetGenerator.setCover(testResults);
    }

    /**
     * Parses the configuration file and returns the ConfigFile object.
     *
     * @param configFilePath The path to the configuration file.
     * @return The ConfigFile object containing information about the test generation.
     * @throws IOException            If there is an issue reading the configuration file.
     * @throws InvalidConfigException If the configuration file is invalid.
     */
    private static ConfigFile parseConfigFile(String configFilePath)
            throws IOException, InvalidConfigException {
        ConfigFileParser parser = new ConfigFileParser();
        return ConfigFileParser.parse(parser.readFile(configFilePath));
    }

    /**
     * Generates a base test set using configuration information such as the number of random tests
     * and the specified nodes representing different types in the configuration file.
     *
     * @param config The configuration file object containing information about the test generation.
     * @return A base test set generated based on the configuration.
     */
    private static List<TestCase> genBaseTestSet(ConfigFile config) {
        // Extract configuration info
        int numRandom = config.getNumRand();
        List<APyNode<?>> nodes = config.getNodes();

        // Generate the base test set
        BaseSetGenerator baseGen = new BaseSetGenerator(nodes, numRandom);
        return baseGen.genBaseSet();
    }

    /**
     * Runs tests on the implementation using the provided base test set and computes the expected results.
     *
     * @param funcName       The name of the function to be tested.
     * @param baseTestSet    The base test set to be used for testing.
     * @param implDirPath    The path to the implementation directory.
     * @param solutionPath   The path to the solution file.
     * @return The results of running tests on the implementation.
     * @throws IOException           If there is an issue reading files.
     * @throws InterruptedException  If the execution is interrupted during the test generation process.
     */
    private static TestResults runTests(String funcName, List<TestCase> baseTestSet,
                                        String implDirPath, String solutionPath) throws IOException, InterruptedException {
        Tester tester = new Tester(funcName, solutionPath, implDirPath, baseTestSet);
        tester.computeExpectedResults();
        return tester.runTests();
    }

}