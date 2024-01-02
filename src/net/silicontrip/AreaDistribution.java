package net.silicontrip;

// this is one of those Java low point classes.
public class AreaDistribution {
	// making these public cbf writing get/set
	public UniformDistribution mu;
	public Double area;

	@Override
	public String toString() { 
		if (mu!=null)
			return "" + mu.toString() +": " +area; 
		else
			return "[,]: " +area;
	}
}
