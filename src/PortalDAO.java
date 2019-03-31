package net.silicontrip.ingress;

import java.util.ArrayList;
import com.google.common.geometry.*;

public interface PortalDAO {

	public ArrayList<Portal> getInRegion (S2Region reg) throws PortalDAOException;
	public S2LatLng getLocation (String s) throws PortalDAOException;
	public S2LatLng getLocationFromTitle (String title) throws PortalDAOException;
	public S2LatLng getLocationFromGuid (String Guid) throws PortalDAOException;
	public S2LatLng getLocationFromLocation (long latE6, long lngE6) throws PortalDAOException;
	public void delete (String guid) throws PortalDAOException;
//	public void updateTitle(String guid, String title) throws PortalDAOException;
//	public void updateLocation(String guid, long latE6, long lngE6) throws PortalDAOException;
        public void updateFull(String guid, String title, long latE6, long lngE6,String team,int level,int resCount, int health, String image) throws PortalDAOException;
        public void update(String guid, long latE6, long lngE6, String team) throws PortalDAOException;
        public void insertFull(String guid, String title, long latE6, long lngE6,String team,int level,int resCount, int health, String image) throws PortalDAOException;
        public void insert(String guid, long latE6, long lngE6, String team) throws PortalDAOException;
        public void writeFull(String guid, String title, long latE6, long lngE6,String team,int level,int resCount, int health, String image) throws PortalDAOException;
        public void write(String guid, long latE6, long lngE6, String team) throws PortalDAOException;

}
	
