package net.silicontrip.ingress;

import java.util.Map;
import java.util.HashMap;

import jakarta.jms.*;
import jakarta.ejb.*;

import org.json.*;
import com.google.common.geometry.*;



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
	private CellSessionBean cellBean;

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
					return;
				} 
			//  System.out.println("FieldQueue: " + tm);

				//  System.out.println("new Field: " + guid);

				
				// check for validity
				// EJB???
				S2Polygon S2Field = fi.getS2Polygon();

				//cellBean.createCellsForField(S2Field);

				boolean[] valid = new boolean[mu.length()];
				
				for (int i =0; i < mu.length(); i++)
					valid[i] = fieldBean.muFieldValid(S2Field,mu.getInt(i));

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
							fieldBean.processField(fi);
						} else {
							fieldBean.submitField(fi,valid[i]);
						} 
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
			System.out.println( "Error while trying to consume messages: " + e.getMessage());
		} catch (Exception e) {
			System.out.println("FQB: exception");
			System.out.println(tm);
			e.printStackTrace();
		}
	}
}
