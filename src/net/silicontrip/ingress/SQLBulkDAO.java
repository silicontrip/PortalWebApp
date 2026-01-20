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

	public ArrayList<String[]> readAllFields() throws SQLException
	{
		ArrayList<String[]> fieldList = new ArrayList<String[]>();
		
		try (Connection conn=spdDs.getConnection();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM mufields") ) {

			// Process the results
			while (rs.next()) {
				String[] rowArray = new String[16];
				for (int i = 1; i <= 16; i++) {
					rowArray[i - 1] = rs.getString(i).trim();
					// System.out.print(rs.getString(i)+ " ");
				}
				fieldList.add(rowArray);
				//System.out.println("");
			}
		}
		return fieldList;
	}

	public int writeFields(ArrayList<String[]> flist) throws SQLException
	{
		int[] count;
		String sql = "insert into mufields (creator,agent,mu,guid,timestamp,team,pguid1,plat1,plng1,pguid2,plat2,plng2,pguid3,plat3,plng3,valid) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		try (Connection conn=spdDs.getConnection();
			PreparedStatement ps = conn.prepareStatement(sql) ) {

			for (String[] outerField: flist)
			{
				int mu = Integer.parseInt(outerField[2]);
				long plat1 = Long.parseLong(outerField[7]);
				long plng1 = Long.parseLong(outerField[8]);
				long plat2 = Long.parseLong(outerField[10]);
				long plng2 = Long.parseLong(outerField[11]);
				long plat3 = Long.parseLong(outerField[13]);
				long plng3 = Long.parseLong(outerField[14]);
				boolean valid = Boolean.parseBoolean(outerField[15]);
	
				ps.setString(1, outerField[0]); // creator
				ps.setString(2, outerField[1]); // agent
				ps.setInt(3, mu);
				ps.setString(4, outerField[3].stripTrailing()); //guid
				ps.setString(5, outerField[4]); //timestamp sql type
				ps.setString(6, outerField[5]); // team
				ps.setString(7, outerField[6].stripTrailing()); // pguid1
				ps.setLong(8, plat1);
				ps.setLong(9, plng1);
				ps.setString(10, outerField[9].stripTrailing()); // pguid2
				ps.setLong(11, plat2);
				ps.setLong(12, plng2);
				ps.setString(13, outerField[12].stripTrailing()); //pguid3
				ps.setLong(14, plat3);
				ps.setLong(15, plng3);
				ps.setBoolean(16, valid);
	
				//count += ps.executeUpdate();
				ps.addBatch();
			}
			count = ps.executeBatch();
		}
		return count.length;
	}

	public int deleteFields() throws SQLException
	{
		int rDel = 0;
		try (Connection conn=spdDs.getConnection();
			Statement stmt = conn.createStatement() ) {
		
			rDel = stmt.executeUpdate("DELETE FROM MUFIELDS");
		}
		return rDel;
	}

	public int deleteFieldCells() throws SQLException
	{
		int rDel = 0;
		try (Connection conn=spdDs.getConnection();
			Statement stmt = conn.createStatement() ) {
			rDel = stmt.executeUpdate("DELETE FROM FIELDCELLS");

		}
		return rDel;
	}

	public boolean setFieldValid(String guid,boolean valid) throws SQLException
	{

		int fupd = 0;
		int fcdel = 0;

		try (Connection conn=spdDs.getConnection();
			PreparedStatement ps1 = conn.prepareStatement("update mufields set valid=? where guid=?");
			PreparedStatement ps2 = conn.prepareStatement("delete from FIELDCELLS where field_guid=?") ) {

			ps1.setBoolean(1,valid);
			ps1.setString(2,guid);
			ps2.setString(1,guid);

			fupd = ps1.executeUpdate();  // this should be 1 or 0
			fcdel = 1;
			if (!valid)
				fcdel = ps2.executeUpdate(); // this should be 0 or more
		}

		return (fupd == 1 && fcdel > 0);

	}

// Will change this to a single loop
	public int writeFieldCells (ArrayList<String[]> flist) throws SQLException
	{
		int totalCount = 0;
		final int BATCH_SIZE = 30000; // Derby limit is 65534 parameters, 2 params per row = 32767 max, use 30000 for safety

		try (Connection conn=spdDs.getConnection();
			PreparedStatement ps = conn.prepareStatement("insert into fieldcells (field_guid,cellid) values (?,?)") ) {
	
			int batchCount = 0;
			for (String[] fieldCell: flist)
			{
				ps.setString(1, fieldCell[0]);
				ps.setLong(2, Long.parseLong(fieldCell[1])); 
				ps.addBatch();
				batchCount++;
				
				// Execute batch when we hit the limit
				if (batchCount >= BATCH_SIZE) {
					int[] count = ps.executeBatch();
					totalCount += count.length;
					batchCount = 0;
				}
			}
			
			// Execute remaining batch
			if (batchCount > 0) {
				int[] count = ps.executeBatch();
				totalCount += count.length;
			}
		}
		return totalCount;
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
	public int upsertCells(HashMap<S2CellId, UniformDistribution> cellm) throws SQLException 
	{
	
		String updateSql = "UPDATE mucell SET mu_low = ?, mu_high = ? WHERE cellid = ?";
		String insertSql = "INSERT INTO mucell (cellid, mu_low, mu_high) VALUES (?, ?, ?)";
	
		int totalAffected = 0;
		Connection conn = null; // Declare connection outside the try block for the finally block
	
		try {
			conn = spdDs.getConnection();
			conn.setAutoCommit(false); // **CRITICAL:** Start a transaction
	
			try (PreparedStatement updateStmt = conn.prepareStatement(updateSql);
				PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
	
				for (Map.Entry<S2CellId, UniformDistribution> entry : cellm.entrySet()) {
					// Try to UPDATE first
					updateStmt.setDouble(1, entry.getValue().getLower());
					updateStmt.setDouble(2, entry.getValue().getUpper());
					updateStmt.setString(3, entry.getKey().toToken());
	
					int rowsUpdated = updateStmt.executeUpdate();
					totalAffected += rowsUpdated;
	
					// If the UPDATE affected 0 rows, the key doesn't exist, so INSERT it.
					if (rowsUpdated == 0) {
						insertStmt.setString(1, entry.getKey().toToken());
						insertStmt.setDouble(2, entry.getValue().getLower());
						insertStmt.setDouble(3, entry.getValue().getUpper());
						int rowsInserted = insertStmt.executeUpdate();
						totalAffected += rowsInserted;
					}
				}
			}
	
			conn.commit(); // **CRITICAL:** Commit the entire transaction at the end
	
		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.rollback(); // If anything goes wrong, roll back the entire transaction
				} catch (SQLException ex) {
					// Log rollback failure
				}
			}
			throw new SQLException("Error during upsert transaction.", e);
		} finally {
			if (conn != null) {
				try {
					conn.setAutoCommit(true); // Reset auto-commit state
					conn.close(); // **CRITICAL:** Return the connection to the pool
				} catch (SQLException e) {
					// Log closing failure
				}
			}
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