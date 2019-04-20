/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.silicontrip.ingress;

import com.google.common.geometry.S2CellId;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 *
 * @author mark
 */
@Entity
public class CellMU implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    private Long id;
    private double min;
    private double max;
    private S2CellId s2id = null;
    
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

    public S2CellId getS2CellId() {
        if (s2id == null)
            s2id = new S2CellId(id);
        return s2id;
    }
    
    @Override
    public int hashCode() {
        return s2id.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CellMU)) {
            return false;
        }
        CellMU other = (CellMU) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "" + s2id.toToken() +":["+min+", "+max+"]";
    }
    
}
