package net.silicontrip.ingress;

import java.io.Serializable;
import com.google.common.geometry.*;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "links")
@XmlRootElement
@NamedQueries({
	@NamedQuery(name = "Link.findAll", query = "SELECT m FROM Link m"),
	@NamedQuery(name = "Link.findByGuid", query = "SELECT m FROM Link m WHERE m.guid = :guid")
})

public class Link implements Serializable {

	@Id
	@Column(name = "guid")
	@Basic(optional = false)
	@NotNull
	private String guid;
	@Basic(optional = false)
	@NotNull
	@Column(name = "d_guid")
	private String d_guid;
	@Basic(optional = false)
	@NotNull
	@Column(name = "d_latE6")
	private long d_latE6;
	@Basic(optional = false)
	@NotNull
	@Column(name = "d_lngE6")
	private long d_lngE6;
	@Basic(optional = false)
	@NotNull
	@Column(name = "o_guid")
	private String o_guid;
	@Basic(optional = false)
	@NotNull
	@Column(name = "o_latE6")
	private long o_latE6;
	@Basic(optional = false)
	@NotNull
	@Column(name = "o_lngE6")
	private long o_lngE6;
	@Basic(optional = false)
	@NotNull
	@Column(name = "team")
	private String team;

	public Link() { } 

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
	public double getAngle(){ return getoS2Point().angle(getdS2Point());}


}
