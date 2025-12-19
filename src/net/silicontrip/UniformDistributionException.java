package net.silicontrip;

/**
 * EntityDAOException is an exception that extends the standard
 * RunTimeException Exception. This is thrown by the DAOs of the catalog
 * component when there is some irrecoverable error (like SQLException)
 */

public class UniformDistributionException extends Exception {

    /**
     * Constructor
     * @param str    a string that explains what the exception condition is
     */
    public UniformDistributionException (String str) {
        super(str);
    } 

    /**
     * Constructor with cause
     * @param str    a string that explains what the exception condition is
     * @param cause  the underlying cause of this exception
     */
    public UniformDistributionException (String str, Throwable cause) {
        super(str, cause);
    }

    /**
     * Default constructor. Takes no arguments
     */
    public UniformDistributionException () {
        super();
    }
}

