package net.silicontrip.ingress;

import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2CellUnion;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2LatLngRect;
import java.util.ArrayList;
import java.util.HashMap;
import net.silicontrip.UniformDistribution;

// should split the pure db queries from the business logic
// would make it easier to replace the data source with a different db engine

public interface EntityDAO {

//LINKS
	public ArrayList<Link> getLinksInRect (S2LatLngRect reg) throws EntityDAOException;
	public Link getLinkGuid(String guid) throws EntityDAOException;
	public ArrayList<Link> getLinkAll () throws EntityDAOException;
	public void purgeLink() throws EntityDAOException;
	public void deleteLink(String guid) throws EntityDAOException;
	public void insertLink(String guid,String dguid, long dlatE6, long dlngE6,String oguid, long olatE6, long olngE6, String team) throws EntityDAOException;
	public boolean existsLink(String guid) throws EntityDAOException;

// CELLS
	//public HashMap<S2CellId,UniformDistribution> getMUAll() throws EntityDAOException;
	//public void updateMUAll(HashMap<S2CellId,UniformDistribution> cellmu) throws EntityDAOException;

//FIELDS
	public boolean existsField(String guid) throws EntityDAOException;
	//public ArrayList<Field> getFieldAll() throws EntityDAOException;
	public Field getField(String guid) throws EntityDAOException;
	public ArrayList<Field> findField (Field f) throws EntityDAOException;
	public void updateFieldMU(String guid,int mu) throws EntityDAOException;
	public void deleteField(String guid) throws EntityDAOException;
	public void insertField(String creator,String agent,int mu, String guid,long timestamp,String team, String pguid1, long plat1, long plng1, String pguid2, long plat2, long plng2, String pguid3, long plat3, long plng3, boolean valid) throws EntityDAOException;
	// future expasion for joined cell table
	// public void insertCellsForField (String guid, S2CellUnion cells) throws EntityDAOException;
	// public ArrayList<String> fieldGuidsForCell(S2CellId cell) throws EntityDAOException;

//PORTALS
	//public ArrayList<Portal> getPortalsInRegion (S2Region reg) throws EntityDAOException;
	public ArrayList<Portal> getPortalsAll() throws EntityDAOException;
	public ArrayList<Portal> getPortalsInRect (S2LatLngRect bound) throws EntityDAOException;
	//public S2LatLng getPortalLocation (String s) throws EntityDAOException;
	public S2LatLng getPortalLocationFromTitle (String title) throws EntityDAOException;
	public S2LatLng getPortalLocationFromGuid (String Guid) throws EntityDAOException;
	public S2LatLng getPortalLocationFromLocation (long latE6, long lngE6) throws EntityDAOException;
	public void deletePortal (String guid) throws EntityDAOException;
//	public void updateTitle(String guid, String title) throws EntityDAOException;
//	public void updateLocation(String guid, long latE6, long lngE6) throws EntityDAOException;
	public void updatePortalFull(String guid, String title, long latE6, long lngE6,String team,int level,int resCount, int health, String image) throws EntityDAOException;
	public void updatePortal(String guid, long latE6, long lngE6, String team) throws EntityDAOException;
	public void insertPortalFull(String guid, String title, long latE6, long lngE6,String team,int level,int resCount, int health, String image) throws EntityDAOException;
	public void insertPortal(String guid, long latE6, long lngE6, String team) throws EntityDAOException;
	public void writePortalFull(String guid, String title, long latE6, long lngE6,String team,int level,int resCount, int health, String image) throws EntityDAOException;
	public void writePortal(String guid, long latE6, long lngE6, String team) throws EntityDAOException;

	//public UniformDistribution getMU(S2CellId cell);

}
