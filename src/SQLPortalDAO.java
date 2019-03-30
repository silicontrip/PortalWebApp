package net.silicontrip.ingress;

import java.util.ArrayList;
import com.google.common.geometry.*;

import java.sql.*;
import javax.sql.*;
import javax.naming.*;

public class SQLPortalDAO implements PortalDAO {

	public static String GET_FROM_BOX = "select guid,title,latE6,lngE6 from portals where latE6>=? and latE6<=? and lngE6>=? and lngE6<=? and deleted!=true";
	public static String GET_LOCATION_FROM_TITLE = "select guid,latE6,lngE6 from portals where title=? and deleted!=true";
	public static String GET_LOCATION_FROM_GUID = "select latE6,lngE6 from portals where guid=? and deleted!=true";
	public static String GET_LOCATION_FROM_LOCATION = "select latE6,lngE6 from portals where latE6=? and lngE6=? and deleted!=true";
	public static String UPDATE_DELETED = "update portals set deleted=true where guid=?";
	public static String UPDATE_TITLE = "update portals set title=? where guid=?";
	public static String UPDATE_LOCATION = "update portals set latE6=?,lngE6=? where guid=?";
	public static String INSERT = "insert into portals (guid,title,latE6,lngE6) values (?,?,?,?)";
	

	//private Connection spdConn = null;
	private DataSource spdDs = null;
	
	public SQLPortalDAO()  throws PortalDAOException {
		spdDs = getDataSource();
		/*
		try {
			spdConn = spdDs.getConnection();
		} catch (SQLException se) {
			throw new PortalDAOException("SQLException: " + se.getMessage());
		}
		*/
	}
	

	protected static DataSource getDataSource() throws PortalDAOException {
		try {
			InitialContext ctx = new InitialContext();
			return (DataSource) ctx.lookup("jdbc/IngressResource");
		} catch (NamingException slx) {   
			throw new PortalDAOException("NamingException while looking up DB context : " + slx.getMessage());
		}
	}

	public ArrayList<Portal> getInRegion (S2Region reg) throws PortalDAOException
	{
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<Portal> ret = new ArrayList<Portal>();
		Connection c;
      
		try {

			S2LatLngRect bound  = reg.getRectBound();
			c = spdDs.getConnection();		
			ps = c.prepareStatement(GET_FROM_BOX, ResultSet.CONCUR_READ_ONLY);
			ps.setString(1, Long.toString(bound.latLo().e6()));
			ps.setString(2, Long.toString(bound.latHi().e6()));
			ps.setString(3, Long.toString(bound.lngLo().e6()));
			ps.setString(4, Long.toString(bound.lngHi().e6()));
			rs = ps.executeQuery();

			while(rs.next()) {
				long plat = rs.getLong("latE6");
				long plng = rs.getLong("lngE6");
				S2LatLng latLng = S2LatLng.fromE6(plat,plng);
				S2Cell cell = new S2Cell(latLng); // I wonder what level cell this produces
				if (reg.contains(cell))
				{
					Portal p = new Portal (rs.getString("guid"), rs.getString("title"),plat,plng);
					ret.add(p);
				}
			}	

			rs.close();
			ps.close();
			c.close();
			return ret;
		} catch (SQLException se) {
			throw new PortalDAOException("SQLException: " + se.getMessage());
		}
	}

	public S2LatLng getLocation(String s) throws PortalDAOException
	{
	//	System.out.println(">>> getLocation: '" + s +"'");
		if (s==null)
			return null;
		if (s.matches("^[0-9a-fA-F]{32}\\.1[16]$"))
			return getLocationFromGuid(s);
		if (s.matches("(\\+|-)?([0-9]+(\\.[0-9]+)),(\\+|-)?([0-9]+(\\.[0-9]+))"))
		{
			String[] ll = s.split(",");
			Double lat = Double.parseDouble(ll[0]);
			Double lng = Double.parseDouble(ll[1]);
			long latE6 = Math.round(lat * 1000000);
			long lngE6 = Math.round(lng * 1000000);

			return getLocationFromLocation(latE6,lngE6);
		}
		return getLocationFromTitle(s);	
	}

	public S2LatLng getLocationFromTitle (String title) throws PortalDAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		S2LatLng ret = null;
      
		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(GET_LOCATION_FROM_TITLE, ResultSet.CONCUR_READ_ONLY);
			ps.setString(1, title);
			rs = ps.executeQuery();

				boolean searchError = false;
				String err = "" +title +": ";
				while(rs.next()) {
//		System.out.println(">>> getLocationFromTitle: results next");
					if (ret != null)
					{
						searchError = true;
						err = err + ret.toStringDegrees() + " ";
					}
						
					long plat = rs.getLong("latE6");
					long plng = rs.getLong("lngE6");
					ret = S2LatLng.fromE6(plat,plng);
					err = err +  rs.getString("guid") + " " ;
				}	
				if (searchError)
				{
					err = err + ret.toStringDegrees() ;
					throw new PortalDAOException("Ambiguous Title: " + err);
				}

//		System.out.println(">>> getLocationFromTitle: close return");
			rs.close();
			ps.close();
			c.close();
			return ret;
		} catch (SQLException se) {
			throw new PortalDAOException("SQLException: " + se.getMessage());
		}
	}
	public S2LatLng getLocationFromGuid (String guid) throws PortalDAOException
	{
		System.out.println(">>> getLocationFromGuid: " + guid);
		PreparedStatement ps = null;
		ResultSet rs = null;
		S2LatLng ret = null;
		Connection c = null;
      
		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(GET_LOCATION_FROM_GUID, ResultSet.CONCUR_READ_ONLY);
			ps.setString(1, guid);
			rs = ps.executeQuery();
			if(rs.first()) {
				long plat = rs.getLong("latE6");
				long plng = rs.getLong("lngE6");
				ret = S2LatLng.fromE6(plat,plng);
			}	

			rs.close();
			ps.close();
			c.close();
			return ret;
		} catch (SQLException se) {
			throw new PortalDAOException("SQLException: " + se.getMessage());
		}
	}
	public S2LatLng getLocationFromLocation (long latE6, long lngE6) throws PortalDAOException
	{
		return S2LatLng.fromE6(latE6,lngE6);
/*
		System.out.println(">>> getLocationFromLocation: " + latE6 + ", " + lngE6);
		PreparedStatement ps = null;
		ResultSet rs = null;
		S2LatLng ret = null;
		Connection c = null;
      
		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(GET_LOCATION_FROM_LOCATION, ResultSet.CONCUR_READ_ONLY);
			ps.setString(1, Long.toString(latE6));
			ps.setString(2, Long.toString(lngE6));
			rs = ps.executeQuery();
			if(rs.first()) {
				long plat = rs.getLong("latE6");
				long plng = rs.getLong("lngE6");
				ret = S2LatLng.fromE6(plat,plng);
			}	

			rs.close();
			ps.close();
			c.close();
			return ret;
		} catch (SQLException se) {
			throw new PortalDAOException("SQLException: " + se.getMessage());
		}
*/
	}
	public void delete (String guid) throws PortalDAOException
	{
		PreparedStatement ps = null;
		Connection c = null;
		int rs;
      
		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(UPDATE_DELETED);
			ps.setString(1, guid);
			rs = ps.executeUpdate();

			ps.close();
			c.close();
		} catch (SQLException se) {
			throw new PortalDAOException("SQLException: " + se.getMessage());
		}
	}
        public void updateTitle(String guid, String title) throws PortalDAOException
	{
		if (title.length() > 0) {
			PreparedStatement ps = null;
			int rs;
			Connection c = null;
	      
			try {
				c = spdDs.getConnection();
				ps = c.prepareStatement(UPDATE_TITLE);
				ps.setString(1, title);
				ps.setString(2, guid);
				rs = ps.executeUpdate();

				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new PortalDAOException("SQLException: " + se.getMessage());
			}
		}
	}
        public void updateLocation(String guid, long latE6, long lngE6) throws PortalDAOException
	{
		PreparedStatement ps = null;
		Connection c = null;
		int rs;
      
		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(UPDATE_LOCATION);
			ps.setString(1, Long.toString(latE6));
			ps.setString(2, Long.toString(lngE6));
			ps.setString(3, guid);
			rs = ps.executeUpdate();

			ps.close();
			c.close();
		} catch (SQLException se) {
			throw new PortalDAOException("SQLException: " + se.getMessage());
		}
	}

        public void insert(String guid, String title, long latE6, long lngE6) throws PortalDAOException
	{
		PreparedStatement ps = null;
		Connection c=null;
		int rs;
      
		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(INSERT);
			ps.setString(1, guid);
			ps.setString(2, title);
			ps.setString(3, Long.toString(latE6));
			ps.setString(4, Long.toString(lngE6));
			rs = ps.executeUpdate();

			ps.close();
			c.close();
		} catch (SQLException se) {
			throw new PortalDAOException("SQLException: " + se.getMessage());
		}
	}

}
	
