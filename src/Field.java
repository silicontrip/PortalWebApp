package net.silicontrip.ingress;

import java.io.Serializable;
import com.google.common.geometry.*;

public class Field implements Serializable {

	private String creator;
	private String agent;
	private int mu; 
	private String guid;
	private long timestamp;
	private String team; 
	private String pguid1; 
	private long plat1; 
	private long plng1; 
	private String pguid2; 
	private long plat2; 
	private long plng2; 
	private String pguid3; 
	private long plat3; 
	private long plng3;

	
	public Field(String c,String a,int m, String g,long t,String tm, String pg1, long pa1, long po1, String pg2, long pa2, long po2, String pg3, long pa3, long po3)
	{
		creator = c;
		agent = a;
		mu = m;
		guid = g;
		timestamp=t;
		team = tm;
		pguid1 = pg1; plat1 = pa1; plng1 = po1;
		pguid2 = pg2; plat2 = pa2; plng2 = po2;
		pguid3 = pg3; plat3 = pa3; plng3 = po3;
	}


	public void setCreator(String s) { creator = s;}
	public void setAgent(String s) { agent = s;}
	public void setMU(int i) { mu = i; }
	public void setGuid(String s) { guid = s;}
	public void setTimestamp(long l) { timestamp = l; }
	public void setTeam(String s) { team = s;}
	public void setPGuid1(String s) { pguid1 = s;} public void setPLat1(long l) { plat1 = l; } public void setPLng1(long l) { plng1 = l; }
	public void setPGuid2(String s) { pguid2 = s;} public void setPLat2(long l) { plat2 = l; } public void setPLng2(long l) { plng2 = l; }
	public void setPGuid3(String s) { pguid3 = s;} public void setPLat3(long l) { plat3 = l; } public void setPLng3(long l) { plng3 = l; }
	
	public String getCreator() { return creator; }
	public String getAgent() { return agent; }
	public int getMU() { return mu; }
	public String getGuid() { return guid; }
	public long getTimestamp() { return timestamp; }
	public String getTeam() { return team; }
	public String getPGuid1() { return pguid1; } public long getPLat1() { return plat1; } public long getPLng1() { return plng1; }
	public String getPGuid2() { return pguid2; } public long getPLat2() { return plat2; } public long getPLng2() { return plng2; }
	public String getPGuid3() { return pguid3; } public long getPLat3() { return plat3; } public long getPLng3() { return plng3; }
	
	// and some S2 utility methods here
	// maybe cache these, because writing occurs once but reading may occur several times.
	public S2LatLng getP1LatLng() { return S2LatLng.fromE6(plat1,plng1); }
	public S2LatLng getP2LatLng() { return S2LatLng.fromE6(plat2,plng2); }
	public S2LatLng getP3LatLng() { return S2LatLng.fromE6(plat3,plng3); }
	public S2Point getP1Point() { return getP1LatLng().toPoint(); }
	public S2Point getP2Point() { return getP2LatLng().toPoint(); }
	public S2Point getP3Point() { return getP3LatLng().toPoint(); }

	public S2Polygon getS2Polygon ()
        {
                S2PolygonBuilder pb = new S2PolygonBuilder(S2PolygonBuilder.Options.UNDIRECTED_UNION);
                pb.addEdge(getP1Point(),getP2Point());
                pb.addEdge(getP2Point(),getP3Point());
                pb.addEdge(getP3Point(),getP1Point());
                return pb.assemblePolygon();
        }
}
