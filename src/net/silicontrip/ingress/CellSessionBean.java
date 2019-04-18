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
import javax.ejb.Stateless;
import javax.jms.*;
import javax.naming.*;
import net.silicontrip.AreaDistribution;
import net.silicontrip.UniformDistribution;

@Stateless

public class CellSessionBean {

	private HashMap<S2CellId, UniformDistribution> cellmu = null;
	private EntityDAO dao = null;
	/** threshold for MU rounding */
	public double range = 0.5; 
        private QueueConnectionFactory qcf = null;
        private InitialContext ctx = null;
        private QueueConnection queueCon = null;
        private QueueSession queueSession = null;
        private Queue submitQueue = null;
	private QueueSender sender = null;

	private void initCellQueue() throws NamingException, JMSException  {
		if (ctx == null) 
		{
			ctx = new InitialContext();
                        qcf = (QueueConnectionFactory) ctx.lookup("jms/QueueConnectionFactory");
                        queueCon = qcf.createQueueConnection();
                        queueSession = queueCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			submitQueue = (Queue)ctx.lookup("jms/cellQueue");
			sender = queueSession.createSender(submitQueue);
		}
	}

	private EntityDAO getDAO() {
		if (dao==null)
			dao = new SQLEntityDAO();
		return dao;
	}

	/**
   * This will return the mu uniform distribution for
   * the requested S2CellId.  The S2CellId HashMap is quite small
   * so is cached here for speed, rather than querying the DAO each time.
   * @param cell The S2CellId for which the MU is required
   * @return UniformDistribution of the MU. May be null if no information
   * exists for the specified cell.
   */

	public UniformDistribution getMU(S2CellId cell) {
		if (cellmu != null)
			return cellmu.get(cell);

		cellmu = getDAO().getMUAll();
		return cellmu.get(cell);
	}
	/**
	 * Returns an S2Polygon of the requested cell.
   * @param cell The S2CellId for which the S2Polygon is required.
   * @return S2Polygon of the requested cell.
   */
	public static S2Polygon getS2Polygon (S2Cell cell) {
		S2Loop cellLoop = new S2Loop(cell);
		return new S2Polygon(cellLoop);
	}
	/**
	 * Assembles a Triangular S2Polygon for the specified points.
	 * The points are in integer E6 format (divide by 1E6 (1,000,000) to get
	 * floating point coordinates)
	 * @param lat1 The Latitude of point 1 in E6 format
	 * @param lng1 The Longitude of point 1 in E6 format
	 * @param lat2 The Latitude of point 2 in E6 format
	 * @param lng2 The Longitude of point 2 in E6 format
	 * @param lat3 The Latitude of point 3 in E6 format
	 * @param lng3 The Longitude of point 3 in E6 format
	 * @return S2Polygon of the triangle
	 */
	public static S2Polygon getS2Polygon (long lat1, long lng1, long lat2, long lng2, long lat3, long lng3)
	{
		return getS2Polygon( S2LatLng.fromE6(lat1,lng1), S2LatLng.fromE6(lat2,lng2), S2LatLng.fromE6(lat3,lng3));
	}
	/**
	 * Assembles a Triangular S2Polygon from S2LatLng points.
	 * @param v1 S2LatLng of point 1
	 * @param v2 S2LatLng of point 2
	 * @param v3 S2LatLng of point 3
	 * @return S2Polygon of the Triangle
	 */
	public static S2Polygon getS2Polygon (S2LatLng v1,S2LatLng v2,S2LatLng v3)
	{
		return getS2Polygon(v1.toPoint(), v2.toPoint(), v3.toPoint());
	}
	/**
	 * Assembles a triangular S2Polygon from S2Point points
	 * This method uses the S2PolygonBuilder of an Undirected Union
	 * so that the points may be in any winding order (CW or CCW) and
	 * the resulting polygon is always smaller than the region is doesn't cover.
	 * @param v1 S2Point of point 1
	 * @param v2 S2Point of point 2
	 * @param v3 S2Point of point 3
	 * @return S2Polygon of the Triangle
	 */
	public static S2Polygon getS2Polygon (S2Point v1,S2Point v2,S2Point v3)
	{
		S2PolygonBuilder pb = new S2PolygonBuilder(S2PolygonBuilder.Options.UNDIRECTED_UNION);
		pb.addEdge(v1,v2);
		pb.addEdge(v2,v3);
		pb.addEdge(v3,v1);
		return pb.assemblePolygon();
	}
	/**
	 * Generates a list of cells for the specified polygon.
	 * Uses Ingress specific values for the cell covering generation.
	 * @param thisField The S2Polygon of the region to be covered
	 * @return S2CellUnion of the cells
	 */
	public static S2CellUnion getCellsForField(S2Polygon thisField)
	{
		S2RegionCoverer rc = new S2RegionCoverer();
		// ingress mu calculation specifics
		rc.setMaxLevel(13);
		rc.setMinLevel(0);
		rc.setMaxCells(20);
		return rc.getCovering (thisField);
	}
	/**
	 * Generates an intersection map of a Field and its cells.
	 * Returns a HashMap of the S2CellId and its area in Km^2 and the min and max MU for that cell.
	 * @param thisField the S2Polygon
	 * @return HashMap containing the S2CellId with its Area and Uniform Distribution
	 */
	public HashMap<S2CellId,AreaDistribution> getIntersectionMU(S2Polygon thisField)
	{

		HashMap<S2CellId,AreaDistribution> response = new HashMap<S2CellId,AreaDistribution>();

		for (S2CellId cell: getCellsForField(thisField))
		{
		//System.out.println("getIntersectionMU: " + cell.toToken());
			S2Polygon intPoly = new S2Polygon();
			S2Polygon cellPoly = getS2Polygon(new S2Cell(cell));

			intPoly.initToIntersection(thisField, cellPoly);
			AreaDistribution ad = new AreaDistribution();
			ad.area = intPoly.getArea() * 6367 * 6367 ;
			ad.mu = getMU(cell);

			response.put(cell,ad);
		}
		return response;
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
		for (Field f : getDAO().findField(field))
			return f.getMU();
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
			//System.out.println(cell);
			// do we have information for this cell
			// if not we should skip this whole thing
			if (cell.mu!=null)
			{
				UniformDistribution uniformMU = new UniformDistribution(cell.mu);
				uniformMU = uniformMU.mul(cell.area);
				fieldmu = fieldmu.add(uniformMU);
			} else {
				return new UniformDistribution(-1,-1);
			}
		}
		return fieldmu;
	}
	/**
	 * Checks the DAO to see if this field exists.
	 * @param guid the guid of the field to check
	 * @return boolean if the field exists or not
	 */
	public boolean hasFieldGuid(String guid)
	{
		return getDAO().existsField(guid);
	}

	/**
	 * inserts the field using the DAO.
	 * @param field the field entity to insert
	 * @param valid the mu validity flag
	 */
	public void submitField(Field field, boolean valid)
	{
		getDAO().insertField(field.getCreator(),
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
		getDAO().insertCellsForField(field.getGuid(),fieldCells);
		// too easy?
		// should this be here or in the caller?
		//System.out.println("Valid for process? " + valid);
		if (valid)
		{
		//	System.out.println("Process Field...");
			processField(field);
		}
	}

	private static Double getIntersectionArea(S2Polygon field, S2CellId cell)
	{
		S2Polygon intPoly = new S2Polygon();
		S2Polygon cellPoly = getS2Polygon(new S2Cell(cell));
		intPoly.initToIntersection(field, cellPoly);
		return intPoly.getArea() * 6367 * 6367 ;
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
		Integer score = field.getMU();
		S2Polygon thisField = field.getS2Polygon();
		S2CellUnion cells = getCellsForField(thisField);
		Double area;

/*
		System.out.print ("Processing Cells: ");
		for (S2CellId cello: cells) 
			System.out.print (cello.toToken() + ", ");
		System.out.println(".");
*/


		UniformDistribution initialMU = new UniformDistribution(score,range);

		// an algorithmic version of the following equation
		// mu[cell] = ( MU - intersectionArea[cell1] x mu[cell1] ... - intersectionArea[cellN] x mu[cellN]) / intersectionArea[cell]

		for (S2CellId cellOuter: cells) {
			StringBuilder cellLog = new StringBuilder();
			UniformDistribution mus = new UniformDistribution(initialMU);
			cellLog.append("( ");
			cellLog.append(mus.toString());
			for (S2CellId cellInner: cells) {
				// if not cell from outer loop
				if (!cellOuter.equals(cellInner))
				{
					double areaInner = getIntersectionArea(thisField,cellInner);
					UniformDistribution cellInnerMU = getMU(cellInner);

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
			double areaOuter = getIntersectionArea(thisField,cellOuter);
			cellLog.append(areaOuter);
			mus=mus.div(areaOuter);
			cellLog.insert(0," = ");
			cellLog.insert(0,mus);
			mus.clampLower(0.0);


			UniformDistribution cellOuterMU = getMU(cellOuter);
			cellLog.insert(0," <=> ");
			cellLog.insert(0,cellOuterMU.toString());
			cellLog.insert(0,": ");
			cellLog.insert(0,cellOuter.toToken());
		//	System.out.println(cellLog.toString());

			// refine UD:cello with mus

			if (cellOuterMU == null)
			{
				cellOuterMU = mus;
			}
			else
			{
				try {
					if (cellOuterMU.refine(mus)){
						System.out.println("" + cellOuter.toToken() + "->" + cellOuterMU.toString());
						initCellQueue();
						Message msg = queueSession.createTextMessage(cellOuter.toToken());
						sender.send(msg);
						// add cell to modified array
						// nah, just dump it straight on the cell queue. (might be a bit premature MU hasn't been saved)
					}
				} catch (ArithmeticException | JMSException | NamingException e) {
					; // something something, out of range error
				}
			}

			// end refine UD:cello
			//multi.put(cello,cellomu);
		}

		// push modified cell array into jms/cellQueue
		
	/*
		for (S2CellId cell: multi.keySet())
		{
			response.put(cell.toToken(), multi.get(cell).getArrayList());
			putMU(cell, multi.get(cell));
		}
		return response;
		*/
	}
}

