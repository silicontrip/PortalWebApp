/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.silicontrip.ingress;

import com.google.common.geometry.S2CellId;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import net.silicontrip.UniformDistribution;

/**
 *
 * @author mark
 */
@Entity
@Table(name = "mucell")
@XmlRootElement
@NamedQueries({
	@NamedQuery(name = "CellMUEntity.findAll", query = "SELECT m FROM CellMUEntity m"),
	@NamedQuery(name = "CellMUEntity.findByCellid", query = "SELECT m FROM CellMUEntity m WHERE m.id = :cellid")
})
public class CellMUEntity implements Serializable {

	private static final long serialVersionUID = 1L;
	@Id
	@Column(name = "cellid")
	@Basic(optional = false)
	@NotNull

	private Long id;
	@Basic(optional = false)

	@NotNull
	@Column(name = "mu_low")
	private double min;
	@Basic(optional = false)

	@NotNull
	@Column(name = "mu_high")
	private double max;
	
	@Transient private S2CellId s2id = null;

	public double getMin() {
		return min;
	}

	public void setMin(double min) {
		this.min = min;
	}

	public double getMax() {
		return max;
	}

	public void setMax(double max) {
		this.max = max;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
		this.s2id = new S2CellId(id);
	}

	public void setId(String token) {
		this.s2id = S2CellId.fromToken(token);
		this.id = s2id.id();
	}

	public void setId(S2CellId cellid) {
		this.s2id = cellid;
		this.id = s2id.id();
	}

	public S2CellId getS2CellId() {
		if (s2id == null) {
			s2id = new S2CellId(id);
		}
		return s2id;
	}
	
	public UniformDistribution getDistribution()
	{
		return new UniformDistribution(min,max);
	}

	public void setDistribution(UniformDistribution ud)
	{
		this.min = ud.getLower();
		this.max = ud.getUpper();
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
		return "" + s2id.toToken() + ":[" + min + ", " + max + "]";
	}

}
