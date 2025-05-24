package net.silicontrip.ingress;

import java.util.Map;
import java.util.HashMap;

import jakarta.jms.*;
import jakarta.ejb.*;

import org.json.*;
import com.google.common.geometry.*;

import java.util.logging.Level;
import java.util.logging.Logger;



@MessageDriven(
mappedName = "jms/fieldQueue",
activationConfig = { 
	@ActivationConfigProperty(
	  propertyName = "acknowledgeMode", 
	  propertyValue = "Auto-acknowledge"), 
	@ActivationConfigProperty(
	  propertyName = "destinationType", 
	  propertyValue = "jakarta.jms.Queue")
})

public class FieldQueueMDB implements MessageListener {

	@EJB
	private FieldSessionBean fieldBean;

	@EJB
	private SQLEntityDAO dao;

	@EJB
	private FieldsCellsBean fieldsCells;

	@EJB
	FieldProcessCache fpCache;


	private Field makeField (JSONObject pobj)
	{
		// unwrap the JSON message

		JSONObject options = pobj.getJSONObject("options");
		JSONArray ent = options.getJSONArray("ent");
		JSONArray entPoints = ent.getJSONArray(2);
		JSONArray points = options.getJSONObject("data").getJSONArray("points");
		JSONObject p1 = points.getJSONObject(0);
		JSONObject p2 = points.getJSONObject(1);
		JSONObject p3 = points.getJSONObject(2);

		String guid = pobj.getString("guid");
				

		return new Field (
			pobj.getString("creator"),
			pobj.getString("agent"),
			-1, // place holder
			guid,
			options.getInt("timestamp"),
			entPoints.getString(1),
			p1.getString("guid"),p1.getLong("latE6"),p1.getLong("lngE6"),
			p2.getString("guid"),p2.getLong("latE6"),p2.getLong("lngE6"),
			p3.getString("guid"),p3.getLong("latE6"),p3.getLong("lngE6")
		);
	}


	@Override
	public void onMessage(Message message) {
		TextMessage textMessage = (TextMessage) message;
		String tm = "";
		boolean refield = false;
		try {
			tm= textMessage.getText();

			//Logger.getLogger(FieldQueueMDB.class.getName()).log(Level.INFO, "process: " + tm);

			JSONObject pobj = new JSONObject (textMessage.getText());

			if (pobj.has("mu")) {
			//System.out.println("field has mu");

				refield = pobj.has("refield");
				JSONArray mu = pobj.getJSONArray("mu");

				Field fi = makeField(pobj);

				// perform business logic...
				// check for duplicates

				//System.out.println("Check for duplicates");
				
				
				if (!refield && fieldBean.hasFieldGuid(fi.getGuid()))
				{
				//  System.out.println("field exists: " + guid);
					Logger.getLogger(FieldQueueMDB.class.getName()).log(Level.WARNING, "Field Exists: " + fi.getGuid());
					return;
				} 
			//  System.out.println("FieldQueue: " + tm);

				//  System.out.println("new Field: " + guid);

				
				// check for validity
				// EJB???

				//cellBean.createCellsForField(S2Field);

				boolean[] valid = new boolean[mu.length()];
				
				for (int i =0; i < mu.length(); i++) {
					try { 
						valid[i] = fieldBean.muFieldValid(fi,mu.getInt(i));
					} catch (Exception e) {
						Logger.getLogger(FieldQueueMDB.class.getName()).log(Level.WARNING, null, e);
						valid[i]=false;
					}
				}

			//what to do if different MU are valid?
			// which one is more accurate?
			// really need split field handler.
			// problem is we don't know the matching split field.

			// some business logic, while I work out the code;
			// valid[0], valid[1]
			// false, false  -> don't know which to submit, safest is not to submit
			// false, true -> submit 1
			// true, false -> submit 0
			// true, true -> don't know
			// true -> submit 0
			// false -> submit 0

	//  System.out.println ("FieldQMDB: " + guid + " valid: " + valid[0]);

			// we don't want to actually submit a field twice.
				boolean submit = false;
				for (int i =0; i < mu.length(); i++)
				{
					//System.out.println ("submit field");
					fi.setMU(mu.getInt(i));

					if (mu.length() == 1 || (valid[i] && (valid[0] ^ valid[1])))
					{
						// above business logic decribed in 1 if statement
						submit = true;
						if (refield)
						{
							//System.out.println("Resubmit: " + fi );
							fieldBean.processField(fi.getGuid());
						} else {
							if (!fieldBean.hasFieldGuid(fi.getGuid())) // this is handled above
							{
								// same geo field with different guid
								if (fieldBean.muKnownField(fi) == -1) {
									// validate this field against others.
									if (fieldBean.disagreements(fi) == 0)
									{
										// does this field improve (or is unknown) the cell model.
										if (fieldBean.improvesModel(fi)) 
										{

											Logger.getLogger(FieldQueueMDB.class.getName()).log(Level.INFO, "INSERTING: " + tm);


											dao.insertField(fi.getCreator(),
												fi.getAgent(),
												fi.getMU(),
												fi.getGuid(),
												fi.getTimestamp(),
												fi.getTeam(),
												fi.getPGuid1(),
												fi.getPLat1(),
												fi.getPLng1(),
												fi.getPGuid2(),
												fi.getPLat2(),
												fi.getPLng2(),
												fi.getPGuid3(),
												fi.getPLat3(),
												fi.getPLng3(),
												true);
											//S2CellUnion fieldCells = getCellsForField(field.getS2Polygon());
											S2CellUnion fieldCells = fi.getCells();
											fieldsCells.insertCellsForField(fi.getGuid(),fieldCells);

											fpCache.addFieldGuid(fi.getGuid());
											fieldBean.beginProcessing();
										} else {
											Logger.getLogger(FieldQueueMDB.class.getName()).log(Level.WARNING, "Does not improve model: " + fi.getGuid() + " " + fi.getCreator() + " " + fi.getMU());
										}
									} else {
										Logger.getLogger(FieldQueueMDB.class.getName()).log(Level.WARNING, "Has Disagreements: " + fi.getGuid() + " " + fi.getCreator() + " " + fi.getMU());
									}
								} else {
									Logger.getLogger(FieldQueueMDB.class.getName()).log(Level.WARNING, "Already Known: " + fi.getGuid() + " " + fi.getCreator() + " " + fi.getMU());
								}
							}
						} 
					} else {
						Logger.getLogger(FieldQueueMDB.class.getName()).log(Level.WARNING, "Fails valid logic: " + fi.getGuid() + " " + fi.getCreator() +  " MU: [" + valid[0] + ": " +mu.getLong(0) + ", "+valid[1] + ": "+mu.getLong(1)+"]" );
					}
				}
				// maybe set up an invalid table with split MU
				if (!submit)
					if(mu.length()==2)
						System.out.println ("not Submitting field: " + fi.getGuid() + "MU: [" + valid[0] + ": " +mu.getLong(0) + ", "+valid[1] + ": "+mu.getLong(1)+"]");
					else
						System.out.println ("not Submitting field: " + fi.getGuid() + "MU: " + valid[0] + ": " +mu.getLong(0));
			} 
		} catch (JMSException e) {
			Logger.getLogger(FieldQueueMDB.class.getName()).log(Level.SEVERE, null, e);
			System.out.println( "Error while trying to consume messages: " + e.getMessage());
		} catch (Exception e) {
			Logger.getLogger(FieldQueueMDB.class.getName()).log(Level.SEVERE, null, e);
			System.out.println("FQB: exception");
			System.out.println(tm);
			e.printStackTrace();
		}
	}
}
