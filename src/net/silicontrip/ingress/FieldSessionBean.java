
/**
* The FieldSessionBean contains all code for working with fields
*
* @author  Silicon Tripper
* @version 1.0
* @since   2019-03-31
*/
package net.silicontrip.ingress;

import com.google.common.geometry.*;
//import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;

import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.QueueSender;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;

import net.silicontrip.AreaDistribution;
import net.silicontrip.UniformDistribution;
import net.silicontrip.UniformDistributionException;
//import static net.silicontrip.ingress.CellSessionBean.getS2Polygon;

import static net.silicontrip.S2PolygonUtils.*;

import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
// @Singleton
public class FieldSessionBean {

	// private HashMap<S2CellId, UniformDistribution> cellmu = null;
	/** threshold for MU rounding */
	public final double range = 0.5;

	@EJB
	private MUSessionBean muBean;

	@EJB
	private FieldProcessCache fpCache;

	@EJB
	private FieldsCellsBean fieldsCells;

	@EJB
	private EntityDAO dao;

	private AtomicBoolean running;

	@PostConstruct
	public void init() {
		running = new AtomicBoolean(false);
	}

	/**
	 * Generates an intersection map of a Field and its cells.
	 * Returns a HashMap of the S2CellId and its area in Km^2 and the min and max MU
	 * for that cell.
	 * 
	 * @param thisField the S2Polygon
	 * @return HashMap containing the S2CellId with its Area and Uniform
	 *         Distribution
	 */
	// public HashMap<S2CellId,AreaDistribution> getIntersectionMU(S2Polygon
	// thisField)
	public HashMap<S2CellId, AreaDistribution> getIntersectionMU(Field field) {

		HashMap<S2CellId, AreaDistribution> response = new HashMap<>();

		for (S2CellId cell : field.getCells()) {
			// System.out.println("getIntersectionMU: " + cell.toToken());
			S2Polygon intPoly = new S2Polygon();
			S2Polygon cellPoly = getS2Polygon(new S2Cell(cell));

			intPoly.initToIntersection(field.getS2Polygon(), cellPoly);
			AreaDistribution ad = new AreaDistribution();
			ad.mu = null;
			ad.area = intPoly.getArea() * 6367 * 6367;
			if (cell == null)
				System.out.println("null cell id");
			else
				ad.mu = muBean.getMU(cell.toToken());

			// System.out.println("getIntersectionMU: " + cell.toToken() + ": " + ad.mu);

			response.put(cell, ad);
		}
		return response;
	}

	/**
	 * Checks the DAO to see if this field exists.
	 * 
	 * @param guid the guid of the field to check
	 * @return boolean if the field exists or not
	 */
	public boolean hasFieldGuid(String guid) {
		try {
			return dao.existsField(guid);
		} catch (EntityDAOException e) {
			return false;
		}
	}

	/**
	 * This calls the MU worker method for all fields that use specified cell
	 * Proof of concept for realtime cell updating. I'm excited.
	 * 
	 * @param cell the modified cell to reprocess
	 */
	public void processCell(S2CellId cell) {
		// try {
		ArrayList<String> fieldGuids = fieldsCells.fieldGuidsForCell(cell);
		// System.out.println("Found " + fieldGuids.size() + " for cell " +
		// cell.toToken());
		for (String guid : fieldGuids) {
			// Field fi = dao.getField(guid);
			processFieldGuid(guid);
		}
		// } catch (EntityDAOException e) {
		// Logger.getLogger(FieldSessionBean.class.getName()).log(Level.SEVERE, null,
		// e);
		// ; // do nothing
		// }
	}

	@Asynchronous
	@Lock(LockType.WRITE)
	// @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void beginProcessing() {
		if (!running.getAndSet(true)) {
			HashSet<S2CellId> totalModified = new HashSet<S2CellId>();
			Logger.getLogger(FieldSessionBean.class.getName()).log(Level.INFO, "started processing");

			// try {
			String fieldGuid;
			int count = 0;
			while ((fieldGuid = fpCache.nextFieldGuid()) != null && count < 1000) {
				fpCache.removeFieldGuid(fieldGuid);

				HashSet<S2CellId> modCells = processFieldGuid(fieldGuid);

				for (S2CellId cell : modCells) {
					totalModified.add(cell);
					ArrayList<String> fieldGuids = fieldsCells.fieldGuidsForCell(cell);
					// System.out.println("Found " + fieldGuids.size() + " for cell " +
					// cell.toToken());
					for (String guid : fieldGuids) {
						// Field fi = dao.getField(guid);
						if (!guid.equals(fieldGuid))
							fpCache.addFieldGuid(guid);
					}
				}
				count++;
				if (count % 100 == 0)
					Logger.getLogger(FieldSessionBean.class.getName()).log(Level.INFO,
							"Count: " + count + " field cache size: " + fpCache.size());

			}
			running.set(false);

			Logger.getLogger(FieldSessionBean.class.getName()).log(Level.INFO,
					"end processing " + totalModified.size() + " cell updated");
		} else {
			Logger.getLogger(FieldSessionBean.class.getName()).log(Level.INFO, "already processing");
		}

	}

	public HashSet<S2CellId> processFieldGuid(String fieldGuid) {

		Logger.getLogger(FieldSessionBean.class.getName()).log(Level.INFO, "processFieldGuid: " + fieldGuid);

		try {
			Field field = dao.getField(fieldGuid);

			if (field == null) {
				Logger.getLogger(FieldSessionBean.class.getName()).log(Level.WARNING,
						"cannot find field guid: " + fieldGuid);
				return new HashSet<S2CellId>();
			}

			return processField(field);

		} catch (EntityDAOException e) {
			Logger.getLogger(FieldSessionBean.class.getName()).log(Level.WARNING,
					"cannot find field guid: " + fieldGuid);
		}
		return new HashSet<S2CellId>();

	}

	public HashSet<S2CellId> processField(Field field) {
		HashSet<S2CellId> modifiedCells = new HashSet<>();

		UniformDistribution initialMU;
		Double area;

		Integer score = field.getMU();
		S2Polygon thisField = field.getS2Polygon();

		// Logger.getLogger(FieldSessionBean.class.getName()).log(Level.INFO,null,field);

		// Logger.getLogger(FieldSessionBean.class.getName()).log(Level.INFO,"mu: "+
		// score);

		try {

			if (score == 1)
				initialMU = new UniformDistribution(0.0, 1.5);
			else
				initialMU = new UniformDistribution(score, range);

			// an algorithmic version of the following equation
			// mu[cell] = ( MU - intersectionArea[cell1] x mu[cell1] ... -
			// intersectionArea[cellN] x mu[cellN]) / intersectionArea[cell]

			final S2CellUnion cells = field.getCells();

			for (S2CellId cellOuter : cells) {
				StringBuilder cellLog = new StringBuilder();
				UniformDistribution mus = new UniformDistribution(initialMU);
				cellLog.append("( ");
				cellLog.append(mus.toString());
				for (S2CellId cellInner : cells) {
					// if not cell from outer loop
					if (!cellOuter.equals(cellInner)) {
						// System.out.println("i<->o: " + cellOuter.toToken() + " - "+
						// cellInner.toToken());
						double areaInner = getIntersectionArea(thisField, cellInner);

						// Logger.getLogger(FieldSessionBean.class.getName()).log(Level.INFO,"get mu for
						// cell:" + cellInner.toToken());

						UniformDistribution cellInnerMU = muBean.getMU(cellInner.toToken()); // em.find(CellMUEntity.class,
																								// cellInner.id());

						cellLog.append(" - ");
						cellLog.append(areaInner);
						cellLog.append(" x ");
						cellLog.append(cellInner.toToken());
						cellLog.append(":");

						if (cellInnerMU != null) {
							cellLog.append(cellInnerMU);
							UniformDistribution cma = cellInnerMU.mul(areaInner);
							mus = mus.sub(cma);
						} else {
							cellLog.append("undefined");
							mus.setLower(0.0);
						}
					}
				}
				cellLog.append(" ) / ");
				double areaOuter = getIntersectionArea(thisField, cellOuter);
				cellLog.append(areaOuter);
				mus = mus.div(areaOuter);
				cellLog.insert(0, " = ");

				try {
					mus.clampLower(0.0);
					cellLog.insert(0, mus);

					UniformDistribution cellOuterMU = muBean.getMU(cellOuter.toToken());

					cellLog.insert(0, " => ");
					if (cellOuterMU == null)
						cellLog.insert(0, "null");
					else
						cellLog.insert(0, cellOuterMU.toString());

					cellLog.insert(0, ": ");
					cellLog.insert(0, cellOuter.toToken());

					if (muBean.refineMU(cellOuter.toToken(), mus)) {
						if (cellOuterMU != null) {
							mus.refine(cellOuterMU);
							Logger.getLogger(FieldSessionBean.class.getName()).log(Level.INFO,
									"UPDATED: " + cellOuter.toToken() + " old "
											+ cellOuterMU.toStringWithPrecision(3) + " new "
											+ mus.toStringWithPrecision(3) + " fguid: " + field.getGuid());
						} else {
							Logger.getLogger(FieldSessionBean.class.getName()).log(Level.INFO,
									"NEW: " + cellOuter.toToken() + " " + mus.toStringWithPrecision(3) + " fguid: "
											+ field.getGuid());
						}
						modifiedCells.add(cellOuter);
					}

				} catch (UniformDistributionException ae) {
					// field error
					Logger.getLogger(FieldSessionBean.class.getName()).log(Level.WARNING, "Field: " + field.getGuid()
							+ " Cell: " + cellOuter.toToken() + " MU exception " + ae.getMessage());

					System.out.println(ae.getMessage() + " " + cellLog.toString());
					// findInvalid(field);
					// something something, out of range error
					// mark field as invalid
					// logic to find invalid field for cell
				}

				// Logger.getLogger(FieldSessionBean.class.getName()).log(Level.INFO,cellLog.toString());
			}
			// em.getTransaction().commit(); // I hope this works.

			// unlock cells

			// it didn't work, it threw some error about being incompatible with JTA
			// but I really think I need to control the transaction

			StringBuilder fieldLog = new StringBuilder();
			return modifiedCells;

		} catch (Exception e) {
			Logger.getLogger(FieldSessionBean.class.getName()).log(Level.SEVERE, null, e);

			System.out.println("processFieldException: " + e.getMessage());
			e.printStackTrace();
		}

		return modifiedCells;
	}

	/**
	 * find the MU for a known field
	 *
	 * @param field the field to search for
	 *
	 * @return int of the known mu or -1 if unknown
	 */
	public int muKnownField(Field field) {
		try {
			for (Field f : dao.findField(field))
				return f.getMU();

		} catch (Exception e) {
			Logger.getLogger(FieldSessionBean.class.getName()).log(Level.SEVERE, null, e);

			System.out.println("muKnownFieldException: " + e.getMessage());
			e.printStackTrace();
		}
		return -1;
	}

	/**
	 * Determines if a field's mu is possibly valid.
	 * Requires adequate cell data for the field being tested
	 * if not enough cell data is available this method will return true.
	 * 
	 * @param s2Field The field to test.
	 * @param mu      the integer value of the field. Fields have floating point mu
	 *                values but are rounded to the nearest integer greater than 0
	 * @return boolean indicating if the MU specified agrees with the cell data.
	 */
	public boolean muFieldValid(Field field, int mu) {
		UniformDistribution muest = muForField(field);
		// System.out.println("muFieldValid: MU est: "+ muest);
		if (muest.getLower() == -1)
			return true;

		return muest.roundAboveZero().contains(mu);
	}

	/**
	 * Get the MU UniformDistribution for a field.
	 * 
	 * @param s2Field the field to calculate its mu
	 * @return UniformDistribution of the mu.
	 */
	// public UniformDistribution muForField(S2Polygon s2Field)
	public UniformDistribution muForField(Field field) {

		HashMap<S2CellId, AreaDistribution> muArea = getIntersectionMU(field);
		UniformDistribution fieldmu = new UniformDistribution(0, 0);

		for (Map.Entry<S2CellId, AreaDistribution> entry : muArea.entrySet()) {
			AreaDistribution cell = entry.getValue();
			// System.out.println("muForField: " + entry.getKey().toToken() + ": " +
			// cell.mu);
			// do we have information for this cell
			// if not we should skip this whole thing
			if (cell.mu != null) {
				UniformDistribution uniformMU = new UniformDistribution(cell.mu);
				uniformMU = uniformMU.mul(cell.area);
				fieldmu = fieldmu.add(uniformMU);
			} else {
				return new UniformDistribution(-1.0, -1.0);
			}
		}
		return fieldmu;
	}

	private UniformDistribution remaining(Field fi, S2CellId cell) {
		UniformDistribution mu;

		Integer score = fi.getMU();
		// if (score == 50)
		// Logger.getLogger(FieldQueueMDB.class.getName()).log(Level.INFO, "Remaining
		// required field: " + fi.getGuid() + " for cell: " + cell.toToken());

		double area = getIntersectionArea(fi.getS2Polygon(), cell);

		if (score == 1)
			mu = new UniformDistribution(0.0, 1.5);
		else
			mu = new UniformDistribution(score, range);

		S2CellUnion fieldsCells = fi.getCells();

		// if (score == 50)
		// Logger.getLogger(FieldQueueMDB.class.getName()).log(Level.INFO, "Remaining
		// MU: " + mu);

		for (S2CellId innerCell : fieldsCells) {
			// if (score == 50)
			// Logger.getLogger(FieldQueueMDB.class.getName()).log(Level.INFO, "Remaining
			// inner cell: " + innerCell.toToken());

			if (!innerCell.equals(cell)) {
				double areaInner = getIntersectionArea(fi.getS2Polygon(), innerCell);

				UniformDistribution cellInnerMU = muBean.getMU(innerCell.toToken());

				if (cellInnerMU != null) {
					UniformDistribution cma = cellInnerMU.mul(areaInner);
					mu = mu.sub(cma);
				} else {
					mu.setLower(0.0);
				}
			}
		}
		// if (score == 50)
		// Logger.getLogger(FieldQueueMDB.class.getName()).log(Level.INFO, "Remaining
		// MU: " + mu);

		mu = mu.div(area);
		// if (score == 50)
		// Logger.getLogger(FieldQueueMDB.class.getName()).log(Level.INFO, "Remaining
		// MU/km: " + mu);

		return mu;
	}

	public ArrayList<S2CellId> disagreements(Field field) throws EntityDAOException {

		int disagree = 0;
		ArrayList<S2CellId> disagreecells = new ArrayList<S2CellId>();

		if (field == null) {
			Logger.getLogger(FieldSessionBean.class.getName()).log(Level.WARNING, "Null field passed to disagreements");
			return disagreecells;
		}
		final S2CellUnion cells = field.getCells();
		try {
			for (S2CellId cell : cells) {

				UniformDistribution outerRemaining = remaining(field, cell);

				ArrayList<String> fieldGuids = fieldsCells.fieldGuidsForCell(cell);

				for (String fguid : fieldGuids) {
					if (!fguid.equals(field.getGuid())) {
						Field fi = dao.getField(fguid);
						if (fi != null) {
							UniformDistribution innerRemaining = remaining(fi, cell);

							if (!outerRemaining.intersects(innerRemaining)) {
								disagree++;
								disagreecells.add(cell);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			Logger.getLogger(FieldSessionBean.class.getName()).log(Level.SEVERE, null, e);

		}
		return disagreecells;
	}

	private int countMissingCells(S2CellUnion c) {
		int missing = 0;
		for (S2CellId cid : c)
			if (muBean.getMU(cid.toToken()) == null)
				missing++;
		return missing;
	}

	public boolean improvesModel(Field field) throws EntityDAOException {
		S2CellUnion cellsOuter = field.getCells();
		// if (cellsOuter.size() == 1 && field.getMU() > 1) {
		// return true;
		// }
		// int fieldCount = 0;
		// if (field.getMU() == 50)
		// Logger.getLogger(FieldQueueMDB.class.getName()).log(Level.INFO, "Processing
		// required field: " + field.getGuid());
		if (countMissingCells(cellsOuter) == 0) {
			for (S2CellId cell : cellsOuter) {
				// ArrayList<String> fieldGuids = fieldsCells.fieldGuidsForCell(cell);

				// fieldCount += fieldGuids.size();

				// for (String fguid : fieldGuids)
				// {
				// if (fguid != field.getGuid())
				// {
				// Field fi = dao.getField(fguid);

				UniformDistribution innerRemaining = remaining(field, cell);
				UniformDistribution cellMU = muBean.getMU(cell.toToken());
				// if (field.getMU() == 50)
				// Logger.getLogger(FieldQueueMDB.class.getName()).log(Level.INFO, "Remaining: "
				// + innerRemaining + " cell mu: "+cellMU + " for cell " + cell.toToken());
				// this shouldn't return null as we did a countMissingCells check earlier
				if (cellMU.edgeWithin(innerRemaining))
					return true;
				// }
				// }
			}
			// if (fieldCount == 0)
			// {
			// Logger.getLogger(FieldQueueMDB.class.getName()).log(Level.WARNING, "Cells
			// exist but no field guids exist: " + field.getGuid());
			// return true;
			// }
			return false;
		}
		return true;
	}

	public void findInvalid(Field field) {
		Double area;
		HashMap<String, UniformDistribution> resultMap = new HashMap<>();
		try {

			final S2CellUnion cells = field.getCells();

			for (S2CellId cell : cells) {
				ArrayList<String> fieldGuids = fieldsCells.fieldGuidsForCell(cell);

				for (String fguid : fieldGuids) {
					Field fi = dao.getField(fguid);

					UniformDistribution mu = remaining(fi, cell);

					// System.out.println(cell.toToken() + ": " + fguid + " -> " + mu);
					resultMap.put(fguid, mu);
				}
				// System.out.println(cell.toToken() + ": " + resultMap.size());
				for (Map.Entry<String, UniformDistribution> res1 : resultMap.entrySet()) {
					int count = 0;
					for (Map.Entry<String, UniformDistribution> res2 : resultMap.entrySet()) {

						if (res1.getValue().intersects(res2.getValue()))
							count++;
					}
					if (count == resultMap.size()) {
						Field fi = dao.getField(res1.getKey());
						DrawTools dt = new DrawTools();
						dt.addField(fi.getS2Polygon());
						System.out.println(res1.getKey() + ": " + count + " " + dt.toString());
					}
				}
			}
			/*
			 * for (Map.Entry<String,UniformDistribution> res : resultMap.entrySet())
			 * {
			 * System.out.println(res.getKey() + " -> " + res.getValue());
			 * }
			 */
		} catch (Exception e) {
			Logger.getLogger(FieldSessionBean.class.getName()).log(Level.SEVERE, null, e);

			// um what am I doing here?
			System.out.println("FindInvalidException: " + e.getMessage());
		}
	}

	public HashMap<String, UniformDistribution> fieldMUCell(S2CellId cell) {
		HashMap<String, UniformDistribution> results = new HashMap<String, UniformDistribution>();
		ArrayList<String> fieldGuids = fieldsCells.fieldGuidsForCell(cell);
		for (String fieldGuid : fieldGuids) {
			try {
				Field field = dao.getField(fieldGuid);
				UniformDistribution mus = remaining(field, cell);
				results.put(fieldGuid, mus);
			} catch (EntityDAOException e) {
				Logger.getLogger(FieldSessionBean.class.getName()).log(Level.WARNING,
						"cannot find field guid: " + fieldGuid);
			}
		}
		return results;
	}

	/**
	 * For a 'split' field situation where an initial check shows multiple MUs are
	 * possible for a field,
	 * this method performs a deeper validation to disambiguate. It takes two fields
	 * and two MUs
	 * and returns a boolean array indicating which MU is valid for the first field
	 * (fieldA).
	 *
	 * @param fieldA The first field.
	 * @param fieldB The second field.
	 * @param mu1    The first mu value.
	 * @param mu2    The second mu value.
	 * @return A boolean array `valid` of size 2, where `valid[0]` corresponds to
	 *         `mu1`'s validity for `fieldA`,
	 *         and `valid[1]` corresponds to `mu2`'s validity for `fieldA`.
	 */
	public boolean[] isSingleValidSplitFieldCombination(Field fieldA, Field fieldB, int mu1, int mu2) {
		boolean combo1_isValid = isCombinationValid(fieldA, mu1, fieldB, mu2); // A:mu1, B:mu2
		boolean combo2_isValid = isCombinationValid(fieldA, mu2, fieldB, mu1); // A:mu2, B:mu1

		return new boolean[] { combo1_isValid, combo2_isValid };
	}

	private boolean isCombinationValid(Field fieldA, int muA, Field fieldB, int muB) {
		fieldA.setMU(muA);
		fieldB.setMU(muB);

		S2CellUnion cellsA = fieldA.getCells();
		S2CellUnion cellsB = fieldB.getCells();

		ArrayList<S2CellId> allCellsList = new ArrayList<>();
		allCellsList.addAll(cellsA.cellIds());
		allCellsList.addAll(cellsB.cellIds());
		HashSet<S2CellId> allCellsSet = new HashSet<>(allCellsList);

		Map<S2CellId, UniformDistribution> tempCellMus = new HashMap<>();
		for (S2CellId cellId : allCellsSet) {
			UniformDistribution mu = muBean.getMU(cellId.toToken());
			if (mu == null) {
				// Cannot validate if cell data is missing.
				// Following the logic of muFieldValid, we return true.
				return true;
			}
			tempCellMus.put(cellId, mu);
		}

		S2CellUnion sharedCells = new S2CellUnion();
		sharedCells.getIntersection(cellsA, cellsB);

		for (S2CellId sharedCellId : sharedCells) {
			UniformDistribution muFromA = remaining(fieldA, sharedCellId);
			UniformDistribution muFromB = remaining(fieldB, sharedCellId);

			UniformDistribution intersection = muFromA.intersection(muFromB);
			if (intersection.isEmpty()) {
				// The MUs implied by the two fields for the shared cell are disjoint.
				return false;
			}
			tempCellMus.put(sharedCellId, intersection);
		}

		// With updated MUs for shared cells, check if the field MUs are still valid.
		UniformDistribution muEstA = muForField(fieldA, tempCellMus);
		if (muEstA.getLower() != -1 && !muEstA.roundAboveZero().contains(muA)) {
			return false;
		}

		UniformDistribution muEstB = muForField(fieldB, tempCellMus);
		if (muEstB.getLower() != -1 && !muEstB.roundAboveZero().contains(muB)) {
			return false;
		}

		return true;
	}

	/**
	 * Get the MU UniformDistribution for a field, using a provided map of cell MUs.
	 * 
	 * @param field   the field to calculate its mu
	 * @param cellMus a map of S2CellId to UniformDistribution for the cells.
	 * @return UniformDistribution of the mu.
	 */
	private UniformDistribution muForField(Field field, Map<S2CellId, UniformDistribution> cellMus) {
		UniformDistribution fieldmu = new UniformDistribution(0, 0);

		for (S2CellId cellId : field.getCells()) {
			S2Polygon intPoly = new S2Polygon();
			S2Polygon cellPoly = getS2Polygon(new S2Cell(cellId));

			intPoly.initToIntersection(field.getS2Polygon(), cellPoly);
			double area = intPoly.getArea() * 6367 * 6367;

			UniformDistribution cellMu = cellMus.get(cellId);

			if (cellMu != null) {
				UniformDistribution uniformMU = new UniformDistribution(cellMu);
				uniformMU = uniformMU.mul(area);
				fieldmu = fieldmu.add(uniformMU);
			} else {
				return new UniformDistribution(-1.0, -1.0);
			}
		}
		return fieldmu;
	}

}
