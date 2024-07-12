package main.rice.basegen;

import main.rice.node.APyNode;
import main.rice.obj.APyObj;
import main.rice.test.TestCase;
import java.util.*;

/**
 * A class that is used to generate a "base" set of test cases, comprised of the union
 * of a "semi-exhaustive" test set generated within a smaller domain, plus some number of
 * random probes from a larger domain.
 */
public class BaseSetGenerator {

    /**
     * The nodes that will be used to perform generation.
     */
    private final List<APyNode<?>> nodes;

    /**
     * The number of random cases to generate.
     */
    private final int numRand;

    /**
     * Constructor for a BaseSetGenerator, which initializes the fields.
     *
     * @param nodes   the nodes that will be used to perform generation
     * @param numRand the number of random cases to generate
     */
    public BaseSetGenerator(List<APyNode<?>> nodes, int numRand) {
        this.nodes = nodes;
        this.numRand = numRand;
    }

    /**
     * Generates a the base test set (the union of the semi-exhaustive and random test
     * sets) according to the type and domain specifications in the input list of nodes.
     * Each output test case encapsulates a list of arguments (APyObjs), where the i-th
     * argument is typified by the i-th element in nodes.
     *
     * @return the base test set (as a List, so that we can use indices in the Tester)
     */
    public List<TestCase> genBaseSet() {
        Set<TestCase> exTests = this.genExTests();
        Set<TestCase> randTests = this.genRandTests(exTests);

        // Convert to a list so that we can use indices in testing
        List<TestCase> allTests = new ArrayList<>();
        allTests.addAll(exTests);
        allTests.addAll(randTests);
        return allTests;
    }

    /**
     * Exbaustively generates a set of all valid test cases within the exhaustive
     * domains stored within the nodes.
     *
     * @return a set of valid test cases according to the given specifications
     */
    public Set<TestCase> genExTests() {
        // For each parameter, generate the set of all possible arguments
        List<Set<? extends APyObj<?>>> possibleArgs = new ArrayList<>();
        for (APyNode<?> node : this.nodes) {
            Set<? extends APyObj<?>> args = node.genExVals();
            possibleArgs.add(args);
        }

        // Generate all possible combinations of arguments (selecting one valid argument
        // for each parameter)
        List<List<APyObj<?>>> combos = generateArgCombos(possibleArgs, 0);
        Set<TestCase> tests = new HashSet<>();

        // Encapsulate the results within TestCase objects and return
        for (List<APyObj<?>> combo : combos) {
            tests.add(new TestCase(combo));
        }
        return tests;
    }

    /**
     * Randomly generates a list of valid test cases of size numTests, according to the
     * random domains stored within the nodes. Each output test case encapsulates a list
     * of arguments (APyObjs), where the i-th argument is typified by the i-th element in
     * nodes.
     *
     * @param exTests the set of exhaustive tests that have been generated previously
     * @return a list of valid test cases, according to the given specifications
     */
    public Set<TestCase> genRandTests(Set<TestCase> exTests) {
        int numAccepted = 0;
        Set<TestCase> randTests = new HashSet<>();

        // Randomly generate one test at a time until we've generated enough. Need to
        // continually check the size in case we randomly generate the same test twice
        while (numAccepted < this.numRand) {

            // Randomly generate each argument
            List<APyObj<?>> args = new ArrayList<>();
            for (APyNode<?> node : this.nodes) {
                APyObj<?> arg = node.genRandVal();
                args.add(arg);
            }

            // Wrap arguments in a TestCase object and add to the base set, making sure
            // that it isn't a duplicate of an object in the exhaustive set
            TestCase test = new TestCase(args);
            if (!exTests.contains(test) && !randTests.contains(test)) {
                randTests.add(test);
                numAccepted++;
            }
        }
        return randTests;
    }

    /**
     * Helper function for generateTest; a recursive algorithm for generating all possible
     * combinations of arguments across multiple parameters. Uses an additional input,
     * index, to keep track of which arguments have already been combined.
     *
     * @param possibleArgs a list of sets, where the i-th set contains all possible
     *                     arguments (as PyObjs) for the i-th parameter
     * @param index        the next index within possibleArgs for which we need to
     *                     generate all possible combinations
     * @return all possible combinations of args, where the i-th element in each sublist
     * within the returned list is an element of the i-th set in possibleArgs
     */
    private static List<List<APyObj<?>>> generateArgCombos(
            List<Set<? extends APyObj<?>>> possibleArgs, int index) {

        // BASE CASE: index is greater than the last index in possibleArgs ->
        // no args remain -> return one empty combination
        if (index == possibleArgs.size()) {
            List<List<APyObj<?>>> combos = new ArrayList<>();
            combos.add(new ArrayList<APyObj<?>>());
            return combos;
        }

        // RECURSIVE CASE: index is within the bounds of possibleArgs ->
        // Get all combinations of the later args
        List<List<APyObj<?>>> laterCombos = generateArgCombos(possibleArgs, index + 1);

        // Get all possible args for the current index
        Set<? extends APyObj<?>> currArgs = possibleArgs.get(index);

        // For every existing combination, add every possible arg in currArs
        List<List<APyObj<?>>> allCombos = new ArrayList<>();
        for (APyObj<?> arg : currArgs) {
            for (List<APyObj<?>> combo : laterCombos) {
                // Make sure to clone so as to not mutate the original
                List<APyObj<?>> comboClone = new ArrayList<>(combo);
                comboClone.add(0, arg);
                allCombos.add(comboClone);
            }
        }
        return allCombos;
    }
}