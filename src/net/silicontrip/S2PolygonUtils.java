/**
* The CellSessionBean contains all code for working with fields and cells
*
* @author  Silicon Tripper
* @version 1.0
* @since   2025-08-21
*/
package net.silicontrip;

import com.google.common.geometry.*;
import java.util.HashMap;

public class S2PolygonUtils {
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
     * Assembles a Triangular S2Polygon from a field string array object.
     * @param flist String Array field object
     * @return S2Polygon of the Triangle
     */
    public static S2Polygon getS2Polygon (String[] flist)
    {
        int lat1 = Integer.valueOf(flist[7].trim());
        int lng1 = Integer.valueOf(flist[8].trim());
        int lat2 = Integer.valueOf(flist[10].trim());
        int lng2 = Integer.valueOf(flist[11].trim());
        int lat3 = Integer.valueOf(flist[13].trim());
        int lng3 = Integer.valueOf(flist[14].trim());

        return getS2Polygon(lat1,lng1,lat2,lng2,lat3,lng3);
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

    public static S2CellUnion getCells(S2Polygon s2p)
    {
        S2RegionCoverer rc = new S2RegionCoverer();
        // ingress mu calculation specifics
        rc.setMaxLevel(13);
        rc.setMinLevel(0);
        rc.setMaxCells(20);
        return rc.getCovering (s2p);
    }

    static public HashMap<S2CellId,Double> getCellIntersection(S2CellUnion cells, S2Polygon fieldPoly)
    {

        HashMap<S2CellId,Double> polyArea = new HashMap<S2CellId,Double>();
        for (S2CellId cellid: cells)
        {
            S2Cell cell = new S2Cell(cellid);
            // I think there is a way to turn a cell into an S2Loop which can be turned into an s2polygon
            S2PolygonBuilder pb = new S2PolygonBuilder(S2PolygonBuilder.Options.UNDIRECTED_UNION);
            pb.addEdge(cell.getVertex(0),cell.getVertex(1));
            pb.addEdge(cell.getVertex(1),cell.getVertex(2));
            pb.addEdge(cell.getVertex(2),cell.getVertex(3));
            pb.addEdge(cell.getVertex(3),cell.getVertex(0));
            S2Polygon cellPoly = pb.assemblePolygon();
            S2Polygon intPoly = new S2Polygon();
            intPoly.initToIntersection(fieldPoly, cellPoly);

            polyArea.put(cellid,intPoly.getArea());
        }
        return polyArea;
        
    }

}