package net.silicontrip.ingress;

import javax.ejb.Stateless;
import net.silicontrip.*;
import java.util.HashMap;
import java.util.Map;
import com.google.common.geometry.*;

 
@Stateless
public class CellSessionBean implements CellSessionBeanRemote {

	private HashMap<S2CellId, UniformDistribution> cellmu = null;

	private UniformDistribution getMU(S2CellId cell)
	{
		if (cellmu != null)
			return cellmu.get(cell);
		MUCellDAO dao = new SQLMUCellDAO();
		cellmu = dao.getAll();
		return cellmu.get(cell);
	}
	public S2Polygon getS2Polygon (S2Cell cell) {
		S2Loop cellLoop = new S2Loop(cell);
		return new S2Polygon(cellLoop);
	}
	public S2Polygon getS2Polygon (S2LatLng v1,S2LatLng v2,S2LatLng v3)
	{
		S2PolygonBuilder pb = new S2PolygonBuilder(S2PolygonBuilder.Options.UNDIRECTED_UNION);
		pb.addEdge(v1.toPoint(),v2.toPoint());
		pb.addEdge(v2.toPoint(),v3.toPoint());
		pb.addEdge(v3.toPoint(),v1.toPoint());
		return pb.assemblePolygon();
	}
	public S2CellUnion getCellsForField(S2Polygon thisField)
	{
		S2RegionCoverer rc = new S2RegionCoverer();
		// ingress mu calculation specifics
		rc.setMaxLevel(13);
		rc.setMinLevel(0);
		rc.setMaxCells(20);
		return rc.getCovering (thisField);
	}
	public HashMap<S2CellId,AreaDistribution> getIntersectionMU(S2CellUnion cells,S2Polygon thisField)
	{
		HashMap<S2CellId,AreaDistribution> response = new HashMap<S2CellId,AreaDistribution>();
		for (S2CellId cell: cells)
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
}

