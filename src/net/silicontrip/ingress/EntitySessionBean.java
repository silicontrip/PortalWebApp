/**
* The EntitySessionBean handles all the requests for DAO entities
*
* @author  Silicon Tripper
* @version 1.0
* @since   2019-03-31
*/
package net.silicontrip.ingress;

import java.util.ArrayList;

import javax.ejb.Stateless;
import com.google.common.geometry.*;
import javax.ejb.EJB;
import javax.ejb.LocalBean;

@Stateless
@LocalBean
public class EntitySessionBean {

	@EJB
	private SQLEntityDAO dao;

	
/**
 * get the S2LatLng location of a portal description.
 *
 *This is a conditional logic method which compares the description with a few known formats
 *and calls the appropriate method to determine the location.
 *
 * @param s the description of a portals location.
 *
 * @return S2LatLng of the location.
 *
 */
	public S2LatLng getPortalLocation(String s)
	{
		if (s==null)
			return null;
		if (s.matches("^[0-9a-fA-F]{32}\\.1[16]$"))
			return dao.getPortalLocationFromGuid(s);
		if (s.matches("(\\+|-)?([0-9]+(\\.[0-9]+)),(\\+|-)?([0-9]+(\\.[0-9]+))"))
		{
			String[] ll = s.split(",");
			Double lat = Double.parseDouble(ll[0]);
			Double lng = Double.parseDouble(ll[1]);
			long latE6 = Math.round(lat * 1000000);
			long lngE6 = Math.round(lng * 1000000);

			return S2LatLng.fromE6(latE6,lngE6);
		
			//return getDAO().getPortalLocationFromLocation(latE6,lngE6);
		}
		return dao.getPortalLocationFromTitle(s);
	}
	/**
 * Gets all Portals within an S2Region.
 *
 * @param reg S2Region describing the area which contains the portals
 *
 * @return ArrayList of the portals
 *
 */
	public ArrayList<Portal> getPortalInRegion(S2Region reg)
	{
		ArrayList<Portal> ret = new ArrayList<Portal>();

		ArrayList<Portal> rectPortals = dao.getPortalsInRect(reg.getRectBound());
		for (Portal p : rectPortals) {
			long plat = p.getLatE6();
			long plng = p.getLngE6();
			
			S2LatLng latLng = p.getS2LatLng();
			S2Cell cell = new S2Cell(latLng); // I wonder what level cell this produces
			if (reg.contains(cell))
			{
				ret.add(p);
			}
		}
		return ret;
	}
	
	public ArrayList<Link> getLinkInRect(S2LatLngRect rect)
	{
		ArrayList<Link> all  = dao.getLinkAll();
		ArrayList<Link> ret = new ArrayList<Link>();
		// should I just copy the getAll method to save memory.
		for (Link l : all)
		{
			S2LatLngRect lb = l.getBounds();
			if (rect.contains(lb) || rect.intersects(lb))
				ret.add(l);
		}
		return ret;
	}
	public ArrayList<Link> getLinkAll()
	{
			return dao.getLinkAll();
	}
	

}
