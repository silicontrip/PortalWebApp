package net.silicontrip.ingress;

/**
 * MUFieldDAOException is an exception that extends the standard
 * RunTimeException Exception. This is thrown by the DAOs of the catalog
 * component when there is some irrecoverable error (like SQLException)
 */

public class MUFieldDAOException extends RuntimeException {

    /**
     * Constructor
     * @param str    a string that explains what the exception condition is
     */
    public MUFieldDAOException (String str) {
        super(str);
    } 

    /**
     * Default constructor. Takes no arguments
     */
    public MUFieldDAOException () {
        super();
    }
}

