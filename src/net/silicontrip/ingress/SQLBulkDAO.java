package net.silicontrip.ingress;

import com.google.common.geometry.*;


import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ejb.Singleton;
import javax.naming.*;
import javax.sql.*;


import net.silicontrip.UniformDistribution;
import net.silicontrip.UniformDensityCurve;



@Singleton
public class SQLBulkDAO  {

	//@PersistenceContext
	//private EntityManagerFactory emf;
	
	private DataSource spdDs = null;

	/**
	 * Constructor
	 * @throws EntityDAOException if there is a problem with the jdbc resource
	 */
	public SQLBulkDAO() throws EntityDAOException {
		try {
			InitialContext ctx = new InitialContext();
			spdDs = (DataSource) ctx.lookup("jdbc/IngressResource");
		} catch (NamingException slx) {
			throw new EntityDAOException("NamingException while looking up DB context : " + slx.getMessage());
		}
	}

    public ArrayList<String[]> readValidFields() throws SQLException
    {
        // Create a statement to execute SQL commands
        ArrayList<String[]> fieldList = new ArrayList<String[]>();

        try (Connection conn=spdDs.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM mufields where valid = true") ) {

            // Process the results
            while (rs.next()) {
                String[] rowArray = new String[16];

                for (int i = 1; i <= 16; i++) {
                    rowArray[i - 1] = rs.getString(i);
                    // System.out.print(rs.getString(i)+ " ");
                }
                fieldList.add(rowArray);
                //System.out.println("");

            }
        }

        return fieldList;
    }

/* 
    * Retrieves the full data for a given list of field GUIDs.
    * This method executes one query per GUID. It is suitable for fetching
    * a moderate number of specific fields.
    *
    * @param guids A List of field GUID strings to retrieve.
    * @return An ArrayList of String arrays, where each array represents a fields data.
    * @throws SQLException if a database access error occurs.
    */
   public ArrayList<String[]> getFieldsForGuid(ArrayList<String> guids) throws SQLException {
   
       ArrayList<String[]> fieldList = new ArrayList<>();
       // The SQL query to fetch a single field by its GUID.
       String sql = "SELECT * FROM mufields WHERE guid = ? AND valid = true";
   
       // We use try-with-resources for the Connection and PreparedStatement
       // to ensure they are always closed.
       try (Connection conn = spdDs.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
   
           // Loop through each GUID provided in the input list
           for (String guid : guids) {
               // Set the GUID parameter in the prepared statement
               pstmt.setString(1, guid);
   
               // Execute the query for the current GUID
               try (ResultSet rs = pstmt.executeQuery()) {
                   // If a result is found, process it
                   if (rs.next()) {
                       String[] rowArray = new String[16];
                       for (int i = 1; i <= 16; i++) {
                           // We'll trim the strings here as a good practice
                           String value = rs.getString(i);
                           rowArray[i - 1] = (value != null) ? value.trim() : null;
                       }
                       fieldList.add(rowArray);
                   }
                   // If no result is found for a GUID, we simply move on.
               }
           }
       }
       // The Connection, PreparedStatement, and ResultSet are all automatically closed here.
   
       return fieldList;
   }
   

    public HashMap<S2CellId,UniformDistribution> readCells() throws SQLException
    {
        HashMap<S2CellId,UniformDistribution> cellMap = new HashMap<S2CellId,UniformDistribution>();

        try (Connection conn=spdDs.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM mucell")) {

            while (rs.next()) {
            // Use the split() method to split each line into fields based on '|' delimiter
                    S2CellId cid = S2CellId.fromToken(rs.getString(1));
                    double lower = rs.getDouble(2);
                    double upper = rs.getDouble(3);

                    UniformDistribution ud = new UniformDistribution(lower, upper);
                    cellMap.put(cid,ud);
            }

        }
        return cellMap;
    }

    public int writeCells(HashMap<S2CellId,UniformDistribution> cellm) throws SQLException
    {
        int count = 0;
        try (Connection conn=spdDs.getConnection();
            PreparedStatement ps = conn.prepareStatement("insert into mucell (cellid,mu_low,mu_high) values (?,?,?)")) {

            for (Map.Entry<S2CellId, UniformDistribution> entry : cellm.entrySet()) {
                    
                    ps.setString(1, entry.getKey().toToken());
                    UniformDistribution ud = entry.getValue();
                    ps.setDouble(2,ud.getLower());
                    ps.setDouble(3,ud.getUpper());

                    count += ps.executeUpdate();
            }
        }
        return count;
    }

    /**
    * Performs a bulk "upsert" (update or insert) for a batch of cell data.
    * This method uses a single, efficient MERGE statement to update existing cells
    * and insert new ones in a single atomic transaction.
    *
    * @param cellm A HashMap of S2CellId to UniformDistribution to be upserted.
    * @return The total number of rows affected (inserted + updated).
    * @throws SQLException if a database access error occurs.
    */
    public int upsertCells(HashMap<S2CellId, UniformDistribution> cellm) throws SQLException {
    
        // The powerful MERGE statement.
        // It uses a "VALUES" clause to create a temporary "source" table in memory.
        String sql = "MERGE INTO mucell AS t " +
                        "USING (VALUES (?, ?, ?)) AS s (cellid, mu_low, mu_high) " + // Note: Derby requires 'hight' typo fix if in DB
                        "ON t.cellid = s.cellid " +
                        "WHEN MATCHED THEN " +
                        "    UPDATE SET t.mu_low = s.mu_low, t.mu_high = s.mu_high " +
                        "WHEN NOT MATCHED THEN " +
                        "    INSERT (cellid, mu_low, mu_high) VALUES (s.cellid, s.mu_low, s.mu_high)";
        
        int[] updateCounts;
        int totalAffected = 0;
        
        // We use try-with-resources for the Connection and PreparedStatement
        try (Connection conn = spdDs.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
        
            // Loop through each entry in your map to add it to the batch
            for (Map.Entry<S2CellId, UniformDistribution> entry : cellm.entrySet()) {
                pstmt.setString(1, entry.getKey().toToken());
                UniformDistribution ud = entry.getValue();
                pstmt.setDouble(2, ud.getLower());
                pstmt.setDouble(3, ud.getUpper());
        
                // Add the current set of parameters to the batch
                pstmt.addBatch();
            }
        
            // Execute the entire batch of MERGE operations at once
            updateCounts = pstmt.executeBatch();
        }
        // The Connection and PreparedStatement are automatically closed here.
        
        // Sum the results from the updateCounts array to get the total number of affected rows
        for (int count : updateCounts) {
            totalAffected += count;
        }
        
        return totalAffected;
    }

    public int deleteCells() throws SQLException
    {
        int rDel = 0;
        try (Connection conn=spdDs.getConnection();
            Statement stmt = conn.createStatement()) {
            rDel = stmt.executeUpdate("DELETE FROM MUCELL");// of course I had to name one of the tables without an S
        }
        return rDel;
    }

}