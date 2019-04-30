package net.silicontrip.ingress;

import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2LatLngRect;
import com.google.common.geometry.S2Point;
import java.util.Map;
import java.util.HashMap;

import javax.jms.*;
import javax.ejb.*;
import org.json.JSONException;

import org.json.JSONObject;


@MessageDriven(
mappedName = "jms/linkQueue",
activationConfig = { 
    @ActivationConfigProperty(
      propertyName = "acknowledgeMode", 
      propertyValue = "Auto-acknowledge"), 
    @ActivationConfigProperty(
      propertyName = "destinationType", 
      propertyValue = "javax.jms.Queue")
})

public class LinkQueueMDB implements MessageListener {

	//@EJB
	//private SQLEntityDAO dao;

	@EJB
	private EntitySessionBean ent;
	
    private static final Map<Integer, Integer> ZoomDistance = new HashMap<Integer, Integer>() 
	{
		{
			put( 3, 200000);
			put( 4, 200000);
			put( 5, 60000);
			put( 6, 60000);
			put( 7, 10000);
			put( 8, 5000);
			put( 9, 2500);
			put(10, 2500);
			put(11, 800);
			put(12, 300);
			put(13, 0);
			put(14, 0);
			put(15, 0);
			put(16, 0);
			put(17, 0);
			put(18, 0);
			put(19, 0);
			put(20, 0);
			put(21, 0);
			put(22, 0);
			put(23, 0);
			put(24, 0);
		} 
	};
 
	S2Point getS2Point (long latE6, long lngE6)
	{
		return S2LatLng.fromE6(latE6,lngE6).toPoint(); 
	}
	
	S2LatLng getLatLng (JSONObject point)
	{
		if(point.has("lat") && point.has("lng")) {
				return  S2LatLng.fromDegrees(point.getDouble("lat"), point.getDouble("lng"));
			}
		return null;
	}
	
	S2LatLngRect getMapBounds (JSONObject mapBounds)
	{
		if (mapBounds.has("_southWest") && mapBounds.has("_northEast"))
			return new S2LatLngRect(getLatLng(mapBounds.getJSONObject("_southWest")),
				getLatLng(mapBounds.getJSONObject("_northEast")));
		return null;
	}
	
	@Override
    public void onMessage(Message message) {
        TextMessage textMessage = (TextMessage) message;
		String tm = "";
	String exceptionLog = new String();
        try {
			tm= textMessage.getText();
			//System.out.println("LinkQueue: "+ tm);
			JSONObject pobj = new JSONObject (textMessage.getText());
			if (pobj.has("delete"))
			{
				//System.out.println("delete");

				// check zoom 
				Link l = ent.getLinkGuid(pobj.getString("guid"));
				// hmm how do we have a link to delete that isn't in the DB?
				if (l != null) {
					// get link length
					double length = l.getAngle() * 6367000;
					if ( length > ZoomDistance.get(pobj.getInt("zoom")))
					{
						System.out.println("DELETE: " + tm);
						ent.removeLink(l);
					}
				}
			} else if (pobj.has("team")) {
				
				if (ent.existsLinkGuid(pobj.getString("guid")))
					return;
				

//{"dLngE6":145176878,"oGuid":"704dbc67aed54b73b153bbb5be3a9fed.16","oLatE6":-37818041,"guid":"e11e884cdb0e438e9c25c6bc0b909b14.9","team":"E","oLngE6":145157441,"dLatE6":-37802085,"dGuid":"5f7ef86ee21f4c83809527b917cad585.16"}
				
				// check link is within mapBounds
				S2LatLngRect mapBounds = getMapBounds(pobj.getJSONObject("bounds"));
				
				exceptionLog ="insert: " + pobj.getString("guid") + " / " + pobj.getString("oGuid") + "-" + pobj.getString("dGuid") + " mapBounds: " + mapBounds;
				//System.out.println("mapBounds s2llr:" + mapBounds);
				
				S2LatLngRect linkBounds  = S2LatLngRect.fromEdge(getS2Point(pobj.getLong("oLatE6"), pobj.getLong("oLngE6")),
						getS2Point(pobj.getLong("dLatE6"), pobj.getLong("dLngE6")));
				
				//System.out.println("linkBounds s2llr:" + linkBounds);

				if (mapBounds.contains(linkBounds) || mapBounds.intersects(linkBounds))
				{
					//do we even check that the link exists?
					System.out.println("INSERT: " + tm);
					Link li = new Link(
						pobj.getString("guid"),
						pobj.getString("dGuid"), pobj.getLong("dLatE6"), pobj.getLong("dLngE6"),
						pobj.getString("oGuid"), pobj.getLong("oLatE6"), pobj.getLong("oLngE6"),
						pobj.getString("team"));
					ent.insertLink(li);
				}
			} else if (pobj.has("purge")) { // old logic. not sure if this will be used going forward
				//dao.purgeLink();  // deprecated.
			}
        } catch (JMSException e) {
            System.out.println(
              "Error while trying to consume messages: " + e.getMessage());
        } catch (JSONException e) {
			System.out.println("LinkQueueMDB::"+tm);
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("LinkQueueMDBException: " + e.getMessage());
			System.out.println(exceptionLog);
			e.printStackTrace();
		}
    }
}
