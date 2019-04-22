/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.silicontrip.ingress;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author mark
 */
@Entity
@Table(name = "links")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Links.findAll", query = "SELECT l FROM Links l"),
    @NamedQuery(name = "Links.findByGuid", query = "SELECT l FROM Links l WHERE l.guid = :guid"),
    @NamedQuery(name = "Links.findByDGuid", query = "SELECT l FROM Links l WHERE l.dGuid = :dGuid"),
    @NamedQuery(name = "Links.findByDlatE6", query = "SELECT l FROM Links l WHERE l.dlatE6 = :dlatE6"),
    @NamedQuery(name = "Links.findByDlngE6", query = "SELECT l FROM Links l WHERE l.dlngE6 = :dlngE6"),
    @NamedQuery(name = "Links.findByOGuid", query = "SELECT l FROM Links l WHERE l.oGuid = :oGuid"),
    @NamedQuery(name = "Links.findByOlatE6", query = "SELECT l FROM Links l WHERE l.olatE6 = :olatE6"),
    @NamedQuery(name = "Links.findByOlngE6", query = "SELECT l FROM Links l WHERE l.olngE6 = :olngE6"),
    @NamedQuery(name = "Links.findByTeam", query = "SELECT l FROM Links l WHERE l.team = :team")})
public class Links implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "guid")
    private String guid;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "d_guid")
    private String dGuid;
    @Basic(optional = false)
    @NotNull
    @Column(name = "d_latE6")
    private int dlatE6;
    @Basic(optional = false)
    @NotNull
    @Column(name = "d_lngE6")
    private int dlngE6;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "o_guid")
    private String oGuid;
    @Basic(optional = false)
    @NotNull
    @Column(name = "o_latE6")
    private int olatE6;
    @Basic(optional = false)
    @NotNull
    @Column(name = "o_lngE6")
    private int olngE6;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 16)
    @Column(name = "team")
    private String team;

    public Links() {
    }

    public Links(String guid) {
        this.guid = guid;
    }

    public Links(String guid, String dGuid, int dlatE6, int dlngE6, String oGuid, int olatE6, int olngE6, String team) {
        this.guid = guid;
        this.dGuid = dGuid;
        this.dlatE6 = dlatE6;
        this.dlngE6 = dlngE6;
        this.oGuid = oGuid;
        this.olatE6 = olatE6;
        this.olngE6 = olngE6;
        this.team = team;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getDGuid() {
        return dGuid;
    }

    public void setDGuid(String dGuid) {
        this.dGuid = dGuid;
    }

    public int getDlatE6() {
        return dlatE6;
    }

    public void setDlatE6(int dlatE6) {
        this.dlatE6 = dlatE6;
    }

    public int getDlngE6() {
        return dlngE6;
    }

    public void setDlngE6(int dlngE6) {
        this.dlngE6 = dlngE6;
    }

    public String getOGuid() {
        return oGuid;
    }

    public void setOGuid(String oGuid) {
        this.oGuid = oGuid;
    }

    public int getOlatE6() {
        return olatE6;
    }

    public void setOlatE6(int olatE6) {
        this.olatE6 = olatE6;
    }

    public int getOlngE6() {
        return olngE6;
    }

    public void setOlngE6(int olngE6) {
        this.olngE6 = olngE6;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (guid != null ? guid.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Links)) {
            return false;
        }
        Links other = (Links) object;
        if ((this.guid == null && other.guid != null) || (this.guid != null && !this.guid.equals(other.guid))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "net.silicontrip.ingress.Links[ guid=" + guid + " ]";
    }
    
}
