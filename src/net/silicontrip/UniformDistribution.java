package net.silicontrip;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * A uniform distribution statistics class.
 *
 */
public class UniformDistribution implements Serializable {

/**
 * the lower bounds of the distribution
 */
	private double lower;
	/**
	 * the upper bounds of the distribution
	 */
	private double upper;
	
	transient private double epsilon=1E-5;
/**
 * Constructor.
 *
 * @param a the mid point of the distribution.  Assumes a default range of +/- 0.5
 */
	public UniformDistribution (int a) { this (a,0.5); }
	/**
 * Constructor.
 *
 * @param a the mid point of the distribution.
 * @param range the value of the +/- from the midpoint.
 */
	public UniformDistribution (int a, double range) { this(a-range, a + range);}
	/**
	 * Constructor.
	 *
	 * This accepts arguments in any order and will sort upper and lower
	 *
	 * @param a a double value.
	 * @param b a double value.
	 */
	public UniformDistribution (double a, double b) { 
		if (a<=b) {
			lower = a; 
			upper = b; 
		} else {
			lower = b; 
			upper = a; 
		} 
	}
	/**
	 * Constructor.
	 *
	 * Accepts 4 values, picks the lowest and highest.
	 *
	 * @param a1 a double value
	 * @param a2 a double value
	 * @param b1 a double value
	 * @param b2 double value
	 */
	public UniformDistribution ( double a1, double a2, double b1, double b2) { 
		//System.out.println("NEW UD: " + a1 +","+a2+","+b1+","+b2);
		if (a1 <= a2 && a1 <= b1 && a1 <= b2) 
			lower = a1; 
		if (a2 <= a1 && a2 <= b1 && a2 <= b2) 
			lower = a2; 
		if (b1 <= a1 && b1 <= a2 && b1 <= b2) 
			lower = b1; 
		if (b2 <= a1 && b2 <= a2 && b2 <= b1)
			lower = b2; 

		if (a1 >= a2 && a1 >= b1 && a1 >= b2) 
			upper = a1; 
		if (a2 >= a1 && a2 >= b1 && a2 >= b2) 
			upper = a2; 
		if (b1 >= a1 && b1 >= a2 && b1 >= b2) 
			upper = b1; 
		if (b2 >= a1 && b2 >= a2 && b2 >= b1)
			upper = b2; 
	}
	/**
	 * Copy Constructor.
	 *
	 * @param ud A UniformDistribution to copy values from
	 */
	public UniformDistribution(UniformDistribution ud)
	{
		this(ud.getLower(),ud.getUpper());
	}

	/**
	 * Accessor method for the upper bounds.
	 *
	 * @param d A double for the upper bounds
	 */
	public void setUpper(double d) { upper = d; }
	/**
	 * Accessor method for the lower bounds.
	 *
	 * @param d A double for the lower bounds
	 */
	public void setLower(double d) { lower = d; }

	/**
	 * Accessor method for the Upper bounds.
	 *
	 * @return double of the upper bounds.
	 */
	public double getUpper() { return upper; }
		/**
	 * Accessor method for the lower bounds.
	 *
	 * @return double of the lower bounds.
	 */
	public double getLower() { return lower; }

	/**
	 * Rounds the upper bounds to the nearest integer.
	 *
	 * @return double rounded upper bound
	 */
	public double getUpperRound() { return Math.round(upper); }
		/**
	 * Rounds the lower bounds to the nearest integer.
	 *
	 * @return double rounded lower bound
	 */
	public double getLowerRound() { return Math.round(lower); }

	/**
	 * Sets a lowest cut off point for the lower bounds.
	 * if the lower bounds exceeds the cut off point it is set to the cut off
	 * point.
	 *
	 * @param d a double specifying the lower cut off point
	 *
	 * @return boolean if value was clamped
	 */
	public boolean clampLower(double d) { if (lower < d) {lower=d; return true;} return false; }
	// never implemented a clampUpper, maybe one day for completeness
	public boolean clampUpper(double d) { if (upper > d) {upper=d; return true;} return false; }
	// one day seriously? copy... paste... s/lower/upper/i s/</>/ 

	/**
	 * Gets the mean value (that's mean as in average, not horrible)
	 *
	 * @return double the mean value
	 */
	public double mean () { return (upper + lower) / 2.0; }
	/**
	 * Gets the absolute error (or range)
	 *
	 * @return double the absolute error.
	 */
	public double error() { return (upper - lower); }
	/**
	 * gets the error as a value between 0 and 1
	 *
	 * @return double the error
	 */
	public double perror() { return (upper - lower) / (upper + lower); }

	/**
	 * gets the UniformDistribution as an ArrayList of Doubles
	 *
	 * @return ArrayList of the lower and upper values.
	 */
	public ArrayList<Double> getArrayList()
	{
		ArrayList<Double> da = new ArrayList<>();
		da.add(getLower());
		da.add(getUpper());
		return da;
	}

	/**
	 * Adds two UniformDistributions together
	 *
	 * @param a the UniformDistribution to add
	 *
	 * @return UniformDistribution a new UD of the addition result.
	 */
	public UniformDistribution add (UniformDistribution a) { return new UniformDistribution(upper+a.getUpper(), upper+a.getLower(), lower+a.getLower(),lower+a.getUpper()); }
		/**
	 * Multiplies two UniformDistributions together
	 *
	 * @param a the UniformDistribution to multiply
	 *
	 * @return UniformDistribution a new UD of the multiplication result.
	 */
	public UniformDistribution mul (UniformDistribution a) { return new UniformDistribution(upper*a.getUpper(), upper*a.getLower(), lower*a.getLower(),lower*a.getUpper()); }
	/**
	 * Subtracts a UniformDistributions from this
	 *
	 * @param a the UniformDistribution to subtract
	 *
	 * @return UniformDistribution a new UD of the subtraction result.
	 */
	public UniformDistribution sub (UniformDistribution a) { return new UniformDistribution(upper-a.getUpper(), upper-a.getLower(), lower-a.getLower(),lower-a.getUpper()); }
		/**
	 * Divides this by a UniformDistribution
	 *
	 * @param a the UniformDistribution to divide by
	 *
	 * @return UniformDistribution a new UD of the division result.
	 */
	public UniformDistribution div (UniformDistribution a) { return new UniformDistribution(upper/a.getUpper(), upper/a.getLower(), lower/a.getLower(),lower/a.getUpper()); }
	/**
	 * subtracts a scalar from this
	 *
	 * @param a double to subtract
	 *
	 * @return a new UniformDistribution
	 */
	public UniformDistribution sub (double a) { return new UniformDistribution(upper-a, lower-a); }
		/**
	 * divides this by a scalar
	 *
	 * @param a double to divide by
	 *
	 * @return a new UniformDistribution
	 */
	public UniformDistribution div (double a) { return new UniformDistribution(upper/a, lower/a); }
		/**
	 * multiply a scalar with this
	 *
	 * @param a double to multiply
	 *
	 * @return a new UniformDistribution
	 */
	public UniformDistribution mul (double a) { return new UniformDistribution(upper*a, lower*a); }

	/**
	 * makes both upper and lower bounds positive
	 *
	 * @return a positive UniformDistribution
	 */
	public UniformDistribution abs () { 
		if (lower>=0 && upper>=0)
			return new UniformDistribution(lower,upper);
		if (lower<0 && upper <0)
			return new UniformDistribution(-lower,-upper);

		return new UniformDistribution(0,0,-lower,upper);

	}

	/**
	 * Tests if a value is within this, inclusive
	 *
	 * @param n Integer to test
	 *
	 * @return boolean true if the Integer is within the bounds
	 */
	public boolean contains(Integer n) { return lower <= n && n <= upper; }
	/**
	 * Tests if a value is within this, inclusive
	 *
	 * @param n Double to test
	 *
	 * @return boolean true if the Integer is within the bounds
	 */
	public boolean contains(Double n) { return lower <= n && n <= upper; }
		/**
	 * Tests if a UniformDistribution is completely enclosed within this, inclusive
	 *
	 * @param n UniformDistribution to test
	 *
	 * @return boolean true if the UD is within the bounds
	 */
	public boolean contains(UniformDistribution n)
	{
		// we shouldn't have an invalid UD where lower > upper
		return lower <= n.getLower() && n.getUpper() <= upper;
	}

	// anything else is false or uncertain.
			/**
	 * Tests if a UniformDistribution is completely greater than
	 *
	 * @param a UniformDistribution to test
	 *
	 * @return boolean true if the UD is greater
	 */
	public boolean gt (UniformDistribution a) { return lower > a.getUpper(); }
	/**
	 * Tests if a UniformDistribution is completely less than
	 *
	 * @param a UniformDistribution to test
	 *
	 * @return boolean true if the UD is greater
	 */
	public boolean lt (UniformDistribution a) { return upper < a.getLower(); }
	/**
	 * Tests if a UniformDistribution is completely greater than inclusive
	 * <i> greater than or equal doesn't seem to make sense with distributions</i>
	 *
	 * @param a UniformDistribution to test
	 *
	 * @return boolean true if the UD is greater
	 */
	public boolean ge (UniformDistribution a) { return lower >= a.getUpper(); }
	/**
	 * Tests if a UniformDistribution is completely less than inclusive
	 *
	 * @param a UniformDistribution to test
	 *
	 * @return boolean true if the UD is greater
	 */
	public boolean le (UniformDistribution a) { return upper <= a.getLower(); }
		/**
	 * Tests if a UniformDistribution is equal to this
	 *
	 * @param a UniformDistribution to test
	 *
	 * @return boolean true if the UD is equal
	 */
	public boolean equals (UniformDistribution a) { 
		if ( this == a ) return true;
		if ( !(a instanceof UniformDistribution) ) return false;

		return (lower == a.getLower() && upper == a.getUpper()); 
	}

	/**
	 * get the rounded uniformdistribution value
	 *
	 * @return Uniformdistribution rounded.
	 */
	public UniformDistribution round() { return new UniformDistribution(this.getUpperRound(),this.getLowerRound()); }
	
	/**
	 * get the rounded uniformdistribution value
	 * Fields cannot have a value of 0 so are rounded up to 1
	 * This only works for positive UD
	 *
	 * @return Uniformdistribution rounded.
	 */
	public UniformDistribution roundAboveZero()  { 
		double l = getLowerRound(); if (l==0) l=1;
		double u = getUpperRound(); if (u==0) u=1;
	
		return new UniformDistribution(l,u);
	}

	/**
	 * Performs an intersection with another Uniform Distribution and updates this one in place
	 * The idea is that improves the accuracy of the current UD
	 *
	 * @param a the UniformDistribution to refine this one with
	 *
	 * @return boolean indicating if this UD was improved
	 *
	 * @throws ArithmeticException if there was no overlap between the two UD
	 */
	public boolean refine (UniformDistribution a) throws ArithmeticException { 

		// sanity check

		boolean changed = false;
		if ((upper > a.getUpper() || upper > a.getLower()) && ( lower < a.getLower() || lower < a.getUpper())) 
		{

			if (lower < a.getLower())
			{
				double d = a.getLower() - lower;
				//System.err.println("lower diff: " + d + "/" + lower);
				lower=a.getLower();
				if (d>epsilon)
					changed=true;
			}

			if (upper > a.getUpper())
			{
				double d = upper - a.getUpper();
				//System.err.println("upper diff: " + d +  "/" + upper + " : " + lower);
				upper=a.getUpper();
				if (d>epsilon)
					changed=true;
			}
		} else {
			throw new ArithmeticException( "REFINE Sanity check Exception : " + toString() + " x " + a);
		}
		return changed;
	}
	/**
	 * 	return a printable string, it's also a compatible JSON array.
	 *
	 * 	@return String representation of the UD, could be used to reconstruct the UD.
	 */
	@Override
	public String toString () { return "[" + lower + "," + upper + "]"; }
}
