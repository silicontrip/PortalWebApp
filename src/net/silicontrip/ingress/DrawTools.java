package net.silicontrip.ingress;

import com.google.common.geometry.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.io.IOException;
import java.util.Arrays;
import org.json.*;



public class DrawTools {

	private JSONArray entities;
	private String colour;
	private int outputType = 0;
	private final double zoomView = 0.075;  // zoom factor for type of display ... pc browser or mobile

	// blatently stolen from the Drawtools IITC plugin
	private final ArrayList<String>  colourPresets =  new ArrayList<>(Arrays.asList(
		"#a24ac3",
		"#514ac3",
		"#4aa8c3",
		"#51c34a",
		"#c1c34a",
		"#c38a4a",
		"#c34a4a",
		"#c34a6f",
		"#000000",
		"#666666",
		"#bbbbbb",
		"#ffffff"));

	public DrawTools() {
		this.entities = new JSONArray();
		this.setDefaultColour(0);
	}

	public DrawTools(String clusterDescription) throws IOException
	{
		this();

		this.entities = new JSONArray(clusterDescription);
                
		// should do some validity checking.
		for (int count=0; count<entities.length(); count++)
		{
			JSONObject ent = entities.getJSONObject(count);
			if (!ent.has("type")) {
				throw (new IOException("Drawtools entity error"));
			} else {
				// depending on the entity we might have different point(s) array
			}
		}
	
	}

	public void erase() {entities = new JSONArray(); }
	public void setOutputAsPolyline() { outputType = 1; }  // I know I should ENUM this
	public void setOutputAsPolygon() { outputType = 2; }
	public void setOutputAsIntel() { outputType = 3; }
	public void setOutputAsIs() { outputType = 0; }

	public void setDefaultColour(String c) { colour = c; }
	public void setDefaultColour(int i) { colour = colourPresets.get(i); }

	protected static JSONObject newPoint(S2Point p) { return newPoint(new S2LatLng(p)); }
	protected static JSONObject newPoint(S2LatLng l) { return newPoint(l.latDegrees(),l.lngDegrees()); }
	
	protected static JSONObject newPoint (double lat, double lng)
	{
		JSONObject pt = new JSONObject();
		pt.put("lat",lat);
		pt.put("lng",lng);
		return pt;
	}

	public void addLine(S2Edge l) { entities.put(newLine(l,this.colour)); }
	public void addLine(S2Point a, S2Point b) { entities.put(newLine(a,b,this.colour)); }
	public void addField(S2Polygon p) { entities.put(newField(p,this.colour)); }
	public void addMarker(S2LatLng l) { entities.put(newMarker(l,this.colour)); }
	public void addCircle(S2LatLng l, S1Angle a) { entities.put(newCircle(l,a,this.colour)); }

	protected static JSONObject newLine (S2Edge l, String colour) { return newLine(l.getStart(),l.getEnd(),colour); }
	protected static JSONObject newLine (S2Point a, S2Point b, String colour) { return newLine(newPoint(a),newPoint(b),colour); }
	protected static JSONObject newLine (JSONObject a, JSONObject b, String colour)
	{
		JSONObject pg = new JSONObject();
		pg.put("type","polyline");
		JSONArray latLngs = new JSONArray();
		latLngs.put(a);
		latLngs.put(b);
		pg.put("color",colour);
		pg.put("latLngs",latLngs);
		return pg;
	}	
	
	protected static JSONObject newField (S2Polygon f, String colour) { return newField (f.loop(0), colour); }
	protected static JSONObject newField (S2Loop f, String colour) { return newField(f.vertex(0),f.vertex(1),f.vertex(2),colour); }
	protected static JSONObject newField (S2Point a, S2Point b, S2Point c, String colour) { return newField(newPoint(a),newPoint(b),newPoint(c),colour); }
	
	protected static JSONObject newField (JSONObject l1, JSONObject l2, JSONObject l3, String colour)
	{
		JSONObject pg = new JSONObject();
		pg.put("type","polygon");
		JSONArray latLngs = new JSONArray();
		latLngs.put(l1);
		latLngs.put(l2);
		latLngs.put(l3);
		pg.put("color",colour);
		pg.put("latLngs",latLngs);
		return pg;
	}	

	protected static JSONObject newMarker (double lat, double lng, String colour) {
		JSONObject pg = new JSONObject();
		pg.put("type","marker");
		pg.put("latLng",newPoint(lat,lng));
		pg.put("color",colour);
		return pg;
	}
	
	protected static JSONObject newMarker (S2LatLng p, String colour) { return newMarker(p.latDegrees(),p.lngDegrees(), colour); }
	protected static JSONObject newMarker (S2Point p, String colour) { return newMarker(new S2LatLng(p), colour); }

	protected static JSONObject newCircle (S2Cap c, String colour) { return newCircle(c.axis(),c.angle(),colour);}
	protected static JSONObject newCircle (S2Point p, S1Angle a, String colour) { return newCircle (new S2LatLng(p), a ,colour); }
	protected static JSONObject newCircle (S2LatLng p, S1Angle a, String colour) { return newCircle (p.latDegrees(), p.lngDegrees(), a.radians()* 6367.0, colour); }
	protected static JSONObject newCircle (double lat, double lng, double r, String colour) {
		JSONObject pg = new JSONObject();
		pg.put("type","circle");
		pg.put("latLng",newPoint(lat,lng));
		pg.put("radius",r);
		pg.put("color",colour);
		return pg;
	}

	/**
	*
	*/
	private static void addAll (JSONArray a1, JSONArray a2)
	{
		for (int i = 0; i < a2.length(); i++) 
			a1.put(a2.get(i));
	}
	
	public JSONArray getAsFields() { return getAsFields(entities); }

	private static JSONArray getAsFields(JSONArray ent) {
		JSONArray asFields = new JSONArray();
		JSONArray ff = new JSONArray();
		for (int l1 =0; l1< ent.length(); l1++) {
			JSONObject dto1 = ent.getJSONObject(l1);
			JSONObject dto3;
			if (dto1.getString("type").equals("polyline"))
			{
				String colour = dto1.getString("color");
				for (int l2=0; l2 <ent.length(); l2++) {
					JSONObject dto2 = ent.getJSONObject(l2);
					if (dto2.getString("type").equals("polyline"))
					{
						if (dto1.getJSONArray("latLngs").getJSONObject(0).equals(dto2.getJSONArray("latLngs").getJSONObject(0))) 
						{
							for (int l3=0; l3 <ent.length(); l3++) 
							{
								dto3 = ent.getJSONObject(l3);
								if (dto3.get("type").equals("polyline"))
								{
									if ((dto1.getJSONArray("latLngs").getJSONObject(1).equals(dto3.getJSONArray("latLngs").getJSONObject(0)) && 
										dto2.getJSONArray("latLngs").getJSONObject(1).equals(dto3.getJSONArray("latLngs").getJSONObject(1))) || 
										(dto1.getJSONArray("latLngs").getJSONObject(1).equals(dto3.getJSONArray("latLngs").getJSONObject(1)) && 
										dto2.getJSONArray("latLngs").getJSONObject(1).equals(dto3.getJSONArray("latLngs").getJSONObject(0)) )) 
									{						
										ff.put(newField(dto1.getJSONArray("latLngs").getJSONObject(0),
											dto1.getJSONArray("latLngs").getJSONObject(1),
											dto2.getJSONArray("latLngs").getJSONObject(0),colour));
									}
								}
							}
						}
					}
					if (dto1.getJSONArray("latLngs").getJSONObject(0).equals(dto2.getJSONArray("latLngs").getJSONObject(1))) 
					{
						for (int l3=0; l3 <ent.length(); l3++) 
						{
							dto3 = ent.getJSONObject(l3);
							if (dto3.getString("type").equals("polyline")) 
							{
								if ((dto1.getJSONArray("latLngs").getJSONObject(1).equals(dto3.getJSONArray("latLngs").getJSONObject(0)) && 
										dto2.getJSONArray("latLngs").getJSONObject(0).equals(dto3.getJSONArray("latLngs").getJSONObject(1))) || 
										(dto1.getJSONArray("latLngs").getJSONObject(1).equals(dto3.getJSONArray("latLngs").getJSONObject(1)) && 
										dto2.getJSONArray("latLngs").getJSONObject(0).equals(dto3.getJSONArray("latLngs").getJSONObject(0)) )) 
								{

									ff.put(newField(dto1.getJSONArray("latLngs").getJSONObject(0),
										dto1.getJSONArray("latLngs").getJSONObject(1),
										dto2.getJSONArray("latLngs").getJSONObject(0),colour));
								}
							}
						}
					}
				}
			} else {
				asFields.put(dto1);
			}

		}
		addAll(asFields,ff);
		return asFields;
	}

	public JSONArray getAsLines() {

		JSONArray asLines = new JSONArray();
		//HashSet<JSONObject> lines = new HashSet<>();
		JSONArray lines = new JSONArray();

		for (int i=0; i < entities.length(); i++)
		{
			JSONObject po = entities.getJSONObject(i);
			if (po.getString("type").equals("polygon"))
			{
				//System.out.println("Drawtools::toLines polygon");
				// delete this and add line
				// would like to check for duplicates
				JSONObject oldpoint = null;
				JSONObject firstpoint = null;
				for (int j=0; j<po.getJSONArray("latLngs").length(); j++)
				{
					JSONObject pp = po.getJSONArray("latLngs").getJSONObject(j);
					if (oldpoint != null)
					{
						//lines.add(newLine(oldpoint,pp,colour));
						lines.put(newLine(oldpoint,pp,colour));
					} else {
						firstpoint = pp;
					}
					oldpoint = pp;  
					
				}
				//lines.add(newLine(oldpoint,firstpoint,colour));
				lines.put(newLine(oldpoint,firstpoint,colour));
			} else {
				asLines.put(po);
			}
		}

		// add hashSet to arraylist
		addAll(asLines,lines);

		return asLines;
		
	}

	private static S2Point getS2Point(JSONObject o)
	{
		return S2LatLng.fromDegrees(o.getLong("lat"),o.getLong("lng")).toPoint();
	}

	private static double getAngle(JSONObject p)
	{
		S2Point p1 = getS2Point(p.getJSONArray("latLngs").getJSONObject(0));
		S2Point p2 = getS2Point(p.getJSONArray("latLngs").getJSONObject(1));
		return p1.angle(p2);
	}
	
// https://intel.ingress.com/intel?ll=-37.818269,144.94853&z=18&pls=-37.818663,144.949228,-37.818645,144.948802_-37.818478,144.94907,-37.818645,144.948802_-37.818478,144.94907,-37.818663,144.949228
	public String asIntelLink() {
		
		StringBuilder intelLink = new StringBuilder("https://www.ingress.com/intel?pls=");
		JSONArray pos = this.getAsLines();
		boolean first = true;
		double maxLength =0;
		double centreLat =0;
		double centreLng =0;
		double pointCount = 0;
		for (int i = 0; i < pos.length(); i++)
		{
			JSONObject po = pos.getJSONObject(i);
			if (po.getString("type").equals("polyline"))
			{
				double d = getAngle(po) * 6367.0;
				if (d> maxLength)
					maxLength = d;

				JSONArray ll = po.getJSONArray("latLngs");
			
				double lat0 = ll.getJSONObject(0).getDouble("lat");
				double lng0 = ll.getJSONObject(0).getDouble("lng");
				double lat1 = ll.getJSONObject(1).getDouble("lat");
				double lng1 = ll.getJSONObject(1).getDouble("lng");

				String pl = "" + lat0 +"," + lng0 + "," + lat1 + "," + lng1;

				centreLat += lat0;
				centreLng += lng0;
				centreLat += lat1;
				centreLng += lng1;
				pointCount +=2;

				if (!first) 
					intelLink.append("_");
			
				intelLink.append(pl);
				first = false;
			}
		}
		// add zoom and centre
		// 21 = max browser zoom.
		Double zoom =  21 - Math.log(maxLength / zoomView) / Math.log(2);
		//System.out.println("max: " + maxLength + " zoom: " + zoom);

		centreLat /= pointCount;
		centreLng /= pointCount;
		// round these to E6 format...
		centreLat = Math.round(centreLat*1000000)/1000000.0;
		centreLng = Math.round(centreLng*1000000)/1000000.0;

		intelLink.append("&ll=");
		intelLink.append(centreLat);
		intelLink.append(",");
		intelLink.append(centreLat);
		intelLink.append("&z=");
		intelLink.append(zoom.intValue());
		return intelLink.toString();
	}
	public String toString() { 
		if (outputType == 1)
			return this.getAsLines().toString();
		if (outputType == 2)
			return this.getAsFields().toString();
		if (outputType == 3)
			return this.asIntelLink();
	// default as is
		return entities.toString();
	}
			
	public int length()  { return entities.length(); }

	public int countPolygons() {
		int count =0;
		for (int i=0; i < entities.length(); i++)
		{
			JSONObject po= entities.getJSONObject(i);
			if (po.get("type").equals("polygon"))
				count++;
		}
		return count;
	}
	

			
		


}
