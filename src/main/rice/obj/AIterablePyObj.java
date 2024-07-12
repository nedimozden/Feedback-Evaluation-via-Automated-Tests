package main.rice.obj;

import java.util.Collection;

/**
 * An abstract class representing an iterable Python object (list, tuple, set, or
 * string).
 *
 * @param <ElemType> the type of object contained within this iterable object
 */
public abstract class AIterablePyObj<ElemType extends APyObj<?>> extends APyObj<Collection<ElemType>> {

    // This class exists not for the purpose of sharing functionalities within this class hierarchy,
    // but rather for the purpose of defining an is-a relationship that will group together related
    // classes. This, in turn, will allow us to share functionalities across these related classes
    // when we get to a future assignment.

}