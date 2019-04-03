/*
mysql> desc mufields;
+-----------+-------------+------+-----+-------------------+-----------------------------+
| Field     | Type        | Null | Key | Default           | Extra                       |
+-----------+-------------+------+-----+-------------------+-----------------------------+
| creator   | varchar(36) | YES  |     | NULL              |                             |
| agent     | varchar(36) | NO   |     | NULL              |                             |
| mu        | int(8)      | NO   |     | NULL              |                             |
| guid      | char(36)    | NO   | PRI | NULL              |                             |
| timestamp | timestamp   | NO   |     | CURRENT_TIMESTAMP | on update CURRENT_TIMESTAMP |
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
