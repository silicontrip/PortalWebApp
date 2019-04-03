/*
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
*/
package net.silicontrip.ingress;

import java.util.ArrayList;
import com.google.common.geometry.*;

import java.sql.*;
import javax.sql.*;
import javax.naming.*;

public class SQLLinkDAO implements LinkDAO {

        public static String GET_ALL = "select * from links";
	public static String GET_GUID = "select * from links where guid=?";
        public static String PURGE = "delete from links";
        public static String DELETE= "delete from links where guid=?";
        public static String INSERT= "insert into links (guid,d_guid,d_latE6,d_lngE6,o_guid,o_latE6,o_lngE6,team) values (?,?,?,?,?,?,?,?)";

        private Connection c = null;
        private DataSource spdDs = null;
        
        public SQLLinkDAO()  throws LinkDAOException {
            try {
                InitialContext ctx = new InitialContext();
                spdDs =  (DataSource) ctx.lookup("jdbc/IngressResource");
            } catch (NamingException slx) {   
                throw new LinkDAOException("NamingException while looking up DB context : " + slx.getMessage());
            }
        }

        public ArrayList<Link> getInRect (S2LatLngRect reg) throws LinkDAOException
        {
		ArrayList<Link> all  = getAll();
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

        public Link getGuid(String guid) throws LinkDAOException
	{
		PreparedStatement ps = null;
		ResultSet rs = null;
		Link ret = null;
      
		try {
			c = spdDs.getConnection();		
			ps = c.prepareStatement(GET_GUID, ResultSet.CONCUR_READ_ONLY);
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
			throw new LinkDAOException(e.getMessage());
		} finally {
			try {
				rs.close();
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new LinkDAOException("SQLException: " + se.getMessage());
			}
		}
		return ret;
        }

        public ArrayList<Link> getAll () throws LinkDAOException
	    {
		    PreparedStatement ps = null;
		    ResultSet rs = null;
		    ArrayList<Link> ret = new ArrayList<Link>();
      
		    try {
			    c = spdDs.getConnection();		
                ps = c.prepareStatement(GET_ALL, ResultSet.CONCUR_READ_ONLY);
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
                throw new LinkDAOException(e.getMessage());
            } finally {
                try {
                    rs.close();
                    ps.close();
                    c.close();
                } catch (SQLException se) {
                    throw new LinkDAOException("SQLException: " + se.getMessage());
                }
            }
            return ret;
        }

        public void purge() throws LinkDAOException
        {
		PreparedStatement ps = null;
		int rs;
      
		try {
			c = spdDs.getConnection();		
			ps = c.prepareStatement(PURGE); // Little Bobby DROP TABLES
			rs = ps.executeUpdate();
		} catch (SQLException se) {
			throw new LinkDAOException("SQLException: " + se.getMessage());
		} finally {
			try {
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new LinkDAOException("SQLException: " + se.getMessage());
			}
		}
        }

        public void delete(String guid) throws LinkDAOException
        {
		//System.out.println("SQLLinkDAO: delete("+guid+")");
		PreparedStatement ps = null;
		int rs;
      
		try {
			c = spdDs.getConnection();		
			ps = c.prepareStatement(DELETE);
			ps.setString(1, guid);
			rs = ps.executeUpdate();
		} catch (SQLException se) {
			throw new LinkDAOException("SQLException: " + se.getMessage());
		} finally {
			try {
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new LinkDAOException("SQLException: " + se.getMessage());
			}
		}
        }

        public void insert(String guid,String dguid, long dlatE6, long dlngE6,String oguid, long olatE6, long olngE6, String team) throws LinkDAOException
        {

		if (getGuid(guid) == null)
		{

			PreparedStatement ps = null;
			int rs;
      
			try {
				c = spdDs.getConnection();		
				ps = c.prepareStatement(INSERT);
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
				throw new LinkDAOException("SQLException: " + se.getMessage());
			} finally {
				try {
					ps.close();
					c.close();
				} catch (SQLException se) {
					throw new LinkDAOException("SQLException: " + se.getMessage());
				}
			}
		}
	}

}
