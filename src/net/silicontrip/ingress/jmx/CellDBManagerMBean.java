package net.silicontrip.ingress.jmx;

public interface CellDBManagerMBean {

    // Method without parameters
    public String refineCells(); // celldbtool - refine

    // Method with a parameter
    public String traceCell(String cellIdToken); // celldbtool - trace

    public String eraseCells(); // celldbtool - erase

    // this builds the cell model from scratch, once complete run refine, then run
    // refine every few days
    public String rebuildCells(); // celldbtool - build

    // this builds the fieldCells many to many table. Joining the Field table (on
    // guid) to the Cell table (on id)
    public String rebuildFieldCells(); // celldbtool - rebuild

    public String invalidateField(String fieldGuid);

    public String exportBestFields();

    public String exportAllFields();

    // Background task methods for long-running refinement
    public String startRefineCellsBackground();

    public String refineCellsStatus();

    public String cancelRefineCells();

    // Background task methods for long-running build cells
    public String startRebuildCellsBackground();

    public String rebuildCellsStatus();

    public String cancelRebuildCells();


    // public String eraseFields();
    // public String importFieldsMerge();
    // public String importFieldsReplace();
    // public String revalidateAllFields();

    // celldbtool commands not implemented

    // prune - invalid logic, better to export best fields and then import and
    // replace

    // dedupe - caused by incorrect import and merge, should not be required.

    // parent - problems similar to poison fields.
    // it is difficult to correctly determine invalid parent cells, this is causing
    // more trouble than it's worth,
    // but it is indicative that there is an invalid state between the child cells
    // and their parents, which should
    // not happen if the model is correct.

    // testagree - trialing a different build method, moved back into the build
    // function

    // newagree - another trial build method

    // process or query - processes a single field, shouldn't query be similar to
    // trace, I've forgotten my own code.

    // exportlevel13cells - for the javascript tool

    // shared - attempts to find matching split fields. My initial thought was that
    // poison fields were caused by incorrectly reported split
    // fields, but this is not the case, occasionally the field mu gatherer reports
    // an incorrect mu value, suck it up sunshine.

    // Add any other methods you want to expose here...
    // public int getCachedCellCount();
    // public void clearCache();
}
