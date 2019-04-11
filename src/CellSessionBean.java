/**
* The CellSessionBean contains all code for working with fields and cells
*
* @author  Silicon Tripper
* @version 1.0
* @since   2019-03-31 
*/
package net.silicontrip.ingress;

import javax.ejb.Stateless;
import net.silicontrip.*;
import java.util.HashMap;
import java.util.Map;
import com.google.common.geometry.*;

@Stateless

public class CellSessionBean {

	private HashMap<S2CellId, UniformDistribution> cellmu = null;

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
		MUCellDAO dao = new SQLMUCellDAO();
		cellmu = dao.getAll();
		return cellmu.get(cell);
	}
	/**
	 * Returns an S2Polygon of the requested cell.
   * @param cell The S2CellId for which the S2Polygon is required.
   * @return S2Polygon of the requested cell.
   */
	public S2Polygon getS2Polygon (S2Cell cell) {
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
	public S2Polygon getS2Polygon (long lat1, long lng1, long lat2, long lng2, long lat3, long lng3)
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
	public S2Polygon getS2Polygon (S2LatLng v1,S2LatLng v2,S2LatLng v3)
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
	public S2Polygon getS2Polygon (S2Point v1,S2Point v2,S2Point v3)
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
	public S2CellUnion getCellsForField(S2Polygon thisField)
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
	 * Determines if a field's mu is possibly valid.
	 * Requires adequate cell data for the field being tested
	 * if not enough cell data is available this method will return true.
	 * @param s2Field The field to test.
	 * @param mu the integer value of the field. Fields have floating point mu values but are rounded to the nearest integer greater than 0
	 * @return boolean indicating if the MU specified agrees with the cell data.
	 */
	public boolean fieldMUValid(S2Polygon s2Field,int mu)
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

		S2CellUnion cells = getCellsForField(s2Field);
		HashMap<S2CellId,AreaDistribution> muArea = getIntersectionMU(cells,s2Field);
		UniformDistribution fieldmu = new UniformDistribution(0,0);

		for (Map.Entry<S2CellId,AreaDistribution> entry : muArea.entrySet())
		{
			AreaDistribution cell = entry.getValue();
			//System.out.println(cell);
			// do we have information for this cell
			// if not we should skip this whole thing
			if (cell.mu!=null)
			{
				UniformDistribution cellmu = new UniformDistribution(cell.mu);
				cellmu = cellmu.mul(cell.area);
				fieldmu = fieldmu.add(cellmu);
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
		MUFieldDAO dao = new SQLMUFieldDAO();
		return dao.exists(guid);
	}
	
}

