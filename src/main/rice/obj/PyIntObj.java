package main.rice.obj;

/**
 * A representation of Python objects of type int.
 */
public class PyIntObj extends APyObj<Integer> {

    /**
     * Constructor for a PyIntObj; initializes its value to the input.
     *
     * @param value the value of this PyIntObj
     */
    public PyIntObj(Integer value) {
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