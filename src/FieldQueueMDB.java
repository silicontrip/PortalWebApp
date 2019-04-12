package net.silicontrip.ingress;

import java.util.Map;
import java.util.HashMap;

import javax.jms.*;
import javax.ejb.*;

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
	  propertyValue = "javax.jms.Queue")
})

public class FieldQueueMDB implements MessageListener {

	@EJB
	private CellSessionBean cellBean;

	public void onMessage(Message message) {
		TextMessage textMessage = (TextMessage) message;
		String tm = "";
		try {
			tm= textMessage.getText();

			JSONObject pobj = new JSONObject (textMessage.getText());
//			SQLMUFieldDAO dao = new SQLMUFieldDAO();
			if (pobj.has("mu")) {

				// unwrap the JSON message

				JSONObject options = pobj.getJSONObject("options");
				JSONArray ent = options.getJSONArray("ent");
				JSONArray entPoints = ent.getJSONArray(2);
				JSONArray points = options.getJSONObject("data").getJSONArray("points");
				JSONObject p1 = points.getJSONObject(0);
				JSONObject p2 = points.getJSONObject(1);
				JSONObject p3 = points.getJSONObject(2);

				String guid = pobj.getString("guid");
				
				//JSONArray mu = pobj.getJSONArray("mu");
		// PLUGIN isn't sending arrays yet.
				JSONArray mu =  new JSONArray();
		mu.put(pobj.getJSONArray("mu"));

				// perform business logic...
				// check for duplicates
				//if (dao.exists(guid))
				if (cellBean.hasFieldGuid(guid))
				{
					System.out.println("field exists: " + guid);
					return;
				}
				
				Field fi = new Field (
					pobj.getString("creator"),
					pobj.getString("agent"),
					0, // place holder
					guid,
					options.getInt("timestamp"),
					entPoints.getString(1),
					p1.getString("guid"),p1.getLong("latE6"),p1.getLong("lngE6"),
					p2.getString("guid"),p2.getLong("latE6"),p2.getLong("lngE6"),
					p3.getString("guid"),p3.getLong("latE6"),p3.getLong("lngE6")
				);
				
				// check for validity
				// EJB???
				S2Polygon S2Field = fi.getS2Polygon();
				// thinking about making MU an array
				// and submitting both values for split fields.
				// TODO: look at the IITC plugin code
				// remove this line once the iitc code is changed
				boolean[] valid = new boolean[mu.length()];
				
				int validCount = 0;
				for (int i =0; i < mu.length(); i++)
					if (cellBean.muFieldValid(S2Field,mu.getInt(i)))
					{
						valid[i]=true;
						validCount ++;
					} else
						valid[i]=false;
					
					if (validCount > 1)
						System.out.println("multiple valid mu field: " + guid  );
					//what to do if different MU are valid?

			// which one is more accurate?
			// really need split field handler.
			// problem is we don't know the matching split field.

				System.out.println ("" + guid + ": " + pobj.getInt("mu") + " valid: " + valid);
				
				for (int i =0; i < mu.length(); i++)
				{
					fi.setMU(mu.getInt(i));
						// not  implemented yet
					cellBean.submitField(fi,valid[i]);
				}
/*	
					dao.insert(
						pobj.getString("creator"),
						pobj.getString("agent"),
						pobj.getInt("mu"),
						guid,
						options.getInt("timestamp"),
						entPoints.getString(1),
						p1.getString("guid"),p1.getLong("latE6"),p1.getLong("lngE6"),
						p2.getString("guid"),p2.getLong("latE6"),p2.getLong("lngE6"),
						p3.getString("guid"),p3.getLong("latE6"),p3.getLong("lngE6"),
						valid
					);
*/
				
			} 
		} catch (JMSException e) {
			System.out.println( "Error while trying to consume messages: " + e.getMessage());
		} catch (Exception e) {
			System.out.println(tm);
			e.printStackTrace();
		}
	}
}
