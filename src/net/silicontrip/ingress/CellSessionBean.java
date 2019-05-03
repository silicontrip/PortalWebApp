/**
* The CellSessionBean contains all code for working with fields and cells
*
* @author  Silicon Tripper
* @version 1.0
* @since   2019-03-31
*/
package net.silicontrip.ingress;

import com.google.common.geometry.*;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import net.silicontrip.UniformDistribution;

@Stateless
@LocalBean
public class CellSessionBean {

	@PersistenceContext(unitName="net.silicontrip.ingress.persistence")
	EntityManager em;


	@EJB
	private MUSessionBean muBean;
	
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
	 * Get the area of the intersection between a polygon and cell.
	 * @param field the field polygon
	 * @param cell the cell polygon
	 * @return Double of the area in km^2.
	 */
	
	public static Double getIntersectionArea(S2Polygon field, S2CellId cell)
	{
		S2Polygon intPoly = new S2Polygon();
		S2Polygon cellPoly = getS2Polygon(new S2Cell(cell));
		intPoly.initToIntersection(field, cellPoly);
		return intPoly.getArea() * 6367 * 6367 ;
	}

	public void createCellsForField (S2Polygon field)
	{
		S2CellUnion cells = getCellsForField(field);
		for (S2CellId cellid : cells)
		{
			System.out.println("CHECK FOR: " + cellid.toToken());
			CellMUEntity cmu = em.find(CellMUEntity.class, cellid.toToken());
			if (cmu == null)
			{
				try {
					cmu = new CellMUEntity(cellid.toToken());
					em.persist(cmu);
					em.flush();
				} catch (PersistenceException e) {
					// dc;dn  don't care, do nothing.
				}
			}
		}
	}
	
	
}
