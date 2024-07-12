package main.rice.obj;

import java.util.Collection;

/**
 * An abstract class whose instances represent specific Python objects.
 * @param <JavaType> the internal Java representation
 */
public abstract class APyObj<JavaType> {

    /**
     * The internal representation of this object.
     */
    protected JavaType value;

    /**
     * Returns the Java object that is the internal representation of this Python object.
     *
     * @return the Java object that is the internal representation of this Python object
     */
    public JavaType getValue() {
        return this.value;
    }

    /**
     * Compares this to the input object by value.
     *
     * @param obj the object to compare against
     * @return true if this is equivalent by value to obj; false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        // If obj is the wrong type, it's clearly not equivalent
        if (!(obj instanceof APyObj other)) {
            return false;
        }

        // Compare by value
        return this.value.equals(other.getValue());
    }

    /**
     * Computes a hash code based on this object's value, such that two objects that are
     * considered equal by .equals() will also have the same hash code.
     *
     * @return the hash code for this object
     */
    @Override
    public int hashCode() {
        return this.value.hashCode();
    }
}