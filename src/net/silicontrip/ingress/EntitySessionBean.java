/**
* The EntitySessionBean handles all the requests for DAO entities
*
* @author  Silicon Tripper
* @version 1.0
* @since   2019-03-31
*/
package net.silicontrip.ingress;

import java.util.ArrayList;
import java.util.Collection;

import com.google.common.geometry.*;
import jakarta.ejb.Stateless;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
@LocalBean
public class EntitySessionBean {

	// migrate away from DAO/SQL
	@EJB
	private SQLEntityDAO dao;

        @PersistenceContext(unitName="net.silicontrip.ingress.persistence")
        private EntityManager em;

	
/**
 * get the S2LatLng location of a portal description.
 *
 *This is a conditional logic method which compares the description with a few known formats
 *and calls the appropriate method to determine the location.
 *
 * @param s the description of a portals location.
 *
 * @return S2LatLng of the location.
 * @throws EntityDAOException if there is a problem locating the portal
 */
	public S2LatLng getPortalLocation(String s) throws EntityDAOException
	{
		S2LatLng result = null;
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
		result =  dao.getPortalLocationFromTitle(s);
		if (result == null)
			throw new EntityDAOException("Portal Title not found: " + s);

		return result;

	}
	
	/**
 * get the S2LatLng location of a portal description.
 *
 *This is a conditional logic method which compares the description with a few known formats
 *and calls the appropriate method to determine the location.
 *
 * @param s the description of a portals location. title, lat/lng or guid
 *
 * @return the Portal object.
 * @throws EntityDAOException if there is a problem locating the portal
 */
	public Portal getPortal(String s) throws EntityDAOException
	{
		Portal result = null;
		if (s==null)
			return null;
		if (s.matches("^[0-9a-fA-F]{32}\\.1[16]$"))
			return dao.getPortalFromGuid(s);
		if (s.matches("(\\+|-)?([0-9]+(\\.[0-9]+)),(\\+|-)?([0-9]+(\\.[0-9]+))"))
		{
			// not implemented
			return null;
			//return getDAO().getPortalLocationFromLocation(latE6,lngE6);
		}
		result =  dao.getPortalFromTitle(s);
		if (result == null)
			throw new EntityDAOException("Portal Title not found: " + s);

		return result;

	}

	public ArrayList<Portal> getPortalAll() throws EntityDAOException
	{
		return dao.getPortalsAll();
	}
	
	/**
 * Gets all Portals within an S2Region.
 *
 * @param reg S2Region describing the area which contains the portals
 *
 * @return ArrayList of the portals
 * @throws EntityDAOException if a problem occurs finding portal
 */
	public ArrayList<Portal> getPortalInRegion(S2Region reg) throws EntityDAOException
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

		Collection<Link> all  = getLinkAll();
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
	public Collection<Link> getLinkAll()
	{
		Query queryAllLink = em.createNamedQuery("Link.findAll");
		return queryAllLink.getResultList();
	}

	public Link getLinkGuid(String guid)
	{
		return em.find(Link.class, guid);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void removeLink(Link link) { 
		Link tempLink = getLinkGuid(link.getGuid());
		em.remove(tempLink); 
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void insertLink(Link link) { em.persist(link); }	
	public boolean existsLinkGuid(String guid)
	{
		return (getLinkGuid(guid)!=null);
	}

}
