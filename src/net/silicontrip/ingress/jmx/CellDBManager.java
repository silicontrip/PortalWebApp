 // In file: src/net/silicontrip/ingress/jmx/CellDBManager.java
 
package net.silicontrip.ingress.jmx;
 
import com.google.common.geometry.*;

import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.silicontrip.ingress.EntityDAO;
import net.silicontrip.ingress.Field;
import net.silicontrip.ingress.FieldSessionBean;
import net.silicontrip.ingress.SQLBulkDAO;

import static net.silicontrip.S2PolygonUtils.*;
import net.silicontrip.UniformDensityCurve;
import net.silicontrip.UniformDistribution;
import net.silicontrip.UniformDistributionException;


 // Your other necessary imports for database connection, celldbtool logic, etc.
 // import net.silicontrip.celldbtool;
 // import java.sql.Connection;
 
@Singleton
@Startup
public class CellDBManager implements CellDBManagerMBean {
 
	 // You can manage your database connection here.
	 // private Connection conn;
	 // private celldbtool tool;

	private static final Logger LOGGER = Logger.getLogger(CellDBManager.class.getName());

	@EJB
	private EntityDAO edao;

	@EJB
	private SQLBulkDAO bdao;

	private HashMap<String,S2CellUnion> fieldUnion;
	private HashMap<String,HashMap<S2CellId,Double>> fieldIntersections;
	private HashMap<S2CellId,HashSet<String[]>> cellField;
 
	@PostConstruct
	public void register() {
		try {
			// Use your corrected, unique ObjectName
			ObjectName name = new ObjectName("net.silicontrip.ingress:type=CellDBManager");
			ManagementFactory.getPlatformMBeanServer().registerMBean(this, name);

			// Initialize your database connection and celldbtool instance here
			// String url = "jdbc:derby://...";
			// this.tool = new celldbtool(url, "APP", "APP");
			System.out.println("CellDBManager MBean registered successfully.");

		} catch (Exception e) {
			throw new IllegalStateException("Problem during MBean registration", e);
		}
	}
 
	@PreDestroy
	public void unregister() {
		try {
			ObjectName name = new ObjectName("net.silicontrip.ingress:type=CellDBManager");
			ManagementFactory.getPlatformMBeanServer().unregisterMBean(name);

			// Close your database connection here
			// if (this.tool != null) { this.tool.close(); }
			//System.out.println("CellDBManager MBean unregistered successfully.");
			Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO, "CellDBManager MBean unregistered successfully.");

		} catch (Exception e) {
			// Log error, but don't prevent shutdown
			e.printStackTrace();
		}
	}
 
	private static UniformDistribution getEstMu(String[] f, HashMap<S2CellId,UniformDistribution> cellm)
	{
		S2Polygon fpoly = getS2Polygon(f);
		S2CellUnion funion = getCells(fpoly);
		HashMap<S2CellId,Double> fint =  getCellIntersection ( funion, fpoly);

		UniformDistribution mu = new UniformDistribution(0.0,0.0);

		for (S2CellId cid : funion)
		{
			double a = fint.get(cid) * 6367.0 * 6367.0;

			if (cellm.containsKey(cid))
			{
				mu = mu.add(cellm.get(cid).mul(a));
			   // System.out.println("" + cid.toToken() + " " + a + " x " + cellm.get(cid) + " = " + mu);

			} else {
			   // System.out.println("" + cid.toToken() + " " + a  + " x UNKNOWN");
				mu.setLower(0.0);
				mu.setUpper(Float.MAX_VALUE);
				break;
			}
		}
		return mu;
	}

	private static UniformDistribution remaining (S2CellUnion cells, S2CellId analyseId, HashMap<S2CellId,UniformDistribution> cellm, HashMap<S2CellId,Double>intersections, UniformDistribution muOuterError)
	{
		for (S2CellId cellOuter: cells)
		{
			if (!cellOuter.equals(analyseId))
			{
				if (cellm.containsKey(cellOuter))
				{
					UniformDistribution cellOuterMU = cellm.get(cellOuter);
					double areaOuter = intersections.get(cellOuter) * 6367.0 * 6367.0;

					UniformDistribution cma = cellOuterMU.mul(areaOuter);
					muOuterError = muOuterError.sub(cma);
				} else {
					// this is the short way of saying muOuterError - (cellOuterMu = [0,+infinity] * areaOuter)
					muOuterError.setLower(0.0);
				}
			}
		}
		return muOuterError;
}

	private static String dtPolygon (String[] flist,String colour)
	{
		int lat1 = Integer.valueOf(flist[7].trim());
		int lng1 = Integer.valueOf(flist[8].trim());
		int lat2 = Integer.valueOf(flist[10].trim());
		int lng2 = Integer.valueOf(flist[11].trim());
		int lat3 = Integer.valueOf(flist[13].trim());
		int lng3 = Integer.valueOf(flist[14].trim());
		return "{\"type\":\"polygon\",\"latLngs\":[{\"lat\":" + lat1/1000000.0 +
					",\"lng\":" + lng1/1000000.0 +
					"},{\"lat\":" + lat2/1000000.0 +
					",\"lng\":" + lng2/1000000.0 +
					"},{\"lat\":" + lat3/1000000.0 +
					",\"lng\":" + lng3/1000000.0 +
					"}],\"color\":\"" + colour + "\"}";
	}

	private static UniformDistribution makeDist(String mus)
	{
		int mu = Integer.parseInt(mus.trim());

		if (mu == 1)
			return new UniformDistribution(0,1.5);
		return new UniformDistribution(mu,0.5);
	}

	private void populate (ArrayList<String[]> flist)
	{
		fieldUnion = new HashMap<String,S2CellUnion>();
		fieldIntersections = new HashMap<String,HashMap<S2CellId,Double>>();
		cellField = new HashMap<S2CellId,HashSet<String[]>>();

		int n = flist.size();
		// System.err.println("prepopulating: " + n);

		for (int io =0; io < n; io++)
		{
			String[] outerField = flist.get(io);
			String guidOuter = outerField[3].trim();

			S2Polygon s2polyOuter = getS2Polygon(outerField);
			S2CellUnion cellsOuter = getCells(s2polyOuter);
			HashMap<S2CellId,Double> intersectionsOuter = getCellIntersection(cellsOuter,s2polyOuter);
			
			fieldUnion.put(guidOuter,cellsOuter);
			fieldIntersections.put(guidOuter,intersectionsOuter);

			for (S2CellId analyseId : cellsOuter)
			{
				if (!cellField.containsKey(analyseId))
					cellField.put(analyseId,new HashSet<String[]>());
				cellField.get(analyseId).add(outerField);
			}
		}
	}
	
	private static ArrayList<String[]> buildFieldCells(ArrayList<String[]> flist)
	{
		ArrayList<String[]> guidIds = new ArrayList<String[]>();

		for (String[] outerField: flist)
		{
			S2Polygon s2polyOuter = getS2Polygon(outerField);
			S2CellUnion cellsOuter = getCells(s2polyOuter);
			String guidOuter = outerField[3].trim();
			for (S2CellId cid: cellsOuter)
			{
				String[] guidId = new String[2];
				guidId[0] = guidOuter;
				guidId[1] = "" + (cid.id() >> 32);
				
				guidIds.add(guidId);
			}
		}
		return guidIds;
	}

	private static String printTable(Iterable<String[]> fieldList)
	{
		StringBuilder sb = new StringBuilder();
		int count = 0;
		for (String[] r : fieldList)
		{
			boolean first = true;

			for (String s : r)
			{
				if (!first)
						sb.append(" |");
				first = false;
				sb.append(s);
			}
			sb.append("\n");
			count ++;
		}
		return sb.toString();
	}


	private Iterable<String[]> best (ArrayList<String[]> flist, HashMap<S2CellId,UniformDistribution> cellm)
	{
		populate(flist);
		int ksn = cellField.size();
		HashMap<String,String[]> bestFields = new HashMap<>();
		ArrayList<S2CellId> keys = new ArrayList<S2CellId>(cellField.keySet());

		for (int iii = 0 ; iii<ksn; iii++ )
		{
			S2CellId io = keys.get(iii);
			ArrayList<UniformDistribution>bestMUList = new ArrayList<>();
			ArrayList<String[]>bestFieldList = new ArrayList<>();
			for (String[] ii : cellField.get(io))
			{
				String guidOuter = ii[3].trim();

				S2CellUnion cellsOuter = fieldUnion.get(guidOuter);

				HashMap<S2CellId,Double> intersectionsOuter = fieldIntersections.get(guidOuter);
				UniformDistribution muOuterError = makeDist(ii[2]);

				//System.err.println("before mu: " + muOuterError);

				muOuterError = remaining(cellsOuter, io, cellm, intersectionsOuter, muOuterError);
				//System.err.println("after mu: " + muOuterError);

				double areaOuter = intersectionsOuter.get(io) * 6367.0 * 6367.0;
				muOuterError=muOuterError.div(areaOuter);

				bestFieldList.add(ii);
				// try {
					// muOuterError.clampLower(0.0);
				bestMUList.add(muOuterError);
				// } catch (UniformDistributionException e) {
				//    ;
				// }

			}

			UniformDensityCurve udc = new UniformDensityCurve(bestMUList);

			for (int k=0; k < bestMUList.size(); k++)
			{
				if (udc.isPeak(bestMUList.get(k)))
				{
					String guid = bestFieldList.get(k)[3].trim();
					bestFields.put(guid,bestFieldList.get(k));
				}
			}
		}
		return bestFields.values();
		//return bestFields.size();
	}

	private static S2CellId getMissingCell (S2CellUnion c, HashMap<S2CellId,UniformDistribution> cellm)
	{
			// assumes only 1 mmissing
			for (S2CellId cid: c)
					if (!cellm.containsKey(cid))
							return cid;
			return new S2CellId(0);
	}

	private static int countMissingCells (S2CellUnion c, HashMap<S2CellId,UniformDistribution> cellm)
	{
			int missing = 0;
			for (S2CellId cid: c)
					if (!cellm.containsKey(cid))
							missing++;
			return missing;
	}

	private static HashMap<S2CellId,UniformDistribution> agreements(ArrayList<String[]> flist,HashMap<S2CellId,UniformDistribution> cellw) 
	{
		ArrayList<String[]>outList = new ArrayList<String[]>();
		HashMap<S2CellId,UniformDistribution> newCellm = new HashMap<S2CellId,UniformDistribution>(cellw);
		int cellmCount = newCellm.size();
		HashSet<S2CellId> processedCells = new HashSet<S2CellId>();
		int count = 0;
		do {
			count = newCellm.size();
			int n = flist.size();

			HashMap<S2CellId,HashSet<String[]>> cellField = new HashMap<S2CellId,HashSet<String[]>>();
			for (int io =0; io < n; io++)
			{
				String[] outerField = flist.get(io);
				S2Polygon s2polyOuter = getS2Polygon(outerField);
				S2CellUnion cellsOuter = getCells(s2polyOuter);
				if (countMissingCells(cellsOuter,newCellm) == 1)
				{

					S2CellId analyseId = getMissingCell(cellsOuter, newCellm);
					if (!cellField.containsKey(analyseId))
						cellField.put(analyseId,new HashSet<String[]>());
					cellField.get(analyseId).add(outerField);
				}
			}
			for (S2CellId io : cellField.keySet())
			{
				ArrayList<String[]>bestFieldList = new ArrayList<String[]>();
				ArrayList<UniformDistribution>bestMUList = new ArrayList<>();

				for (String[] ii : cellField.get(io))
				{
					S2Polygon s2polyOuter = getS2Polygon(ii);
					S2CellUnion cellsOuter = getCells(s2polyOuter);
					String guidOuter = ii[3].trim();

					int muOuter = Integer.valueOf(ii[2].trim());
					HashMap<S2CellId,Double> intersectionsOuter = getCellIntersection(cellsOuter,s2polyOuter);

					UniformDistribution muOuterError = makeDist(ii[2]); // should move this into UniformDistribution

					// System.err.println("mu: " + muOuterError);

					muOuterError = remaining(cellsOuter, io, newCellm, intersectionsOuter, muOuterError);
					double areaOuter = intersectionsOuter.get(io) * 6367.0 * 6367.0;
					muOuterError=muOuterError.div(areaOuter);

					bestFieldList.add(ii);
					bestMUList.add(muOuterError);

				}

				UniformDensityCurve udc = new UniformDensityCurve(bestMUList);

				if (!udc.allValid())
				{
					// as this implementation is non interactive, we should throw at this point and let the user deal with cleaning up invalid 
					// fields first.
					System.out.println (io.toToken() + " " + udc.getPeakValue() + " " + udc.getPeakDistribution() + " " + udc.allValid());
					// where is the invalid field? there was supposed to be an earth shattering invalid field
					throw new ArithmeticException("INVALID FIELD: " + io.toToken() + " " + udc.getPeakValue() + " " + udc.getPeakDistribution() + " " + udc.allValid());
				}
				
				UniformDistribution celld = udc.getPeakDistribution();
				try {
					celld.clampLower(0.0);
					celld.clampUpper(1000000.0);
					newCellm.put(io,celld);

				} catch (UniformDistributionException e) {
					// is this as bad as we think?
					System.err.println("Clamp EXCEPTION. " + celld);
				}
			}
		} while ( count != newCellm.size());
		return newCellm;
	}

	@Override
	public String refineCells() {
		long startTime = System.currentTimeMillis();
		Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO, "CellDBManager: Starting refineCells()...");

		try {
			// 1. Read fields from DB
			ArrayList<String[]> flist = bdao.readValidFields();

			// 2. Read cells from DB
			HashMap<S2CellId,UniformDistribution> cellm = bdao.readCells();

			int count;

			// 3. Call your existing refine logic
			// cellm = tool.refine(fieldList, cellm);

			HashMap<S2CellId,UniformDistribution> newCellm = new HashMap<S2CellId,UniformDistribution>(cellm);
			populate(flist);

			HashMap<S2CellId,Integer> updateCounts = new HashMap<S2CellId,Integer>();

			int iterations = 0;

			do {
				count = 0;

				int ksn = cellField.size();
				ArrayList<S2CellId> keys = new ArrayList<S2CellId>(cellField.keySet());

				for (int iii = 0 ; iii<ksn; iii++ )
				{
					S2CellId io = keys.get(iii);

					ArrayList<String[]>bestFieldList = new ArrayList<String[]>();
					ArrayList<UniformDistribution>bestMUList = new ArrayList<>();
	
					for (String[] ii : cellField.get(io))
					{
						String guidOuter = ii[3].trim();

						S2CellUnion cellsOuter = fieldUnion.get(guidOuter);

						HashMap<S2CellId,Double> intersectionsOuter = fieldIntersections.get(guidOuter);
						UniformDistribution muOuterError = makeDist(ii[2]);

						muOuterError = remaining(cellsOuter, io, newCellm, intersectionsOuter, muOuterError);
						double areaOuter = intersectionsOuter.get(io) * 6367.0 * 6367.0;
						muOuterError=muOuterError.div(areaOuter);

						bestFieldList.add(ii);
						bestMUList.add(muOuterError);
					}
				
					UniformDensityCurve udc = new UniformDensityCurve(bestMUList);
					
					// Handle invalid field reporting.
					if (!udc.allValid())
					{
						//System.out.println ("" + iii + "/" + ksn + " " + io.toToken() + " " + udc.getPeakValue() + " " + udc.getPeakDistribution() +  " " + udc.countInvalid());
						Logger.getLogger(CellDBManager.class.getName()).log(Level.WARNING, "CellDBManager: " + iii + "/" + ksn + " " + io.toToken() + " " + udc.getPeakValue() + " " + udc.getPeakDistribution() +  " " + udc.countInvalid());

						for (int i=0; i < bestMUList.size(); i++)
							if (!udc.isValid(bestMUList.get(i))) {
								String[] f = bestFieldList.get(i);
								UniformDistribution mu = getEstMu(f,newCellm);
								long epoch = Integer.toUnsignedLong(Integer.parseInt(f[4]));
								Date time = new Date(epoch);
								Logger.getLogger(CellDBManager.class.getName()).log(Level.WARNING, "CellDBManager: " + f[3] +" " + time + " ("+ epoch+ ") " + bestMUList.get(i)+ " est: " + mu + " mu: " + f[2]);
								//System.out.println("" + f[3] +" " + time + " ("+ epoch+ ") " + bestMUList.get(i)+ " est: " + mu + " mu: " + f[2]);
								
							}

						return "Encountered potential Error MU fields. See Log.";
					}
					try {
						UniformDistribution ud = udc.getPeakDistribution();
						ud.clampLower(0.0);
						
						if (newCellm.containsKey(io))
						{
							UniformDistribution cud = newCellm.get(io);
							if (Math.abs(ud.mean() - cud.mean())  / cud.mean() > 0.1)
							{
	
								if (updateCounts.containsKey(io))
								{
									Integer c = updateCounts.get(io);
									c++;
									updateCounts.put(io,c);
								}
								else
								{
									updateCounts.put(io,1);
								}
								count ++;
							}
	
							if (ud.getLower() > 0) {
								// I'm quite impressed with the results from UniformDensityCurve
								// as long as the cells stabilise.
								newCellm.put(io,ud);
							} else if (cud.getLower() == 0 && cud.improves(ud)) {
								newCellm.put(io,ud);
								count++;
								if (updateCounts.containsKey(io))
								{
									Integer c = updateCounts.get(io);
									c++;
									updateCounts.put(io,c);
								}
								else
								{
									updateCounts.put(io,1);
								}
							}
	
						} else {
							newCellm.put(io,ud);
							count++;
							if (updateCounts.containsKey(io))
							{
								Integer c = updateCounts.get(io);
								c++;
								updateCounts.put(io,c);
							}
							else
							{
								updateCounts.put(io,1);
							}
						}
					} catch (UniformDistributionException e) {
						// Again need to handle this error reporting.
						Logger.getLogger(CellDBManager.class.getName()).log(Level.WARNING, "CellDBManager REFINE ERROR: " + io.toToken() + " " + udc.getPeakValue() + " P:" + udc.getPeakDistribution() + " <-> C:" + newCellm.get(io)+ " " + udc.allValid());
						return("REFINE ERROR: " + io.toToken() + " " + udc.getPeakValue() + " P:" + udc.getPeakDistribution() + " <-> C:" + newCellm.get(io)+ " " + udc.allValid());

					}
				}
				iterations ++;
			} while (count > 0);


			// 4. Write results back to DB
			// tool.writeCells(cellm); // Assuming you'll add a method to clear and write

			int update = bdao.upsertCells(newCellm);


			long endTime = System.currentTimeMillis();
			long duration = (endTime - startTime) / 1000; // in seconds

			// A meaningful return value for the admin
			Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO, "CellDBManager Refinement process completed in " + duration + " seconds. After " + iterations + " iterations. " + update + " cells updated.");
			return "Refinement process completed in " + duration + " seconds. After " + iterations + " iterations. " + update + " cells updated.";

		} catch (Exception e) {
			e.printStackTrace();
			return "ERROR: Refinement failed. Reason: " + e.getMessage();
		}
	}
 
	@Override
	public String traceCell(String cellToken) {
		System.out.println("JMX: Tracing cell " + cellToken);

		if (cellToken == null || cellToken.trim().isEmpty()) {
			return "ERROR: cellToken parameter cannot be empty.";
		}

		try {

			HashMap<S2CellId,UniformDistribution> cellm;

			cellm = bdao.readCells();
			
			S2CellId cid = S2CellId.fromToken(cellToken);
			ArrayList<String>fieldGuids = edao.fieldGuidsForCell(cid);
			ArrayList<String[]>flist = bdao.getFieldsForGuid(fieldGuids);

			populate(flist);

			ArrayList<String[]> bestFieldList = new ArrayList<String[]>();
			ArrayList<UniformDistribution> bestMUList = new ArrayList<UniformDistribution>();
	 
			for (String[] ii : cellField.get(cid))
			{
				String guidOuter = ii[3].trim();
	
				S2CellUnion cellsOuter = fieldUnion.get(guidOuter);
	
	//            int muOuter = Integer.valueOf(ii[2].trim());
				HashMap<S2CellId,Double> intersectionsOuter = fieldIntersections.get(guidOuter);
	
				UniformDistribution muOuterError = makeDist(ii[2]);
	
				// System.err.println("mu: " + muOuterError);
	
				muOuterError = remaining(cellsOuter, cid, cellm, intersectionsOuter, muOuterError);
				double areaOuter = intersectionsOuter.get(cid) * 6367.0 * 6367.0;
				muOuterError=muOuterError.div(areaOuter);
	
				bestFieldList.add(ii);
				bestMUList.add(muOuterError);
			}
	 
			ArrayList<String[]> bestFields = new ArrayList<String[]>();
	
			UniformDensityCurve udc = new UniformDensityCurve(bestMUList);
	 
			StringBuilder sb = new StringBuilder();

			sb.append(cid.toToken() + "\n");
			sb.append("<b>MU (DB)         </b> : " + cellm.get(cid).toStringWithPrecision(3) + " " + cellm.get(cid) + "\n");
			sb.append("<b>Fields          </b> : " + udc.getPeakValue() + "\n");
			sb.append("<b>MU (calc)       </b> : " + udc.getPeakDistribution() + "\n");
			sb.append("<b>All fields valid</b> : " + udc.allValid() + "\n\n");
			for (int i=0; i < bestFieldList.size(); i++)
			{
				String[] f = bestFieldList.get(i);
				UniformDistribution tmu = bestMUList.get(i);
				if (udc.isPeak(tmu)) {
					int imu = Integer.parseInt(f[2]);
					UniformDistribution fmu = makeDist(f[2]);
					bestFields.add(f);
					S2Polygon s2poly = getS2Polygon(f);
					double area = s2poly.getArea() * 6367.0 * 6367.0;
					double mukm = 1.0 * imu / area;
					sb.append("<b>Field " + i + " guid</b>    : " + f[3] + "\n");
					sb.append("<b>MU             </b> : " + imu + " error: " + fmu.perror() * 100.0 +"%\n");
					sb.append("<b>Scaled MU range</b> : " + tmu + " error: " + tmu.perror() * 100.0 + "%\n");
					sb.append("<b>Area           </b> : " + area + " km " + mukm + " mukm\n\n");
					
					for (S2CellId tcid : fieldUnion.get(f[3].trim()))
						if (!tcid.equals(cid))
						{
							HashMap<S2CellId,Double> intersectionsOuter = fieldIntersections.get(f[3].trim());

							UniformDistribution cmu = cellm.get(tcid).mul(intersectionsOuter.get(tcid) * 6367.0 * 6367.0);
							double err = 100.0 * cmu.range() / imu;
							sb.append("<b>Cell contribution</b>: " + tcid.toToken() + " range: " + cmu + " error: " + cmu.range() + " mu\n\n");
						}

					sb.append("[" + dtPolygon(f, "#FFFFFF") +"]\n\n");
					//System.out.println("");
	
				}
			}

			return sb.toString();

			//return "Trace for " + cellToken + " completed. See server log for output.";

		} catch (Exception e) {
			e.printStackTrace();
			return "ERROR: Trace for " + cellToken + " failed. Reason: " + e.getMessage();
		}
	}
	
	@Override
	public String rebuildFieldCells()
	{
		StringBuilder sb = new StringBuilder();

		try {
			ArrayList<String[]> fieldList;
			ArrayList<String[]> guidList;
			fieldList = bdao.readValidFields();
			sb.append("" + fieldList.size() + " fields read.\n");
			guidList = buildFieldCells(fieldList);
			sb.append("" + guidList.size() + " rows built.\n");
			int del = bdao.deleteFieldCells();
			sb.append("" + del + " rows deleted.\n");
			int upd = bdao.writeFieldCells(guidList);
			sb.append("" + upd + " rows updated.\n");
		} catch (Exception e) {
			e.printStackTrace();
			return "ERROR: RebuildFieldCells failed. " + e.getMessage();
		}
		return sb.toString();
	}
	
	@Override
	public String eraseCells()
	{
		StringBuilder sb = new StringBuilder();

		try {
			int d = bdao.deleteCells();
			sb.append("" + d + " rows erased.\n");
		} catch (Exception e) {
			e.printStackTrace();
			return "ERROR: eraseCells failed. " + e.getMessage();
		}
		return sb.toString();
	}
	
	@Override
	public String rebuildCells()
	{
		StringBuilder sb = new StringBuilder();

		try {
			ArrayList<String[]> fieldList;
			HashMap<S2CellId,UniformDistribution> cellm;

			fieldList = bdao.readValidFields();
			sb.append("" + fieldList.size() + " fields read.\n");
			cellm = bdao.readCells();
			int osize = cellm.size();
			sb.append("" + osize + " cells read.\n");
			
			long startTime = System.currentTimeMillis();
			Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO, "CellDBManager: Starting buildCells()...");
			
			cellm = agreements(fieldList, cellm);

			int update = cellm.size();
			if (update != osize)
			{
				int d = bdao.deleteCells();
				sb.append("" + d + " cells erased.\n");
				int cr = bdao.writeCells(cellm);
				sb.append("" + cr + " rows updated.\n");
			} else {
				update = 0;
				sb.append("No new cells created.\n");
			}

			long endTime = System.currentTimeMillis();
			long duration = (endTime - startTime) / 1000; // in seconds

			// A meaningful return value for the admin
			Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO, "CellDBManager build process completed in " + duration + " seconds. " + update + " cells updated.");
			sb.append("CellDBManager build process completed in " + duration + " seconds.\n");
		} catch (Exception e) {
			e.printStackTrace();
			return "ERROR: rebuildCells failed. " + e.getMessage();
		}
		return sb.toString();
	}
	
	@Override
    public String invalidateField(String fieldGuid)
    {
    	try {
	    		boolean success = bdao.setFieldValid(fieldGuid, false);
			if (success)
    			return "SUCCESS.";
    	
    		return "Unable to update database. (most likely guid not found)";
    	} catch (Exception e) {
			return "ERROR: invalidateField failed. " + e.getMessage();
		}
    }

	@Override
	public String exportBestFields()
	{
		try {
				ArrayList<String[]> fieldList;
				HashMap<S2CellId,UniformDistribution> cellm;

				fieldList =  bdao.readAllFields();
				cellm = bdao.readCells();
				return printTable(best(fieldList,cellm));
		} catch (Exception e) {
			;  // We really need to throw an exception because nothing is presented to the user
		}
		return "";
	}
	
	@Override
	public String exportAllFields()
		{
		try {
				ArrayList<String[]> fieldList;
				HashMap<S2CellId,UniformDistribution> cellm;

				fieldList =  bdao.readAllFields();
				cellm = bdao.readCells();
				return printTable(fieldList);
		} catch (Exception e) {
			;  // We really need to throw an exception because nothing is presented to the user
		}
		return "";
	}


}
