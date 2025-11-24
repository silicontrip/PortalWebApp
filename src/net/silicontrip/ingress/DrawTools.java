package net.silicontrip.ingress;

import com.google.common.geometry.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.math.BigInteger;
import java.io.IOException;
import java.util.Arrays;
import org.json.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

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

	public static DrawTools fromJSON(String j) 
	{
		DrawTools dt = new DrawTools();
		dt.entities = new JSONArray(j);
		return dt;
	}

	public static DrawTools fromIntelQuery(String q)
	{
		String[] lines = q.split("_");
		
		JSONArray lineEnt = new JSONArray();

		for (int i=0; i < lines.length ; i++)
		{
			String[] points = lines[i].split(",");
			JSONObject lEnt = new JSONObject();

			lEnt.put("type","polyline");
			lEnt.put("color","#fad920"); // intel colour

			JSONArray ll = new JSONArray();
			JSONObject p1 = new JSONObject();
			JSONObject p2 = new JSONObject();

			p1.put("lat",Double.parseDouble(points[0]));
			p1.put("lng",Double.parseDouble(points[1]));
			p2.put("lat",Double.parseDouble(points[2]));
			p2.put("lng",Double.parseDouble(points[3]));
			

			ll.put(p1);
			ll.put(p2);

			lEnt.put("latLngs",ll);

			lineEnt.put(lEnt);
		}
		DrawTools dt = new DrawTools();
		dt.entities = lineEnt;
		return dt;

	}

	public static DrawTools fromIntel(String url)
	{
		return fromIntelQuery(getIntelQuery(url));

	}
	protected static String getIntelQuery(String url)
	{
		try {
			String decode = java.net.URLDecoder.decode(url, StandardCharsets.UTF_8.name());
			String[] query = decode.split("?");
			String[] getvar = query[1].split("&");

			for (int i =0; i < getvar.length ; i++)
			{
				if (getvar[i].startsWith("pls="))
					return getvar[i].split("=")[1];
			}
		} catch (UnsupportedEncodingException e) {
			// not going to happen - value came from JDK's own StandardCharsets
		}

		return "";

	}

	protected static long toLong (String s)
	{

		long l =0;
		for (int i =0; i < s.length(); i++)
		{
			l = l << 1;
			if (s.charAt(i) == '1') 
				l = l | 1;
		}

		return l;

	}

	// a special string encoded format -- base64ish
	public static DrawTools fromStringEncoding(String s)
	{
		long oldLat = 0;
		long oldLng = 0;
		ArrayList<Long> la = decode(s);
		JSONArray lineEnt = new JSONArray();
		System.out.println("size:" + la.size());
		for (int i =0; i < la.size(); i+=4)
		{
			oldLat += la.get(i);
			oldLng += la.get(i+1);
			double lat0 = oldLat / 1000000.0;
			double lng0 = oldLng / 1000000.0;
			oldLat += la.get(i+2);
			oldLng += la.get(i+3);
			double lat1 = oldLat / 1000000.0;
			double lng1 = oldLng / 1000000.0;
			
			JSONObject lEnt = new JSONObject();

			lEnt.put("type","polyline");
			lEnt.put("color","#fad920"); // intel colour

			JSONArray ll = new JSONArray();
			JSONObject p1 = new JSONObject();
			JSONObject p2 = new JSONObject();

				p1.put("lat",lat0);
				p1.put("lng",lng0);
				p2.put("lat",lat1);
				p2.put("lng",lng1);

				ll.put(p1);
				ll.put(p2);

				lEnt.put("latLngs",ll);

				lineEnt.put(lEnt);
		}
		DrawTools dt = new DrawTools();
		dt.entities = lineEnt;
		return dt;
	}
	
// a special compact binary format.
	public static DrawTools fromByteArray(byte[] enc)
	{

		String bin = toBinaryString(enc);
	//System.out.println(bin.length());
	while (bin.length() % 114 != 0)
		bin = "0" + bin;
	//System.out.println(bin);
		JSONArray lineEnt = new JSONArray();

		for (int i = 0; i < bin.length(); i +=114)
		{
			long lat0F = toLong(bin.substring(i,i+28));
			long lng0F = toLong(bin.substring(i+28,i+57));
			long lat1F = toLong(bin.substring(i+57,i+85));
			long lng1F = toLong(bin.substring(i+85,i+114));

			if (!(lat0F == 0 && lng0F == 0 && lat1F == 0 && lng1F == 0))
			{
			//System.out.println(bin.substring(i,i+28) + ","+ bin.substring(i+28,i+57) + " - " + bin.substring(i+57,i+85) + "," + bin.substring(i+85,i+114));

		//	System.out.println("" + lat0F + "," + lng0F + " - " + lat1F + "," + lng1F);

				JSONObject lEnt = new JSONObject();

				lEnt.put("type","polyline");
				lEnt.put("color","#fad920"); // intel colour

				JSONArray ll = new JSONArray();
				JSONObject p1 = new JSONObject();
				JSONObject p2 = new JSONObject();

				p1.put("lat",lat0F/1000000.0 - 90);
				p1.put("lng",lng0F/1000000.0 - 180);
				p2.put("lat",lat1F/1000000.0 - 90);
				p2.put("lng",lng1F/1000000.0 - 180);

				ll.put(p1);
				ll.put(p2);

				lEnt.put("latLngs",ll);

				lineEnt.put(lEnt);
			}
		}
		DrawTools dt = new DrawTools();
		dt.entities = lineEnt;
		return dt;

	}

	protected static String toBinaryString(byte n) {
		StringBuilder sb = new StringBuilder("00000000");
		for (int bit = 0; bit < 8; bit++) {
			if (((n >> bit) & 1) > 0) {
				sb.setCharAt(7 - bit, '1');
			}
		}
		return sb.toString();
	}

	protected static String toBinaryString(byte[] n) {
		StringBuilder sb = new StringBuilder(n.length*8);
		for (int i =0 ; i < n.length; i++)
			sb.append(toBinaryString(n[i]));
		return sb.toString();
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
	public void addPolygon(S2Polygon p) { entities.put(newPolygon(p,colour)); }

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

	protected static JSONObject newPolygon(S2Polygon f, String colour)
	{
		JSONObject pg = new JSONObject();
		pg.put("type","polygon");
		JSONArray latLngs = new JSONArray();
		pg.put("color",colour);
		for (int j = 0; j < f.loop(0).numVertices(); j++)
			latLngs.put(newPoint(f.loop(0).vertex(j)));
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

	private static S2LatLng getS2LatLng(JSONObject o) 
	{
		return S2LatLng.fromDegrees(o.getDouble("lat"),o.getDouble("lng"));
	}

	private static S2Point getS2Point(JSONObject o)
	{
		return getS2LatLng(o).toPoint();
	}

	private static double getAngle(JSONObject p)
	{
		JSONArray ll = p.getJSONArray("latLngs");
		JSONObject jp1 = ll.getJSONObject(0);
		JSONObject jp2 = ll.getJSONObject(1);
		//System.out.println("DT::getAngle " + jp1.toString());
		//System.out.println("DT::getAngle " + jp2.toString());

		S2LatLng ll1 = S2LatLng.fromDegrees(jp1.getDouble("lat"),jp1.getDouble("lng"));
		S2LatLng ll2 = S2LatLng.fromDegrees(jp2.getDouble("lat"),jp2.getDouble("lng"));
		//getS2LatLng(jp1);
		//S2LatLng ll2 = getS2LatLng(jp2);

		//System.out.println(ll1.toStringDegrees());
		//System.out.println(ll2.toStringDegrees());

		S2Point p1 = ll1.toPoint();
		S2Point p2 = ll2.toPoint();
		//System.out.println(p1);
		//System.out.println(p2);
		double an = p1.angle(p2);
		//System.out.println(an);
		return p1.angle(p2);
	}

	private String getE6String(double d)
	{
		return String.format("%1$.6f",d);
/*
		boolean sign = (d<0);
		double da = Math.abs(d);
		long whole = (long)Math.floor(da);
		long dec = (long)Math.round(( da * 1000000 )  % 1000000);

		String signs = sign?"-":"";
		String decp = dec>0?".":"";
		String decs = "" + dec;
		String decn = dec>0?decs:"";

		return signs+ whole + decp + decn;
*/
	}
	
// https://intel.ingress.com/intel?ll=-37.818269,144.94853&z=18&pls=-37.818663,144.949228,-37.818645,144.948802_-37.818478,144.94907,-37.818645,144.948802_-37.818478,144.94907,-37.818663,144.949228
	public String asIntelLink() {
		
		StringBuilder intelLink = new StringBuilder("https://intel.ingress.com/?pls=");
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

				String pl = "" + getE6String(lat0) +"," + getE6String(lng0) + "," + getE6String(lat1) + "," + getE6String(lng1);

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
		System.out.println("max length: " + maxLength);
		Double zoom =  21 - Math.log(maxLength / zoomView) / Math.log(2);
		if (zoom > 21) 
			zoom = 21.0;
		//System.out.println("max: " + maxLength + " zoom: " + zoom);

		centreLat /= pointCount;
		centreLng /= pointCount;
		// round these to E6 format...
		centreLat = Math.round(centreLat*1000000)/1000000.0;
		centreLng = Math.round(centreLng*1000000)/1000000.0;

		intelLink.append("&ll=");
		intelLink.append(getE6String(centreLat));
		intelLink.append(",");
		intelLink.append(getE6String(centreLng));
		// zoom is a bit wrong and appears intel can handle not having it
		//intelLink.append("&z=");
		//intelLink.append(zoom.intValue());
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

	
	private String latStringBytes(double l) 
	{
		StringBuilder sb = new StringBuilder();
		long latF0 = Math.round((l + 90) * 1000000);
		
		while (latF0 > 0) 
		{
			if ((latF0 & 1) == 1)
				sb.insert(0,"1");
			else
				sb.insert(0,"0");
			latF0 = latF0 >> 1;
		}
		while (sb.length() < 28)
			sb.insert(0,"0");
			
		return sb.toString();
	}

	private String lngStringBytes(double l) 
	{
		StringBuilder sb = new StringBuilder();
		long latF0 = Math.round((l + 180) * 1000000);
		
		while (latF0 > 0) 
		{
			if ((latF0 & 1) == 1)
				sb.insert(0,"1");
			else
				sb.insert(0,"0");
			latF0 = latF0 >> 1;
		}
		while (sb.length() < 29)
			sb.insert(0,"0");
			
		return sb.toString();
	}
	
	private static long[] decodePair(long lenc)
	{
		long[] ll = new long[2];
		
		int base =(int)Math.floor( (Math.sqrt(8 * lenc +1) - 1)/2);
		int tri = (base * (base+1))/2;
		ll[0]= lenc - tri;
		ll[1]= base - ll[0];
		
		System.out.println("" + lenc + " -> " + ll[0] +", "+ll[1]);
		
		return ll;
	}
	
	private static ArrayList<Long> decode (String s)
	{
		String codes = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-";
		ArrayList<Long> res = new ArrayList<>();
		
		long num = 0;
		for (int i=0 ; i < s.length(); i++)
		{
			int ch = s.charAt(i);
			int nn = codes.indexOf(ch);
			//System.out.println("" + ch + " -> " + nn);
			if (nn>=32)
			{
				nn -= 32;
				num = num << 5;
				num += nn;
				

				//System.out.println("decode: " + num);
				//long[] ll = decodePair(num);
				res.add(neg(num));
				//res.add(neg(ll[1]));
				num=0;
			} else {
				num = num << 5;
				num += nn;	
			}
		}
		return res;
	}
	
	private static long pos (long l)
	{			
			return l<0?-l*2-1:l*2;
	}
	
	private static long neg (long num)
	{
		if ((num & 1) == 1)
			num = -(num + 1);
		num = num / 2;
		return num;
	}
	
	private static String encodePair (long la, long lo)
	{
		long lat = pos(la);
		long lng = pos(lo);
		
		long enc = ((lat+lng)*(lat+lng+1))/2+lat;
		return encode(enc);
	}
	
	private static String encode(long i)
	{
		String codes = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-";
		
		StringBuilder sb = new StringBuilder();
		//int i = (int)l; // it shouldn't be more than 29 bits anyway
		boolean first = true;
		int ch = (int)(i % 32);
			i = i >> 5;
			
			if (first)
			{
				ch += 32;
				first = false;
			}
			sb.insert(0,codes.substring(ch,ch+1));
		
		while (i>0) {
			ch = (int)(i % 32);
			i = i >> 5;
			
			if (first)
			{
				ch += 32;
				first = false;
			}
			sb.insert(0,codes.substring(ch,ch+1));

		}
		String enc = sb.toString();
		return enc;
	}

// this is an attempt at plan compressing.

	public String asLinesPack() 
	{
		StringBuilder sb = new StringBuilder();

		JSONArray lines = this.getAsLines();

		long oldLat =0;
		long oldLng =0;
		for (int i =0; i < lines.length(); i++) 
		{
			JSONObject po= lines.getJSONObject(i);
			if (po.getString("type").equals ("polyline"))
			{
				JSONArray points = po.getJSONArray("latLngs");
				long latFixed;
				long lngFixed;
				latFixed = Math.round(points.getJSONObject(0).getDouble("lat") * 1000000);
				lngFixed = Math.round(points.getJSONObject(0).getDouble("lng") * 1000000);
				//sb.append(encodePair(latFixed-oldLat,lngFixed-oldLng)); 
				sb.append(encode(pos(latFixed-oldLat)));
				sb.append(encode(pos(lngFixed-oldLng))); 
				oldLat = latFixed;
				oldLng = lngFixed;

				latFixed= Math.round(points.getJSONObject(1).getDouble("lat") * 1000000);
				lngFixed= Math.round(points.getJSONObject(1).getDouble("lng") * 1000000);
				//sb.append(encodePair(latFixed-oldLat,lngFixed-oldLng)); 
				sb.append(encode(pos(latFixed-oldLat)));
				sb.append(encode(pos(lngFixed-oldLng))); 
				oldLat = latFixed;
				oldLng = lngFixed;
			}
		}
		return sb.toString();
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
