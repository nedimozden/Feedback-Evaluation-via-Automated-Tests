package main.rice.parse;
import main.rice.node.*;
import org.json.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

// TODO: implement the ConfigFileParser class here

public class ConfigFileParser {

    /**
     * Reads the content of a file and returns it as a string.
     *
     * @param filepath The path to the file to be read.
     * @return The contents of the file as a string.
     * @throws IOException If an I/O error occurs.
     */
    public String readFile(String filepath) throws IOException {
        // Create a StringBuilder to store the content of the file.
        StringBuilder sb = new StringBuilder();

        // Open a BufferedReader to efficiently read the file.
        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;

            // Read each line from the file until the end is reached.
            while ((line = reader.readLine()) != null) {
                // Append the line to the StringBuilder with a newline character.
                sb.append(line).append("\n");
            }
        }

        // Convert the StringBuilder content to a string.
        String contents = sb.toString();

        // Close the BufferedReader to release resources.
        return contents;
    }

    /**
     * Parses the contents of a configuration file and creates a ConfigFile instance.
     *
     * @param contents The contents of the configuration file as a string in JSON format.
     * @return A ConfigFile instance representing the parsed configuration.
     * @throws InvalidConfigException If the configuration is invalid or missing required keys.
     */
    public static ConfigFile parse(String contents) throws InvalidConfigException {
        // JSONObject is from an external library, assumed to be available in the context.
        JSONObject jsonObject;

        // Attempt to parse the contents as a JSON object.
        try {
            jsonObject = new JSONObject(contents);
        } catch (JSONException e) {
            // If parsing fails, throw an exception indicating that the file is not in JSON format.
            throw new InvalidConfigException("File not in JSON Format");
        }

        // Extract the "num random" value from the JSON object.
        int numRandom = parseNumRandom(jsonObject);

        String fname;
        JSONArray types, exDomain, ranDomain;

        // Attempt to extract required keys from the JSON object.
        try {
            fname = jsonObject.getString("fname");
            types = jsonObject.getJSONArray("types");
            ranDomain = jsonObject.getJSONArray("random domain");
            exDomain = jsonObject.getJSONArray("exhaustive domain");
        } catch (JSONException e) {
            // If any required key is missing, throw an exception indicating the missing keys.
            throw new InvalidConfigException("Does not contain all valid keys");
        }

        // Parse types, exhaustive domains, and random domains to create a list of APyNode instances.
        List<APyNode<?>> typesList = parseTypeDomains(types, exDomain, ranDomain);

        // Create and return a ConfigFile instance with the parsed values.
        return new ConfigFile(fname, typesList, numRandom);
    }


    /**
     * Parses the "num random" field from a JSON object.
     *
     * @param jsonObject The JSON object to parse.
     * @return The parsed integer value of "num random."
     * @throws InvalidConfigException If the "num random" field is missing, not an integer,
     *                                or the value is negative.
     */
    private static int parseNumRandom(JSONObject jsonObject) throws InvalidConfigException {
        if (jsonObject.has("num random") && jsonObject.opt("num random") instanceof Integer) {
            int numRandom = jsonObject.getInt("num random");
            if (numRandom >= 0) {
                return numRandom;
            } else {
                throw new InvalidConfigException("Invalid value for num random");
            }
        } else {
            throw new InvalidConfigException("Invalid type/value for num random");
        }
    }

    /**
     * Parses type domains from three JSON arrays: types, exDomain, and ranDomain.
     *
     * @param types      The JSON array containing types.
     * @param exDomain   The JSON array containing expected domains.
     * @param ranDomain  The JSON array containing random domains.
     * @return A list of APyNode instances with parsed types and associated domains.
     * @throws InvalidConfigException If the number of types, expected domains, and random domains do not match,
     *                                or if there are issues with type parsing or domain parsing.
     */
    private static List<APyNode<?>> parseTypeDomains(JSONArray types, JSONArray exDomain, JSONArray ranDomain)
            throws InvalidConfigException {
        if (types.length() != exDomain.length() || types.length() != ranDomain.length()) {
            throw new InvalidConfigException("Number of types does not match number of domains");
        }

        List<APyNode<?>> typesList = new ArrayList<>();

        for (int i = 0; i < types.length(); i++) {
            try {
                // Parse type from the types array.
                String currType = types.getString(i);
                APyNode<?> parsedType = parseTypes(currType);

                // Handle invalid type or type not found.
                if (parsedType == null) {
                    throw new InvalidConfigException("Invalid type and/or type not found");
                }

                // Parse expected and random domains and set them in the parsedType.
                List<? extends Number> expectedDomain = new ArrayList<>(parseDomain(parsedType, exDomain.getString(i), "expected"));
                List<? extends Number> randomDomain = new ArrayList<>(parseDomain(parsedType, ranDomain.getString(i), "random"));

                parsedType.setExDomain(expectedDomain);
                parsedType.setRanDomain(randomDomain);

                typesList.add(parsedType);
            } catch (JSONException e) {
                throw new InvalidConfigException("Invalid type in JSONArray parsing");
            }
        }

        return typesList;
    }



    public static APyNode<?> parseTypes(String JSONObj) {
        String strippedVal = formatInput(JSONObj);

        // Checking if the type is a primitive
        APyNode<?> node = simpleTypes(strippedVal);
        if (node != null) {
            return node;
        }

        // Checking for iterable types
        else if (strippedVal.contains("(")) {
            String[] splitTypes = strippedVal.split("\\(", 2);
            splitTypes[0] = splitTypes[0].trim();
            splitTypes[1] = splitTypes[1].trim();

            // Parsing inner and iterable types
            APyNode<?> innerNode = parseTypes(splitTypes[1]);
            APyNode<?> iterableType = iterableTypes(splitTypes[0], innerNode);

            // Validating both inner and iterable types
            if (innerNode != null && iterableType != null) {
                return iterableType;
            }

            // Handling string type
            else if (splitTypes[0].equals("str")) {
                return createStringNode(splitTypes[1]);
            }

            // Handling dictionaries
            else if (splitTypes[0].equals("dict") && splitTypes[1].contains(":")) {
                return createDictionaryNode(splitTypes[1]);
            }
        }

        return null;
    }

    /**
     * Creates a PyStringNode from the given string type.
     *
     * @param strType The string type to create a PyStringNode from.
     * @return A PyStringNode representing the given string type.
     */
    private static APyNode<?> createStringNode(String strType) {
        // Convert the string type to a char array and create a set of characters as the domain.
        char[] charList = strType.toCharArray();
        Set<Character> charDomain = new HashSet<>();
        for (char c : charList) {
            charDomain.add(c);
        }

        // Return a new PyStringNode with the created character domain.
        return new PyStringNode(charDomain);
    }

    /**
     * Creates a PyDictNode from the given dictionary type.
     *
     * @param dictType The dictionary type in the format "keyType:valueType".
     * @return A PyDictNode with the specified key and value types.
     */
    private static APyNode<?> createDictionaryNode(String dictType) {
        // Split the dictionary type into key and value types.
        String[] dictTypes = dictType.split(":", 2);

        // Parse the key and value types using the parseTypes method.
        APyNode<?> keyType = parseTypes(dictTypes[0]);
        APyNode<?> valType = parseTypes(dictTypes[1]);

        // Check if both keyType and valType are successfully parsed.
        if (keyType != null && valType != null) {
            // Return a new PyDictNode with the parsed key and value types.
            return new PyDictNode<>(keyType, valType);
        }

        // Return null if there are issues with parsing keyType or valType.
        return null;
    }


    /**
     * Creates an iterable node based on the given iterable type and inner node.
     *
     * @param iterableType The iterable type ("list", "tuple", or "set").
     * @param innerNode    The inner node representing the type of elements within the iterable.
     * @return An iterable node of the specified type with the provided inner node.
     */
    private static APyNode<?> iterableTypes(String iterableType, APyNode<?> innerNode) {
        return switch (iterableType) {
            case "list" -> new PyListNode<>(innerNode);
            case "tuple" -> new PyTupleNode<>(innerNode);
            case "set" -> new PySetNode<>(innerNode);
            default -> null;
        };
    }

    /**
     * Creates a simple node based on the given type ("int", "float", or "bool").
     *
     * @param type The simple type for which to create a node.
     * @return A simple node of the specified type.
     */
    private static APyNode<?> simpleTypes(String type) {
        return switch (type) {
            case "int" -> new PyIntNode();
            case "float" -> new PyFloatNode();
            case "bool" -> new PyBoolNode();
            default -> null;
        };
    }

    /**
     * Parses the domain of a given APyNode based on the provided raw domain string.
     *
     * @param node       The APyNode for which the domain is being parsed.
     * @param rawDomain  The raw domain string to be parsed.
     * @param type       The type of domain being parsed ("expected" or "random").
     * @return A set representing the parsed domain, which may be a set of numbers or strings.
     * @throws InvalidConfigException If there is an issue with the domain parsing or the provided raw domain is invalid.
     */
    private static Set<? extends Number> parseDomain(APyNode<?> node, String rawDomain, String type) throws InvalidConfigException {
        // Clean the raw domain string to remove unnecessary characters.
        String cleanDomain = formatInput(rawDomain);

        // Check the type of the node and parse the domain accordingly.
        if (node instanceof PyFloatNode || node instanceof PyIntNode || node instanceof PyBoolNode) {
            return parseSimpleDomain(node, cleanDomain);
        } else if (node instanceof PyStringNode) {
            // Parse domain for PyStringNode (strings).
            Set<Integer> exDomain = new HashSet<>();
            if (cleanDomain.contains("~")) {
                expandDomain(exDomain, cleanDomain);
            } else {
                parseIterableDomain(exDomain, cleanDomain);
            }
            return exDomain;
        } else if (node instanceof AIterablePyNode<?, ?>) {
            // Parse domain for simple iterable types.
            Set<Integer> exDomain = new HashSet<>();
            if (cleanDomain.contains("(")) {
                String[] splitDomain = cleanDomain.split("\\(", 2);
                String iterableDomain = splitDomain[0];
                String innerDomain = splitDomain[1];
                if (type.equals("expected"))
                    node.getLeftChild().setExDomain(new ArrayList<>(parseDomain(node.getLeftChild(), innerDomain, type)));
                else {
                    node.getLeftChild().setRanDomain(new ArrayList<>(parseDomain(node.getLeftChild(), innerDomain, type)));
                }
                parseIterableDomain(exDomain, iterableDomain);
                return exDomain;
            }
        } else if (node instanceof PyDictNode<?, ?>) {
            // Parse domain for PyDictNode (dictionaries).
            Set<Integer> exDomain = new HashSet<>();
            if (cleanDomain.contains("(")) {
                String[] splitDict = cleanDomain.split("\\(", 2);
                if (cleanDomain.contains(":")) {
                    String[] splitDomain = splitDict[1].split(":", 2);
                    String keyDomain = splitDomain[0];
                    String valDomain = splitDomain[1];
                    if (type.equals("expected")) {
                        node.getLeftChild().setExDomain(new ArrayList<>(parseDomain(node.getLeftChild(), keyDomain, type)));
                        node.getRightChild().setExDomain(new ArrayList<>(parseDomain(node.getRightChild(), valDomain, type)));
                    } else {
                        node.getLeftChild().setRanDomain(new ArrayList<>(parseDomain(node.getLeftChild(), keyDomain, type)));
                        node.getRightChild().setRanDomain(new ArrayList<>(parseDomain(node.getRightChild(), valDomain, type)));
                    }
                    parseIterableDomain(exDomain, splitDict[0]);
                    return exDomain;
                }
            }
        }
        // Throw an exception if no valid domain is found.
        throw new InvalidConfigException("No valid domain found");
    }


    /**
     * Parses the domain of a given APyNode with a simple type (int, float, or bool) based on the provided raw domain string.
     *
     * @param node   The APyNode for which the domain is being parsed.
     * @param domain The raw domain string to be parsed.
     * @return A set representing the parsed domain of integers, floats, or booleans.
     * @throws InvalidConfigException If there is an issue with the domain parsing or the provided raw domain is invalid.
     */
    private static Set<? extends Number> parseSimpleDomain(APyNode<?> node, String domain) throws InvalidConfigException {
        // Clean the raw domain string to remove unnecessary characters.
        domain = formatInput(domain);

        // Check the type of the node and parse the domain accordingly.
        if (node instanceof PyIntNode) {
            return parseIntegerDomain(domain);
        } else if (node instanceof PyFloatNode) {
            return parseFloatDomain(domain);
        } else if (node instanceof PyBoolNode) {
            return parseBooleanDomain(domain);
        }

        // Throw an exception if no valid domain is found.
        throw new InvalidConfigException("No valid domain found");
    }

    /**
     * Parses the domain of integers based on the provided raw domain string.
     *
     * @param domain The raw domain string to be parsed for integers.
     * @return A set representing the parsed domain of integers.
     * @throws InvalidConfigException If there is an issue with the domain parsing or the provided raw domain is invalid.
     */
    private static Set<Integer> parseIntegerDomain(String domain) throws InvalidConfigException {
        // Create a set to store the parsed integer values.
        Set<Integer> parsedDomain = new HashSet<>();

        // Check if the domain contains the "~" symbol for expansion or if it's a list of discrete values.
        if (domain.contains("~")) {
            expandDomain(parsedDomain, domain);
        } else {
            parseAndAddIntegerValues(parsedDomain, domain);
        }

        return parsedDomain;
    }


    /**
     * Parses and adds integer values to the provided set from the given raw domain string.
     *
     * @param parsedDomain The set to which the parsed integer values will be added.
     * @param domain       The raw domain string containing integer values.
     * @throws InvalidConfigException If there is an issue with parsing the domain or a non-integer value is found.
     */
    private static void parseAndAddIntegerValues(Set<Integer> parsedDomain, String domain) throws InvalidConfigException {
        // Split the domain into individual elements.
        String[] elements = domain.split(", ");
        try {
            // Iterate through each element and add its parsed integer value to the set.
            for (String element : elements) {
                parsedDomain.add(Integer.parseInt(element));
            }
        } catch (NumberFormatException e) {
            // Throw an exception if a non-integer value is found when an integer value is expected.
            throw new InvalidConfigException("Non-integer value found when integer value expected");
        }
    }

    /**
     * Parses the domain of double values based on the provided raw domain string.
     *
     * @param domain The raw domain string to be parsed for double values.
     * @return A set representing the parsed domain of double values.
     * @throws InvalidConfigException If there is an issue with parsing the domain or invalid bounds are specified.
     */
    private static Set<Double> parseFloatDomain(String domain) throws InvalidConfigException {
        // Create a set to store the parsed double values.
        Set<Double> parsedDomain = new HashSet<>();

        // Check if the domain contains the "~" symbol for range or if it's a list of discrete values.
        if (domain.contains("~")) {
            parseAndAddDoubleValuesInRange(parsedDomain, domain);
        } else {
            parseAndAddDoubleValues(parsedDomain, domain);
        }

        return parsedDomain;
    }

    /**
     * Parses and adds double values within a specified range to the provided set from the given raw domain string.
     *
     * @param parsedDomain The set to which the parsed double values will be added.
     * @param domain       The raw domain string specifying the range of double values.
     * @throws InvalidConfigException If there is an issue with parsing the domain or invalid bounds are specified.
     */
    private static void parseAndAddDoubleValuesInRange(Set<Double> parsedDomain, String domain) throws InvalidConfigException {
        // Split the domain into lower and upper bounds.
        String[] bounds = domain.split("~");
        try {
            // Parse the lower and upper bounds.
            int start = Integer.parseInt(bounds[0]);
            int end = Integer.parseInt(bounds[1]);

            // Check if the bounds are valid and add the values to the set.
            if (start < end) {
                for (int domainVal = start; domainVal <= end; domainVal++) {
                    parsedDomain.add(Double.valueOf(domainVal));
                }
            } else {
                throw new InvalidConfigException("Invalid bounds in domain");
            }
        } catch (NumberFormatException e) {
            // Throw an exception if there is an issue with parsing the bounds.
            throw new InvalidConfigException("Invalid bounds in domain");
        }
    }

    /**
     * Parses and adds double values to the provided set from the given raw domain string.
     *
     * @param parsedDomain The set to which the parsed double values will be added.
     * @param domain       The raw domain string containing double values.
     * @throws InvalidConfigException If there is an issue with parsing the domain or an invalid float value is found.
     */
    private static void parseAndAddDoubleValues(Set<Double> parsedDomain, String domain) throws InvalidConfigException {
        // Split the domain into individual elements.
        String[] elements = domain.split(", ");
        try {
            // Iterate through each element and add its parsed double value to the set.
            for (String element : elements) {
                parsedDomain.add(Double.parseDouble(element));
            }
        } catch (NumberFormatException e) {
            // Throw an exception if an invalid float value is found.
            throw new InvalidConfigException("Invalid value in float domain");
        }
    }

    /**
     * Parses the domain of boolean values based on the provided raw domain string.
     *
     * @param domain The raw domain string to be parsed for boolean values.
     * @return A set representing the parsed domain of boolean values.
     * @throws InvalidConfigException If there is an issue with parsing the domain or an invalid input is found in the bool domain.
     */
    private static Set<Integer> parseBooleanDomain(String domain) throws InvalidConfigException {
        // Create a set to store the parsed boolean values.
        Set<Integer> parsedDomain = new HashSet<>();

        // Check if the domain contains the "~" symbol for range and matches the pattern "[01]~[01]".
        if (domain.contains("~") && domain.matches("[01]~[01]")) {
            expandDomain(parsedDomain, domain);
        } else {
            parseAndAddBooleanValues(parsedDomain, domain);
        }

        return parsedDomain;
    }

    /**
     * Parses and adds boolean values to the provided set from the given raw domain string.
     *
     * @param parsedDomain The set to which the parsed boolean values will be added.
     * @param domain       The raw domain string containing boolean values.
     * @throws InvalidConfigException If there is an issue with parsing the domain or an invalid input is found in the bool domain.
     */
    private static void parseAndAddBooleanValues(Set<Integer> parsedDomain, String domain) throws InvalidConfigException {
        // Split the domain into individual elements.
        String[] elements = domain.split(", ");
        try {
            // Iterate through each element and add its parsed boolean value to the set.
            for (String element : elements) {
                int domainVal = Integer.parseInt(element);
                if (domainVal == 0 || domainVal == 1) {
                    parsedDomain.add(domainVal);
                } else {
                    // Throw an exception if an invalid input is found in the bool domain.
                    throw new InvalidConfigException("Invalid input in bool domain");
                }
            }
        } catch (NumberFormatException e) {
            // Throw an exception if a non-integer value is found when an integer value is expected.
            throw new InvalidConfigException("Non-integer value found when integer value expected");
        }
    }

    /**
     * Formats the raw input string by removing square brackets and quotes, and trimming whitespace.
     *
     * @param rawString The raw input string to be formatted.
     * @return The formatted string.
     */
    private static String formatInput(String rawString) {
        return rawString.replaceAll("\"?\\[\"?", "").replaceAll("\"?]\"?", "").trim();
    }


    /**
     * Expands the provided domain set with integer values within the specified interval.
     *
     * @param domain   The set to which the expanded integer values will be added.
     * @param interval The raw interval string in the format "start~stop".
     * @throws InvalidConfigException If there is an issue with parsing the interval or if invalid bounds are specified.
     */
    private static void expandDomain(Set<Integer> domain, String interval) throws InvalidConfigException {
        // Split the interval into lower and upper bounds.
        String[] bounds = interval.split("~");
        int start, stop;

        try {
            // Parse the lower and upper bounds.
            start = Integer.parseInt(bounds[0].trim());
            stop = Integer.parseInt(bounds[1].trim());
        } catch (NumberFormatException e) {
            // Throw an exception if there is an issue with parsing the bounds.
            throw new InvalidConfigException("Invalid bounds for interval expansion");
        }

        // Check if the bounds are valid and add the values to the set.
        if (start <= stop) {
            for (int domainIdx = start; domainIdx <= stop; domainIdx++) {
                domain.add(domainIdx);
            }
        } else {
            // Throw an exception if negative integers are found in the iterable domain.
            throw new InvalidConfigException("Negative integer in iterable domain");
        }
    }

    /**
     * Parses the iterable domain and adds integer values to the provided set.
     *
     * @param exDomain        The set to which the parsed integer values will be added.
     * @param iterableDomain  The raw iterable domain string, either a range or a list of values.
     * @throws InvalidConfigException If there is an issue with parsing the iterable domain or if invalid bounds are specified.
     */
    private static void parseIterableDomain(Set<Integer> exDomain, String iterableDomain) throws InvalidConfigException {
        // Check if the iterable domain contains the "~" symbol for range or if it's a list of discrete values.
        if (iterableDomain.contains("~")) {
            parseAndExpandRangeDomain(exDomain, iterableDomain);
        } else {
            parseAndAddListDomain(exDomain, iterableDomain);
        }
    }


    /**
     * Parses and expands the range domain, adding integer values to the provided set.
     *
     * @param exDomain       The set to which the parsed and expanded integer values will be added.
     * @param iterableDomain The raw iterable domain string in the format "start~stop".
     * @throws InvalidConfigException If there is an issue with parsing the iterable domain or if invalid bounds are specified.
     */
    private static void parseAndExpandRangeDomain(Set<Integer> exDomain, String iterableDomain) throws InvalidConfigException {
        // Split the iterable domain into lower and upper bounds.
        String[] bounds = iterableDomain.split("~");

        try {
            // Parse the lower and upper bounds.
            int start = Integer.parseInt(bounds[0].trim());

            // Check if the start value is non-negative and expand the domain.
            if (start >= 0) {
                expandDomain(exDomain, iterableDomain);
            } else {
                // Throw an exception if a negative integer is found in the iterable domain.
                throw new InvalidConfigException("Negative integer in iterable domain");
            }
        } catch (NumberFormatException e) {
            // Throw an exception if there is an issue with parsing the bounds.
            throw new InvalidConfigException("Invalid bounds in iterable domain");
        }
    }

    /**
     * Parses and adds integer values from a list domain to the provided set.
     *
     * @param exDomain       The set to which the parsed integer values will be added.
     * @param iterableDomain The raw iterable domain string containing a list of values.
     * @throws InvalidConfigException If there is an issue with parsing the iterable domain or if invalid bounds are specified.
     */
    private static void parseAndAddListDomain(Set<Integer> exDomain, String iterableDomain) throws InvalidConfigException {
        // Split the iterable domain into individual elements.
        String[] elements = iterableDomain.split(",");

        try {
            // Iterate through each element and add its parsed integer value to the set.
            for (String element : elements) {
                int num = Integer.parseInt(element.trim());

                // Check if the parsed integer value is non-negative.
                if (num >= 0) {
                    exDomain.add(num);
                } else {
                    // Throw an exception if a negative integer is found in the iterable domain.
                    throw new InvalidConfigException("Negative integer in iterable domain");
                }
            }
        } catch (NumberFormatException e) {
            // Throw an exception if there is an issue with parsing the number format.
            throw new InvalidConfigException("Invalid number format in iterable domain");
        }
    }


}