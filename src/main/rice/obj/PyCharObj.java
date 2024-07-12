package main.rice.obj;

/**
 * A helper class for implementing PyStringObjs; each instance represents a single
 * character in a Python string.
 */
public class PyCharObj extends APyObj<Character> {

    /**
     * Constructor for a PyCharObj; initializes its value to a string of the input.
     *
     * @param value a character representing the value of this PyCharObj
     */
    public PyCharObj(Character value) {
        this.value = value;
    }

    /**
     * Builds and returns a string representation of this object that mirrors the Python
     * string representation; uses single quotes for compatibility with command-line
     * invocation of Python scripts.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "'" + this.value.toString() + "'";
    }
}
