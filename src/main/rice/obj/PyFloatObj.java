package main.rice.obj;

/**
 * A representation of Python objects of type float.
 */
public class PyFloatObj extends APyObj<Double> {

    /**
     * Constructor for a PyFloatObj; initializes its value to the input.
     *
     * @param value the value of this PyFloatObj
     */
    public PyFloatObj(Double value) {
        this.value = value;
    }

    /**
     * Builds and returns a string representation of this object that mirrors the Python
     * string representation.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return this.value.toString();
    }
}