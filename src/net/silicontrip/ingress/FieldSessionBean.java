
/**
* The CellSessionBean contains all code for working with fields and cells
*
* @author  Silicon Tripper
* @version 1.0
* @since   2019-03-31
*/
package net.silicontrip.ingress;

import com.google.common.geometry.*;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import net.silicontrip.AreaDistribution;
import net.silicontrip.UniformDistribution;
import static net.silicontrip.ingress.CellSessionBean.getCellsForField;
import static net.silicontrip.ingress.CellSessionBean.getS2Polygon;

@Stateless
@LocalBean
public class FieldSessionBean {
	
		// private HashMap<S2CellId, UniformDistribution> cellmu = null;
	/** threshold for MU rounding */
	public final double range = 0.5; 
	
	private InitialContext ctx = null;
    private QueueConnectionFactory qcf = null;
    private QueueConnection queueCon = null;
    private QueueSession queueSession = null;
    private Queue submitQueue = null;
	private QueueSender sender = null;
	
	@EJB
	private CellSessionBean cellBean;
	
	@EJB
	private SQLEntityDAO dao;
	
	@EJB 
	private FieldProcessCache fpCache;
	
	@PersistenceContext(unitName="net.silicontrip.ingress.persistence")
	private EntityManager em;
	
	private void initCellQueue() throws NamingException, JMSException  {
		if (ctx == null) 
			ctx = new InitialContext();
		if (qcf==null)
            qcf = (QueueConnectionFactory) ctx.lookup("jms/QueueConnectionFactory");
		if (queueCon==null)
            queueCon = qcf.createQueueConnection();
		if (queueSession==null)
            queueSession = queueCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		if (submitQueue==null)
			submitQueue = (Queue)ctx.lookup("jms/cellQueue");
		if (sender==null)
			sender = queueSession.createSender(submitQueue);
		
	}
	
		/**
	 * Generates an intersection map of a Field and its cells.
	 * Returns a HashMap of the S2CellId and its area in Km^2 and the min and max MU for that cell.
	 * @param thisField the S2Polygon
	 * @return HashMap containing the S2CellId with its Area and Uniform Distribution
	 */
	public HashMap<S2CellId,AreaDistribution> getIntersectionMU(S2Polygon thisField)
	{

		HashMap<S2CellId,AreaDistribution> response = new HashMap<>();

		for (S2CellId cell: getCellsForField(thisField))
		{
		//System.out.println("getIntersectionMU: " + cell.toToken());
			S2Polygon intPoly = new S2Polygon();
			S2Polygon cellPoly = getS2Polygon(new S2Cell(cell));

			intPoly.initToIntersection(thisField, cellPoly);
			AreaDistribution ad = new AreaDistribution();
			ad.area = intPoly.getArea() * 6367 * 6367 ;
			ad.mu = cellBean.getMU(cell);

			// System.out.println("getIntersectionMU: " + cell.toToken() + ": " + ad.mu);
			
			response.put(cell,ad);
		}
		return response;
	}
	
		/**
	 * Checks the DAO to see if this field exists.
	 * @param guid the guid of the field to check
	 * @return boolean if the field exists or not
	 */
	public boolean hasFieldGuid(String guid)
	{
		return dao.existsField(guid);
	}
	
	/**
	 * inserts the field using the DAO.
	 * @param field the field entity to insert
	 * @param valid the mu validity flag
	 */
	public void submitField(Field field, boolean valid)
	{
		// System.out.println(">>> submitField: " );
		try {
		dao.insertField(field.getCreator(),
					field.getAgent(),
					field.getMU(),
					field.getGuid(),
					field.getTimestamp(),
					field.getTeam(),
					field.getPGuid1(),
					field.getPLat1(),
					field.getPLng1(),
					field.getPGuid2(),
					field.getPLat2(),
					field.getPLng2(),
					field.getPGuid3(),
					field.getPLat3(),
					field.getPLng3(),
					valid);
		S2CellUnion fieldCells = getCellsForField(field.getS2Polygon());
		dao.insertCellsForField(field.getGuid(),fieldCells);
		// too easy?
		// should this be here or in the caller?
		//System.out.println("Valid for process? " + valid);
		if (valid)
		{
			//System.out.println("Process Field...");
			processField(field);
		}
		} catch (Exception e) {
			System.out.println("submitFieldException: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * This calls the MU worker method for all fields that use specified cell
	 * Proof of concept for realtime cell updating. I'm excited.
	 * @param cell the modified cell to reprocess
	 */
	public void processCell(S2CellId cell)
	{
			ArrayList<String> fieldGuids = dao.fieldGuidsForCell(cell);
			//System.out.println("Found " + fieldGuids.size() + " for cell " + cell.toToken());
			for (String guid : fieldGuids)
			{
				Field fi = dao.getField(guid);
				processField(fi);
			}
	}
		/**
	 * This is the MU worker method converting fields into cell mu
	 * Any cells which have been modified are placed into the cellQueue
	 * which then resubmits any fields which use that cell.
	 * Proof of concept for realtime cell updating. I'm excited.
	 * @param field the field entity to process into cells
	 */
	public void processField(Field field)
	{
		UniformDistribution initialMU;
		Double area;

		Integer score = field.getMU();
		S2Polygon thisField = field.getS2Polygon();
		HashSet<S2CellId> modifiedCells = new HashSet<>();
		//EntityManager em = emf.createEntityManager();
		// HashMap<S2CellId,CellMUEntity> = new HashMap<>();

		/*
		System.out.print ("Processing Cells: ");
		for (S2CellId cello: cells) 
			System.out.print (cello.toToken() + ", ");
		System.out.println(".");
		*/
		
		try {
		
		if (score==1) 
			initialMU = new UniformDistribution(0.0,1.5);
		else
			initialMU = new UniformDistribution(score,range);

		// an algorithmic version of the following equation
		// mu[cell] = ( MU - intersectionArea[cell1] x mu[cell1] ... - intersectionArea[cellN] x mu[cellN]) / intersectionArea[cell]

		// em.getTransaction().begin(); // avoid concurrency issues.
		final S2CellUnion cells = getCellsForField(thisField);

		for (S2CellId cellOuter: cells)
		{
			StringBuilder cellLog = new StringBuilder();
			UniformDistribution mus = new UniformDistribution(initialMU);
			cellLog.append("( ");
			cellLog.append(mus.toString());
			for (S2CellId cellInner: cells)
			{
				// if not cell from outer loop
				if (!cellOuter.equals(cellInner))
				{
					// System.out.println("i<->o: " + cellOuter.toToken() + " - "+ cellInner.toToken());
					double areaInner = CellSessionBean.getIntersectionArea(thisField,cellInner);
					CellMUEntity cellmu = em.find(CellMUEntity.class, cellInner.id());
					UniformDistribution cellInnerMU = null;
					if (cellmu!=null)
						cellInnerMU = cellmu.getDistribution();
					/*
					else
						System.out.println(cellInner.toToken() + ": not found");
					*/
					cellLog.append(" - ");
					cellLog.append(areaInner);
					cellLog.append(" x ");
					cellLog.append(cellInner.toToken());
					cellLog.append(":");

					if (cellInnerMU != null)
					{
						cellLog.append(cellInnerMU);
						UniformDistribution cma = cellInnerMU.mul(areaInner);
						mus = mus.sub(cma);
					}
					else
					{
						cellLog.append("undefined");
						mus.setLower(0.0);
					}
				}
			}
			cellLog.append(" ) / ");
			double areaOuter = CellSessionBean.getIntersectionArea(thisField,cellOuter);
			cellLog.append(areaOuter);
			mus=mus.div(areaOuter);
			cellLog.insert(0," = ");
			
			mus.clampLower(0.0);
			cellLog.insert(0,mus);

			CellMUEntity cellmu = em.find(CellMUEntity.class, cellOuter.id());
			/*
			UniformDistribution cellOuterMU = null;
			if (cellmu!=null)
				cellOuterMU = cellmu.getDistribution();
			*/
			//UniformDistribution cellOuterMU = getMU(cellOuter);
			cellLog.insert(0," => ");
			if (cellmu==null)
				cellLog.insert(0,"null");
			else
				cellLog.insert(0,cellmu.getDistribution().toString());
			cellLog.insert(0,": ");
			cellLog.insert(0,cellOuter.toToken());
			//System.out.println(cellLog.toString());

			// refine UD:cello with mus

			if (cellmu == null)
			{
				//cellOuterMU = mus;
				CellMUEntity cellmuNew = new CellMUEntity();
				cellmuNew.setId(cellOuter);
				cellmuNew.setDistribution(mus);
				em.persist(cellmuNew);
				S2LatLng cellp = cellOuter.toLatLng();
				//System.out.println(cellLog.toString());
				System.out.println("NEW " + cellLog.toString() + " https://www.ingress.com/intel?z=15&ll="+cellp.latDegrees()+","+cellp.lngDegrees());

				modifiedCells.add(cellOuter);
				/*				try{
				initCellQueue();
				Message msg = queueSession.createTextMessage(cellOuter.toToken());
				sender.send(msg);
				} catch (JMSException | NamingException e) {
				System.out.println(e.getMessage());
				}*/
			}
			else
			{
				try {
					UniformDistribution oldMU = new UniformDistribution(cellmu.getDistribution());
					if (cellmu.refine(mus)){  // this only returns true if the cell is modified.
						//cellmu.setDistribution();
						S2LatLng cellp = cellOuter.toLatLng();
						System.out.println("UPDATE " + cellLog.toString() + " https://www.ingress.com/intel?z=15&ll="+cellp.latDegrees()+","+cellp.lngDegrees());
						em.merge(cellmu);
						modifiedCells.add(cellOuter);
						/*						
						initCellQueue();
						Message msg = queueSession.createTextMessage(cellOuter.toToken());
						sender.send(msg);*/
						
						// add cell to modified array
						// nah, just dump it straight on the cell queue. 
						//  a xenomorph may be involved.
					}
				} catch (ArithmeticException ae) {
					// field error
					System.out.println(ae.getMessage() + " " + cellLog.toString());
					// something something, out of range error
					// mark field as invalid 
				} catch (Exception e) {
					// we should throw something?
				}
			}
		}
		//	em.getTransaction().commit(); // I hope this works. 
		// it didn't work, it threw some error about being incompatible with JTA
		// but I really think I need to control the transaction 
		
		try {
			StringBuilder fieldLog = new StringBuilder();
			boolean sendMessage=false;
			for (S2CellId cell : modifiedCells)
			{
				fieldLog.append(cell.toToken());
				fieldLog.append(": ");
				ArrayList<String> fieldGuids = dao.fieldGuidsForCell(cell);
			//System.out.println("Found " + fieldGuids.size() + " for cell " + cell.toToken());
				for (String guid : fieldGuids)
				{
					if (!guid.equals(field.getGuid()))
					{
						fieldLog.append(guid);
						fieldLog.append(", ");
						
						if (!fpCache.hasFieldGuid(guid))
						{
							fpCache.addFieldGuid(guid);
							sendMessage=true;
						}
					}
				}
				//fieldLog.append("");
			}
			if (sendMessage)
				sendMessage=fpCache.isEmpty();
			if (sendMessage)
			{
				initCellQueue();
				Message msg = queueSession.createTextMessage(""); // something to prompt the MDB
				//System.out.println("FIELDLOG: " + fieldLog.toString());
				sender.send(msg);
			}
		} catch (JMSException | NamingException e) {
			System.out.println("processField JMS Exception: " + e.getMessage());
					//	e.printStackTrace();

		}
		} catch (Exception e) {
			System.out.println("processFieldException: " + e.getMessage());
			e.printStackTrace();
		}
	}
	/**
	 * find the MU for a known field
	 *
	 * @param field the field to search for
	 *
	 * @return int of the known mu or -1 if unknown
	 */
	public int muKnownField(Field field)
	{
		try {
			for (Field f : dao.findField(field))
				return f.getMU();
		
		} catch (Exception e) {
			System.out.println("muKnownFieldException: " + e.getMessage());
			e.printStackTrace();
		}
		return -1;
	}
	
	/**
	 * Determines if a field's mu is possibly valid.
	 * Requires adequate cell data for the field being tested
	 * if not enough cell data is available this method will return true.
	 * @param s2Field The field to test.
	 * @param mu the integer value of the field. Fields have floating point mu values but are rounded to the nearest integer greater than 0
	 * @return boolean indicating if the MU specified agrees with the cell data.
	 */
	public boolean muFieldValid(S2Polygon s2Field,int mu)
	{
		UniformDistribution muest = muForField(s2Field);
		// System.out.println("muFieldValid: MU est: "+ muest);
		if (muest.getLower() == -1)
			return true;

		return muest.roundAboveZero().contains(mu);
	}
	/**
	 * Get the MU UniformDistribution for a field.
	 * @param s2Field the field to calculate its mu
	 * @return UniformDistribution of the mu.
	 */
	public UniformDistribution muForField(S2Polygon s2Field)
	{

		//S2CellUnion cells = getCellsForField(s2Field);
		HashMap<S2CellId,AreaDistribution> muArea = getIntersectionMU(s2Field);
		UniformDistribution fieldmu = new UniformDistribution(0,0);

		for (Map.Entry<S2CellId,AreaDistribution> entry : muArea.entrySet())
		{
			AreaDistribution cell = entry.getValue();
			// System.out.println("muForField: " + entry.getKey().toToken() + ": " + cell.mu);
			// do we have information for this cell
			// if not we should skip this whole thing
			if (cell.mu!=null)
			{
				UniformDistribution uniformMU = new UniformDistribution(cell.mu);
				uniformMU = uniformMU.mul(cell.area);
				fieldmu = fieldmu.add(uniformMU);
			} else {
				return new UniformDistribution(-1.0,-1.0);
			}
		}
		return fieldmu;
	}
	
	// public UniformDistribution getMU(S2CellId id) { return dao.getMU(id); }
	
}
