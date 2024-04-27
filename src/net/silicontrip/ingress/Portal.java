package net.silicontrip.ingress;

import java.io.Serializable;
//import javax.persistence.*;
import com.google.common.geometry.*;

//@Entity
//@Table(name="portals")

public class Portal implements Serializable {

	private String guid;
	private String title;
	private long latE6;
	private long lngE6;
	private int health;
	private String team;
	private int level;

	public Portal (String g, String t, long la, long ln) { guid = g; title = t; latE6 = la; lngE6 = ln;} 
	public Portal (String g, String t, long la, long ln, int h, String te, int le) { guid = g; title = t; latE6 = la; lngE6 = ln; health = h; team = te; level=le; } 

        public String getGuid() { return guid; }
	public String getTitle() { return title; }
	public long getLatE6() { return latE6; }
	public long getLngE6() { return lngE6; }
	public int getHealth() { return health; }
	public String getTeam() { return team; }
	public int getLevel() { return level; }

	public void setGuid(String g) { guid = g; }
	public void setTitle(String t) { title = t; }
	public void setLatE6(long l) { latE6 = l; }
	public void setLngE6(long l) { lngE6 = l; }
	public void setHealth(int h) { health = h; }
	public void setTeam(String t) { team = t; }
	public void setLevel(int l) { level = l; }
	
	public S2LatLng getS2LatLng() { return S2LatLng.fromE6(latE6,lngE6); }	
	public S2Point getS2Point() { return S2LatLng.fromE6(latE6,lngE6).toPoint(); }	


}
