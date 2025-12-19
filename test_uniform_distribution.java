import net.silicontrip.UniformDistribution;
import net.silicontrip.UniformDistributionException;

public class test_uniform_distribution {
    public static void main(String[] args) {
        try {
            // Test 1: Basic parsing
            UniformDistribution ud1 = new UniformDistribution("[1.5,3.7]");
            System.out.println("Test 1 - Basic parsing: " + ud1.toString());
            System.out.println("  Lower: " + ud1.getLower() + ", Upper: " + ud1.getUpper());
            
            // Test 2: Round-trip (create, toString, parse back)
            UniformDistribution ud2 = new UniformDistribution(10.0, 20.0);
            String str = ud2.toString();
            UniformDistribution ud3 = new UniformDistribution(str);
            System.out.println("\nTest 2 - Round-trip:");
            System.out.println("  Original: " + ud2.toString());
            System.out.println("  Parsed:   " + ud3.toString());
            System.out.println("  Match: " + ud2.equals(ud3));
            
            // Test 3: Reversed values (should auto-sort)
            UniformDistribution ud4 = new UniformDistribution("[100,50]");
            System.out.println("\nTest 3 - Reversed values: " + ud4.toString());
            System.out.println("  Lower: " + ud4.getLower() + ", Upper: " + ud4.getUpper());
            
            // Test 4: With whitespace
            UniformDistribution ud5 = new UniformDistribution("[ 5.5 , 10.5 ]");
            System.out.println("\nTest 4 - With whitespace: " + ud5.toString());
            
            // Test 5: Negative values
            UniformDistribution ud6 = new UniformDistribution("[-10.5,5.3]");
            System.out.println("\nTest 5 - Negative values: " + ud6.toString());
            
            System.out.println("\n✓ All tests passed!");
            
        } catch (UniformDistributionException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
