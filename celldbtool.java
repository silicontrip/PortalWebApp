package net.silicontrip;

import com.google.common.geometry.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.*;
import java.util.Set;


//import net.silicontrip.UniformDistribution;


public class celldbtool {

	protected Connection conn;
	HashMap<String,S2CellUnion> fieldUnion;
	HashMap<String,HashMap<S2CellId,Double>> fieldIntersections;
	HashMap<S2CellId,HashSet<String[]>> cellField;
	
	// Trace logging - if not null, log all changes to this cell
	private S2CellId cellTrace = null;


	public celldbtool(String url, String user, String password) throws ClassNotFoundException, SQLException
	{
			// Load the JDBC driver from the derby.jar file
			Class.forName("org.apache.derby.jdbc.ClientDriver");

			// Establish a connection to Derby
			conn = DriverManager.getConnection(url, user, password);

			fieldUnion = new HashMap<String,S2CellUnion>();
			fieldIntersections = new HashMap<String,HashMap<S2CellId,Double>>();
			cellField = new HashMap<S2CellId,HashSet<String[]>>();
	}

	void close() throws SQLException
	{
		conn.close();
	}

// upsertCells in SQLBulkDAO
	boolean setCellMU(String celltok, UniformDistribution ud) throws SQLException
	{
		PreparedStatement ps1 = conn.prepareStatement("delete from mucell where cellid = ?");
		PreparedStatement ps2 = conn.prepareStatement("insert into mucell (cellid,mu_low,mu_high) values (?,?,?)");

		//String celltok = cid.toToken();
		ps1.setString(1, celltok);
		ps2.setString(1, celltok);
		ps2.setDouble(2,ud.getLower());
		ps2.setDouble(3,ud.getUpper());

		int count = 0;
		count += ps1.executeUpdate();
		count += ps2.executeUpdate();

		return count >0;
	}

//readCells SQLBulkDAO
	HashMap<S2CellId,UniformDistribution> readCells() throws SQLException
	{
			HashMap<S2CellId,UniformDistribution> cellMap = new HashMap<S2CellId,UniformDistribution>();

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM mucell");

			while (rs.next()) {
			// Use the split() method to split each line into fields based on '|' delimiter
					S2CellId cid = S2CellId.fromToken(rs.getString(1));
					double lower = rs.getDouble(2);
					double upper = rs.getDouble(3);

					UniformDistribution ud = new UniformDistribution(lower, upper);
					cellMap.put(cid,ud);
			}
			rs.close();
			stmt.close();
			return cellMap;
	}

//writeCells SQLBulkDAO
	int writeCells(HashMap<S2CellId,UniformDistribution> cellm) throws SQLException
	{
		PreparedStatement ps = conn.prepareStatement("insert into mucell (cellid,mu_low,mu_high) values (?,?,?)");

		int count = 0;
		for (Map.Entry<S2CellId, UniformDistribution> entry : cellm.entrySet()) {
				
				ps.setString(1, entry.getKey().toToken());
				UniformDistribution ud = entry.getValue();
				ps.setDouble(2,ud.getLower());
				ps.setDouble(3,ud.getUpper());

				count += ps.executeUpdate();
		}
		return count;
	}

//readValidFields SQLBulkDAO
	ArrayList<String[]> readValidFields() throws SQLException
	{
		// Create a statement to execute SQL commands
		Statement stmt = conn.createStatement();

		// Execute an SQL query
		ResultSet rs = stmt.executeQuery("SELECT * FROM mufields where valid = true");
		ArrayList<String[]> fieldList = new ArrayList<String[]>();

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
		rs.close();
		stmt.close();
		return fieldList;
	}

// readAllFields SQLBulkDAO
	ArrayList<String[]> readAllFields() throws SQLException
	{
		// Create a statement to execute SQL commands
		Statement stmt = conn.createStatement();

		// Execute an SQL query
		ResultSet rs = stmt.executeQuery("SELECT * FROM mufields");
		ArrayList<String[]> fieldList = new ArrayList<String[]>();

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
		rs.close();
		stmt.close();
		return fieldList;
	}

	ArrayList<String[]> getRequest (ArrayList<String[]> fieldList, PreparedStatement ps, long pa1, long pl1, long pa2, long pl2) throws SQLException
	{
		ps.setLong(1,pa1);
		ps.setLong(2,pl1);
		ps.setLong(3,pa2);
		ps.setLong(4,pl2);

		ResultSet rs = ps.executeQuery();

		while (rs.next()) {
			String[] rowArray = new String[16];

			for (int i = 1; i <= 16; i++) {
				rowArray[i - 1] = rs.getString(i);
				// System.out.print(rs.getString(i)+ " ");
			}
			fieldList.add(rowArray);
			//System.out.println("");

		}
		rs.close();
		
		return fieldList;
	}
	ArrayList<String[]> findSharedFields(String[] outerField) throws SQLException
	{
		ArrayList<String[]> fieldList = new ArrayList<String[]>();
		PreparedStatement ps1 = conn.prepareStatement("select * from mufields where plat1=? and plng1=? and plat2=? and plng2=?");
		PreparedStatement ps2 = conn.prepareStatement("select * from mufields where plat2=? and plng2=? and plat3=? and plng3=?");
		PreparedStatement ps3 = conn.prepareStatement("select * from mufields where plat3=? and plng3=? and plat1=? and plng1=?");

		long plat1 = Long.parseLong(outerField[7]);
		long plng1 = Long.parseLong(outerField[8]);
		long plat2 = Long.parseLong(outerField[10]);
		long plng2 = Long.parseLong(outerField[11]);
		long plat3 = Long.parseLong(outerField[13]);
		long plng3 = Long.parseLong(outerField[14]);

		fieldList = getRequest(fieldList, ps1, plat1, plng1, plat2, plng2);
		fieldList = getRequest(fieldList, ps1, plat2, plng2, plat1, plng1);
		fieldList = getRequest(fieldList, ps1, plat2, plng2, plat3, plng3);
		fieldList = getRequest(fieldList, ps1, plat3, plng3, plat2, plng2);
		fieldList = getRequest(fieldList, ps1, plat3, plng3, plat1, plng1);
		fieldList = getRequest(fieldList, ps1, plat1, plng1, plat3, plng3);

		fieldList = getRequest(fieldList, ps2, plat1, plng1, plat2, plng2);
		fieldList = getRequest(fieldList, ps2, plat2, plng2, plat1, plng1);
		fieldList = getRequest(fieldList, ps2, plat2, plng2, plat3, plng3);
		fieldList = getRequest(fieldList, ps2, plat3, plng3, plat2, plng2);
		fieldList = getRequest(fieldList, ps2, plat3, plng3, plat1, plng1);
		fieldList = getRequest(fieldList, ps2, plat1, plng1, plat3, plng3);

		fieldList = getRequest(fieldList, ps3, plat1, plng1, plat2, plng2);
		fieldList = getRequest(fieldList, ps3, plat2, plng2, plat1, plng1);
		fieldList = getRequest(fieldList, ps3, plat2, plng2, plat3, plng3);
		fieldList = getRequest(fieldList, ps3, plat3, plng3, plat2, plng2);
		fieldList = getRequest(fieldList, ps3, plat3, plng3, plat1, plng1);
		fieldList = getRequest(fieldList, ps3, plat1, plng1, plat3, plng3);

		return fieldList;

	}

// writeFields SQLBulkDAO
	int writeFields(ArrayList<String[]> flist) throws SQLException
	{

		PreparedStatement ps = conn.prepareStatement("insert into mufields (creator,agent,mu,guid,timestamp,team,pguid1,plat1,plng1,pguid2,plat2,plng2,pguid3,plat3,plng3,valid) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

		int count = 0;
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

			count += ps.executeUpdate();

		}
		return count;
	}

//deleteFieldCells SQLBulkDAO
	int deleteFieldCells() throws SQLException
	{
		Statement stmt = conn.createStatement();
		int rDel = stmt.executeUpdate("DELETE FROM FIELDCELLS");

		stmt.close();
		return rDel;

	}

//deleteFields SQLBulkDAO
	int deleteFields() throws SQLException
	{
		Statement stmt = conn.createStatement();
		int rDel = stmt.executeUpdate("DELETE FROM MUFIELDS");

		stmt.close();
		return rDel;

	}

//deleteCells SQLBulkDAO
	int deleteCells() throws SQLException
	{
		Statement stmt = conn.createStatement();
		int rDel = stmt.executeUpdate("DELETE FROM MUCELL");  // of course I had to name one of the tables without an S

		stmt.close();
		return rDel;

	}

//setFieldValid SQLBulkDAO
	boolean setFieldValid(String guid,boolean valid) throws SQLException
	{

		PreparedStatement ps1 = conn.prepareStatement("update mufields set valid=? where guid=?");
		PreparedStatement ps2 = conn.prepareStatement("delete from FIELDCELLS where field_guid=?");

		ps1.setBoolean(1,valid);
		ps1.setString(2,guid);
		ps2.setString(1,guid);

		int fupd = ps1.executeUpdate();  // this should be 1 or 0
		int fcdel = 1;
		if (!valid)
			fcdel = ps2.executeUpdate(); // this should be 0 or more

		return (fupd == 1 && fcdel > 0);

	}

	int batchValidate(HashMap<String,Boolean> vm) throws SQLException
	{

		int count = 0;
		PreparedStatement ps1 = conn.prepareStatement("update mufields set valid=? where guid=?");
	   // PreparedStatement ps2 = conn.prepareStatement("delete from FIELDCELLS where field_guid=?");

		for (String guid : vm.keySet())
		{
			ps1.setBoolean(1,vm.get(guid));
			ps1.setString(2,guid);
		//ps2.setString(1,guid);

			count += ps1.executeUpdate();  // this should be 1 or 0
		}
		return count;

	}

	int batchDeleteFields(ArrayList<String> dl) throws SQLException
	{

		int count = 0;
		PreparedStatement ps1 = conn.prepareStatement("delete from mufields where guid=?");
		PreparedStatement ps2 = conn.prepareStatement("delete from FIELDCELLS where field_guid=?");

		for (String guid : dl)
		{
			ps1.setString(1,guid);
			count += ps1.executeUpdate();  // this should be 1 or 0
		}
		/*
		for (String guid : dl)
		{
			ps2.setString(1,guid);
			ps2.executeUpdate(); // this should be 0 or more
		}
		*/
		return count;

	}

//unused
	boolean deleteField(String guid) throws SQLException
	{

		PreparedStatement ps1 = conn.prepareStatement("delete from mufields  where guid=?");
		PreparedStatement ps2 = conn.prepareStatement("delete from FIELDCELLS where field_guid=?");

		ps1.setString(1,guid);
		ps2.setString(1,guid);

		int fupd = 0;
		int fcdel = 0;
		fupd = ps1.executeUpdate();  // this should be 1 or 0
		fcdel = ps2.executeUpdate(); // this should be 0 or more

		return (fupd == 1 && fcdel > 0);

	}
	
// writeFieldCells SQLBulkDAO
	int rebuildCells (ArrayList<String[]> flist) throws SQLException
	{
		PreparedStatement ps = conn.prepareStatement("insert into fieldcells (field_guid,cellid) values (?,?)");

		int count=0;
		for (String[] outerField: flist)
		{
				S2Polygon s2polyOuter = getS2Polygon(outerField);
				S2CellUnion cellsOuter = getCells(s2polyOuter);
				String guidOuter = outerField[3].trim();
				for (S2CellId cid: cellsOuter)
				{
						// System.out.println("INSERT INTO FIELDCELLS VALUES ('" +guidOuter+ "', " + Long.toUnsignedString(cid.id() >> 32) + ");");
						ps.setString(1, guidOuter);
						ps.setLong(2, cid.id() >> 32); // DB only supports signed 64 bit
						count += ps.executeUpdate();
				}
		}
		return count;
	}

	static String dtPolygon (String[] flist,String colour)
	{
			int lat1 = Integer.valueOf(flist[7].trim());
			int lng1 = Integer.valueOf(flist[8].trim());
			int lat2 = Integer.valueOf(flist[10].trim());
			int lng2 = Integer.valueOf(flist[11].trim());
			int lat3 = Integer.valueOf(flist[13].trim());
			int lng3 = Integer.valueOf(flist[14].trim());
			return "{\"type\":\"polygon\",\"latLngs\":[{\"lat\":" + lat1/1000000.0 +
											   ",\"lng\":" + lng1/1000000.0 +
											   "},{\"lat\":" + lat2/1000000.0 +
											   ",\"lng\":" + lng2/1000000.0 +
											   "},{\"lat\":" + lat3/1000000.0 +
											   ",\"lng\":" + lng3/1000000.0 +
											   "}],\"color\":\"" + colour + "\"}";
	}

	static UniformDistribution remaining (S2CellUnion cells, S2CellId analyseId, HashMap<S2CellId,UniformDistribution> cellm, HashMap<S2CellId,Double>intersections, UniformDistribution muOuterError)
	{
			for (S2CellId cellOuter: cells)
			{
					if (!cellOuter.equals(analyseId))
					{
						if (cellm.containsKey(cellOuter))
						{
							UniformDistribution cellOuterMU = cellm.get(cellOuter);
							double areaOuter = intersections.get(cellOuter) * 6367.0 * 6367.0;

							UniformDistribution cma = cellOuterMU.mul(areaOuter);
							muOuterError = muOuterError.sub(cma);
						} else {
							// this is the short way of saying muOuterError - (cellOuterMu = [0,+infinity] * areaOuter)
							muOuterError.setLower(0.0);
						}
					}
			}
			return muOuterError;
	}

	static UniformDistribution remaining (S2CellUnion cells, HashSet<S2CellId> analyseId, HashMap<S2CellId,UniformDistribution> cellm, HashMap<S2CellId,Double>intersections, UniformDistribution muOuterError)
	{
			for (S2CellId cellOuter: cells)
			{
					if (!analyseId.contains(cellOuter))
					{
						if (cellm.containsKey(cellOuter))
						{
							UniformDistribution cellOuterMU = cellm.get(cellOuter);
							double areaOuter = intersections.get(cellOuter) * 6367.0 * 6367.0;

							UniformDistribution cma = cellOuterMU.mul(areaOuter);
							muOuterError = muOuterError.sub(cma);
						} else {
							// this is the short way of saying muOuterError - (cellOuterMu = [0,+infinity] * areaOuter)
							muOuterError.setLower(0.0);
						}
					}
			}
			return muOuterError;
	}

	static ArrayList<String[]> prune (ArrayList<String[]> flist, HashMap<S2CellId,UniformDistribution> cellm)
	{
			ArrayList<String[]>outList = new ArrayList<String[]>();
			for (String[] outerField: flist)
			{
					S2Polygon s2polyOuter = getS2Polygon(outerField);
					S2CellUnion cellsOuter = getCells(s2polyOuter);
					String guidOuter = outerField[3].trim();

					if (countMissingCells(cellsOuter,cellm) == 0)  // this is not likely to be false.
					{

							int muOuter = Integer.valueOf(outerField[2].trim());
							HashMap<S2CellId,Double> intersectionsOuter = getCellIntersection(cellsOuter,s2polyOuter);
							int improveCount = 0;
							for (S2CellId analyseId: cellsOuter)
							{
									UniformDistribution muOuterError = makeDist(outerField[2]);

									// System.err.println("mu: " + muOuterError);

									muOuterError = remaining(cellsOuter, analyseId, cellm, intersectionsOuter, muOuterError);
									double areaOuter = intersectionsOuter.get(analyseId) * 6367.0 * 6367.0;
									muOuterError=muOuterError.div(areaOuter);

									// determine if muOuterError improved cellm analyseId
									// should break as soon as we get 1
									if (cellm.get(analyseId).edgeWithin(muOuterError))
											improveCount ++;

									System.out.println (""+guidOuter+ " " + muOuter+ " " +analyseId.toToken() +  " " + muOuterError + " <-> " + cellm.get(analyseId) + " " + improveCount);

							}
							if (improveCount>0)
									outList.add(outerField);
							// We also want to check if there are no other fields covering these cells, we should keep this field.

					} else {
							outList.add(outerField);
					}
			}
			return outList;
	}

	static ArrayList<String[]> dedupe(ArrayList<String[]> flist)
	{
		ArrayList<String[]>outList = new ArrayList<String[]>();
		HashSet<String>exists = new HashSet<>();
		int n = flist.size();
		for (int io =0; io < n; io++)
		{
				String[] outerField = flist.get(io);
				String key = outerField[2] +":"+ outerField[7] +":"+ outerField[8] +":"+ outerField[10] +":"+ outerField[11] +":"+ outerField[13] +":"+ outerField[14];
				if (!exists.contains(key))
				{
					exists.add(key);
					System.out.println ("" + io + "/" + n + " " +outerField[3]);
					outList.add(outerField);
				}
		}
		return outList;
	}

	UniformDistribution process(ArrayList<String[]> flist,HashMap<S2CellId,UniformDistribution> cellm, String s2token) throws UniformDistributionException
	{
		ArrayList<String[]> processList = new ArrayList<String[]>();
	   // HashMap<String,S2CellUnion> fieldUnion = new HashMap<String,S2CellUnion>();
	   // HashMap<String,HashMap<S2CellId,Double>> fieldIntersections = new HashMap<String,HashMap<S2CellId,Double>>();

		S2CellId io =  S2CellId.fromToken(s2token);


		for (String[] outerField : flist)
		{
			String guidOuter = outerField[3].trim();

			S2Polygon s2polyOuter = getS2Polygon(outerField);
			S2CellUnion cellsOuter = getCells(s2polyOuter);

			for (S2CellId analyseId : cellsOuter)
			{
				if (analyseId.equals(io))
				{
					HashMap<S2CellId,Double> intersectionsOuter = getCellIntersection(cellsOuter,s2polyOuter);
					fieldUnion.put(guidOuter,cellsOuter);
					fieldIntersections.put(guidOuter,intersectionsOuter);
					processList.add(outerField);
				}
			}
		}

		System.out.println("" + processList.size() + " suitable fields found.");

		ArrayList<String[]>bestFieldList = new ArrayList<String[]>();
		ArrayList<UniformDistribution>bestMUList = new ArrayList<>();
		for (String[] ii : processList)
		{
			String guidOuter = ii[3].trim();

			S2CellUnion cellsOuter = fieldUnion.get(guidOuter);

			// int muOuter = Integer.valueOf(ii[2].trim());

			HashMap<S2CellId,Double> intersectionsOuter = fieldIntersections.get(guidOuter);
			UniformDistribution muOuterError = makeDist(ii[2]);

			muOuterError = remaining(cellsOuter, io, cellm, intersectionsOuter, muOuterError);
			double areaOuter = intersectionsOuter.get(io) * 6367.0 * 6367.0;
			muOuterError=muOuterError.div(areaOuter);

			// bestFieldList.add(ii);
			try {
				muOuterError.clampLower(0.0);
				bestMUList.add(muOuterError);
				bestFieldList.add(ii);
			} catch (UniformDistributionException e) {
				;
			}
		}

		UniformDensityCurve udc = new UniformDensityCurve(bestMUList);
		System.out.println (io.toToken() + " " + udc.getPeakValue() + " " + udc.getPeakDistribution() +  " " + udc.allValid());
		for (int i=0; i<bestMUList.size(); i++)
		{
			UniformDistribution tud = bestMUList.get(i);
			String[] f = bestFieldList.get(i);
			System.out.println(f[3].trim() + " " + f[2] + " " + tud + " " + udc.isValid(tud) );
		}

		UniformDistribution res = udc.getPeakDistribution();

		res.clampLower(0.0);
		res.clampUpper(1000000.0);

		return udc.getPeakDistribution();

	}

	HashMap<S2CellId,UniformDistribution> revalidate (ArrayList<String[]> flist,HashMap<S2CellId,UniformDistribution> newCellm )
	{
		ArrayList<String[]>outList = new ArrayList<String[]>();
		//HashMap<S2CellId,UniformDistribution> newCellm = new HashMap<S2CellId,UniformDistribution>();
	   // int cellmCount = newCellm.size();
		HashSet<S2CellId> processedCells = new HashSet<S2CellId>();
		HashMap<String,Integer> updateCounts = new HashMap<String,Integer>();

		HashSet<String> brokenGuid = new HashSet<String>();

		int count;

		populate(flist);

		do {
			count = 0;
			int cellmCount = newCellm.size();

			HashSet<String[]> nextFields = new HashSet<String[]>();

			int ksn = cellField.size();

			System.out.println("" + ksn + " cells to process");
			ArrayList<S2CellId> keys = new ArrayList<S2CellId>(cellField.keySet());
			for (int iii = 0 ; iii<ksn; iii++ )
			{
				S2CellId io = keys.get(iii);
				//System.out.println("Processing cell: " + io.toToken() + " " + cellField.get(io).size() + " fields to process");

				ArrayList<String[]>bestFieldList = new ArrayList<String[]>();
				ArrayList<UniformDistribution>bestMUList = new ArrayList<>();

				for (String[] ii : cellField.get(io))
				{
					String guidOuter = ii[3].trim();

					S2CellUnion cellsOuter = fieldUnion.get(guidOuter);

				   // int muOuter = Integer.valueOf(ii[2].trim());

					//System.out.println("Field: " + guidOuter + " " + cellsOuter.size() + " cells " + muOuter + " mu");


					HashMap<S2CellId,Double> intersectionsOuter = fieldIntersections.get(guidOuter);
					UniformDistribution muOuterError = makeDist(ii[2]);
				   
					//System.err.println("before mu: " + muOuterError);

					muOuterError = remaining(cellsOuter, io, newCellm, intersectionsOuter, muOuterError);
					//System.err.println("after mu: " + muOuterError);

					double areaOuter = intersectionsOuter.get(io) * 6367.0 * 6367.0;
					muOuterError=muOuterError.div(areaOuter);

					bestFieldList.add(ii);
				   // try {
					   // muOuterError.clampLower(0.0);
						bestMUList.add(muOuterError);
				   // } catch (UniformDistributionException e) {
					//    ;
				   // }

				}

				//System.out.println("Build curve: " + bestMUList.size() + " distributions");
				// here we check for disagreements. where the muErrors do not overlap

				UniformDensityCurve udc = new UniformDensityCurve(bestMUList);
				//System.out.println("Curve Built.");

				boolean header = false;
				for (int i=0; i < bestMUList.size(); i++)
				{
					String[] f = bestFieldList.get(i);
					UniformDistribution tmu = bestMUList.get(i);
					boolean oldvalid = Boolean.parseBoolean(f[15]);
					String guid = f[3].trim();
					if (udc.isValid(tmu) != oldvalid) {
						if (!header)
						{
						 //   System.out.println ("" + iii + "/" + ksn + " " + io.toToken() + " " + udc.getPeakValue() + " " + udc.getPeakDistribution() +  " " + udc.countInvalid());
							header = true;
						}
						UniformDistribution mu = getEstMu(f,newCellm);
					   // System.out.println("" + guid] + " " + bestMUList.get(i)+ " est: " + mu + " mu: " + f[2] + " " + oldvalid);
						try {
							setFieldValid(guid, !oldvalid);
							f[15] = "" + (!oldvalid);
							if (updateCounts.containsKey(guid))
							{
								Integer c=updateCounts.get(guid);
								c++;
								updateCounts.put(guid,c);
							} else {
								updateCounts.put(guid,1);
							}
						} catch (SQLException e) {
							System.err.println("Unable to update field: " + f[3]);
						}
						
					}
				}
				   // for (double[] g : udc.getXYPlot())
				   //     System.out.println(""+g[0]+ " " + g[1]);
				
			   // {
				   /*
					for (int iud =0; iud < bestFieldList.size(); iud ++)
					{
						UniformDistribution tmu = bestMUList.get(iud);
						String[] fmu = bestFieldList.get(iud);
					   // if (!udc.isValid(tmu))
						System.out.println(io.toToken() + " " + fmu[3] + " " + tmu + " " + udc.isValid(tmu));
						for (S2CellId cid : fieldUnion.get(fmu[3].trim()))
							System.out.print(cid.toToken() + " ");
						System.out.println("");
						if (!udc.isValid(tmu))
						{
							brokenGuid.add(fmu[3].trim());
							System.out.println("[" + dtPolygon(fmu,"#ffffff") + "]");
						}
					}
					for (String g : brokenGuid)
						System.out.print ( g + " ");
					System.out.println("");
					for (double[] xy : udc.getXYPlot())
					{
						System.out.println("" + xy[0] + " " + xy[1]);
					}
						*/
			   // }

				try {
				 //   String old = newCellm.get(io).toString();
			   //  if (udc.getPeakValue() > 1)
				// {
					UniformDistribution ud = udc.getPeakDistribution();
					ud.clampLower(0.0);
					
					if (newCellm.containsKey(io))
					{
					   // if (newCellm.get(io).improves(ud))
					   // {
							//System.out.println ("intersect: " + newCellm.get(io) + " -> " + ud);
						//    count++;

							//for (String[] ii : cellField.get(io))
							//    nextFields.add(ii);

					   // }
					   UniformDistribution cud = newCellm.get(io);

					  // if (io.toToken().equals("6ad635a4"))
					  // {
					  //     double diff = Math.abs(ud.mean() - cud.mean())  / ud.mean();
					  //     System.out.println("" + cud + " " + cud.mean() + " " + cud.perror());
					  //     System.out.println("" + ud + " " + ud.mean() + " " + ud.perror() + " " + diff);
					  // }

						if (Math.abs(ud.mean() - cud.mean())  / cud.mean() > 0.1)
						{
							count ++;
						}

						if (ud.getLower() > 0) {
							// I'm quite impressed with the results from UniformDensityCurve
							// as long as the cells stabilise.
							newCellm.put(io,ud);
						} else if (cud.getLower() == 0 && cud.improves(ud)) {
							newCellm.put(io,ud);
							count++;
						}

					} else {
						newCellm.put(io,ud);
						count++;
					}
						//for (String[] ii : cellField.get(io))
						//    nextFields.add(ii);

						/*
						if (newCellm.get(io).refine(udc.getPeakDistribution()))
						{
							//System.out.println ("" + iii + "/" + ksn + " " + io.toToken() + " " + udc.getPeakValue() + " P:" + udc.getPeakDistribution() + " <-> C:" + old + " " + udc.allValid());
							count ++;
							for (String[] ii : cellField.get(io))
								nextFields.add(ii);
						}
					} else {
					 */
				   // }
			   // } /*
			   // else {
				 //   if (newCellm.get(io).edgeWithin(udc.getPeakDistribution()))
				   //     System.out.println( io.toToken() + " " + udc.getPeakValue() + " P:" + udc.getPeakDistribution() + " <-> C:" + newCellm.get(io)+ " " + udc.allValid());
			  //  }
				
				} catch (UniformDistributionException e) {
				   System.out.println("REFINE ERROR: " + io.toToken() + " " + udc.getPeakValue() + " P:" + udc.getPeakDistribution() + " <-> C:" + newCellm.get(io)+ " " + udc.allValid());
				}

			}

			/*
			cellField = new HashMap<S2CellId,HashSet<String[]>>();
			for (String[] outerField : nextFields)
			{
				String guidOuter = outerField[3].trim();
				S2CellUnion cellsOuter = fieldUnion.get(guidOuter);
				for (S2CellId analyseId : cellsOuter)
				{
					if (!cellField.containsKey(analyseId))
						cellField.put(analyseId,new HashSet<String[]>());
					cellField.get(analyseId).add(outerField);
				}
			}
			*/

		// if (updateCounts.size() < 10)
	  List<Entry<String, Integer>> updatelist = new ArrayList<>(updateCounts.entrySet());
	   updatelist.sort(Entry.comparingByValue());
	   for (Entry<String, Integer> e : updatelist)
				System.out.println("" + e.getValue() + " " + e.getKey() );

		 System.out.println ("" + count + " cells updated.");



		} while (count > 0);
		return newCellm;
	}

	static UniformDistribution makeDist(String mus)
	{
		int mu = Integer.parseInt(mus.trim());

		if (mu == 1)
			return new UniformDistribution(0,1.5);
		return new UniformDistribution(mu,0.5);
	}

	HashMap<S2CellId,UniformDistribution> cellParents(HashMap<S2CellId,UniformDistribution> cellm)
	{
		HashMap<S2CellId,UniformDistribution> ncellm = new HashMap<S2CellId,UniformDistribution>(cellm);
		for (int tlevel = 12; tlevel >= 6; tlevel --)
		{
			for (S2CellId cid : cellm.keySet())
			{
				int level = cid.level();
				if (level == tlevel)
				{
					StringBuilder serr = new StringBuilder(cid.toToken());
					serr.append(" = (");

					try {
						//parent = avg(children)
						UniformDistribution ncud = new UniformDistribution(0.0,0.0);
						boolean first = true;
						for(S2CellId c = cid.childBegin(); !c.equals(cid.childEnd()); c = c.next())
						{
							if (!first)
								serr.append(" + ");
							first = false;
							if (cellm.containsKey(c))
								ncud = ncud.add(cellm.get(c));
							else
							{
								ncud = null;
								break;
							}
							serr.append(cellm.get(c));
						}
						serr.append(") / 4");
						if (ncud != null)
						{
							ncud = ncud.div(4);
							UniformDistribution cud = cellm.get(cid);
							if (cud.refine(ncud))
							{
								System.err.println("Parent update: " + cid.toToken() + " = " + cud);
								System.err.println(serr.toString());
								ncellm.put(cid,cud);
							}
						}
					} catch (UniformDistributionException e) {
						System.err.println("parent refine error: " + cid.toToken() + " " + e.getMessage());
						System.err.println(serr.toString());

					}
					// child4 = parent * 4 - c1 - c2 - c3
					for(S2CellId co = cid.childBegin(); !co.equals(cid.childEnd()); co = co.next())
					{
						serr = new StringBuilder (co.toToken());
						serr.append(" = 4 * ");
						serr.append(cellm.get(cid));

						try {
							UniformDistribution cncud = cellm.get(cid).mul(4);
							boolean first = true;
							for(S2CellId ci = cid.childBegin(); !ci.equals(cid.childEnd()); ci = ci.next())
							{
								if (!co.equals(ci))
								{
									if (cellm.containsKey(ci))
										cncud = cncud.sub(cellm.get(ci));
									else
									{
										cncud = null;
										break;
									}
									serr.append(" - ");
									serr.append(cellm.get(ci));
								}
							}
							if (cncud != null)
							{
								if (cellm.containsKey(co))
								{
									UniformDistribution cud = cellm.get(co);
									if (cud.refine(cncud))
									{
										System.err.println("Update child: " + co.toToken() + " = " + cud);
										System.err.println(serr.toString());

										ncellm.put(co,cud);
									}
								} else {
									cncud.clampLower(0.0);
									System.err.println("Set child: " + co.toToken() + " = " + cncud);
									System.err.println(serr.toString());

									ncellm.put(co,cncud);
								}
							}
						} catch (UniformDistributionException e) {
							System.err.println("child refine error: " +co.toToken() + " " + e.getMessage());
							System.err.println(serr.toString());

						}
					}

				}
			}
		}
		return ncellm;
	}

	int best (ArrayList<String[]> flist,HashMap<S2CellId,UniformDistribution> cellm)
	{
		populate(flist);
		int ksn = cellField.size();
		HashMap<String,String[]> bestFields = new HashMap<>();
		ArrayList<S2CellId> keys = new ArrayList<S2CellId>(cellField.keySet());

		for (int iii = 0 ; iii<ksn; iii++ )
		{
			S2CellId io = keys.get(iii);
			ArrayList<UniformDistribution>bestMUList = new ArrayList<>();
			ArrayList<String[]>bestFieldList = new ArrayList<>();
			for (String[] ii : cellField.get(io))
			{
				String guidOuter = ii[3].trim();

				S2CellUnion cellsOuter = fieldUnion.get(guidOuter);

				HashMap<S2CellId,Double> intersectionsOuter = fieldIntersections.get(guidOuter);
				UniformDistribution muOuterError = makeDist(ii[2]);

				//System.err.println("before mu: " + muOuterError);

				muOuterError = remaining(cellsOuter, io, cellm, intersectionsOuter, muOuterError);
				//System.err.println("after mu: " + muOuterError);

				double areaOuter = intersectionsOuter.get(io) * 6367.0 * 6367.0;
				muOuterError=muOuterError.div(areaOuter);

				bestFieldList.add(ii);
				// try {
					// muOuterError.clampLower(0.0);
				bestMUList.add(muOuterError);
				// } catch (UniformDistributionException e) {
				//    ;
				// }

			}

			UniformDensityCurve udc = new UniformDensityCurve(bestMUList);

			for (int k=0; k < bestMUList.size(); k++)
			{
				if (udc.isPeak(bestMUList.get(k)))
				{
					String guid = bestFieldList.get(k)[3].trim();
					bestFields.put(guid,bestFieldList.get(k));
				}
			}
		}
		printTable(bestFields.values());
		return bestFields.size();
	}

	HashMap<S2CellId,UniformDistribution> refine (ArrayList<String[]> flist,HashMap<S2CellId,UniformDistribution> cellm)
	{
		ArrayList<String[]>outList = new ArrayList<String[]>();
		HashMap<S2CellId,UniformDistribution> newCellm = new HashMap<S2CellId,UniformDistribution>(cellm);
		int cellmCount = cellm.size();
		HashSet<S2CellId> processedCells = new HashSet<S2CellId>();
		HashMap<S2CellId,Integer> updateCounts = new HashMap<S2CellId,Integer>();

		HashSet<String> brokenGuid = new HashSet<String>();
		int iterations = 0;
		int count;

		populate(flist);

		do {
			updateCounts = new HashMap<S2CellId,Integer>();

			iterations ++;
			count = 0;
			cellmCount = newCellm.size();

			HashSet<String[]> nextFields = new HashSet<String[]>();

			int ksn = cellField.size();

			System.out.println("" + ksn + " cells to process");
			ArrayList<S2CellId> keys = new ArrayList<S2CellId>(cellField.keySet());
			for (int iii = 0 ; iii<ksn; iii++ )
			{
				S2CellId io = keys.get(iii);
				
				// TRACE LOGGING: Track if this is our target cell
				boolean isTraceCell = (cellTrace != null && io.equals(cellTrace));
				UniformDistribution oldValue = null;
				if (isTraceCell) {
					oldValue = newCellm.get(io);
				}
				
				//System.out.println("Processing cell: " + io.toToken() + " " + cellField.get(io).size() + " fields to process");

				ArrayList<String[]>bestFieldList = new ArrayList<String[]>();
				ArrayList<UniformDistribution>bestMUList = new ArrayList<>();

				for (String[] ii : cellField.get(io))
				{
					String guidOuter = ii[3].trim();

					S2CellUnion cellsOuter = fieldUnion.get(guidOuter);

				   // int muOuter = Integer.valueOf(ii[2].trim());

					//System.out.println("Field: " + guidOuter + " " + cellsOuter.size() + " cells " + muOuter + " mu");


					HashMap<S2CellId,Double> intersectionsOuter = fieldIntersections.get(guidOuter);
					UniformDistribution muOuterError = makeDist(ii[2]);

					//System.err.println("before mu: " + muOuterError);

					muOuterError = remaining(cellsOuter, io, newCellm, intersectionsOuter, muOuterError);
					//System.err.println("after mu: " + muOuterError);

					double areaOuter = intersectionsOuter.get(io) * 6367.0 * 6367.0;
					muOuterError=muOuterError.div(areaOuter);

					bestFieldList.add(ii);
				   // try {
					   // muOuterError.clampLower(0.0);
						bestMUList.add(muOuterError);
				   // } catch (UniformDistributionException e) {
					//    ;
				   // }

				}

				//System.out.println("Build curve: " + bestMUList.size() + " distributions");
				// here we check for disagreements. where the muErrors do not overlap

				UniformDensityCurve udc = new UniformDensityCurve(bestMUList);
				//System.out.println("Curve Built.");

				if (!udc.allValid())
				{
					System.out.println ("" + iii + "/" + ksn + " " + io.toToken() + " " + udc.getPeakValue() + " " + udc.getPeakDistribution() +  " " + udc.countInvalid());
					for (int i=0; i < bestMUList.size(); i++)
						if (!udc.isValid(bestMUList.get(i))) {
							String[] f = bestFieldList.get(i);
							UniformDistribution mu = getEstMu(f,newCellm);
							long epoch = Integer.toUnsignedLong(Integer.parseInt(f[4]));
							Date time = new Date(epoch);
							System.out.println("" + f[3] +" " + time + " ("+ epoch+ ") " + bestMUList.get(i)+ " est: " + mu + " mu: " + f[2]);
							
						}
				   // for (double[] g : udc.getXYPlot())
				   //     System.out.println(""+g[0]+ " " + g[1]);
				}
			   // {
				   /*
					for (int iud =0; iud < bestFieldList.size(); iud ++)
					{
						UniformDistribution tmu = bestMUList.get(iud);
						String[] fmu = bestFieldList.get(iud);
					   // if (!udc.isValid(tmu))
						System.out.println(io.toToken() + " " + fmu[3] + " " + tmu + " " + udc.isValid(tmu));
						for (S2CellId cid : fieldUnion.get(fmu[3].trim()))
							System.out.print(cid.toToken() + " ");
						System.out.println("");
						if (!udc.isValid(tmu))
						{
							brokenGuid.add(fmu[3].trim());
							System.out.println("[" + dtPolygon(fmu,"#ffffff") + "]");
						}
					}
					for (String g : brokenGuid)
						System.out.print ( g + " ");
					System.out.println("");
					for (double[] xy : udc.getXYPlot())
					{
						System.out.println("" + xy[0] + " " + xy[1]);
					}
						*/
			   // }

				try {
				 //   String old = newCellm.get(io).toString();
			   //  if (udc.getPeakValue() > 1)
				// {
					UniformDistribution ud = udc.getPeakDistribution();
					ud.clampLower(0.0);
					
					if (newCellm.containsKey(io))
					{
					   // if (newCellm.get(io).improves(ud))
					   // {
							//System.out.println ("intersect: " + newCellm.get(io) + " -> " + ud);
						//    count++;

							//for (String[] ii : cellField.get(io))
							//    nextFields.add(ii);

					   // }
					   UniformDistribution cud = newCellm.get(io);

					  // if (io.toToken().equals("6ad635a4"))
					  // {
					  //     double diff = Math.abs(ud.mean() - cud.mean())  / ud.mean();
					  //     System.out.println("" + cud + " " + cud.mean() + " " + cud.perror());
					  //     System.out.println("" + ud + " " + ud.mean() + " " + ud.perror() + " " + diff);
					  // }

						if (Math.abs(ud.mean() - cud.mean())  / cud.mean() > 0.1)
						{

							if (updateCounts.containsKey(io))
							{
								Integer c = updateCounts.get(io);
								c++;
								updateCounts.put(io,c);
							}
							else
							{
								updateCounts.put(io,1);
							}
							count ++;
						}

						if (ud.getLower() > 0) {
							// I'm quite impressed with the results from UniformDensityCurve
							// as long as the cells stabilise.
							newCellm.put(io,ud);
						} else if (cud.getLower() == 0 && cud.improves(ud)) {
							newCellm.put(io,ud);
							count++;
							if (updateCounts.containsKey(io))
							{
								Integer c = updateCounts.get(io);
								c++;
								updateCounts.put(io,c);
							}
							else
							{
								updateCounts.put(io,1);
							}
						}

					} else {
						newCellm.put(io,ud);
						count++;
						if (updateCounts.containsKey(io))
						{
							Integer c = updateCounts.get(io);
							c++;
							updateCounts.put(io,c);
						}
						else
						{
							updateCounts.put(io,1);
						}
					}
						//for (String[] ii : cellField.get(io))
						//    nextFields.add(ii);

						/*
						if (newCellm.get(io).refine(udc.getPeakDistribution()))
						{
							//System.out.println ("" + iii + "/" + ksn + " " + io.toToken() + " " + udc.getPeakValue() + " P:" + udc.getPeakDistribution() + " <-> C:" + old + " " + udc.allValid());
							count ++;
							for (String[] ii : cellField.get(io))
								nextFields.add(ii);
						}
					} else {
					 */
				   // }
			   // } /*
			   // else {
				 //   if (newCellm.get(io).edgeWithin(udc.getPeakDistribution()))
				   //     System.out.println( io.toToken() + " " + udc.getPeakValue() + " P:" + udc.getPeakDistribution() + " <-> C:" + newCellm.get(io)+ " " + udc.allValid());
			  //  }
				
				} catch (UniformDistributionException e) {
				   System.out.println("REFINE ERROR: " + io.toToken() + " " + udc.getPeakValue() + " P:" + udc.getPeakDistribution() + " <-> C:" + newCellm.get(io)+ " " + udc.allValid());
				}

			}

			/*
			cellField = new HashMap<S2CellId,HashSet<String[]>>();
			for (String[] outerField : nextFields)
			{
				String guidOuter = outerField[3].trim();
				S2CellUnion cellsOuter = fieldUnion.get(guidOuter);
				for (S2CellId analyseId : cellsOuter)
				{
					if (!cellField.containsKey(analyseId))
						cellField.put(analyseId,new HashSet<String[]>());
					cellField.get(analyseId).add(outerField);
				}
			}
			*/

			if (updateCounts.size() < 10)
			{
				List<Entry<S2CellId, Integer>> updatelist = new ArrayList<>(updateCounts.entrySet());
				updatelist.sort(Entry.comparingByValue());
				for (Entry<S2CellId, Integer> e : updatelist)
					System.out.println("updated: " + e.getValue() + " " + e.getKey().toToken()  + " " + newCellm.get(e.getKey()));
			}

			System.out.println ("iteration:" + iterations + " " + count + " cells updated.");

			if (iterations % 20 == 0)
			{
				System.out.println("Updating DB.");
				try {
					int d = deleteCells();
					System.out.println("" + d + " cells erased.");
					int cr = writeCells(newCellm);
					System.out.println("" + cr + " rows updated.");
				} catch (SQLException e) {
					System.out.println("Unable to update the DB.");
				}
			}

		} while (count > 0);
		return newCellm;
	}

	HashMap<S2CellId,UniformDistribution> agreements (ArrayList<String[]> flist,HashMap<S2CellId,UniformDistribution> cellw)
	{
		ArrayList<String[]>outList = new ArrayList<String[]>();
		HashMap<S2CellId,UniformDistribution> newCellm = new HashMap<S2CellId,UniformDistribution>(cellw);
		int cellmCount = newCellm.size();
		HashSet<S2CellId> processedCells = new HashSet<S2CellId>();
		int count = 0;
		int iteration = 0;
		do {
			iteration++;
			count = newCellm.size();
			int n = flist.size();

			HashMap<S2CellId,HashSet<String[]>> cellField = new HashMap<S2CellId,HashSet<String[]>>();
			System.out.println("extend phase prepopulating: " + n);

			for (int io =0; io < n; io++)
			{
				String[] outerField = flist.get(io);
				S2Polygon s2polyOuter = getS2Polygon(outerField);
				S2CellUnion cellsOuter = getCells(s2polyOuter);
				if (countMissingCells(cellsOuter,newCellm) == 1)
				{

					S2CellId analyseId = getMissingCell(cellsOuter, newCellm);
					if (!cellField.containsKey(analyseId))
						cellField.put(analyseId,new HashSet<String[]>());
					cellField.get(analyseId).add(outerField);
				}
			}
			System.out.println("" + cellField.size() + " cells to process.");
			for (S2CellId io : cellField.keySet())
			{
				// TRACE LOGGING: Log iteration start for target cell
				if (cellTrace != null && io.equals(cellTrace)) {
					System.out.println("\n=== BUILD Iteration " + iteration + " ===");
					System.out.println("Processing target cell: " + io.toToken());
				}
				
				ArrayList<String[]>bestFieldList = new ArrayList<String[]>();
				ArrayList<UniformDistribution>bestMUList = new ArrayList<>();

				for (String[] ii : cellField.get(io))
				{
					S2Polygon s2polyOuter = getS2Polygon(ii);
					S2CellUnion cellsOuter = getCells(s2polyOuter);
					String guidOuter = ii[3].trim();

					int muOuter = Integer.valueOf(ii[2].trim());
					HashMap<S2CellId,Double> intersectionsOuter = getCellIntersection(cellsOuter,s2polyOuter);

					UniformDistribution muOuterError = makeDist(ii[2]); // should move this into UniformDistribution

					// System.err.println("mu: " + muOuterError);

					// TRACE LOGGING: Log field contribution details
					if (cellTrace != null && io.equals(cellTrace)) {
						System.out.println("\nField: " + guidOuter + " (MU: " + muOuter + ")");
						System.out.println("  Scaled MU: " + makeDist(ii[2]));
						System.out.println("  Other cell contributions:");
						for (S2CellId otherCell : cellsOuter) {
							if (!otherCell.equals(io) && newCellm.containsKey(otherCell)) {
								double area = intersectionsOuter.get(otherCell) * 6367.0 * 6367.0;
								UniformDistribution contrib = newCellm.get(otherCell).mul(area);
								System.out.println("    " + otherCell.toToken() + ": " + newCellm.get(otherCell) + " * " + area + " = " + contrib);
							}
						}
					}
					
					muOuterError = remaining(cellsOuter, io, newCellm, intersectionsOuter, muOuterError);
					double areaOuter = intersectionsOuter.get(io) * 6367.0 * 6367.0;
					muOuterError=muOuterError.div(areaOuter);
					
					// TRACE LOGGING: Log remaining and contribution
					if (cellTrace != null && io.equals(cellTrace)) {
						System.out.println("  Remaining for target: " + muOuterError.mul(areaOuter));
						System.out.println("  Target area: " + areaOuter);
						System.out.println("  Contribution: " + muOuterError);
					}

					bestFieldList.add(ii);
					bestMUList.add(muOuterError);

				}

				// here we check for disagreements. where the muErrors do not overlap

				UniformDensityCurve udc = new UniformDensityCurve(bestMUList);

				if (!udc.allValid())
					System.out.println (io.toToken() + " " + udc.getPeakValue() + " " + udc.getPeakDistribution() + " " + udc.allValid());

				//if (udc.getPeakValue() > 1) // a bit more restrictive but lets see if it fixes refinement errors.
			   // {
					UniformDistribution celld = udc.getPeakDistribution();
					try {
						celld.clampLower(0.0);
						celld.clampUpper(1000000.0);
						newCellm.put(io,celld);
						
						// TRACE LOGGING: Log final UDC result
						if (cellTrace != null && io.equals(cellTrace)) {
							System.out.println("\n  UDC Analysis:");
							System.out.println("    " + bestFieldList.size() + " fields contribute");
							System.out.println("    Peak: " + celld);
							System.out.println("    All valid: " + udc.allValid());
							System.out.println("  FINAL VALUE: " + celld);
						}

					} catch (UniformDistributionException e) {
						System.err.println("Clamp EXCEPTION. " + celld);
					}
				//} else {
				//    System.out.println (io.toToken() + " " + udc.getPeakValue() + " " + udc.getPeakDistribution() + " " + udc.allValid());

				//}

			}
		} while ( count != newCellm.size());
			System.out.println("CELL count: " + newCellm.size());
		//    System.out.println("field size: " + outList.size());
	   // } while (cellmCount != cellm.size());
		return newCellm;
	}

	ArrayList<String[]> iprune (ArrayList<String[]> flist)
	{
		ArrayList<String[]>outList = new ArrayList<String[]>();
		HashMap<S2CellId,UniformDistribution> cellm = new HashMap<S2CellId,UniformDistribution>();
		int cellmCount = cellm.size();
		HashSet<S2CellId> processedCells = new HashSet<S2CellId>();

		populate(flist);

		do {
			cellmCount = cellm.size();

			for (S2CellId io : cellField.keySet())
			{
				ArrayList<String[]>bestFieldList = new ArrayList<String[]>();
				ArrayList<UniformDistribution>bestMUList = new ArrayList<>();

				for (String[] ii : cellField.get(io))
				{
					//S2Polygon s2polyOuter = getS2Polygon(ii);
					String guidOuter = ii[3].trim();
 //                   int muOuter = Integer.valueOf(ii[2].trim());

					UniformDistribution muOuterError = makeDist(ii[2]);
					// System.err.println("mu: " + muOuterError);

					HashMap<S2CellId,Double> intersectionsOuter = fieldIntersections.get(guidOuter);
					S2CellUnion cellsOuter = fieldUnion.get(guidOuter);

					muOuterError = remaining(cellsOuter, io, cellm, intersectionsOuter, muOuterError);
					double areaOuter = intersectionsOuter.get(io) * 6367.0 * 6367.0;
					muOuterError=muOuterError.div(areaOuter);

					bestFieldList.add(ii);
					bestMUList.add(muOuterError);

				}

				// here we check for disagreements. where the muErrors do not overlap

				UniformDensityCurve udc = new UniformDensityCurve(bestMUList);

				System.out.println (io.toToken() + " " + udc.getPeakValue() + " " + udc.getPeakDistribution() + " " + udc.allValid());

				HashMap<String,Boolean> validMap = new HashMap<String,Boolean>();
				for (int i=0; i<bestFieldList.size(); i++ )
					validMap.put(bestFieldList.get(i)[3],udc.isValid(bestMUList.get(i)));
			
					try {
						int val= batchValidate(validMap);
						System.out.println("" + val + " fields re-validated");
					} catch (SQLException e) {
						System.err.println("Couldn't re-validate fields.");
					}
			   // if (udc.allValid())
			   // {
					ArrayList<String> delFields = new ArrayList<String>();
					if (udc.hasDualPeak(bestMUList))
					{
						for (int i=0; i<bestFieldList.size(); i++ )
						{
							if (udc.isDualPeak(bestMUList.get(i)))
							{
								// outList.add(bestFieldList.get(i));
								System.out.println(io.toToken() + " Dual: " + bestMUList.get(i));
							} else {
								delFields.add(bestFieldList.get(i)[3]);

							}
						}
					} else {
						int lowerField = 0;
						int upperField = 0;
						for (int i=0; i<bestFieldList.size(); i++ )
						{
							if (udc.isLowerPeak(bestMUList.get(i)))
							{
								//outList.add(bestFieldList.get(i));
								System.out.println(io.toToken() + " Lower: " + bestMUList.get(i));
								lowerField = i;
								break;
							}
						}
						for (int i=0; i<bestFieldList.size(); i++)
						{
							if (udc.isUpperPeak(bestMUList.get(i)))
							{
								// outList.add(bestFieldList.get(i));
								System.out.println(io.toToken() + " Upper: " + bestMUList.get(i));
								upperField = i;
								break;
							}
						}
						for (int i=0; i<bestFieldList.size(); i++)
						{
							if (i != lowerField && i != upperField)
							{
								delFields.add(bestFieldList.get(i)[3]);
							}
						}
					}

					try {
						int val = batchDeleteFields(delFields);
						System.out.println("" + val + " fields deleted.");
					} catch (SQLException e) {
						System.err.println("Cannot delete fields.");
					}

					// System.out.println("field size: " + outList.size());


					UniformDistribution celld = udc.getPeakDistribution();
					try {
						celld.clampLower(0.0);
						cellm.put(io,celld);

					} catch (UniformDistributionException e) {
						System.err.println("Lower Clamp EXCEPTION. " + celld);
					}
/*
				} else {
					for (int i=0; i<bestFieldList.size(); i++)
					{
						if (udc.isValid(bestMUList.get(i)))
						{
							String[] f = bestFieldList.get(i);
							System.out.println(io.toToken() + " Valid: " + bestMUList.get(i) + " " + f[3]);
						} else {
							String[] f = bestFieldList.get(i);
							System.out.println(io.toToken() + " Invalid: " + bestMUList.get(i) + " " + f[3]);
						}
					}
				}
*/
			}
			System.out.println("CELL count: " + cellm.size());
			System.out.println("field size: " + outList.size());
		} while (cellmCount != cellm.size());
		return outList;
	}

	void populate (ArrayList<String[]> flist)
	{
		int n = flist.size();
		System.err.println("prepopulating: " + n);

		for (int io =0; io < n; io++)
		{
			String[] outerField = flist.get(io);
			String guidOuter = outerField[3].trim();

			S2Polygon s2polyOuter = getS2Polygon(outerField);
			S2CellUnion cellsOuter = getCells(s2polyOuter);
			HashMap<S2CellId,Double> intersectionsOuter = getCellIntersection(cellsOuter,s2polyOuter);
			
			fieldUnion.put(guidOuter,cellsOuter);
			fieldIntersections.put(guidOuter,intersectionsOuter);

			for (S2CellId analyseId : cellsOuter)
			{
				if (!cellField.containsKey(analyseId))
					cellField.put(analyseId,new HashSet<String[]>());
				cellField.get(analyseId).add(outerField);
			}
		}
	}
	void trace (S2CellId cid, ArrayList<String[]> flist,HashMap<S2CellId,UniformDistribution> cellm)
	{

	   // populate(flist);

	   ArrayList<String[]> bestFieldList = new ArrayList<String[]>();
	   ArrayList<UniformDistribution> bestMUList = new ArrayList<UniformDistribution>();

		for (String[] ii : cellField.get(cid))
		{
			String guidOuter = ii[3].trim();

			S2CellUnion cellsOuter = fieldUnion.get(guidOuter);

//            int muOuter = Integer.valueOf(ii[2].trim());
			HashMap<S2CellId,Double> intersectionsOuter = fieldIntersections.get(guidOuter);

			UniformDistribution muOuterError = makeDist(ii[2]);

			// System.err.println("mu: " + muOuterError);

			muOuterError = remaining(cellsOuter, cid, cellm, intersectionsOuter, muOuterError);
			double areaOuter = intersectionsOuter.get(cid) * 6367.0 * 6367.0;
			muOuterError=muOuterError.div(areaOuter);

			bestFieldList.add(ii);
			bestMUList.add(muOuterError);
		}

		ArrayList<String[]> bestFields = new ArrayList<String[]>();

		UniformDensityCurve udc = new UniformDensityCurve(bestMUList);

		System.out.println (cid.toToken() + " " + cellm.get(cid) + " " + cellm.get(cid).range() + " " + udc.getPeakValue() + " " + udc.getPeakDistribution() + " " + udc.allValid());
		for (int i=0; i < bestFieldList.size(); i++)
		{
			String[] f = bestFieldList.get(i);
			UniformDistribution tmu = bestMUList.get(i);
			if (udc.isPeak(tmu)) {
				int imu = Integer.parseInt(f[2]);
				UniformDistribution fmu = makeDist(f[2]);
				bestFields.add(f);
				S2Polygon s2poly = getS2Polygon(f);
				double area = s2poly.getArea() * 6367.0 * 6367.0;
				double mukm = 1.0 * imu / area;
				System.out.println(f[3] + " " + imu + " " + fmu.perror() * 100.0 + " " + tmu + " " + tmu.perror() * 100.0 + " " + area + " km " + mukm + " mukm");
				
				for (S2CellId tcid : fieldUnion.get(f[3].trim()))
					if (!tcid.equals(cid))
					{
						HashMap<S2CellId,Double> intersectionsOuter = fieldIntersections.get(f[3].trim());

						UniformDistribution cmu = cellm.get(tcid).mul(intersectionsOuter.get(tcid) * 6367.0 * 6367.0);
						double err = 100.0 * cmu.range() / imu;
						System.out.println("    " + tcid.toToken() + " " + cmu + " " + cmu.range());
					}

				System.out.println("[" + dtPolygon(f, "#FFFFFF") +"]");
				System.out.println("");

			}
		}



	}

	static UniformDistribution getEstMu(String[] f, HashMap<S2CellId,UniformDistribution> cellm)
	{
		S2Polygon fpoly = getS2Polygon(f);
		S2CellUnion funion = getCells(fpoly);
		HashMap<S2CellId,Double> fint =  getCellIntersection ( funion, fpoly);

		UniformDistribution mu = new UniformDistribution(0.0,0.0);

		for (S2CellId cid : funion)
		{
			double a = fint.get(cid) * 6367.0 * 6367.0;

			if (cellm.containsKey(cid))
			{
				mu = mu.add(cellm.get(cid).mul(a));
			   // System.out.println("" + cid.toToken() + " " + a + " x " + cellm.get(cid) + " = " + mu);

			} else {
			   // System.out.println("" + cid.toToken() + " " + a  + " x UNKNOWN");
				mu.setLower(0.0);
				mu.setUpper(Float.MAX_VALUE);
				break;
			}
		}
		return mu;
	}

	static int countMissingOrFullErrorCells (S2CellUnion c, HashMap<S2CellId,UniformDistribution> cellm)
	{
		int missing = 0;
		for (S2CellId cid: c)
		{
			if (!cellm.containsKey(cid))
			{
				missing++;
			} else if (cellm.get(cid).getLower() == 0.0) {
				missing ++;
			}
		}
		return missing;
	}

	static HashSet<S2CellId> getMissingOrFullErrorCells (S2CellUnion c, HashMap<S2CellId,UniformDistribution> cellm)
	{
		HashSet<S2CellId> result = new HashSet<S2CellId>();
		for (S2CellId cid: c)
		{
			if (!cellm.containsKey(cid))
			{
				result.add(cid);
			} else if (cellm.get(cid).getLower() == 0.0) {
				result.add(cid);
			}
		}
		return result;
	}

	static int countMissingCells (S2CellUnion c, HashMap<S2CellId,UniformDistribution> cellm)
	{
			int missing = 0;
			for (S2CellId cid: c)
					if (!cellm.containsKey(cid))
							missing++;
			return missing;
	}

	static S2CellId getMissingCell (S2CellUnion c, HashMap<S2CellId,UniformDistribution> cellm)
	{
			// assumes only 1 mmissing
			for (S2CellId cid: c)
					if (!cellm.containsKey(cid))
							return cid;
			return new S2CellId(0);
	}

	static S2Polygon getS2Polygon (String[] flist)
	{
			int lat1 = Integer.valueOf(flist[7].trim());
			int lng1 = Integer.valueOf(flist[8].trim());
			int lat2 = Integer.valueOf(flist[10].trim());
			int lng2 = Integer.valueOf(flist[11].trim());
			int lat3 = Integer.valueOf(flist[13].trim());
			int lng3 = Integer.valueOf(flist[14].trim());

			return getS2Polygon(lat1,lng1,lat2,lng2,lat3,lng3);
	}

	static S2CellUnion getCells(S2Polygon s2p)
	{
			S2RegionCoverer rc = new S2RegionCoverer();
			// ingress mu calculation specifics
			rc.setMaxLevel(13);
			rc.setMinLevel(0);
			rc.setMaxCells(20);
			return rc.getCovering (s2p);
	}

	static S2Polygon getS2Polygon (long lat1, long lng1, long lat2, long lng2, long lat3, long lng3)
	{

			S2Point p1 = S2LatLng.fromE6(lat1,lng1).toPoint();
			S2Point p2 = S2LatLng.fromE6(lat2,lng2).toPoint();
			S2Point p3 = S2LatLng.fromE6(lat3,lng3).toPoint();

			S2PolygonBuilder pb = new S2PolygonBuilder(S2PolygonBuilder.Options.UNDIRECTED_UNION);
			pb.addEdge(p1,p2);
			pb.addEdge(p2,p3);
			pb.addEdge(p3,p1);
			return pb.assemblePolygon();
	}

	static public HashMap<S2CellId,Double> getCellIntersection(S2CellUnion cells, S2Polygon fieldPoly)
	{

			HashMap<S2CellId,Double> polyArea = new HashMap<S2CellId,Double>();
			for (S2CellId cellid: cells)
			{
					S2Cell cell = new S2Cell(cellid);
					S2PolygonBuilder pb = new S2PolygonBuilder(S2PolygonBuilder.Options.UNDIRECTED_UNION);
					pb.addEdge(cell.getVertex(0),cell.getVertex(1));
					pb.addEdge(cell.getVertex(1),cell.getVertex(2));
					pb.addEdge(cell.getVertex(2),cell.getVertex(3));
					pb.addEdge(cell.getVertex(3),cell.getVertex(0));
					S2Polygon cellPoly = pb.assemblePolygon();
					S2Polygon intPoly = new S2Polygon();
					intPoly.initToIntersection(fieldPoly, cellPoly);

					polyArea.put(cellid,intPoly.getArea());
			}
			return polyArea;
			
	}

	private static String formatDouble(double value) {
		// First, format to 3 decimal places
		String s = String.format("%.3f", value);
		// Then, remove trailing zeros after the decimal point
		s = s.replaceAll("0+$", "");
		// Finally, remove the decimal point if it's the last character (e.g., "123." becomes "123")
		s = s.replaceAll("\\.$", "");
		return s;
	}

	static int printJson(ArrayList<String[]> fieldList)
	{
		int count = 0;
		boolean first = true;
		System.out.print("[");
		for (String[] r : fieldList)
		{
				if (!first)
						System.out.print(", ");
				first = false;
				System.out.print("{");
				System.out.print("\"creator\": \""+r[0]+"\"");
				System.out.print(", \"agent\": \""+r[1]+"\"");
				System.out.print(", \"mu\": "+r[2]);
				System.out.print(", \"guid\": \""+r[3].stripTrailing()+"\"");
				System.out.print(", \"timestamp\": \""+r[4]+"\"");
				System.out.print(", \"team\": \""+r[5]+"\"");
				System.out.print(", \"pguid1\": \""+r[6].stripTrailing()+"\"");
				System.out.print(", \"plat1\": "+r[7]);
				System.out.print(", \"plng1\": "+r[8]);
				System.out.print(", \"pguid2\": \""+r[9].stripTrailing()+"\"");
				System.out.print(", \"plat2\": "+r[10]);
				System.out.print(", \"plng2\": "+r[11]);
				System.out.print(", \"pguid3\": \""+r[12].stripTrailing()+"\"");
				System.out.print(", \"plat3\": "+r[13]);
				System.out.print(", \"plng3\": "+r[14]);
				System.out.print(", \"valid\": "+r[15]);
				System.out.print("}");
				count ++;
		}
		System.out.println("]");
		return count;
	}

	static int printTable(Iterable<String[]> fieldList)
	{
		int count = 0;
		for (String[] r : fieldList)
		{
			boolean first = true;

			for (String s : r)
			{
				if (!first)
						System.out.print(" |");
				first = false;
				System.out.print(s);
			}
			System.out.println("");
			count ++;
		}
		return count;
	}

	static ArrayList<String[]> readFileFields (String name) throws IOException
	{
			ArrayList<String[]> fieldList = new ArrayList<String[]>();

			BufferedReader br = new BufferedReader(new FileReader(name));
			String line;
			while ((line = br.readLine()) != null) {
			// Use the split() method to split each line into fields based on '|' delimiter
					String[] fields = line.split("\\|");
			// Trim trailing spaces from each field
					for (int i = 0; i < fields.length; i++) {
						fields[i] = fields[i].trim();
					}
					fieldList.add(fields);
			}

			return fieldList;
	}

	public static ArrayList<String[]> mergeAndRemoveDuplicates(ArrayList<String[]> list1, ArrayList<String[]> list2) {
		// Step 1: Merge the two lists
		ArrayList<String[]> mergedList = new ArrayList<>(list1);
		mergedList.addAll(list2);

		// Step 2: Remove duplicates
		Set<String> uniqueRows = new HashSet<>();
		ArrayList<String[]> newlist = new ArrayList<>();
		for (String[] row : mergedList) {
			// Convert the array to a string to use as a key in the HashSet
			String rowKey = row[3];
			// If the key is not already in the set, add the row and its key
			if (!uniqueRows.contains(rowKey)) {
				uniqueRows.add(rowKey);
				newlist.add(row);
			}
		}

		return newlist;

	}


	public static void main(String[] args) {
		try {
			String url = "jdbc:derby://localhost:1527/sun-appserv-samples";
			String user = "APP";
			String password = "APP";


			celldbtool fid = new celldbtool(url, user, password);

			ArrayList<String[]> fieldList;
			HashMap<S2CellId,UniformDistribution> cellm;
			if (args[0].equals("rebuild")) {
			 // rebuilds fieldcells after fields have been pruned
			 // run when model build is finished.
			 // or when the field table is modified.
				fieldList = fid.readValidFields();
				System.out.println("" + fieldList.size() + " fields read.");

				int rDel = fid.deleteFieldCells();
				System.out.println("" + rDel + " rows deleted.");
				int upd = fid.rebuildCells(fieldList);
				System.out.println("" + upd + " rows updated.");
			} else if (args[0].equals("prune")) {
				// removes fields that no longer contribute to the model.
				// this appears to be broken in some way, as the model cannot be completely rebuilt
				// after a large prune.
				// Actually I lie, after running rebuild then run refine to make the remaining cells
				// the best method is to perform an export with "best", then import.
				fieldList =  fid.readAllFields();
				System.out.println("" + fieldList.size() + " fields read.");

				fid.iprune(fieldList);

				/*
				int fcount = fieldList.size();
				cellm = fid.readCells();
				fieldList = prune(fieldList,cellm);
				if (fieldList.size() < fcount)
				{
						int rd = fid.deleteFields();
						System.out.println("" + rd + " rows deleted.");
						rd = fid.writeFields(fieldList);
						System.out.println("" + rd + " rows updated.");
				} else {
						System.out.println("No fields pruned.");
				}
				*/
			} else if (args[0].equals("dedupe")) {

				fieldList = fid.readAllFields();
				System.out.println("" + fieldList.size() + " fields read.");
				fieldList = dedupe(fieldList);
				System.out.println("" + fieldList.size() + " fields after dedupe.");
				int rd = fid.deleteFields();
				System.out.println("" + rd + " fields deleted.");
				rd = fid.writeFields(fieldList);
				System.out.println("" + rd + " fields updated.");

			} else if (args[0].equals("build")) {
				// builds missing cells by finding fields with exactly one missing cell.
				// this is the first step when building the cell table from scratch.
				
				fieldList = fid.readValidFields();
				System.out.println("" + fieldList.size() + " fields read.");

				cellm = fid.readCells();
				System.out.println("" + cellm.size() + " cells read.");

				int osize = cellm.size();
				cellm = fid.agreements(fieldList, cellm);
				if (cellm.size() != osize)
				{
					int d = fid.deleteCells();
					System.out.println("" + d + " cells erased.");
					int cr = fid.writeCells(cellm);
					System.out.println("" + cr + " rows updated.");
				} else {
					System.out.println("No new cells created.");
				}

			} else if (args[0].equals("erase")) {
				// erase the cell model. (then repeatedly run build)
				int d = fid.deleteCells();
				System.out.println("" + d + " rows erased.");
			} else if (args[0].equals("backup")) {
				fieldList =  fid.readAllFields();
				//int fc = printJson(fieldList);
				int fc = printTable(fieldList);
				System.err.println("" + fc + " records written.");
			} else if (args[0].equals("best")) {
				fieldList =  fid.readAllFields();
				cellm = fid.readCells();
				int fc = fid.best(fieldList,cellm);
				System.err.println("" + fc + " records written.");
			} else if (args[0].equals("replace")) {
				fieldList = readFileFields(args[1]);
				System.out.println("Read " + fieldList.size() + " fields");
				int rd = fid.deleteFields();
				System.out.println("" + rd + " fields deleted.");
				rd = fid.writeFields(fieldList);
				System.out.println("" + rd + " fields updated.");
			} else if (args[0].equals("merge")) {
				ArrayList<String[]> fl1 = readFileFields(args[1]);
				ArrayList<String[]> fl2 = fid.readAllFields();

				fieldList = mergeAndRemoveDuplicates(fl1, fl2);
				System.out.println("Merged " + fieldList.size() + " fields");

				int rd = fid.deleteFields();
				System.out.println("" + rd + " fields deleted.");
				rd = fid.writeFields(fieldList);
				System.out.println("" + rd + " fields updated.");
			} else if (args[0].equals("tracelog")) {
				// Trace log - read-only diagnostic that logs all changes to a target cell
				if (args.length < 2) {
					System.err.println("Usage: tracelog <cellToken>");
					return;
				}
				
				String cellToken = args[1];
				fid.cellTrace = S2CellId.fromToken(cellToken);
				
				System.out.println("=== TRACE LOG FOR CELL: " + cellToken + " ===\n");
				
				// Read fields
				fieldList = fid.readValidFields();
				System.out.println("Read " + fieldList.size() + " fields\n");
				
				// Start with empty cells
				cellm = new HashMap<S2CellId, UniformDistribution>();
				
				System.out.println("=== BUILD PHASE ===");
				cellm = fid.agreements(fieldList, cellm);
				
				System.out.println("\n=== REFINE PHASE ===");
				cellm = fid.refine(fieldList, cellm);
				
				System.out.println("\n=== TRACE COMPLETE ===");
				// DO NOT WRITE TO DATABASE - read-only diagnostic
				
			} else if (args[0].equals("invalidate")) {
				fid.setFieldValid(args[1], false);
			} else if (args[0].equals("revalidate")) {
				fieldList =  fid.readAllFields();
				System.out.println("Read " + fieldList.size() + " fields");
				cellm = fid.readCells();
				System.out.println("" + cellm.size() + " cells read.");
				fid.revalidate(fieldList,cellm);
			} else if (args[0].equals("parent")) {
				cellm = fid.readCells();
				System.out.println("" + cellm.size() + " cells read.");

				//fid.populate(fieldList);
				cellm = fid.cellParents(cellm);
				int d = fid.deleteCells();
				System.out.println("" + d + " cells erased.");
				int cr = fid.writeCells(cellm);
				System.out.println("" + cr + " rows updated.");
			} else if (args[0].equals("trace")) {
				fieldList = fid.readValidFields();
				System.out.println("" + fieldList.size() + " fields read.");

				cellm = fid.readCells();
				System.out.println("" + cellm.size() + " cells read.");

				fid.populate(fieldList);
				fid.trace(S2CellId.fromToken(args[1]),fieldList,cellm);

			} else if (args[0].equals("refine")) {
				// improves cell mu, where fields have zero missing cells
				fieldList = fid.readValidFields();
				System.out.println("" + fieldList.size() + " fields read.");

				cellm = fid.readCells();
				System.out.println("" + cellm.size() + " cells read.");

				cellm = fid.refine(fieldList, cellm);

				int d = fid.deleteCells();
				System.out.println("" + d + " cells erased.");
				int cr = fid.writeCells(cellm);
				System.out.println("" + cr + " rows updated.");
			} else if (args[0].equals("testagree")) {
				fieldList = fid.readValidFields();
				System.out.println("" + fieldList.size() + " fields read.");

				cellm = new HashMap<>();

				cellm = fid.agreements(fieldList, cellm);
				System.out.println("" + cellm.size() + " cells updated.");

			} else if (args[0].equals("newagree")) {
				fieldList = fid.readValidFields();
				System.out.println("" + fieldList.size() + " fields read.");
				cellm = new HashMap<>();

				cellm = fid.agreements(fieldList, cellm);
				System.out.println("" + cellm.size() + " cells updated.");

				cellm = fid.agreements(fieldList, cellm);
				System.out.println("" + cellm.size() + " cells updated.");

				cellm = fid.agreements(fieldList, cellm);
				System.out.println("" + cellm.size() + " cells updated.");
				int ocellsize = 0;
				while (ocellsize != cellm.size()) {
					ocellsize = cellm.size();
					cellm = fid.refine(fieldList, cellm);
					cellm = fid.agreements(fieldList, cellm);
					System.out.println("" + cellm.size() + " cells updated.");
				}

				int d = fid.deleteCells();
				System.out.println("" + d + " cells erased.");
				int cr = fid.writeCells(cellm);
				System.out.println("" + cr + " rows updated.");

			} else if (args[0].equals("process") || args[0].equals("query")) {
				fieldList = fid.readValidFields();
				System.out.println("" + fieldList.size() + " fields read.");

				cellm = fid.readCells();
				System.out.println("" + cellm.size() + " cells read.");

				UniformDistribution cv = fid.process(fieldList, cellm, args[1]);

				System.out.println("Cell value: " + cv);
				if (args[0].equals("process") )
					fid.setCellMU(args[1],cv);
			} else if (args[0].equals("exportlevel13cells")) {
				cellm = fid.readCells();
				System.out.println("{");
				boolean first = true;
				for (Map.Entry<S2CellId, UniformDistribution> entry : cellm.entrySet()) {
					S2CellId cellId = entry.getKey();
					if (cellId.level() == 13) {
						if (!first) {
							System.out.print(",\n");
						}
						first = false;
						UniformDistribution ud = entry.getValue();
						System.out.print("  \"" + cellId.toToken() + "\": [" + formatDouble(ud.getLower()) + ", " + formatDouble(ud.getUpper()) + "]");
					}
				}
				System.out.println("\n}");
			} else if (args[0].equals("shared")) {
				fieldList = fid.readAllFields();
				System.out.println("" + fieldList.size() + " fields read.");

				cellm = fid.readCells();
				System.out.println("" + cellm.size() + " cells read.");

				ArrayList<String[]> shared = new ArrayList<String[]>();
				for (String[] f : fieldList)
				{
					if (f[3].trim().equals(args[1]))
					{
						UniformDistribution fmu = getEstMu(f,cellm);
						System.out.println("" + f[2] + " " + fmu);
						shared = fid.findSharedFields(f);
						break;
					}
				}
				for (String[] f : shared)
				{
					UniformDistribution fmu = getEstMu(f,cellm);

					System.out.println("" + f[3] + " " + f[2] + " " + fmu);
				}
				//printTable(shared);
			}
			fid.close();
		} catch (UniformDistributionException e) {
			System.err.println("An Uniform Distribution Error occurred. find the broken field.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("An IO Error occurred.");
			e.printStackTrace();
		} catch (SQLException e) {
			System.err.println("An SQL Error occurred.");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.err.println("Have you included the correct JARs in the classpath?");
			e.printStackTrace();
		}
	}
}

