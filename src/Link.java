package net.silicontrip.ingress;

import java.io.Serializable;
import com.google.common.geometry.*;

public class Link implements Serializable {

	private String guid;
	private String d_guid;
	private long d_latE6;
	private long d_lngE6;
	private String o_guid;
	private long o_latE6;
	private long o_lngE6;
	private String team;

	public Link (String g, String dg, long dla, long dln,String og, long ola, long oln, String t) { 
		guid = g; team = t; 
		d_guid= dg; d_latE6 = dla; d_lngE6 = dln;
		o_guid= og; o_latE6 = ola; o_lngE6 = oln;
	} 

	public String getGuid() { return guid; }
	public String getTeam() { return team; }
    
	public String getdGuid() { return d_guid;}
	public long getdLatE6() { return d_latE6; }
	public long getdLngE6() { return d_lngE6; }
	public String getoGuid() { return o_guid;}
	public long getoLatE6() { return o_latE6; }
	public long getoLngE6() { return o_lngE6; }


	public void setGuid(String g) { guid = g; }
	public void setTeam(String t) { team = t; }

	public void setdGuid(String g) {d_guid = g;}
	public void setdLatE6(long l) { d_latE6 = l; }
	public void setdLngE6(long l) { d_lngE6 = l; }
	public void setoGuid(String g) {o_guid = g;}
	public void setoLatE6(long l) { o_latE6 = l; }
	public void setoLngE6(long l) { o_lngE6 = l; }

    
	public S2Edge getS2Edge() { return new S2Edge (getoS2Point(),getdS2Point());}	
	public S2Point getdS2Point() { return S2LatLng.fromE6(d_latE6,d_lngE6).toPoint(); }	
	public S2Point getoS2Point() { return S2LatLng.fromE6(o_latE6,o_lngE6).toPoint(); }	
	public S2LatLng getdS2LatLng() { return S2LatLng.fromE6(d_latE6,d_lngE6); }	
	public S2LatLng getoS2LatLng() { return S2LatLng.fromE6(o_latE6,o_lngE6); }	
	public S2LatLngRect getBounds() { return S2LatLngRect.fromEdge(getoS2Point(),getdS2Point()); }


}
