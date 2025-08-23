package net.silicontrip.ingress.jmx;

public interface CellDBManagerMBean {

    // Method without parameters
    public String refineCells();

    // Method with a parameter
    public String traceCell(String cellIdToken);

    // Add any other methods you want to expose here...
    // public int getCachedCellCount();
    // public void clearCache();
}
