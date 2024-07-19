/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.silicontrip.ingress;

import com.google.common.geometry.S2CellId;
import java.io.Serializable;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlRootElement;
import net.silicontrip.UniformDistribution;
import net.silicontrip.UniformDistributionException;

/**
 *
 * @author mark
 */
@Entity
@Table(name = "mucell",uniqueConstraints={@UniqueConstraint(columnNames={"cellid"})})
@XmlRootElement
@NamedQueries({
	@NamedQuery(name = "CellMUEntity.findAll", query = "SELECT m FROM CellMUEntity m"),
	@NamedQuery(name = "CellMUEntity.findByCellid", query = "SELECT m FROM CellMUEntity m WHERE m.id = :cellid"),
	@NamedQuery(name = "CellMUEntity.findByUnion", query = "SELECT m from CellMUEntity m WHERE m.id in :cellUnion")
})
public class CellMUEntity implements Serializable {

	private static final long serialVersionUID = 1L;
	@Id
	@Column(name = "cellid",unique=true)
	@Basic(optional = false)
	@NotNull
	private String id;
	
	@Basic(optional = false)
	@NotNull
	@Column(name = "mu_low")
	private double min = 0;
	@Basic(optional = false)

	@NotNull
	@Column(name = "mu_high")
	private double max = Double.MAX_VALUE;
	
	// @Transient private UniformDistribution distribution;
	
	@Transient private S2CellId s2id = null;

	public CellMUEntity() { super(); }

	public CellMUEntity(S2CellId token)
	{
		super();
		setId(token);
	}

	public CellMUEntity (String id, UniformDistribution ud)
	{
		super();
		setDistribution(ud);
		setId(id);
	}

	public CellMUEntity(String token)
	{
		super();
		setId(token);
	}
	
	public double getMin() {
		return min;
	}

	public void setMin(double min) {
		//distribution.setLower(min);
		this.min = min;
	}

	public double getMax() {
		//return distribution.getUpper();
		return max;
	}

	public void setMax(double max) {
		//distribution.setUpper(max);
		this.max = max;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
		this.s2id =  S2CellId.fromToken(id);
	}

	public void setId(S2CellId cellid) {
		this.s2id = cellid;
		this.id = s2id.toToken();

	}

	public S2CellId getS2CellId() {
		if (s2id == null) {
			s2id = S2CellId.fromToken(id);
		}
		return s2id;
	}
	
	public UniformDistribution getDistribution()
	{
		return new UniformDistribution(min,max);
	}

	public boolean refine (UniformDistribution ud) throws UniformDistributionException
	{
		UniformDistribution thisDistribution = getDistribution();
		if (thisDistribution.refine(ud))
		{
			this.min = thisDistribution.getLower();
			this.max = thisDistribution.getUpper();
			return true;
		}
		return false;
	}
	
	public void setDistribution(UniformDistribution ud)
	{
		this.min = ud.getLower();
		this.max = ud.getUpper();
		//this.distribution.setLower(ud.getLower());
		//this.distribution.setUpper(ud.getUpper());
	}

	@Override
	public int hashCode() {
		return getS2CellId().hashCode();
	}

	@Override
	public boolean equals(Object object) {
		// TODO: Warning - this method won't work in the case the id fields are not set
		if (!(object instanceof CellMUEntity)) {
			return false;
		}
		CellMUEntity other = (CellMUEntity) object;
		if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "" + s2id + ":[" + min + ", " + max + "]";
	}

}
