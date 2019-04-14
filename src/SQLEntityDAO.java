/*
mysql> desc links;
+---------+-------------+------+-----+---------+-------+
| Field   | Type        | Null | Key | Default | Extra |
+---------+-------------+------+-----+---------+-------+
| guid    | varchar(36) | NO   | PRI | NULL    |       |
| d_guid  | varchar(36) | NO   | MUL | NULL    |       |
| d_latE6 | int(12)     | NO   |     | NULL    |       |
| d_lngE6 | int(12)     | NO   |     | NULL    |       |
| o_guid  | varchar(36) | NO   | MUL | NULL    |       |
| o_latE6 | int(12)     | NO   |     | NULL    |       |
| o_lngE6 | int(12)     | NO   |     | NULL    |       |
| team    | varchar(16) | NO   |     | NULL    |       |
+---------+-------------+------+-----+---------+-------+

mysql> desc mucell;
+---------+------------+------+-----+---------+-------+
| Field   | Type       | Null | Key | Default | Extra |
+---------+------------+------+-----+---------+-------+
| cellid  | varchar(8) | NO   | PRI |         |       |
| mu_low  | double     | NO   |     | NULL    |       |
| mu_high | double     | NO   |     | NULL    |       |
+---------+------------+------+-----+---------+-------+

mysql> desc mufields;
+-----------+-------------+------+-----+-------------------+-----------------------------+
| Field     | Type        | Null | Key | Default           | Extra                       |
+-----------+-------------+------+-----+-------------------+-----------------------------+
| creator   | varchar(36) | YES  |     | NULL              |                             |
| agent     | varchar(36) | NO   |     | NULL              |                             |
| mu        | int(8)      | NO   |     | NULL              |                             |
| guid      | char(36)    | NO   | PRI | NULL              |                             |
| timestamp | int(14)     | NO   |     | CURRENT_TIMESTAMP | on update CURRENT_TIMESTAMP |
| team      | char(1)     | YES  |     | NULL              |                             |
| pguid1    | char(36)    | YES  |     | NULL              |                             |
| plat1     | int(12)     | NO   |     | NULL              |                             |
| plng1     | int(12)     | NO   |     | NULL              |                             |
| pguid2    | char(36)    | YES  |     | NULL              |                             |
| plat2     | int(12)     | NO   |     | NULL              |                             |
| plng2     | int(12)     | NO   |     | NULL              |                             |
| pguid3    | char(36)    | YES  |     | NULL              |                             |
| plat3     | int(12)     | NO   |     | NULL              |                             |
| plng3     | int(12)     | NO   |     | NULL              |                             |
| valid     | tinyint(1)  | YES  |     | NULL    |       |
+-----------+-------------+------+-----+-------------------+-----------------------------+

mysql> desc portals;
+---------------+--------------+------+-----+---------+-------+
| Field         | Type         | Null | Key | Default | Extra |
+---------------+--------------+------+-----+---------+-------+
| guid          | varchar(36)  | NO   | PRI | NULL    |       |
| title         | text         | YES  |     | NULL    |       |
| latE6         | int(12)      | NO   |     | NULL    |       |
| lngE6         | int(12)      | NO   |     | NULL    |       |
| team          | varchar(16)  | NO   |     | NULL    |       |
| level         | int(1)       | YES  |     | NULL    |       |
| res_count     | int(1)       | YES  |     | NULL    |       |
| health        | int(3)       | YES  |     | NULL    |       |
| time_lastseen | int(16)      | YES  |     | NULL    |       |
| deleted       | bit(1)       | YES  |     | b'0'    |       |
| image         | varchar(160) | YES  |     | NULL    |       |
+---------------+--------------+------+-----+---------+-------+

*/

package net.silicontrip.ingress;

import java.util.ArrayList;
import com.google.common.geometry.*;

import java.sql.*;
import javax.sql.*;
import javax.naming.*;

import net.silicontrip.UniformDistribution;
import java.util.HashMap;
import java.util.Map;

public class SQLEntityDAO implements EntityDAO {

	private Connection c = null;
	private DataSource spdDs = null;

	protected static String LINK_GET_ALL = "select * from links";
	protected static String LINK_GET_GUID = "select * from links where guid=?";
	protected static String LINK_PURGE = "delete from links";
	protected static String LINK_DELETE= "delete from links where guid=?";
	protected static String LINK_INSERT= "insert into links (guid,d_guid,d_latE6,d_lngE6,o_guid,o_latE6,o_lngE6,team) values (?,?,?,?,?,?,?,?)";

	protected static String MUCELL_GET_ALL = "Select * from mucell";
	protected static String MUCELL_GET_CELLID = "Select * from mucell where cellid=?";
	protected static String MUCELL_UPDATE = "update mucell set mu_low=?,mu_high=? where cellid=?";
	protected static String MUCELL_INSERT = "insert into mucell (cellid,mu_low,mu_high) values (?,?,?)";

	protected static String FIELD_GET_GUID = "select * from mufields where guid=?";
	protected static String FIELD_UPDATE_MU = "update mufields set mu=? where guid=?";
	protected static String FIELD_DELETE = "delete from mufields where guid=?";
	protected static String FIELD_INSERT = "insert into mufields (creator,agent,mu,guid,timestamp,team,pguid1,plat1,plng1,pguid2,plat2,plng2,pguid3,plat3,plng3,valid) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	protected static String FIELD_FIND = "select * from mufields where (((plat1=? and plng1=?) or (plat2=? and plng2=?) or (plat3=? and plng3=?)) and ((plat1=? and plng1=?) or (plat2=? and plng2=?) or (plat3=? and plng3=?)) and ((plat1=? and plng1=?) or (plat2=? and plng2=?) or (plat3=? and plng3=?))) and valid=true";

	protected static String FIELD_INSERT_CELLS = "insert into fieldcells (field_guid,cellid) values (?,?)";
	protected static String FIELD_FIND_FROM_CELL = "select guid from fieldcells where cellid=?";

	protected static String PORTAL_GET_FROM_BOX = "select guid,title,latE6,lngE6 from portals where latE6>=? and latE6<=? and lngE6>=? and lngE6<=? and deleted!=true";
	protected static String PORTAL_GET_LOCATION_FROM_TITLE = "select guid,latE6,lngE6 from portals where title=? and deleted!=true";
	protected static String PORTAL_GET_LOCATION_FROM_GUID = "select latE6,lngE6 from portals where guid=? and deleted!=true";
	protected static String PORTAL_GET_LOCATION_FROM_LOCATION = "select latE6,lngE6 from portals where latE6=? and lngE6=? and deleted!=true";
	protected static String PORTAL_UPDATE_DELETED = "update portals set deleted=true where guid=?";
	//protected static String PORTAL_UPDATE_TITLE = "update portals set title=? where guid=?";
	//protected static String PORTAL_UPDATE_LOCATION = "update portals set latE6=?,lngE6=? where guid=?";
	protected static String PORTAL_INSERT_FULL = "insert into portals (guid,title,latE6,lngE6,team,level,res_count,health,image,deleted) values (?,?,?,?,?,?,?,?,?,0)";
	protected static String PORTAL_INSERT = "insert into portals (guid,latE6,lngE6,team,deleted) values (?,?,?,?,0)";

	protected static String PORTAL_UPDATE = "update portals set latE6=?,lngE6=?,team=?,deleted=0 where guid=?";
	protected static String PORTAL_UPDATE_FULL = "update portals set title=?,latE6=?,lngE6=?,team=?,level=?,res_count=?,health=?,image=?,deleted=0 where guid=?";
	protected static String PORTAL_GUID_EXISTS =  "select guid from portals where guid=?";

	/** Constructor */
	public SQLEntityDAO() throws EntityDAOException {
		try {
			InitialContext ctx = new InitialContext();
			spdDs =  (DataSource) ctx.lookup("jdbc/IngressResource");
		} catch (NamingException slx) {
			throw new EntityDAOException("NamingException while looking up DB context : " + slx.getMessage());
		}
	}
/** Gets all the links that intersect or are contained within an S2LatLngRect
 *
 * @param reg The S2LatLngRect of the area of interest
 *
 * @return ArrayList of links
 *
 * @throws EntityDAOException
 */
	public ArrayList<Link> getLinksInRect (S2LatLngRect reg) throws EntityDAOException
	{
		ArrayList<Link> all  = getLinkAll();
		ArrayList<Link> ret = new ArrayList<Link>();
		// should I just copy the getAll method to save memory.
		for (Link l : all)
		{
			S2LatLngRect lb = l.getBounds();
			if (reg.contains(lb) || reg.intersects(lb))
				ret.add(l);
		}
		return ret;
	}
/** Gets a link for specified GUID
 *
 * @param guid The guid of the required link
 *
 * @return Link for the guid
 *
 * @throws EntityDAOException
 */
	public Link getLinkGuid(String guid) throws EntityDAOException
	{
		PreparedStatement ps = null;
		ResultSet rs = null;
		Link ret = null;

		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(LINK_GET_GUID, ResultSet.CONCUR_READ_ONLY);
			ps.setString(1,guid);
			rs = ps.executeQuery();
			while (rs.next())
			{
				long dlat = rs.getLong("d_latE6");
				long dlng = rs.getLong("d_lngE6");
				long olat = rs.getLong("o_latE6");
				long olng = rs.getLong("o_lngE6");
				ret = new Link(rs.getString("guid"),
					rs.getString("d_guid"),
					dlat,dlng,
					rs.getString("o_guid"),
					olat,olng,
					rs.getString("team"));
			}
		} catch (SQLException e) {
			throw new EntityDAOException(e.getMessage());
		} finally {
			try {
				rs.close();
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			}
		}
		return ret;
	}
/** Gets all the links in the database
 *
 * @return ArrayList of links
 *
 * @throws EntityDAOException
 */
	public ArrayList<Link> getLinkAll () throws EntityDAOException
	{
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<Link> ret = new ArrayList<Link>();

		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(LINK_GET_ALL, ResultSet.CONCUR_READ_ONLY);
			rs = ps.executeQuery();
			while (rs.next())
			{
				long dlat = rs.getLong("d_latE6");
				long dlng = rs.getLong("d_lngE6");
				long olat = rs.getLong("o_latE6");
				long olng = rs.getLong("o_lngE6");
				Link l = new Link(rs.getString("guid"),
					rs.getString("d_guid"),
					dlat,dlng,
					rs.getString("o_guid"),
					olat,olng,
					rs.getString("team"));

					ret.add(l);
			}
		} catch (SQLException e) {
			throw new EntityDAOException(e.getMessage());
		} finally {
			try {
				rs.close();
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			}
		}
		return ret;
	}
/** Erases all links in the database.
 * This was for an old implementation of the link grabber which required periodic resets.
 * The new version supports rolling updates and hopefully never needs to be reset.
 * New functionality is still being evaluated. so far really good.
 *
 * @throws EntityDAOException
 */
	public void purgeLink() throws EntityDAOException
	{
		PreparedStatement ps = null;
		int rs;

		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(LINK_PURGE); // Little Bobby '); DROP TABLES
			rs = ps.executeUpdate();
		} catch (SQLException se) {
			throw new EntityDAOException("SQLException: " + se.getMessage());
		} finally {
			try {
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			}
		}
	}
/** Deletes a link for specified GUID
 *
 * @param guid The guid of the required link
 *
 * @throws EntityDAOException
 */
	public void deleteLink(String guid) throws EntityDAOException
	{
		//System.out.println("SQLLinkDAO: delete("+guid+")");
		PreparedStatement ps = null;
		int rs;

		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(LINK_DELETE);
			ps.setString(1, guid);
			rs = ps.executeUpdate();
		} catch (SQLException se) {
			throw new EntityDAOException("SQLException: " + se.getMessage());
		} finally {
			try {
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			}
		}
	}
/** Inserts a link into the Link database
 *
 * @param guid The guid of the link
 * @param dguid The guid of the destination portal
 * @param dlatE6 The latitude of the destination portal in E6 format
 * @param dlngE6 The longitude of the destination portal in E6 format
 * @param oguid The guid of the originating portal
 * @param olatE6 The latitude of the originating portal in E6 format
 * @param olngE6 The longitude of the originating portal in E6 format
 * @param team The Faction of the link, 'E' or 'R'
 *
 * @throws EntityDAOException
 */
	public void insertLink(String guid,String dguid, long dlatE6, long dlngE6,String oguid, long olatE6, long olngE6, String team) throws EntityDAOException
	{

		if (getLinkGuid(guid) == null)
		{
			PreparedStatement ps = null;
			int rs;

			try {
				c = spdDs.getConnection();
				ps = c.prepareStatement(LINK_INSERT);
				ps.setString(1, guid);
				ps.setString(2, dguid);
				ps.setLong(3, dlatE6);
				ps.setLong(4, dlngE6);
				ps.setString(5, oguid);
				ps.setLong(6, olatE6);
				ps.setLong(7, olngE6);
				ps.setString(8, team);

				rs = ps.executeUpdate();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			} finally {
				try {
					ps.close();
					c.close();
				} catch (SQLException se) {
					throw new EntityDAOException("SQLException: " + se.getMessage());
				}
			}
		}
	}

// MU CELL
/** Gets all the CELLS:MU in the database
 * This data is small enough to be stored in an instance variable.
 *
 * @return Hashmap of S2CellId keys and UniformDistribution
 *
 * @throws EntityDAOException
 */
	public HashMap<S2CellId,UniformDistribution> getMUAll() throws EntityDAOException
	{
		PreparedStatement ps = null;
		ResultSet rs = null;
		HashMap<S2CellId,UniformDistribution> ret = new HashMap<S2CellId,UniformDistribution>();
		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(MUCELL_GET_ALL, ResultSet.CONCUR_READ_ONLY);
			rs = ps.executeQuery();
			while (rs.next())
			{
				UniformDistribution ud = new UniformDistribution(rs.getDouble("mu_low"), rs.getDouble("mu_high"));
				S2CellId cell = S2CellId.fromToken(rs.getString("cellid"));
				ret.put(cell,ud);
			}
		} catch (SQLException e) {
			throw new EntityDAOException(e.getMessage());
		} finally {
			try {
				rs.close();
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			}
		}
		return ret;
	}
/** Stores all the CELLS:MU in the database

 * @Param cellmu A HashMap with S2CellId keys and UniformDistribution mu
 *
 * @throws EntityDAOException
 */
	public void updateMUAll(HashMap<S2CellId,UniformDistribution> cellmu) throws EntityDAOException
	{
		PreparedStatement psExist = null;
		PreparedStatement psInsert = null;
		PreparedStatement psUpdate = null;
		try {
			c = spdDs.getConnection();
			psExist = c.prepareStatement(MUCELL_GET_CELLID);
			psInsert = c.prepareStatement(MUCELL_INSERT);
			psUpdate = c.prepareStatement(MUCELL_UPDATE);

			for (Map.Entry<S2CellId,UniformDistribution> entry : cellmu.entrySet())
			{
				String id =entry.getKey().toToken();
				UniformDistribution mu = entry.getValue();
				// check cell id exists?
				psExist.setString(1,id);
				ResultSet rsExist = psExist.executeQuery();
				if (rsExist.first())
				{
					// update
					psUpdate.setString(3,id);
					psUpdate.setDouble(1,mu.getLower());
					psUpdate.setDouble(2,mu.getUpper());
					psUpdate.executeUpdate();
				} else {
					// insert
					psInsert.setString(1,id);
					psInsert.setDouble(2,mu.getLower());
					psInsert.setDouble(3,mu.getUpper());
					psInsert.executeUpdate();
				}
				rsExist.close();

			}
		} catch (SQLException e) {
			throw new EntityDAOException(e.getMessage());
		} finally {
			try {
				psExist.close();
				psInsert.close();
				psUpdate.close();
				c.close();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			}
		}
	}
/** Gets a Field matching the specified geometry from the database
 *
 * @param f The field to find.  This field only requires its 3 lat/long points to have values
 *
 * @return ArrayList of Fields
 *
 * @throws EntityDAOException
 */
	public ArrayList<Field> findField (Field f) throws EntityDAOException
	{
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<Field> ret = new ArrayList<Field>();

		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(FIELD_FIND,ResultSet.CONCUR_READ_ONLY);

			ps.setLong(1,f.getPLat1());
			ps.setLong(2,f.getPLng1());
			ps.setLong(3,f.getPLat1());
			ps.setLong(4,f.getPLng1());
			ps.setLong(5,f.getPLat1());
			ps.setLong(6,f.getPLng1());

			ps.setLong( 7,f.getPLat2());
			ps.setLong( 8,f.getPLng2());
			ps.setLong( 9,f.getPLat2());
			ps.setLong(10,f.getPLng2());
			ps.setLong(11,f.getPLat2());
			ps.setLong(12,f.getPLng2());

			ps.setLong(13,f.getPLat3());
			ps.setLong(14,f.getPLng3());
			ps.setLong(15,f.getPLat3());
			ps.setLong(16,f.getPLng3());
			ps.setLong(17,f.getPLat3());
			ps.setLong(18,f.getPLng3());

			rs = ps.executeQuery();

			while (rs.next())
			{
				Field fi = new Field(
					rs.getString("creator"),
					rs.getString("agent"),
					rs.getInt("mu"),
					rs.getString("guid"),
					rs.getLong("timestamp"),
					rs.getString("team"),
					rs.getString("pguid1"), rs.getLong("plat1"), rs.getLong("plng1"),
					rs.getString("pguid2"), rs.getLong("plat2"), rs.getLong("plng2"),
					rs.getString("pguid3"), rs.getLong("plat3"), rs.getLong("plng3")
				);
				ret.add(fi);
			}
		} catch (SQLException e) {
			throw new EntityDAOException(e.getMessage());
		} finally {
			try {
				rs.close();
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			}
		}
		return ret;
	}
/**
 * Checks if a field already exists in the database from its guid.
 * Fields of the same geometry but different guids may be present in the database.
 *
 * @param guid of the field
 *
 * @return boolean of the fields existance
 *
 * @throws EntityDAOException
 */
	public boolean existsField(String guid) throws EntityDAOException
	{
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean ret;

		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(FIELD_GET_GUID, ResultSet.CONCUR_READ_ONLY);
			ps.setString(1,guid);
			rs = ps.executeQuery();
			ret = rs.first();
		} catch (SQLException e) {
			throw new EntityDAOException(e.getMessage());
		} finally {
			try {
				rs.close();
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			}
		}
		return ret;
	}
/**
 * Changes the MU for a field from its guid.
 * Some split field are incorrectly assigned MU and need to be repaired.
 * Current business logic checks a field for valid MU before inserting it, correcting fields may not be a requirement
 *
 * @param guid of the field
 * @param mu change the field to this mu
 *
 * @throws EntityDAOException
 */
	public void updateFieldMU(String guid,int mu) throws EntityDAOException
	{
		System.out.println("SQLMUFieldDAO: updateMU("+guid+" -> " + mu +")");
		PreparedStatement ps = null;
		int rs;

		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(FIELD_UPDATE_MU);
			ps.setInt(1, mu);
			ps.setString(2, guid);
			rs = ps.executeUpdate();
		} catch (SQLException se) {
			throw new EntityDAOException("SQLException: " + se.getMessage());
		} finally {
			try {
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			}
		}
	}
/**
 * Deletes a field specified by its guid.
 * Some fields mu is recorded incorrectly and do not seem to work with the cell data.
 * Current business logic checks a field for valid MU before inserting it, deleting fields may not be a requirement
 *
 * @param guid of the field
 *
 * @throws EntityDAOException
 */
	public void deleteField(String guid) throws EntityDAOException
	{
		System.out.println("SQLMUFieldDAO: delete("+guid+")");
		PreparedStatement ps = null;
		int rs;

		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(FIELD_DELETE);
			ps.setString(1, guid);
			rs = ps.executeUpdate();
		} catch (SQLException se) {
			throw new EntityDAOException("SQLException: " + se.getMessage());
		} finally {
			try {
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			}
		}
	}
/**
 * Inserts a field into the database.
 * Business logic to check for duplicates and for MU validity should be performed first.
 * Exceptions may occur for duplicate GUID.
 * Invalid MU may cause errors in the fieldProcessing logic.
 * Although fields have point 1, point 2 and point 3 which implies order. No order is a requirement.
 *
 * @param creator of the field
 * @param agent who submitted the field via the field grabber plugin
 * @param mu of the field
 * @param guid of the field in string format
 * @param timestamp of the fields creation time, in milliseconds since epoch (java millis)
 * @param team of the field. 'E' or 'R'
 * @param pguid1 guid of the portal for point 1
 * @param plat1 latitude of the portal for point 1 in E6 format
 * @param plng1 longitude of the portal for point 1 in E6 format
 * @param pguid2 guid of the portal for point 2
 * @param plat2 latitude of the portal for point 2 in E6 format
 * @param plng2 longitude of the portal for point 2 in E6 format
 * @param pguid3 guid of the portal for point 3
 * @param plat3 latitude of the portal for point 3 in E6 format
 * @param plng3 longitude of the portal for point 3 in E6 format
 * 
 * @throws EntityDAOException
 */
	
	public void insertField(String creator,String agent,int mu, String guid,long timestamp,String team, String pguid1, long plat1, long plng1, String pguid2, long plat2, long plng2, String pguid3, long plat3, long plng3, boolean valid) throws EntityDAOException
	{
		//System.out.println("SQLMUFieldDAO::insert");
		PreparedStatement ps = null;
		int rs;

		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(FIELD_INSERT);
			ps.setString(1, creator);
			ps.setString(2, agent);
			ps.setInt(3, mu);
			ps.setString(4,guid);
			ps.setLong(5, timestamp ); //timestamp sql type
			ps.setString(6, team);
			ps.setString(7, pguid1); ps.setLong(8, plat1); ps.setLong(9, plng1);
			ps.setString(10, pguid2); ps.setLong(11, plat2); ps.setLong(12, plng2);
			ps.setString(13, pguid3); ps.setLong(14, plat3); ps.setLong(15, plng3);
			ps.setBoolean(16,valid);

			rs = ps.executeUpdate();
		} catch (SQLException se) {
			throw new EntityDAOException("SQLException: " + se.getMessage());
		} finally {
			try {
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			}
		}
	}
/**
 * Inserts a field guid along with the cells which make up its region.
 * I cannot be held responsible for incorrectly calculated S2CellRegions.

 *
 * @param guid of the field
 * @param cells an S2CellUnion of all cells which make this field
 * 
 * @throws EntityDAOException
 */
	public void insertCellsForField (String guid, S2CellUnion cells) throws EntityDAOException
	{
		PreparedStatement ps = null;
		int rs;

		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(FIELD_INSERT_CELLS);
			ps.setString(1, guid);
			for (S2CellId cell: cells)
			{	
				ps.setString(2,cell.toToken() );
				rs = ps.executeUpdate();
			}
		} catch (SQLException se) {
			throw new EntityDAOException("SQLException: " + se.getMessage());
		} finally {
			try {
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			}
		}
	}
	/**
 * Finds all fields guid which use the specified cell.
 *
 * @param cell the S2CellId of the required cell
 *
 * @return ArrayList of strings containing the guids
 * 
 * @throws EntityDAOException
 */
	public ArrayList<String> fieldGuidsForCell(S2CellId cell) throws EntityDAOException
	{
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<String> ret = new ArrayList<String>();

		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(FIELD_FIND_FROM_CELL,ResultSet.CONCUR_READ_ONLY);

			ps.setString(1,cell.toToken());

			rs = ps.executeQuery();

			while (rs.next())
				ret.add(rs.getString("field_guid"));
				
		} catch (SQLException e) {
			throw new EntityDAOException(e.getMessage());
		} finally {
			try {
				rs.close();
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			}
		}
		return ret;
	}
/**
 * Gets all Portals contains within an S2Region.
 *
 * @param reg S2Region describing the area which contains the portals
 *
 * @return ArrayList of the portals
 *
 * @throws EntityDAOException
 */
	public ArrayList<Portal> getPortalsInRegion (S2Region reg) throws EntityDAOException
	{
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<Portal> ret = new ArrayList<Portal>();
		//Connection c;

		try {

			S2LatLngRect bound  = reg.getRectBound();
			c = spdDs.getConnection();
			ps = c.prepareStatement(PORTAL_GET_FROM_BOX, ResultSet.CONCUR_READ_ONLY);
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
			throw new EntityDAOException("SQLException: " + se.getMessage());
		}
	}
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
 * @throws EntityDAOException
 */
	public S2LatLng getPortalLocation(String s) throws EntityDAOException
	{
		if (s==null)
			return null;
		if (s.matches("^[0-9a-fA-F]{32}\\.1[16]$"))
			return getPortalLocationFromGuid(s);
		if (s.matches("(\\+|-)?([0-9]+(\\.[0-9]+)),(\\+|-)?([0-9]+(\\.[0-9]+))"))
		{
			String[] ll = s.split(",");
			Double lat = Double.parseDouble(ll[0]);
			Double lng = Double.parseDouble(ll[1]);
			long latE6 = Math.round(lat * 1000000);
			long lngE6 = Math.round(lng * 1000000);

			return getPortalLocationFromLocation(latE6,lngE6);
		}
		return getPortalLocationFromTitle(s);
	}

	/**
	 * Get the portal location from the portals title.
	 *
	 * As titles are not unique, this method will throw an error describing all the matching portals.
	 * The description contains the guid and lat,long of each matching entry.
	 *
	 * @param title The Portal title.
	 *
	 * @return S2LatLng location of the portal
	 *
	 * @throws EntityDAOException
	 **/
	public S2LatLng getPortalLocationFromTitle (String title) throws EntityDAOException
	{
		//Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		S2LatLng ret = null;

		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(PORTAL_GET_LOCATION_FROM_TITLE, ResultSet.CONCUR_READ_ONLY);
			ps.setString(1, title);
			rs = ps.executeQuery();

			boolean searchError = false;
			String err = "" +title +": ";
			while(rs.next()) {
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
				throw new EntityDAOException("Ambiguous Title: " + err);
			}

			rs.close();
			ps.close();
			c.close();
			return ret;
		} catch (SQLException se) {
			throw new EntityDAOException("SQLException: " + se.getMessage());
		}
	}
	/**
	 * Get the portal location from the portals guid.
	 *
	 * @param guid The Portal guid.
	 *
	 * @return S2LatLng location of the portal
	 *
	 * @throws EntityDAOException
	 **/
	public S2LatLng getPortalLocationFromGuid (String guid) throws EntityDAOException
	{
		PreparedStatement ps = null;
		ResultSet rs = null;
		S2LatLng ret = null;
		//Connection c = null;

		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(PORTAL_GET_LOCATION_FROM_GUID, ResultSet.CONCUR_READ_ONLY);
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
			throw new EntityDAOException("SQLException: " + se.getMessage());
		}
	}
	/**
	 * Get the portal location from the location string.
	 * Initially this would look for the portal in the database.
	 * However this method was being used with arbitrary locations, so now just
	 * converts the lat,long into an S2LatLng.  which is probably redundant as
	 * the S2 Library has an identical method. 
	 *
	 * @param latE6 latitude in E6 format.
	 * @param lngE6 longitude in E6 format.
	*
	 * @return S2LatLng location from the description
	 *
	 * @throws EntityDAOException
	 **/
	public S2LatLng getPortalLocationFromLocation (long latE6, long lngE6) throws EntityDAOException
	{
		return S2LatLng.fromE6(latE6,lngE6);
	// would I ever consider using this code.
	// leaving it in here for prosperity?
/*
		System.out.println(">>> getLocationFromLocation: " + latE6 + ", " + lngE6);
		PreparedStatement ps = null;
		ResultSet rs = null;
		S2LatLng ret = null;
		//Connection c = null;

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
			throw new EntityDAOException("SQLException: " + se.getMessage());
		}
*/
	}
	/**
	 * Flag the portal as deleted in the database identified by its guid.
	 *
	 * @param guid The Portal guid.
	 *
	 * @throws EntityDAOException
	 **/
	public void deletePortal (String guid) throws EntityDAOException
	{
		PreparedStatement ps = null;
		//Connection c = null;
		int rs;

		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(PORTAL_UPDATE_DELETED);
			ps.setString(1, guid);
			rs = ps.executeUpdate();

		} catch (SQLException se) {
			throw new EntityDAOException("SQLException: " + se.getMessage());
		} finally {
			try {
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			}
		}
	}
	/**
	 * Short form update the portal identified by its guid.
	 *
	 * @param guid The Portal guid.
	 * @param latE6 the portal latitude in E6 format
	 * @param lngE6 the portal longiture in E6 format
	 * @param team the portal team either 'R' or 'E'
	 *
	 * @throws EntityDAOException
	 **/
	public void updatePortal(String guid, long latE6, long lngE6, String team) throws EntityDAOException
	{
		PreparedStatement ps = null;
		int rs;

		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(PORTAL_UPDATE);
			ps.setLong(1, latE6);
			ps.setLong(2, lngE6);
			ps.setString(3, team);
			ps.setString(4, guid);
			rs = ps.executeUpdate();

		} catch (SQLException se) {
			throw new EntityDAOException("SQLException: " + se.getMessage());
		} finally {
			try {
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			}
		}
	}
	/**
	 * Long form update the portal identified by its guid.
	 *
	 * @param guid The Portal guid.
	 * @param title The name of the portal.
	 * @param latE6 the portal latitude in E6 format
	 * @param lngE6 the portal longiture in E6 format
	 * @param team the portal team either 'R' or 'E'
	 * @param level the portal level
	 * @param resCount the portal number of resonators
	 * @param health the portal total energy, resonator decay or damage
	 * @param image the portal image url
	 * 
	 * @throws EntityDAOException
	 **/
	public void updatePortalFull(String guid, String title, long latE6, long lngE6, String team, int level, int resCount, int health,String image) throws EntityDAOException
	{
		PreparedStatement ps = null;
		//Connection c = null;
		int rs;

		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(PORTAL_UPDATE_FULL);
			ps.setString(1, title);
			ps.setLong(2, latE6);
			ps.setLong(3, lngE6);
			ps.setString(4,team);
			ps.setInt(5,level);
			ps.setInt(6,resCount);
			ps.setInt(7,health);
			ps.setString(8,image);
			ps.setString(9, guid);
			rs = ps.executeUpdate();

		} catch (SQLException se) {
			throw new EntityDAOException("SQLException: " + se.getMessage());
		} finally {
			try {
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			}
		}
	}
	/**
	 * Long form insert portal.
	 *
	 * @param guid The Portal guid.
	 * @param title The name of the portal.
	 * @param latE6 the portal latitude in E6 format
	 * @param lngE6 the portal longiture in E6 format
	 * @param team the portal team either 'R' or 'E'
	 * @param level the portal level
	 * @param resCount the portal number of resonators
	 * @param health the portal total energy, resonator decay or damage
	 * @param image the portal image url
	 * 
	 * @throws EntityDAOException
	 **/
	public void insertPortalFull(String guid, String title, long latE6, long lngE6,String team,int level,int resCount, int health, String image) throws EntityDAOException
	{
		PreparedStatement ps = null;
		//Connection c=null;
		int rs;

		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(PORTAL_INSERT_FULL);
			ps.setString(1, guid);
			ps.setString(2, title);
			ps.setLong(3, latE6);
			ps.setLong(4, lngE6);
			ps.setString(5, team);
			ps.setInt(6,level);
			ps.setInt(7,resCount);
			ps.setInt(8,health);
			ps.setString(9,image);
			rs = ps.executeUpdate();

		} catch (SQLException se) {
			throw new EntityDAOException("SQLException: " + se.getMessage());
		} finally {
			try {
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			}
		}

	}
	/**
	 * Short form insert portal 
	 *
	 * @param guid The Portal guid.
	 * @param latE6 the portal latitude in E6 format
	 * @param lngE6 the portal longiture in E6 format
	 * @param team the portal team either 'R' or 'E'
	 *
	 * @throws EntityDAOException
	 **/
	public void insertPortal(String guid, long latE6, long lngE6, String team) throws EntityDAOException
	{
		PreparedStatement ps = null;
		//Connection c=null;
		int rs;

		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(PORTAL_INSERT);
			ps.setString(1, guid);
			ps.setLong(2, latE6);
			ps.setLong(3, lngE6);
			ps.setString(4, team);

			rs = ps.executeUpdate();

		} catch (SQLException se) {
			throw new EntityDAOException("SQLException: " + se.getMessage());
		} finally {
			try {
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			}
		}
	}
	/**
	 * Check if a portal guid is in the database (.hasKey()).
	 *
	 * @param guid The Portal guid.
	 *
	 *@return boolean true if the guid is found.
	 *
	 * @throws EntityDAOException
	 **/
	private boolean existsPortal(String guid) throws EntityDAOException
	{
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean exists;
		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(PORTAL_GUID_EXISTS);
			ps.setString(1, guid);
			rs = ps.executeQuery();
			exists=rs.first();

		} catch (SQLException se) {
			throw new EntityDAOException("SQLException: " + se.getMessage());
		} finally {
			try {
				rs.close();
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new EntityDAOException("SQLException: " + se.getMessage());
			}
		}
		return exists;
	}
	/**
	 * Short form insert or update portal depending on its existence.
	 *
	 * @param guid The Portal guid.
	 * @param latE6 the portal latitude in E6 format
	 * @param lngE6 the portal longiture in E6 format
	 * @param team the portal team either 'R' or 'E'
	 *
	 * @throws EntityDAOException
	 **/
	public void writePortal (String guid, long latE6, long lngE6, String team) throws EntityDAOException
	{
		if (existsPortal(guid))
			updatePortal(guid,latE6,lngE6,team);
		else
			insertPortal(guid,latE6,lngE6,team);
	}
		/**
	 * Long form insert or update portal depending on its existence.
	 *
	 * @param guid The Portal guid.
	 * @param title The name of the portal.
	 * @param latE6 the portal latitude in E6 format
	 * @param lngE6 the portal longiture in E6 format
	 * @param team the portal team either 'R' or 'E'
	 * @param level the portal level
	 * @param resCount the portal number of resonators
	 * @param health the portal total energy, resonator decay or damage
	 * @param image the portal image url
	 * 
	 * @throws EntityDAOException
	 **/
	public void writePortalFull (String guid, String title, long latE6, long lngE6, String team, int level, int resCount, int health, String image) throws EntityDAOException
	{
		if (existsPortal(guid))
			updatePortalFull(guid,title,latE6,lngE6,team,level,resCount,health,image);
		else
			insertPortalFull(guid,title,latE6,lngE6,team,level,resCount,health,image);

	}
}
