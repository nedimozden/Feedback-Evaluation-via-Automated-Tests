package main.rice.obj;

/**
 * A representation of Python objects of type bool.
 */
public class PyBoolObj extends APyObj<Boolean> {

    /**
     * Constructor for a PyBoolObj; initializes its value to the input.
     *
     * @param value the value of this PyBoolObj
     */
    public PyBoolObj(Boolean value) {
        this.value = value;
    }

    /**
     * Builds and returns a string representation of this object that mirrors the Python
     * string representation (i.e., True or False).
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        if (!this.value) {
            return "False";
        }
        return "True";
    }
}