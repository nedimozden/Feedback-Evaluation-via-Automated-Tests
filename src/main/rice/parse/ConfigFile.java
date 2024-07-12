package main.rice.parse;

import main.rice.node.APyNode;
import java.util.List;

/**
 * Represents a configuration file with information about function name, nodes, and the number of random values.
 */
public class ConfigFile {

    private final String funcName;

    private final List<APyNode<?>> nodes;

    private final int numRand;

    /**
     * Constructs a ConfigFile instance.
     *
     * @param funcName The name of the function.
     * @param nodes    The list of APyNode instances representing types and domains.
     * @param numRand  The number of random values.
     */
    public ConfigFile(String funcName, List<APyNode<?>> nodes, int numRand) {
        this.funcName = funcName;
        this.nodes = nodes;
        this.numRand = numRand;
    }

    /**
     * Gets the name of the function.
     *
     * @return The function name.
     */
    public String getFuncName() {
        return this.funcName;
    }

    /**
     * Gets the list of APyNode instances representing types and domains.
     *
     * @return The list of APyNode instances.
     */
    public List<APyNode<?>> getNodes() {
        return this.nodes;
    }

    /**
     * Gets the number of random values.
     *
     * @return The number of random values.
     */
    public int getNumRand() {
        return this.numRand;
    }
}
