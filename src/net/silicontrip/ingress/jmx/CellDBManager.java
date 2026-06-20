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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

import net.silicontrip.ingress.DrawTools;

import org.json.JSONArray;
import org.json.JSONObject;

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

	private HashMap<String, S2CellUnion> fieldUnion;
	private HashMap<String, HashMap<S2CellId, Double>> fieldIntersections;
	private HashMap<S2CellId, HashSet<String[]>> cellField;

	// Background task state for refineCells
	private Thread refineCellsThread;
	private volatile boolean refineCellsRunning = false;
	private volatile boolean refineCellsCancelled = false;
	private volatile String refineCellsStatus = "Not running";
	private volatile int refineCellsProgress = 0;
	private volatile int refineCellsTotal = 0;
	private StringBuilder refineCellsResult = new StringBuilder();

	// Background task state for rebuildCells (build-cells action)
	private Thread rebuildCellsThread;
	private volatile boolean rebuildCellsRunning = false;
	private volatile boolean rebuildCellsCancelled = false;
	private volatile String rebuildCellsStatus = "Not running";
	private volatile int rebuildCellsProgress = 0;
	private volatile int rebuildCellsTotal = 0;
	private StringBuilder rebuildCellsResult = new StringBuilder();

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

			Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO,
					"CellDBManager MBean registered successfully.");

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
			// System.out.println("CellDBManager MBean unregistered successfully.");
			Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO,
					"CellDBManager MBean unregistered successfully.");

		} catch (Exception e) {
			// Log error, but don't prevent shutdown
			e.printStackTrace();
		}
	}

	private static UniformDistribution getEstMu(String[] f, HashMap<S2CellId, UniformDistribution> cellm) {
		S2Polygon fpoly = getS2Polygon(f);
		S2CellUnion funion = getCells(fpoly);
		HashMap<S2CellId, Double> fint = getCellIntersection(funion, fpoly);

		UniformDistribution mu = new UniformDistribution(0.0, 0.0);

		for (S2CellId cid : funion) {
			double a = fint.get(cid) * 6367.0 * 6367.0;

			if (cellm.containsKey(cid)) {
				mu = mu.add(cellm.get(cid).mul(a));
				// System.out.println("" + cid.toToken() + " " + a + " x " + cellm.get(cid) + "
				// = " + mu);

			} else {
				// System.out.println("" + cid.toToken() + " " + a + " x UNKNOWN");
				mu.setLower(0.0);
				mu.setUpper(Float.MAX_VALUE);
				break;
			}
		}
		return mu;
	}

	private static UniformDistribution remaining(S2CellUnion cells, S2CellId analyseId,
			HashMap<S2CellId, UniformDistribution> cellm, HashMap<S2CellId, Double> intersections,
			UniformDistribution muOuterError) {
		for (S2CellId cellOuter : cells) {
			if (!cellOuter.equals(analyseId)) {
				if (cellm.containsKey(cellOuter)) {
					UniformDistribution cellOuterMU = cellm.get(cellOuter);
					double areaOuter = intersections.get(cellOuter) * 6367.0 * 6367.0;

					UniformDistribution cma = cellOuterMU.mul(areaOuter);
					muOuterError = muOuterError.sub(cma);
				} else {
					// this is the short way of saying muOuterError - (cellOuterMu = [0,+infinity] *
					// areaOuter)
					muOuterError.setLower(0.0);
				}
			}
		}
		return muOuterError;
	}

	private static String dtPolygon(String[] flist, String colour) {
		int lat1 = Integer.valueOf(flist[7].trim());
		int lng1 = Integer.valueOf(flist[8].trim());
		int lat2 = Integer.valueOf(flist[10].trim());
		int lng2 = Integer.valueOf(flist[11].trim());
		int lat3 = Integer.valueOf(flist[13].trim());
		int lng3 = Integer.valueOf(flist[14].trim());
		return "{\"type\":\"polygon\",\"latLngs\":[{\"lat\":" + lat1 / 1000000.0 +
				",\"lng\":" + lng1 / 1000000.0 +
				"},{\"lat\":" + lat2 / 1000000.0 +
				",\"lng\":" + lng2 / 1000000.0 +
				"},{\"lat\":" + lat3 / 1000000.0 +
				",\"lng\":" + lng3 / 1000000.0 +
				"}],\"color\":\"" + colour + "\"}";
	}

	private static UniformDistribution makeDist(String mus) {
		int mu = Integer.parseInt(mus.trim());

		if (mu == 1)
			return new UniformDistribution(0, 1.5);
		return new UniformDistribution(mu, 0.5);
	}

	private void populate(ArrayList<String[]> flist) {
		fieldUnion = new HashMap<String, S2CellUnion>();
		fieldIntersections = new HashMap<String, HashMap<S2CellId, Double>>();
		cellField = new HashMap<S2CellId, HashSet<String[]>>();

		int n = flist.size();
		// System.err.println("prepopulating: " + n);

		for (int io = 0; io < n; io++) {
			String[] outerField = flist.get(io);
			String guidOuter = outerField[3].trim();

			S2Polygon s2polyOuter = getS2Polygon(outerField);
			S2CellUnion cellsOuter = getCells(s2polyOuter);
			HashMap<S2CellId, Double> intersectionsOuter = getCellIntersection(cellsOuter, s2polyOuter);

			fieldUnion.put(guidOuter, cellsOuter);
			fieldIntersections.put(guidOuter, intersectionsOuter);

			for (S2CellId analyseId : cellsOuter) {
				if (!cellField.containsKey(analyseId))
					cellField.put(analyseId, new HashSet<String[]>());
				cellField.get(analyseId).add(outerField);
			}
		}
	}

	private static ArrayList<String[]> buildFieldCells(ArrayList<String[]> flist) {
		ArrayList<String[]> guidIds = new ArrayList<String[]>();

		for (String[] outerField : flist) {
			S2Polygon s2polyOuter = getS2Polygon(outerField);
			S2CellUnion cellsOuter = getCells(s2polyOuter);
			String guidOuter = outerField[3].trim();
			for (S2CellId cid : cellsOuter) {
				String[] guidId = new String[2];
				guidId[0] = guidOuter;
				guidId[1] = "" + (cid.id() >> 32);

				guidIds.add(guidId);
			}
		}
		return guidIds;
	}

	private static String printTable(Iterable<String[]> fieldList) {
		StringBuilder sb = new StringBuilder();
		int count = 0;
		for (String[] r : fieldList) {
			boolean first = true;

			for (String s : r) {
				if (!first)
					sb.append(" |");
				first = false;
				sb.append(s);
			}
			sb.append("\n");
			count++;
		}
		return sb.toString();
	}

	private Iterable<String[]> best(ArrayList<String[]> flist, HashMap<S2CellId, UniformDistribution> cellm) {
		populate(flist);
		int ksn = cellField.size();
		HashMap<String, String[]> bestFields = new HashMap<>();
		ArrayList<S2CellId> keys = new ArrayList<S2CellId>(cellField.keySet());

		for (int iii = 0; iii < ksn; iii++) {
			S2CellId io = keys.get(iii);
			ArrayList<UniformDistribution> bestMUList = new ArrayList<>();
			ArrayList<String[]> bestFieldList = new ArrayList<>();
			for (String[] ii : cellField.get(io)) {
				String guidOuter = ii[3].trim();

				S2CellUnion cellsOuter = fieldUnion.get(guidOuter);

				HashMap<S2CellId, Double> intersectionsOuter = fieldIntersections.get(guidOuter);
				UniformDistribution muOuterError = makeDist(ii[2]);

				// System.err.println("before mu: " + muOuterError);

				muOuterError = remaining(cellsOuter, io, cellm, intersectionsOuter, muOuterError);
				// System.err.println("after mu: " + muOuterError);

				double areaOuter = intersectionsOuter.get(io) * 6367.0 * 6367.0;
				muOuterError = muOuterError.div(areaOuter);

				bestFieldList.add(ii);
				// try {
				// muOuterError.clampLower(0.0);
				bestMUList.add(muOuterError);
				// } catch (UniformDistributionException e) {
				// ;
				// }

			}

			UniformDensityCurve udc = new UniformDensityCurve(bestMUList);

			for (int k = 0; k < bestMUList.size(); k++) {
				if (udc.isPeak(bestMUList.get(k))) {
					String guid = bestFieldList.get(k)[3].trim();
					bestFields.put(guid, bestFieldList.get(k));
				}
			}
		}
		return bestFields.values();
		// return bestFields.size();
	}

	private static S2CellId getMissingCell(S2CellUnion c, HashMap<S2CellId, UniformDistribution> cellm) {
		// assumes only 1 mmissing
		for (S2CellId cid : c)
			if (!cellm.containsKey(cid))
				return cid;
		return new S2CellId(0);
	}

	private static int countMissingCells(S2CellUnion c, HashMap<S2CellId, UniformDistribution> cellm) {
		int missing = 0;
		for (S2CellId cid : c)
			if (!cellm.containsKey(cid))
				missing++;
		return missing;
	}

	private HashMap<S2CellId, UniformDistribution> agreements(ArrayList<String[]> flist,
			HashMap<S2CellId, UniformDistribution> cellw) {
		ArrayList<String[]> outList = new ArrayList<String[]>();
		HashMap<S2CellId, UniformDistribution> newCellm = new HashMap<S2CellId, UniformDistribution>(cellw);
		int cellmCount = newCellm.size();
		HashSet<S2CellId> processedCells = new HashSet<S2CellId>();
		int count = 0;
		do {
			count = newCellm.size();
			int n = flist.size();

			HashMap<S2CellId, HashSet<String[]>> cellField = new HashMap<S2CellId, HashSet<String[]>>();
			for (int io = 0; io < n; io++) {
				String[] outerField = flist.get(io);
				S2Polygon s2polyOuter = getS2Polygon(outerField);
				S2CellUnion cellsOuter = getCells(s2polyOuter);
				if (countMissingCells(cellsOuter, newCellm) == 1) {

					S2CellId analyseId = getMissingCell(cellsOuter, newCellm);
					if (!cellField.containsKey(analyseId))
						cellField.put(analyseId, new HashSet<String[]>());
					cellField.get(analyseId).add(outerField);
				}
			}
			for (S2CellId io : cellField.keySet()) {
				ArrayList<String[]> bestFieldList = new ArrayList<String[]>();
				ArrayList<UniformDistribution> bestMUList = new ArrayList<>();

				for (String[] ii : cellField.get(io)) {
					S2Polygon s2polyOuter = getS2Polygon(ii);
					S2CellUnion cellsOuter = getCells(s2polyOuter);
					String guidOuter = ii[3].trim();

					int muOuter = Integer.valueOf(ii[2].trim());
					HashMap<S2CellId, Double> intersectionsOuter = getCellIntersection(cellsOuter, s2polyOuter);

					UniformDistribution muOuterError = makeDist(ii[2]); // should move this into UniformDistribution

					// System.err.println("mu: " + muOuterError);

					muOuterError = remaining(cellsOuter, io, newCellm, intersectionsOuter, muOuterError);
					double areaOuter = intersectionsOuter.get(io) * 6367.0 * 6367.0;
					muOuterError = muOuterError.div(areaOuter);

					bestFieldList.add(ii);
					bestMUList.add(muOuterError);

				}

				UniformDensityCurve udc = new UniformDensityCurve(bestMUList);

				if (!udc.allValid()) {
					StringBuilder errorDetails = new StringBuilder();
					appendInvalidFieldTrace(errorDetails, io, udc, bestFieldList, bestMUList, newCellm);
					Logger.getLogger(CellDBManager.class.getName()).log(Level.WARNING, "INVALID FIELD encountered, relying on consensus peak:\n" + errorDetails.toString());
				}

				UniformDistribution celld = udc.getPeakDistribution();
				try {
					celld.clampLower(0.0);
					celld.clampUpper(1000000.0);
					newCellm.put(io, celld);

				} catch (UniformDistributionException e) {
					// is this as bad as we think?
					System.err.println("Clamp EXCEPTION. " + celld);
				}
			}
		} while (count != newCellm.size());
		return newCellm;
	}

	/**
	 * Non-static wrapper for agreements() that adds cancellation checks and
	 * progress updates
	 * for background build cells operation.
	 */
	private HashMap<S2CellId, UniformDistribution> agreementsWithProgress(ArrayList<String[]> flist,
			HashMap<S2CellId, UniformDistribution> cellw) {
		HashMap<S2CellId, UniformDistribution> newCellm = new HashMap<S2CellId, UniformDistribution>(cellw);
		int iteration = 0;
		int count = 0;

		do {
			// Check for cancellation
			if (rebuildCellsCancelled) {
				throw new RuntimeException("Build cancelled by user");
			}

			iteration++;
			int previousCount = newCellm.size();

			// Update status (don't append to result, just update the status message)
			rebuildCellsStatus = "Building cell model - iteration " + iteration + " (analyzing " + newCellm.size()
					+ " cells)...";

			// Process one iteration
			count = newCellm.size();
			int n = flist.size();

			HashMap<S2CellId, HashSet<String[]>> cellField = new HashMap<S2CellId, HashSet<String[]>>();
			for (int io = 0; io < n; io++) {
				String[] outerField = flist.get(io);
				S2Polygon s2polyOuter = getS2Polygon(outerField);
				S2CellUnion cellsOuter = getCells(s2polyOuter);
				if (countMissingCells(cellsOuter, newCellm) == 1) {
					S2CellId analyseId = getMissingCell(cellsOuter, newCellm);
					if (!cellField.containsKey(analyseId))
						cellField.put(analyseId, new HashSet<String[]>());
					cellField.get(analyseId).add(outerField);
				}
			}

			for (S2CellId io : cellField.keySet()) {
				ArrayList<String[]> bestFieldList = new ArrayList<String[]>();
				ArrayList<UniformDistribution> bestMUList = new ArrayList<>();

				for (String[] ii : cellField.get(io)) {
					S2Polygon s2polyOuter = getS2Polygon(ii);
					S2CellUnion cellsOuter = getCells(s2polyOuter);
					String guidOuter = ii[3].trim();

					int muOuter = Integer.valueOf(ii[2].trim());
					HashMap<S2CellId, Double> intersectionsOuter = getCellIntersection(cellsOuter, s2polyOuter);

					UniformDistribution muOuterError = makeDist(ii[2]);

					muOuterError = remaining(cellsOuter, io, newCellm, intersectionsOuter, muOuterError);
					double areaOuter = intersectionsOuter.get(io) * 6367.0 * 6367.0;
					muOuterError = muOuterError.div(areaOuter);

					bestFieldList.add(ii);
					bestMUList.add(muOuterError);
				}

				UniformDensityCurve udc = new UniformDensityCurve(bestMUList);

				if (!udc.allValid()) {
					StringBuilder errorDetails = new StringBuilder();
					appendInvalidFieldTrace(errorDetails, io, udc, bestFieldList, bestMUList, newCellm);
					Logger.getLogger(CellDBManager.class.getName()).log(Level.WARNING, "INVALID FIELD encountered, relying on consensus peak:\n" + errorDetails.toString());
				}

				UniformDistribution celld = udc.getPeakDistribution();
				try {
					celld.clampLower(0.0);
					celld.clampUpper(1000000.0);
					newCellm.put(io, celld);
				} catch (UniformDistributionException e) {
					System.err.println("Clamp EXCEPTION. " + celld);
				}
			}
		} while (count != newCellm.size());

		return newCellm;
	}

	/**
	 * Core refinement logic extracted from refineCells.
	 * Refines cell MU values based on field data through iterative analysis.
	 * 
	 * @param flist List of valid fields from database
	 * @param cellm Current cell map with MU distributions
	 * @return Refined cell map with updated MU distributions
	 * @throws ArithmeticException if invalid fields are encountered
	 */
	private HashMap<S2CellId, UniformDistribution> refineFieldCells(ArrayList<String[]> flist,
			HashMap<S2CellId, UniformDistribution> cellm) {
		HashMap<S2CellId, UniformDistribution> newCellm = new HashMap<S2CellId, UniformDistribution>(cellm);
		populate(flist);

		HashMap<S2CellId, Integer> updateCounts = new HashMap<S2CellId, Integer>();

		HashMap<S2CellId, Integer> errorCells = new HashMap<S2CellId, Integer>();
		HashMap<String, Integer> errorFields = new HashMap<String, Integer>();


		int iterations = 0;
		int count;

		do {
			// Check for cancellation
			if (refineCellsCancelled) {
				throw new RuntimeException("Refinement cancelled by user");
			}

			count = 0;

			int ksn = cellField.size();
			ArrayList<S2CellId> keys = new ArrayList<S2CellId>(cellField.keySet());

			// Update progress
			refineCellsProgress = iterations;
			refineCellsTotal = ksn;
			//refineCellsStatus = "Processing iteration " + iterations + ", analyzing " + ksn + " cells...";

			StringBuilder rcs = new StringBuilder();
			rcs.append("Refining cell model - iteration " + iterations + ", analyzing " + ksn + " cells...\n");

			Object[] fe = errorFields.entrySet().toArray();
			Arrays.sort(fe, new Comparator() {
				public int compare(Object o1, Object o2) {
					return ((Map.Entry<String, Integer>) o2).getValue()
							.compareTo(((Map.Entry<String, Integer>) o1).getValue());
				}
			});
			for (Object f : fe) {
				Map.Entry<String, Integer> entry = (Map.Entry<String, Integer>) f;
				rcs.append("Field " + entry.getKey() + " has " + entry.getValue() + " errors.\n");
			}

			Object[] ce = errorCells.entrySet().toArray();
			Arrays.sort(ce, new Comparator() {
				public int compare(Object o1, Object o2) {
					return ((Map.Entry<S2CellId, Integer>) o2).getValue()
							.compareTo(((Map.Entry<S2CellId, Integer>) o1).getValue());
				}
			});
			for (Object c : ce) {
				Map.Entry<S2CellId, Integer> entry = (Map.Entry<S2CellId, Integer>) c;
				rcs.append("Cell " + entry.getKey().toToken() + " has " + entry.getValue() + " errors.\n");
			}

			Object[] ue = updateCounts.entrySet().toArray();
			Arrays.sort(ue, new Comparator() {
				public int compare(Object o1, Object o2) {
					return ((Map.Entry<S2CellId, Integer>) o2).getValue()
							.compareTo(((Map.Entry<S2CellId, Integer>) o1).getValue());
				}
			});
			for (Object u : ue) {
				Map.Entry<S2CellId, Integer> entry = (Map.Entry<S2CellId, Integer>) u;
				rcs.append("Cell " + entry.getKey().toToken() + " has " + entry.getValue() + " updates " + newCellm.get(entry.getKey()).toString() + "\n");
			}

			refineCellsStatus = rcs.toString();

			for (int iii = 0; iii < ksn; iii++) {
				// Check for cancellation periodically
				if (refineCellsCancelled) {
					return newCellm;
					//throw new RuntimeException("Refinement cancelled by user");
				}

				S2CellId io = keys.get(iii);

				ArrayList<String[]> bestFieldList = new ArrayList<String[]>();
				ArrayList<UniformDistribution> bestMUList = new ArrayList<>();

				for (String[] ii : cellField.get(io)) {
					String guidOuter = ii[3].trim();

					S2CellUnion cellsOuter = fieldUnion.get(guidOuter);

					HashMap<S2CellId, Double> intersectionsOuter = fieldIntersections.get(guidOuter);
					UniformDistribution muOuterError = makeDist(ii[2]);

					muOuterError = remaining(cellsOuter, io, newCellm, intersectionsOuter, muOuterError);
					double areaOuter = intersectionsOuter.get(io) * 6367.0 * 6367.0;
					muOuterError = muOuterError.div(areaOuter);

					bestFieldList.add(ii);
					bestMUList.add(muOuterError);
				}

				UniformDensityCurve udc = new UniformDensityCurve(bestMUList);

				// Handle invalid field reporting.
				if (!udc.allValid()) {
					for (int i = 0; i < bestMUList.size(); i++) {
						if (!udc.isValid(bestMUList.get(i))) {
							String[] f = bestFieldList.get(i);
							if (!errorFields.containsKey(f[3].trim())) {
								errorFields.put(f[3].trim(), 1);
							} else {
								errorFields.put(f[3].trim(), errorFields.get(f[3].trim()) + 1);
							}
						}
					}
					if(!errorCells.containsKey(io)) {
						errorCells.put(io, 1);
					} else {
						errorCells.put(io, errorCells.get(io) + 1);
					}
					//StringBuilder errorDetails = new StringBuilder();
					//appendInvalidFieldTrace(errorDetails, io, udc, bestFieldList, bestMUList, newCellm);
					//Logger.getLogger(CellDBManager.class.getName()).log(Level.WARNING, 
					//		"Encountered potential Error MU fields, relying on consensus peak:\n\n" + errorDetails.toString());
					
				}
				try {
					UniformDistribution ud = udc.getPeakDistribution();
					ud.clampLower(0.0);

					if (newCellm.containsKey(io)) {
						UniformDistribution cud = newCellm.get(io);
						
						// Protect against floating-point noise dividing by near-zero means
						boolean meanShifted = ud.meanDiff(cud) > 1E-5 && 
											  (cud.mean() == 0 || ud.meanDiff(cud) / cud.mean() > 0.1);



						if (ud.getLower() > 0) {
							// I'm quite impressed with the results from UniformDensityCurve
							// as long as the cells stabilise.
							newCellm.put(io, ud);
							if (meanShifted) {

								if (updateCounts.containsKey(io)) {
									Integer c = updateCounts.get(io);
									c++;
									updateCounts.put(io, c);
								} else {
									updateCounts.put(io, 1);
								}
								count++;
							}
						} else if (cud.getLower() == 0 && cud.improves(ud)) {
							newCellm.put(io, ud);
							count++;
							if (updateCounts.containsKey(io)) {
								Integer c = updateCounts.get(io);
								c++;
								updateCounts.put(io, c);
							} else {
								updateCounts.put(io, 1);
							}
						}

					} else {
						newCellm.put(io, ud);
						count++;
						if (updateCounts.containsKey(io)) {
							Integer c = updateCounts.get(io);
							c++;
							updateCounts.put(io, c);
						} else {
							updateCounts.put(io, 1);
						}
					}
				} catch (UniformDistributionException e) {
					// Again need to handle this error reporting.
					Logger.getLogger(CellDBManager.class.getName()).log(Level.WARNING,
							"CellDBManager REFINE ERROR: " + io.toToken() + " " + udc.getPeakValue() + " P:"
									+ udc.getPeakDistribution() + " <-> C:" + newCellm.get(io) + " " + udc.allValid());
					throw new RuntimeException("REFINE ERROR: " + io.toToken() + " " + udc.getPeakValue() + " P:"
							+ udc.getPeakDistribution() + " <-> C:" + newCellm.get(io) + " " + udc.allValid());
				}
			}
			iterations++;
		} while (count > 0);

		return newCellm;
	}

	@Override
	public String refineCells() {
		StringBuilder sb = new StringBuilder();

		try {
			ArrayList<String[]> fieldList;
			HashMap<S2CellId, UniformDistribution> cellm;

			long startTime = System.currentTimeMillis();
			Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO, "CellDBManager: Starting refineCells()...");

			// 1. Read fields from DB
			fieldList = bdao.readValidFields();
			sb.append("" + fieldList.size() + " fields read.\n");

			// 2. Read cells from DB
			cellm = bdao.readCells();
			int osize = cellm.size();
			sb.append("" + osize + " cells read.\n");

			// 3. Refine cells
			cellm = refineFieldCells(fieldList, cellm);
			sb.append("Refinement iterations completed.\n");

			// 4. Write results back to DB
			int update = bdao.upsertCells(cellm);
			sb.append("" + update + " cells updated.\n");

			long endTime = System.currentTimeMillis();
			long duration = (endTime - startTime) / 1000; // in seconds

			Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO,
					"CellDBManager refinement process completed in " + duration + " seconds.");
			sb.append("CellDBManager refinement process completed in " + duration + " seconds.\n");
		} catch (Exception e) {
			e.printStackTrace();
			Logger.getLogger(CellDBManager.class.getName()).log(Level.SEVERE,
					"CellDBManager refinement failed", e);

			// Include what was done before the error occurred
			sb.append("\n=== ERROR ===\n");
			sb.append("Refinement FAILED with error:\n");
			sb.append(e.getMessage());
			sb.append("\n\nStatus: ERROR - Refinement did not complete successfully.\n");

			return sb.toString();
		}
		return sb.toString();
	}

	@Override
	public String traceCell(String cellToken) {
		System.out.println("JMX: Tracing cell " + cellToken);

		if (cellToken == null || cellToken.trim().isEmpty()) {
			return "{\"error\": \"cellToken parameter cannot be empty.\"}";
		}

		JSONObject jsonResponse = new JSONObject();
		try {

			HashMap<S2CellId, UniformDistribution> cellm;

			cellm = bdao.readCells();

			S2CellId cid = S2CellId.fromToken(cellToken);
			ArrayList<String> fieldGuids = edao.fieldGuidsForCell(cid);
			ArrayList<String[]> flist = bdao.getFieldsForGuid(fieldGuids);

			populate(flist);

			ArrayList<String[]> bestFieldList = new ArrayList<String[]>();
			ArrayList<UniformDistribution> bestMUList = new ArrayList<UniformDistribution>();

			for (String[] ii : cellField.get(cid)) {
				String guidOuter = ii[3].trim();

				S2CellUnion cellsOuter = fieldUnion.get(guidOuter);

				// int muOuter = Integer.valueOf(ii[2].trim());
				HashMap<S2CellId, Double> intersectionsOuter = fieldIntersections.get(guidOuter);

				UniformDistribution muOuterError = makeDist(ii[2]);

				// System.err.println("mu: " + muOuterError);

				muOuterError = remaining(cellsOuter, cid, cellm, intersectionsOuter, muOuterError);
				double areaOuter = intersectionsOuter.get(cid) * 6367.0 * 6367.0;
				muOuterError = muOuterError.div(areaOuter);

				bestFieldList.add(ii);
				bestMUList.add(muOuterError);
			}

			ArrayList<String[]> bestFields = new ArrayList<String[]>();

			UniformDensityCurve udc = new UniformDensityCurve(bestMUList);

			// StringBuilder sb = new StringBuilder();

			// sb.append(cid.toToken() + "\n");

			jsonResponse.put("cellid", cid.toToken());

			S2Polygon cpg = getS2Polygon(new S2Cell(cid));
			DrawTools cdt = new DrawTools();
			cdt.setDefaultColour(4);
			cdt.addPolygon(cpg);
			// sb.append(cdt.toString() + "\n");
			jsonResponse.put("cellShapeDrawTools", cdt.toString());
			jsonResponse.put("cellShapeIntel", cdt.asIntelLink());
			// sb.append(cdt.asIntelLink() + "\n");
			// sb.append("\n");

			// sb.append("<b>MU (DB) </b> : " + cellm.get(cid).toStringWithPrecision(3) + "
			// " + cellm.get(cid)
			// + "\n");
			jsonResponse.put("mudb", cellm.get(cid).toString());
			// sb.append("<b>Fields </b> : " + udc.getPeakValue() + "\n");
			jsonResponse.put("numfields", udc.getPeakValue());
			// sb.append("<b>MU (calc) </b> : " + udc.getPeakDistribution() + "\n");
			jsonResponse.put("mucalc", udc.getPeakDistribution());
			// sb.append("<b>All fields valid</b> : " + udc.allValid() + "\n\n");
			jsonResponse.put("valid", udc.allValid());

			JSONArray fields = new JSONArray();

			for (int i = 0; i < bestFieldList.size(); i++) {
				JSONObject field = new JSONObject();
				String[] f = bestFieldList.get(i);
				UniformDistribution tmu = bestMUList.get(i);
				if (udc.isPeak(tmu)) {
					int imu = Integer.parseInt(f[2]);
					UniformDistribution fmu = makeDist(f[2]);
					bestFields.add(f);
					S2Polygon s2poly = getS2Polygon(f);
					double area = s2poly.getArea() * 6367.0 * 6367.0;
					double mukm = 1.0 * imu / area;
					// sb.append("<b>Field " + i + " guid</b> : " + f[3] + "\n");
					field.put("guid", f[3]);

					field.put("index", i);
					field.put("imu", imu);
					field.put("mu", tmu);
					field.put("area", area);
					field.put("mukm", mukm);
					// sb.append("<b>MU </b> : " + imu + " error: " + fmu.perror() * 100.0 + "%\n");
					// sb.append("<b>Scaled MU range</b> : " + tmu + " error: " + tmu.perror() *
					// 100.0 + "%\n");
					// sb.append("<b>Area </b> : " + area + " km " + mukm + " mukm\n\n");

					JSONObject contributions = new JSONObject();
					for (S2CellId tcid : fieldUnion.get(f[3].trim()))
						if (!tcid.equals(cid)) {
							HashMap<S2CellId, Double> intersectionsOuter = fieldIntersections.get(f[3].trim());

							if (cellm.containsKey(tcid)) {
								UniformDistribution cmu = cellm.get(tcid)
										.mul(intersectionsOuter.get(tcid) * 6367.0 * 6367.0);
								double err = 100.0 * cmu.range() / imu;
								// sb.append("<b>Cell contribution</b>: " + tcid.toToken() + " range: " + cmu +
								// " error: "
								// + cmu.range() + " mu\n");
								contributions.put(tcid.toToken(), cmu);
							} else {
								contributions.put(tcid.toToken(), "[-1,-1]");
							}
							// contributions.put("mu",cmu);
						}

					// sb.append("\n");

					field.put("contributions", contributions);

					DrawTools dt = new DrawTools();
					dt.addField(getS2Polygon(f));
					// sb.append(dt.toString() + "\n\n");
					field.put("drawtools", dt.toString());

					// dt.erase();
					// dt.addField(getS2Polygon(f));
					dt.addPolygon(cpg);
					// sb.append(dt.asIntelLink() + "\n\n");
					field.put("intel", dt.asIntelLink());
					// System.out.println("");
					fields.put(field);

				}
			}
			jsonResponse.put("fields", fields);

			return jsonResponse.toString();

			// return "Trace for " + cellToken + " completed. See server log for output.";

		} catch (Exception e) {
			e.printStackTrace();
			return "ERROR: Trace for " + cellToken + " failed. Reason: " + e.getMessage();
		}
	}

	@Override
	public String startRefineCellsBackground() {
		synchronized (this) {
			if (refineCellsRunning) {
				return "ERROR: Refinement task is already running. Use refineCellsStatus() to monitor progress.";
			}

			// Reset state
			refineCellsRunning = true;
			refineCellsCancelled = false;
			refineCellsStatus = "Starting refinement...";
			refineCellsProgress = 0;
			refineCellsTotal = 0;
			refineCellsResult = new StringBuilder();

			// Spawn background thread
			refineCellsThread = new Thread(() -> {
				try {
					ArrayList<String[]> fieldList;
					HashMap<S2CellId, UniformDistribution> cellm;

					long startTime = System.currentTimeMillis();
					Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO,
							"CellDBManager: Starting background refineCells()...");

					// 1. Read fields from DB
					refineCellsStatus = "Reading fields from database...";
					fieldList = bdao.readValidFields();
					refineCellsResult.append("" + fieldList.size() + " fields read.\n");

					// 2. Read cells from DB
					refineCellsStatus = "Reading cells from database...";
					cellm = bdao.readCells();
					int osize = cellm.size();
					refineCellsResult.append("" + osize + " cells read.\n");

					// 3. Refine cells
					refineCellsStatus = "Refining cells...";
					cellm = refineFieldCells(fieldList, cellm);
					refineCellsResult.append("Refinement iterations completed.\n");

					// 4. Write results back to DB
					refineCellsStatus = "Writing results to database...";
					int update = bdao.upsertCells(cellm);
					refineCellsResult.append("" + update + " cells updated.\n");

					long endTime = System.currentTimeMillis();
					long duration = (endTime - startTime) / 1000; // in seconds

					Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO,
							"CellDBManager background refinement process completed in " + duration + " seconds.");
					refineCellsResult
							.append("CellDBManager refinement process completed in " + duration + " seconds.\n");
					refineCellsStatus = "Completed successfully";

				} catch (RuntimeException e) {
					if (refineCellsCancelled) {
						refineCellsStatus = "Cancelled by user";
						refineCellsResult.append("\nRefinement cancelled by user.\n");
						Logger.getLogger(CellDBManager.class.getName()).log(Level.WARNING,
								"CellDBManager: Refinement cancelled by user");
					} else {
						refineCellsStatus = "ERROR: " + e.getMessage();
						refineCellsResult.append("\nERROR: " + e.getMessage() + "\n");
						Logger.getLogger(CellDBManager.class.getName()).log(Level.SEVERE,
								"CellDBManager: Refinement error", e);
					}
				} catch (Exception e) {
					refineCellsStatus = "ERROR: " + e.getMessage();
					refineCellsResult.append("\nERROR: Refinement failed. " + e.getMessage() + "\n");
					e.printStackTrace();
					Logger.getLogger(CellDBManager.class.getName()).log(Level.SEVERE, "CellDBManager: Refinement error",
							e);
				} finally {
					refineCellsRunning = false;
				}
			});

			refineCellsThread.setName("RefineCells-Background");
			refineCellsThread.start();

			return "Refinement task started in background. Use refineCellsStatus() to monitor progress.";
		}
	}

	@Override
	public String refineCellsStatus() {
		StringBuilder sb = new StringBuilder();

		if (refineCellsRunning) {
			sb.append("Status: RUNNING\n");
			sb.append("Current: " + refineCellsStatus + "\n");
			if (refineCellsTotal > 0) {
				sb.append(
						"Progress: Iteration " + refineCellsProgress + ", analyzing " + refineCellsTotal + " cells\n");
			}
			sb.append("\nProgress so far:\n");
			sb.append(refineCellsResult.toString());
		} else {
			if (refineCellsStatus.equals("Not running")) {
				sb.append("Status: NOT RUNNING\n");
				sb.append("No refinement task has been started.\n");
			} else {
				sb.append("Status: COMPLETED\n");
				sb.append("Final status: " + refineCellsStatus + "\n\n");
				sb.append("Results:\n");
				sb.append(refineCellsResult.toString());
			}
		}

		return sb.toString();
	}

	@Override
	public String cancelRefineCells() {
		if (!refineCellsRunning) {
			return "No refinement task is currently running.";
		}

		refineCellsCancelled = true;
		return "Cancellation requested. The task will stop at the next safe point.";
	}

	@Override
	public String rebuildFieldCells() {
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
	public String eraseCells() {
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
	public String rebuildCells() {
		StringBuilder sb = new StringBuilder();

		try {
			ArrayList<String[]> fieldList;
			HashMap<S2CellId, UniformDistribution> cellm;

			fieldList = bdao.readValidFields();
			sb.append("" + fieldList.size() + " fields read.\n");
			cellm = bdao.readCells();
			int osize = cellm.size();
			sb.append("" + osize + " cells read.\n");

			long startTime = System.currentTimeMillis();
			Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO, "CellDBManager: Starting buildCells()...");

			cellm = agreements(fieldList, cellm);

			int update = cellm.size();
			if (update != osize) {
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
			Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO,
					"CellDBManager build process completed in " + duration + " seconds. " + update + " cells updated.");
			sb.append("CellDBManager build process completed in " + duration + " seconds.\n");
		} catch (Exception e) {
			e.printStackTrace();
			return "ERROR: rebuildCells failed. " + e.getMessage();
		}
		return sb.toString();
	}

	@Override
	public String startRebuildCellsBackground() {
		synchronized (this) {
			if (rebuildCellsRunning) {
				return "ERROR: Build cells task is already running. Use rebuildCellsStatus() to monitor progress.";
			}

			// Reset state
			rebuildCellsRunning = true;
			rebuildCellsCancelled = false;
			rebuildCellsStatus = "Starting build...";
			rebuildCellsProgress = 0;
			rebuildCellsTotal = 0;
			rebuildCellsResult = new StringBuilder();

			// Spawn background thread
			rebuildCellsThread = new Thread(() -> {
				try {
					ArrayList<String[]> fieldList;
					HashMap<S2CellId, UniformDistribution> cellm;

					long startTime = System.currentTimeMillis();
					Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO,
							"CellDBManager: Starting background rebuildCells()...");

					// Check for cancellation
					if (rebuildCellsCancelled) {
						throw new RuntimeException("Build cancelled by user");
					}

					// 1. Read fields from DB
					rebuildCellsStatus = "Reading fields from database...";
					rebuildCellsProgress = 1;
					rebuildCellsTotal = 5;
					fieldList = bdao.readValidFields();
					rebuildCellsResult.append("" + fieldList.size() + " fields read.\n");

					// Check for cancellation
					if (rebuildCellsCancelled) {
						throw new RuntimeException("Build cancelled by user");
					}

					// 2. Read existing cells from DB
					rebuildCellsStatus = "Reading existing cells from database...";
					rebuildCellsProgress = 2;
					cellm = bdao.readCells();
					int osize = cellm.size();
					rebuildCellsResult.append("" + osize + " cells read.\n");

					// Check for cancellation
					if (rebuildCellsCancelled) {
						throw new RuntimeException("Build cancelled by user");
					}

					// 3. Build cell model using agreements algorithm
					rebuildCellsStatus = "Building cell model (this may take a while)...";
					rebuildCellsProgress = 3;
					cellm = agreementsWithProgress(fieldList, cellm);
					rebuildCellsResult.append("Cell model built.\n");

					// Check for cancellation
					if (rebuildCellsCancelled) {
						throw new RuntimeException("Build cancelled by user");
					}

					// 4. Delete old cells if needed
					int update = cellm.size();
					if (update != osize) {
						rebuildCellsStatus = "Deleting old cell data...";
						rebuildCellsProgress = 4;
						int d = bdao.deleteCells();
						rebuildCellsResult.append("" + d + " cells erased.\n");

						// Check for cancellation
						if (rebuildCellsCancelled) {
							throw new RuntimeException("Build cancelled by user");
						}

						// 5. Write new cells
						rebuildCellsStatus = "Writing new cell data to database...";
						rebuildCellsProgress = 5;
						int cr = bdao.writeCells(cellm);
						rebuildCellsResult.append("" + cr + " rows updated.\n");
					} else {
						update = 0;
						rebuildCellsResult.append("No new cells created.\n");
					}

					long endTime = System.currentTimeMillis();
					long duration = (endTime - startTime) / 1000; // in seconds

					Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO,
							"CellDBManager background build process completed in " + duration + " seconds. " + update
									+ " cells updated.");
					rebuildCellsResult.append("CellDBManager build process completed in " + duration + " seconds.\n");
					rebuildCellsStatus = "Completed successfully";

				} catch (RuntimeException e) {
					if (rebuildCellsCancelled) {
						rebuildCellsStatus = "Cancelled by user";
						rebuildCellsResult.append("\nBuild cancelled by user.\n");
						Logger.getLogger(CellDBManager.class.getName()).log(Level.WARNING,
								"CellDBManager: Build cancelled by user");
					} else {
						rebuildCellsStatus = "ERROR: " + e.getMessage();
						rebuildCellsResult.append("\nERROR: " + e.getMessage() + "\n");
						Logger.getLogger(CellDBManager.class.getName()).log(Level.SEVERE, "CellDBManager: Build error",
								e);
					}
				} catch (Exception e) {
					rebuildCellsStatus = "ERROR: " + e.getMessage();
					rebuildCellsResult.append("\nERROR: Build failed. " + e.getMessage() + "\n");
					e.printStackTrace();
					Logger.getLogger(CellDBManager.class.getName()).log(Level.SEVERE, "CellDBManager: Build error", e);
				} finally {
					rebuildCellsRunning = false;
				}
			});

			rebuildCellsThread.setName("RebuildCells-Background");
			rebuildCellsThread.start();

			return "Build cells task started in background. Use rebuildCellsStatus() to monitor progress.";
		}
	}

	@Override
	public String rebuildCellsStatus() {
		StringBuilder sb = new StringBuilder();

		if (rebuildCellsRunning) {
			sb.append("Status: RUNNING\n");
			sb.append("Current: " + rebuildCellsStatus + "\n");
			if (rebuildCellsTotal > 0) {
				sb.append("Progress: Step " + rebuildCellsProgress + " of " + rebuildCellsTotal + "\n");
			}
			sb.append("\nProgress so far:\n");
			sb.append(rebuildCellsResult.toString());
		} else {
			if (rebuildCellsStatus.equals("Not running")) {
				sb.append("Status: NOT RUNNING\n");
				sb.append("No build cells task has been started.\n");
			} else {
				sb.append("Status: COMPLETED\n");
				sb.append("Final status: " + rebuildCellsStatus + "\n\n");
				sb.append("Results:\n");
				sb.append(rebuildCellsResult.toString());
			}
		}

		return sb.toString();
	}

	@Override
	public String cancelRebuildCells() {
		if (!rebuildCellsRunning) {
			return "No build cells task is currently running.";
		}

		rebuildCellsCancelled = true;
		return "Cancellation requested. The task will stop at the next safe point.";
	}

	@Override
	public String invalidateField(String fieldGuid) {
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
	public String exportBestFields() {
		try {
			ArrayList<String[]> fieldList;
			HashMap<S2CellId, UniformDistribution> cellm;

			fieldList = bdao.readAllFields();
			cellm = bdao.readCells();
			return printTable(best(fieldList, cellm));
		} catch (Exception e) {
			; // We really need to throw an exception because nothing is presented to the user
		}
		return "";
	}

	@Override
	public String exportAllFields() {
		try {
			ArrayList<String[]> fieldList;
			HashMap<S2CellId, UniformDistribution> cellm;

			fieldList = bdao.readAllFields();
			cellm = bdao.readCells();
			return printTable(fieldList);
		} catch (Exception e) {
			; // We really need to throw an exception because nothing is presented to the user
		}
		return "";
	}

	@Override
	public String exportCells() {
		try {
			HashMap<S2CellId, UniformDistribution> cellm = bdao.readCells();
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<S2CellId, UniformDistribution> entry : cellm.entrySet()) {
				S2CellId cid = entry.getKey();
				UniformDistribution ud = entry.getValue();
				sb.append(cid.toToken())
				  .append("|").append(ud.getLower())
				  .append("|").append(ud.getUpper())
				  .append("|").append(ud.mean())
				  .append("\n");
			}
			return sb.toString();
		} catch (Exception e) {
			Logger.getLogger(CellDBManager.class.getName()).log(Level.SEVERE, "Failed to export cells", e);
		}
		return "";
	}

	@Override
	public String pruneFields() {
		StringBuilder sb = new StringBuilder();
		try {
			long startTime = System.currentTimeMillis();
			Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO, "Starting pruneFields()...");
			
			ArrayList<String[]> allFields = bdao.readValidFields();
			sb.append("Read ").append(allFields.size()).append(" valid fields for pruning.\n");

			// Phase 1: Iterative Build Pruning (Spanning Tree)
			HashSet<String> keptFieldGuids = new HashSet<>();
			HashMap<S2CellId, UniformDistribution> localCellm = new HashMap<>();
			
			int count;
			int iteration = 0;
			do {
				count = localCellm.size();
				iteration++;
				
				HashMap<S2CellId, HashSet<String[]>> localCellField = new HashMap<>();
				for (String[] field : allFields) {
					S2Polygon s2polyOuter = getS2Polygon(field);
					S2CellUnion cellsOuter = getCells(s2polyOuter);
					if (countMissingCells(cellsOuter, localCellm) == 1) {
						S2CellId analyseId = getMissingCell(cellsOuter, localCellm);
						if (!localCellField.containsKey(analyseId)) {
							localCellField.put(analyseId, new HashSet<String[]>());
						}
						localCellField.get(analyseId).add(field);
					}
				}

				for (S2CellId io : localCellField.keySet()) {
					ArrayList<String[]> bestFieldList = new ArrayList<>();
					ArrayList<UniformDistribution> bestMUList = new ArrayList<>();

					for (String[] ii : localCellField.get(io)) {
						S2Polygon s2polyOuter = getS2Polygon(ii);
						S2CellUnion cellsOuter = getCells(s2polyOuter);
						HashMap<S2CellId, Double> intersectionsOuter = getCellIntersection(cellsOuter, s2polyOuter);
						UniformDistribution muOuterError = makeDist(ii[2]);

						muOuterError = remaining(cellsOuter, io, localCellm, intersectionsOuter, muOuterError);
						double areaOuter = intersectionsOuter.get(io) * 6367.0 * 6367.0;
						muOuterError = muOuterError.div(areaOuter);

						bestFieldList.add(ii);
						bestMUList.add(muOuterError);
					}

					UniformDensityCurve udc = new UniformDensityCurve(bestMUList);
					UniformDistribution celld = udc.getPeakDistribution();
					
					try {
						celld.clampLower(0.0);
						celld.clampUpper(1000000.0);
						localCellm.put(io, celld);
						
						double peakMean = celld.mean();
						double bestDiff = Double.MAX_VALUE;
						String bestGuid = null;
						
						for (int k = 0; k < bestMUList.size(); k++) {
							if (udc.isPeak(bestMUList.get(k))) {
								double diff = Math.abs(bestMUList.get(k).mean() - peakMean);
								if (diff < bestDiff) {
									bestDiff = diff;
									bestGuid = bestFieldList.get(k)[3].trim();
								}
							}
						}
						if (bestGuid != null) {
							keptFieldGuids.add(bestGuid);
						}

					} catch (UniformDistributionException e) {
						System.err.println("Clamp EXCEPTION in prune build. " + celld);
					}
				}
			} while (count != localCellm.size());

			sb.append("Phase 1 complete. Iterations: ").append(iteration)
			  .append(". Base cell map anchored: ").append(localCellm.size())
			  .append(". Kept base field anchors: ").append(keptFieldGuids.size()).append("\n");

			// Phase 2: Refinement Anchor
			HashMap<S2CellId, UniformDistribution> refinedCellm = bdao.readCells(); 
			refinedCellm = refineFieldCells(allFields, refinedCellm); 
			sb.append("Phase 2 complete. Max precision refined.\n");

			// Phase 3: Precision Prune
			populate(allFields); // Populate class-level fieldUnion, fieldIntersections, cellField
			
			int precisionAnchorsAdded = 0;
			for (S2CellId io : this.cellField.keySet()) {
				if (this.cellField.get(io) == null) continue;

				if (!refinedCellm.containsKey(io)) continue;
				
				ArrayList<String[]> bestFieldList = new ArrayList<>();
				ArrayList<UniformDistribution> bestMUList = new ArrayList<>();
				
				for (String[] ii : this.cellField.get(io)) { 
					String guidOuter = ii[3].trim();
					S2CellUnion cellsOuter = fieldUnion.get(guidOuter);
					HashMap<S2CellId, Double> intersectionsOuter = fieldIntersections.get(guidOuter);
					UniformDistribution muOuterError = makeDist(ii[2]);
					muOuterError = remaining(cellsOuter, io, refinedCellm, intersectionsOuter, muOuterError);
					double areaOuter = intersectionsOuter.get(io) * 6367.0 * 6367.0;
					muOuterError = muOuterError.div(areaOuter);
					
					bestFieldList.add(ii);
					bestMUList.add(muOuterError);
				}
				
				UniformDistribution targetMU = refinedCellm.get(io);
				double targetLower = targetMU.getLower();
				double targetUpper = targetMU.getUpper();
				
				String bestLowerGuid = null;
				double bestLowerDiff = Double.MAX_VALUE;
				
				String bestUpperGuid = null;
				double bestUpperDiff = Double.MAX_VALUE;
				
				for (int k = 0; k < bestMUList.size(); k++) {
					UniformDistribution candMU = bestMUList.get(k);
					String guid = bestFieldList.get(k)[3].trim();
					
					// Find the field that most tightly defines the lower bound
					double lowerDiff = Math.abs(candMU.getLower() - targetLower);
					if (lowerDiff < bestLowerDiff) {
						bestLowerDiff = lowerDiff;
						bestLowerGuid = guid;
					}
					
					// Find the field that most tightly defines the upper bound
					double upperDiff = Math.abs(candMU.getUpper() - targetUpper);
					if (upperDiff < bestUpperDiff) {
						bestUpperDiff = upperDiff;
						bestUpperGuid = guid;
					}
				}
				
				if (bestLowerGuid != null && keptFieldGuids.add(bestLowerGuid)) {
					precisionAnchorsAdded++;
				}
				// It's possible the same field sets both bounds (if it's incredibly precise)
				if (bestUpperGuid != null && keptFieldGuids.add(bestUpperGuid)) {
					precisionAnchorsAdded++;
				}
			}
			
			sb.append("Phase 3 complete. Precision anchors added: ").append(precisionAnchorsAdded)
			  .append(". Total optimal fields: ").append(keptFieldGuids.size()).append("\n");
			
			// Phase 4: Structural Deduplication
			sb.append("Phase 4: Structural Deduplication of Ambiguous Clusters.\n");
			HashMap<String, ArrayList<String[]>> signatureMap = new HashMap<>();
			for (String[] f : allFields) {
				String guid = f[3].trim();
				if (keptFieldGuids.contains(guid)) continue;
				S2Polygon poly = getS2Polygon(f);
				S2CellUnion cells = getCells(poly);
				
				ArrayList<String> cellList = new ArrayList<>();
				for (S2CellId c : cells) {
					cellList.add(c.toToken());
				}
				java.util.Collections.sort(cellList);
				
				StringBuilder sigBuilder = new StringBuilder();
				for (String c : cellList) {
					sigBuilder.append(c).append(",");
				}
				String signature = sigBuilder.toString();
				
				if (!signatureMap.containsKey(signature)) {
					signatureMap.put(signature, new ArrayList<String[]>());
				}
				signatureMap.get(signature).add(f);
			}

			int structuralBridgesAdded = 0;
			for (Map.Entry<String, ArrayList<String[]>> entry : signatureMap.entrySet()) {
				ArrayList<String[]> fields = entry.getValue();
				String bestGuid = null;
				double bestFit = -1.0;
				for (String[] f : fields) {
					S2Polygon poly = getS2Polygon(f);
					S2CellUnion cells = getCells(poly);
					HashMap<S2CellId, Double> intersections = getCellIntersection(cells, poly);
					
					double fit = 0.0;
					for (Double area : intersections.values()) {
						fit += area;
					}
					if (fit > bestFit) {
						bestFit = fit;
						bestGuid = f[3].trim();
					}
				}
				if (bestGuid != null && keptFieldGuids.add(bestGuid)) {
					structuralBridgesAdded++;
				}
			}
			sb.append("Phase 4 complete. Structural bridges added: ").append(structuralBridgesAdded).append("\n");
			
			// Phase 5: The Purge Execute
			int totalFields = allFields.size();
			ArrayList<String[]> prunedFields = new ArrayList<>();
			
			for (String[] field : allFields) {
				String guid = field[3].trim();
				if (keptFieldGuids.contains(guid)) {
					prunedFields.add(field);
				}
			}
			
			int purgedCount = totalFields - prunedFields.size();
			
			// Log status immediately in case this shifts to a background task
			Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO, "Prune Phase 4: Executing DB wipe and replace. Purging " + purgedCount + " fields.");
			
			int deleted = bdao.deleteFields();
			int written = bdao.writeFields(prunedFields);
			
			sb.append("Phase 4 execute. Wiped ").append(deleted).append(" fields. ")
			  .append("Wrote back ").append(written).append(" optimal fields. ")
			  .append("Effectively purged ").append(purgedCount).append(" fields out of ").append(totalFields).append(".\n");

			long endTime = System.currentTimeMillis();
			long duration = (endTime - startTime) / 1000;
			sb.append("Prune process completed in ").append(duration).append(" seconds.\n");

		} catch (Exception e) {
			Logger.getLogger(CellDBManager.class.getName()).log(Level.SEVERE, "Failed pruneFields", e);
			sb.append("ERROR in pruneFields: ").append(e.getMessage());
		}
		return sb.toString();
	}

	/**
	 * Helper method to parse pipe-delimited field content into ArrayList of String
	 * arrays.
	 * Expected format: field1|field2|field3|...|field16
	 * 
	 * @param fileContent The pipe-delimited content from uploaded file
	 * @return ArrayList of String arrays, each representing a field row
	 */
	private ArrayList<String[]> parseFieldsFromContent(String fileContent) {
		ArrayList<String[]> fieldList = new ArrayList<>();

		if (fileContent == null || fileContent.trim().isEmpty()) {
			return fieldList;
		}

		String[] lines = fileContent.split("\\r?\\n");
		for (String line : lines) {
			if (line.trim().isEmpty()) {
				continue;
			}
			// Split by pipe delimiter
			String[] fields = line.split("\\|");
			// Trim each field
			for (int i = 0; i < fields.length; i++) {
				fields[i] = fields[i].trim();
			}
			fieldList.add(fields);
		}

		return fieldList;
	}

	/**
	 * Merge uploaded fields with existing database fields, removing duplicates by
	 * GUID.
	 * 
	 * @param fileContent Pipe-delimited field data from uploaded file
	 * @return Status message with count of fields imported
	 */
	@Override
	public String importFieldsMerge(String fileContent) {
		StringBuilder sb = new StringBuilder();

		try {
			long startTime = System.currentTimeMillis();
			Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO,
					"CellDBManager: Starting importFieldsMerge()...");

			// 1. Parse uploaded file content
			ArrayList<String[]> uploadedFields = parseFieldsFromContent(fileContent);
			sb.append("Parsed " + uploadedFields.size() + " fields from uploaded file.\n");

			// 2. Read existing fields from database
			ArrayList<String[]> existingFields = bdao.readAllFields();
			sb.append("Read " + existingFields.size() + " existing fields from database.\n");

			// 3. Merge and remove duplicates by GUID (field index 3)
			ArrayList<String[]> mergedList = new ArrayList<>(uploadedFields);
			mergedList.addAll(existingFields);

			HashSet<String> uniqueGuids = new HashSet<>();
			ArrayList<String[]> deduplicatedList = new ArrayList<>();
			for (String[] row : mergedList) {
				String guid = row[3].trim();
				if (!uniqueGuids.contains(guid)) {
					uniqueGuids.add(guid);
					deduplicatedList.add(row);
				}
			}
			sb.append("After deduplication: " + deduplicatedList.size() + " unique fields.\n");

			// 4. Delete all existing fields
			int deleted = bdao.deleteFields();
			sb.append("Deleted " + deleted + " existing fields.\n");

			// 5. Write merged fields
			int written = bdao.writeFields(deduplicatedList);
			sb.append("Wrote " + written + " fields to database.\n");

			long endTime = System.currentTimeMillis();
			long duration = (endTime - startTime) / 1000; // in seconds

			Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO,
					"CellDBManager merge import completed in " + duration + " seconds.");
			sb.append("Merge import completed in " + duration + " seconds.\n");

		} catch (Exception e) {
			e.printStackTrace();
			Logger.getLogger(CellDBManager.class.getName()).log(Level.SEVERE,
					"Error during importFieldsMerge", e);
			return "ERROR: Merge import failed. " + e.getMessage();
		}

		return sb.toString();
	}

	/**
	 * Replace all existing database fields with uploaded fields.
	 * 
	 * @param fileContent Pipe-delimited field data from uploaded file
	 * @return Status message with count of fields imported
	 */
	@Override
	public String importFieldsReplace(String fileContent) {
		StringBuilder sb = new StringBuilder();

		try {
			long startTime = System.currentTimeMillis();
			Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO,
					"CellDBManager: Starting importFieldsReplace()...");

			// 1. Parse uploaded file content
			ArrayList<String[]> uploadedFields = parseFieldsFromContent(fileContent);
			sb.append("Parsed " + uploadedFields.size() + " fields from uploaded file.\n");

			// 2. Delete all existing fields
			int deleted = bdao.deleteFields();
			sb.append("Deleted " + deleted + " existing fields.\n");

			// 3. Write new fields
			int written = bdao.writeFields(uploadedFields);
			sb.append("Wrote " + written + " fields to database.\n");

			long endTime = System.currentTimeMillis();
			long duration = (endTime - startTime) / 1000; // in seconds

			Logger.getLogger(CellDBManager.class.getName()).log(Level.INFO,
					"CellDBManager replace import completed in " + duration + " seconds.");
			sb.append("Replace import completed in " + duration + " seconds.\n");

		} catch (Exception e) {
			e.printStackTrace();
			Logger.getLogger(CellDBManager.class.getName()).log(Level.SEVERE,
					"Error during importFieldsReplace", e);
			return "ERROR: Replace import failed. " + e.getMessage();
		}

		return sb.toString();
	}

	/**
	 * Field diagnostic: shows the per-cell MU breakdown, estimated total MU,
	 * reported MU/km², and DrawTools polygon JSON for the given field GUID.
	 * Mirrors FieldServlet.diag().
	 */
	@Override
	public String diagField(String fieldGuid) {
		try {
			HashMap<S2CellId, UniformDistribution> cellm = bdao.readCells();
			return diagFieldInternal(fieldGuid, cellm);
		} catch (Exception e) {
			e.printStackTrace();
			return new JSONObject()
					.put("status", "ERROR")
					.put("message", "diagField failed. " + e.getMessage())
					.toString();
		}
	}

	private String diagFieldInternal(String fieldGuid, HashMap<S2CellId, UniformDistribution> cellm) {
		try {
			net.silicontrip.ingress.Field fi = edao.getField(fieldGuid.trim());
			if (fi == null)
				return new JSONObject()
						.put("status", "ERROR")
						.put("message", "Field not found: " + fieldGuid)
						.toString();

			String[] fiRow = new String[] {
					"", "", "" + fi.getMU(), fi.getGuid(), "" + fi.getTimestamp(), fi.getTeam(),
					fi.getPGuid1(), "" + fi.getPLat1(), "" + fi.getPLng1(),
					fi.getPGuid2(), "" + fi.getPLat2(), "" + fi.getPLng2(),
					fi.getPGuid3(), "" + fi.getPLat3(), "" + fi.getPLng3(), "true"
			};
			S2Polygon fiPoly = getS2Polygon(fiRow);
			S2CellUnion cells = getCells(fiPoly);
			HashMap<S2CellId, Double> intersections = getCellIntersection(cells, fiPoly);

			double fiArea = fiPoly.getArea() * 6367.0 * 6367.0;
			UniformDistribution estMU = getEstMu(fiRow, cellm);

			JSONObject response = new JSONObject();
			response.put("guid", fi.getGuid());
			response.put("mu", fi.getMU());
			response.put("area", fiArea);
			response.put("mukm", fi.getMU() / fiArea);

			if (estMU.getUpper() >= Float.MAX_VALUE) {
				response.put("estimatedMU", "unknown (missing cell data)");
				response.put("estimatedMUPrecise", "unknown");

			} else {
				response.put("estimatedMU", estMU.toString());
				response.put("estimatedMUPrecise", estMU.toStringWithPrecision(3));
				response.put("gapToPeak", 0.0); // logic placeholder
			}

			// Per-cell breakdown
			JSONObject cellBreakdown = new JSONObject();
			for (S2CellId cid : cells) {
				double area = intersections.get(cid) * 6367.0 * 6367.0;
				JSONObject cellEntry = new JSONObject();
				cellEntry.put("area", area);
				if (cellm.containsKey(cid)) {
					UniformDistribution cmu = cellm.get(cid);
					cellEntry.put("mu", cmu.toString());
					cellEntry.put("muPrecise", cmu.toStringWithPrecision(3));
					cellEntry.put("contribution", cmu.mul(area).toString());
				} else {
					cellEntry.put("mu", "unknown");
					cellEntry.put("contribution","unknown");
				}
				cellBreakdown.put(cid.toToken(), cellEntry);
			}
			response.put("cells", cellBreakdown);

			DrawTools dt = new DrawTools();
			dt.addField(fiPoly);
			response.put("drawtools", dt.toString());
			response.put("intel", dt.asIntelLink());

			return response.toString(2);

		} catch (Exception e) {
			e.printStackTrace();
			return new JSONObject()
					.put("status", "ERROR")
					.put("message", "diagField failed. " + e.getMessage())
					.toString();
		}
	}


/*
    {
      "drawtools": "[{\"latLngs\":[{\"lng\":144.881351,\"lat\":-37.804435000000005},{\"lng\":144.856412,\"lat\":-37.792061},{\"lng\":144.86275899999998,\"lat\":-37.796153}],\"color\":\"#a24ac3\",\"type\":\"polygon\"}]",
      "remainingMUPerKmPrecise": "1379.077 +/- 17.987",
      "fieldArea": 0.11460113837924352,
      "guid": "943d20d1cdc142009b1424ccbb0d8b66.b",
      "mukm": 1369.968939404844,
      "reportedMU": 157,
      "intel": "https://intel.ingress.com/?pls=-37.804435,144.881351,-37.792061,144.856412_-37.792061,144.856412,-37.796153,144.862759_-37.796153,144.862759,-37.804435,144.881351&ll=-37.797550,144.866841",
      "remainingMUPerKm": "[1361.090393794535,1397.0636981777784]"
    },
*/

	private static String formatCell(String diag)
	{
		StringBuilder sb = new StringBuilder();

		try {
			JSONObject cd = new JSONObject(diag);
			sb.append(cd.getString("cellid")).append(" ").append(cd.getString("cellMU")).append("\n");
			JSONArray fields = cd.getJSONArray("fields");
			for (Object o : fields)
			{
				JSONObject fi = (JSONObject)o;
				sb.append(fi.getString("guid")).append(" MU: ").append(fi.getInt("reportedMU"))
					.append(" area: ").append(fi.getFloat("fieldArea"))
					.append(" mukm: ").append(fi.getFloat("mukm")).append("\n");
					
				sb.append(fi.getString("drawtools")).append("\n");
				sb.append("\n");
			}
		} catch (Exception e) {
			sb.append( "Unable to decode cell diag: ") .append(e.getMessage()).append("\n");
		}
		return sb.toString();
	}

	/**
	 * Cell diagnostic: lists all fields that cover the given cell token,
	 * showing each field's reported MU, remaining MU/km² implied for this cell,
	 * field area, and the cell's current DB MU.
	 * Mirrors FieldServlet.celldiag().
	 */
	@Override
	public String diagCell(String cellToken) {
		try {
			S2CellId cid = S2CellId.fromToken(cellToken.trim());
			HashMap<S2CellId, UniformDistribution> cellm = bdao.readCells();

			ArrayList<String> fieldGuids = edao.fieldGuidsForCell(cid);
			ArrayList<String[]> flist = bdao.getFieldsForGuid(fieldGuids);
			populate(flist);

			JSONObject response = new JSONObject();
			response.put("cellid", cellToken);

			if (cellm.containsKey(cid)) {
				UniformDistribution dbmu = cellm.get(cid);
				response.put("cellMU", dbmu.toString());
				response.put("cellMUPrecise", dbmu.toStringWithPrecision(3));
			} else {
				response.put("cellMU", "unknown");
			}

			JSONArray fields = new JSONArray();
			for (String[] f : flist) {
				String guid = f[3].trim();
				S2CellUnion fieldCells = fieldUnion.get(guid);
				if (fieldCells == null)
					continue;
				HashMap<S2CellId, Double> intersections = fieldIntersections.get(guid);

				UniformDistribution muError = makeDist(f[2]);
				muError = remaining(fieldCells, cid, cellm, intersections, muError);
				double area = intersections.get(cid) * 6367.0 * 6367.0;
				muError = muError.div(area);

				S2Polygon fpoly = getS2Polygon(f);
				double fieldArea = fpoly.getArea() * 6367.0 * 6367.0;
				DrawTools dt = new DrawTools();
				dt.addField(fpoly);

				JSONObject field = new JSONObject();
				field.put("guid", guid);
				field.put("reportedMU", Integer.parseInt(f[2].trim()));
				field.put("fieldArea", fieldArea);
				field.put("mukm", Integer.parseInt(f[2].trim()) / fieldArea);
				field.put("remainingMUPerKm", muError.toString());
				field.put("remainingMUPerKmPrecise", muError.toStringWithPrecision(3));
				field.put("drawtools", dt.toString());
				field.put("intel", dt.asIntelLink());
				fields.put(field);
			}
			response.put("fields", fields);
			return response.toString(2);

		} catch (Exception e) {
			e.printStackTrace();
			return "ERROR: diagCell failed. " + e.getMessage();
		}
	}

	private static String formatTrace(String result) {
		JSONObject json = new JSONObject(result);
		StringBuilder sb = new StringBuilder();
		for (Object o : json.getJSONArray("fields")) {
			JSONObject fi = (JSONObject) o;
			sb.append("GUID: " + fi.getString("guid") + "\n");
			UniformDistribution mu = null;
			try {
				mu = new UniformDistribution(fi.getString("mu"));
			} catch (Exception e) {
				mu = new UniformDistribution(0, 0);
			}
			sb.append("MU: " + fi.getInt("imu") + " Scaled MU: " + mu + " : " + mu.toStringWithPrecision(3) + "\n");
			sb.append("area: " + fi.getFloat("area") + " mukm: " + fi.getFloat("mukm") + "\n");
			sb.append(fi.getString("drawtools") + "\n\n");
		}
		return sb.toString();
	}

	/**
	 * Targeted standalone single-field processing. Calculates the potential MU
	 * contribution
	 * for each cell covered by this field. Updates the cellmu table and returns a
	 * JSON report.
	 */
	@Override
	public String processField(String fieldGuid) {
		try {
			net.silicontrip.ingress.Field fi = edao.getField(fieldGuid.trim());
			if (fi == null)
				return new JSONObject()
						.put("status", "ERROR")
						.put("message", "Field not found: " + fieldGuid)
						.toString();

			HashMap<S2CellId, UniformDistribution> cellm = bdao.readCells();

			String[] fiRow = new String[] {
					"", "", "" + fi.getMU(), fi.getGuid(), "" + fi.getTimestamp(), fi.getTeam(),
					fi.getPGuid1(), "" + fi.getPLat1(), "" + fi.getPLng1(),
					fi.getPGuid2(), "" + fi.getPLat2(), "" + fi.getPLng2(),
					fi.getPGuid3(), "" + fi.getPLat3(), "" + fi.getPLng3(), "true"
			};

			S2Polygon targetPoly = getS2Polygon(fiRow);
			S2CellUnion targetCells = getCells(targetPoly);
			HashMap<S2CellId, Double> intersections = getCellIntersection(targetCells, targetPoly);

			JSONObject response = new JSONObject();
			response.put("guid", fi.getGuid());
			response.put("mu", fi.getMU());

			JSONArray cellResults = new JSONArray();
			HashMap<S2CellId, UniformDistribution> toWrite = new HashMap<>();

			for (S2CellId cid : targetCells) {
				UniformDistribution muError = makeDist("" + fi.getMU());
				// Account for other cells in this same field - standalone behavior
				if (targetCells.size() > 1) {
					muError.setLower(0.0);
				}

				double area = intersections.get(cid) * 6367.0 * 6367.0;
				if (area > 1e-9) {
					muError = muError.div(area);
				} else {
					continue;
				}

				toWrite.put(cid, muError);

				JSONObject cr = new JSONObject();
				cr.put("cellid", cid.toToken());
				cr.put("mu", muError.toString());
				cr.put("muPrecise", muError.toStringWithPrecision(3));
				cellResults.put(cr);
			}

			if (!toWrite.isEmpty())
				bdao.upsertCells(toWrite);

			response.put("cells", cellResults);
			response.put("updatedCount", toWrite.size());

			LOGGER.log(Level.INFO,
					"processField standalone for " + fi.getGuid() + ": " + toWrite.size() + " cells updated.");
			return response.toString(2);

		} catch (Exception e) {
			e.printStackTrace();
			try {
				return new JSONObject()
						.put("status", "ERROR")
						.put("message", "processField failed: " + e.getMessage())
						.toString();
			} catch (Exception ex) {
				return new JSONObject()
						.put("status", "ERROR")
						.put("message", "processField failed: " + e.getMessage())
						.toString();
			}
		}
	}

	private void appendInvalidFieldTrace(StringBuilder errorDetails, S2CellId io, UniformDensityCurve udc,
			java.util.ArrayList<String[]> bestFieldList, java.util.ArrayList<UniformDistribution> bestMUList,
			java.util.HashMap<S2CellId, UniformDistribution> newCellm) {
		errorDetails.append("Cell ").append(io.toToken())
				.append(": ").append(udc.countInvalid()).append(" invalid field(s) detected.\n");
		errorDetails.append("  Invalid Fields:\n");
		for (int i = 0; i < bestMUList.size(); i++) {
			if (!udc.isValid(bestMUList.get(i))) {
				String[] f = bestFieldList.get(i);
				errorDetails.append("    - GUID: ").append(f[3].trim()).append("\n");
			}
		}
	}

}