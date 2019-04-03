package net.silicontrip.ingress;

import net.silicontrip.UniformDistribution;
import java.util.HashMap;
import java.util.Map;
import com.google.common.geometry.*;

import java.sql.*;
import javax.sql.*;
import javax.naming.*;


public class SQLMUCellDAO implements MUCellDAO {

	protected static String GET_ALL = "Select * from mucell";
	protected static String GET_CELLID = "Select * from mucell where cellid=?";
	protected static String UPDATE = "update mucell set mu_low=?,mu_high=? where cellid=?";
	protected static String INSERT = "insert into mucell (cellid,mu_low,mu_high) values (?,?,?)";

	private Connection c = null;
        private DataSource spdDs = null;
        
        public SQLMUCellDAO()  throws MUCellDAOException {
            try {
                InitialContext ctx = new InitialContext();
                spdDs =  (DataSource) ctx.lookup("jdbc/IngressResource");
            } catch (NamingException slx) {   
                throw new MUCellDAOException("NamingException while looking up DB context : " + slx.getMessage());
            }
	}

	public HashMap<S2CellId,UniformDistribution> getAll() throws MUCellDAOException
	{
                PreparedStatement ps = null;
                ResultSet rs = null;
		HashMap<S2CellId,UniformDistribution> ret = new HashMap<S2CellId,UniformDistribution>();
		try {
                        c = spdDs.getConnection();              
                        ps = c.prepareStatement(GET_ALL, ResultSet.CONCUR_READ_ONLY);
                        rs = ps.executeQuery();
                        while (rs.next())
                        {
				UniformDistribution ud = new UniformDistribution(rs.getDouble("mu_low"), rs.getDouble("mu_high"));
				S2CellId cell = S2CellId.fromToken(rs.getString("cellid"));
				ret.put(cell,ud);
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

	public void updateAll(HashMap<S2CellId,UniformDistribution> cellmu) throws MUCellDAOException
	{
		PreparedStatement psExist = null;
                PreparedStatement psInsert = null;
                PreparedStatement psUpdate = null;
		try {
                        c = spdDs.getConnection();              
			psExist = c.prepareStatement(GET_CELLID);
			psInsert = c.prepareStatement(INSERT);
			psUpdate = c.prepareStatement(UPDATE);

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
                        throw new MUCellDAOException(e.getMessage());
                } finally { 
                        try {   
                                psExist.close();
                                psInsert.close();
                                psUpdate.close();
                                c.close();
                        } catch (SQLException se) {
                                throw new MUCellDAOException("SQLException: " + se.getMessage());
                       }
                }


	}

}

