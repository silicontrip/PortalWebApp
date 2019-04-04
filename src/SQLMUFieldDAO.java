/*
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
+-----------+-------------+------+-----+-------------------+-----------------------------+
*/
package net.silicontrip.ingress;

import java.sql.*;
import javax.sql.*;
import javax.naming.*;

import java.util.ArrayList;

public class SQLMUFieldDAO implements MUFieldDAO {

	protected static String GET_GUID = "select * from mufields where guid=?";
	protected static String UPDATE_MU = "update mufields set mu=? where guid=?";
	protected static String DELETE = "delete from mufields where guid=?";
        protected static String INSERT = "insert into mufields (creator,agent,mu,guid,timestamp,team,pguid1,plat1,plng1,pguid2,plat2,plng2,pguid3,plat3,plng3) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        private Connection c = null;
        private DataSource spdDs = null;
        
        public SQLMUFieldDAO() throws MUFieldDAOException {
            try {
                InitialContext ctx = new InitialContext();
                spdDs =  (DataSource) ctx.lookup("jdbc/IngressResource");
            } catch (NamingException slx) {   
                throw new MUFieldDAOException("NamingException while looking up DB context : " + slx.getMessage());
            }
        }

	public ArrayList<Field> findField (Field f) throws MUFieldDAOException
	{
		PreparedStatement ps = null;
                ResultSet rs = null;
		ArrayList<Field> ret = new ArrayList<Field>();


		try {
			c = spdDs.getConnection();
			String q = "select * from mufields where (((plat1=? and plng1=?) or (plat2=? and plng2=?) or (plat3=? and plng3=?)) and ((plat1=? and plng1=?) or (plat2=? and plng2=?) or (plat3=? and plng3=?)) and ((plat1=? and plng1=?) or (plat2=? and plng2=?) or (plat3=? and plng3=?)))";
			 ps = c.prepareStatement(q,ResultSet.CONCUR_READ_ONLY);

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
			throw new MUFieldDAOException(e.getMessage());
		} finally {
			try {
				rs.close();
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new MUFieldDAOException("SQLException: " + se.getMessage());
			}
		}
		return ret;
	} 

        public boolean exists(String guid) throws MUFieldDAOException
	{
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean ret;
      
		try {
			c = spdDs.getConnection();		
			ps = c.prepareStatement(GET_GUID, ResultSet.CONCUR_READ_ONLY);
			ps.setString(1,guid);
			rs = ps.executeQuery();
			ret = rs.first();
			
		} catch (SQLException e) {
			throw new MUFieldDAOException(e.getMessage());
		} finally {
			try {
				rs.close();
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new MUFieldDAOException("SQLException: " + se.getMessage());
			}
		}
		return ret;
        }

        public void updateMU(String guid,int mu) throws MUFieldDAOException
        {
		System.out.println("SQLMUFieldDAO: delete("+guid+")");
		PreparedStatement ps = null;
		int rs;
      
		try {
			c = spdDs.getConnection();
			ps = c.prepareStatement(UPDATE_MU);
			ps.setInt(1, mu);
			ps.setString(2, guid);
			rs = ps.executeUpdate();
		} catch (SQLException se) {
			throw new MUFieldDAOException("SQLException: " + se.getMessage());
		} finally {
			try {
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new MUFieldDAOException("SQLException: " + se.getMessage());
			}
		}
        }

        public void delete(String guid) throws MUFieldDAOException
        {
		System.out.println("SQLMUFieldDAO: delete("+guid+")");
		PreparedStatement ps = null;
		int rs;
      
		try {
			c = spdDs.getConnection();		
			ps = c.prepareStatement(DELETE);
			ps.setString(1, guid);
			rs = ps.executeUpdate();
		} catch (SQLException se) {
			throw new MUFieldDAOException("SQLException: " + se.getMessage());
		} finally {
			try {
				ps.close();
				c.close();
			} catch (SQLException se) {
				throw new MUFieldDAOException("SQLException: " + se.getMessage());
			}
		}
        }

        public void insert(String creator,String agent,int mu, String guid,long timestamp,String team, String pguid1, long plat1, long plng1, String pguid2, long plat2, long plng2, String pguid3, long plat3, long plng3) throws MUFieldDAOException
        {

			//System.out.println("SQLMUFieldDAO::insert");

			PreparedStatement ps = null;
			int rs;
      
			try {
				c = spdDs.getConnection();		
				ps = c.prepareStatement(INSERT);
				ps.setString(1, creator);
				ps.setString(2, agent);
				ps.setInt(3, mu);
				ps.setString(4,guid);
				ps.setLong(5, timestamp ); //timestamp sql type
				ps.setString(6, team);
				ps.setString(7, pguid1); ps.setLong(8, plat1); ps.setLong(9, plng1);
				ps.setString(10, pguid2); ps.setLong(11, plat2); ps.setLong(12, plng2);
				ps.setString(13, pguid3); ps.setLong(14, plat3); ps.setLong(15, plng3);

				rs = ps.executeUpdate();
			} catch (SQLException se) {
				throw new MUFieldDAOException("SQLException: " + se.getMessage());
			} finally {
				try {
					ps.close();
					c.close();
				} catch (SQLException se) {
					throw new MUFieldDAOException("SQLException: " + se.getMessage());
				}
			}
	}

}
