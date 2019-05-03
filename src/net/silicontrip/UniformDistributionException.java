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
     * Default constructor. Takes no arguments
     */
    public UniformDistributionException () {
        super();
    }
}

