/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.silicontrip.ingress;

import java.io.Serializable;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author mark
 */
@Entity
@Table(name = "mufields")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Mufields.findAll", query = "SELECT m FROM Mufields m"),
    @NamedQuery(name = "Mufields.findByCreator", query = "SELECT m FROM Mufields m WHERE m.creator = :creator"),
    @NamedQuery(name = "Mufields.findByAgent", query = "SELECT m FROM Mufields m WHERE m.agent = :agent"),
    @NamedQuery(name = "Mufields.findByMu", query = "SELECT m FROM Mufields m WHERE m.mu = :mu"),
    @NamedQuery(name = "Mufields.findByGuid", query = "SELECT m FROM Mufields m WHERE m.guid = :guid"),
    @NamedQuery(name = "Mufields.findByTimestamp", query = "SELECT m FROM Mufields m WHERE m.timestamp = :timestamp"),
    @NamedQuery(name = "Mufields.findByTeam", query = "SELECT m FROM Mufields m WHERE m.team = :team"),
    @NamedQuery(name = "Mufields.findByPguid1", query = "SELECT m FROM Mufields m WHERE m.pguid1 = :pguid1"),
    @NamedQuery(name = "Mufields.findByPlat1", query = "SELECT m FROM Mufields m WHERE m.plat1 = :plat1"),
    @NamedQuery(name = "Mufields.findByPlng1", query = "SELECT m FROM Mufields m WHERE m.plng1 = :plng1"),
    @NamedQuery(name = "Mufields.findByPguid2", query = "SELECT m FROM Mufields m WHERE m.pguid2 = :pguid2"),
    @NamedQuery(name = "Mufields.findByPlat2", query = "SELECT m FROM Mufields m WHERE m.plat2 = :plat2"),
    @NamedQuery(name = "Mufields.findByPlng2", query = "SELECT m FROM Mufields m WHERE m.plng2 = :plng2"),
    @NamedQuery(name = "Mufields.findByPguid3", query = "SELECT m FROM Mufields m WHERE m.pguid3 = :pguid3"),
    @NamedQuery(name = "Mufields.findByPlat3", query = "SELECT m FROM Mufields m WHERE m.plat3 = :plat3"),
    @NamedQuery(name = "Mufields.findByPlng3", query = "SELECT m FROM Mufields m WHERE m.plng3 = :plng3"),
    @NamedQuery(name = "Mufields.findByValid", query = "SELECT m FROM Mufields m WHERE m.valid = :valid")})
public class Mufields implements Serializable {

    private static final long serialVersionUID = 1L;
    @Size(max = 36)
    @Column(name = "creator")
    private String creator;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "agent")
    private String agent;
    @Basic(optional = false)
    @NotNull
    @Column(name = "mu")
    private int mu;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "guid")
    private String guid;
    @Basic(optional = false)
    @NotNull
    @Column(name = "timestamp")
    private int timestamp;
    @Column(name = "team")
    private Character team;
    @Size(max = 36)
    @Column(name = "pguid1")
    private String pguid1;
    @Basic(optional = false)
    @NotNull
    @Column(name = "plat1")
    private int plat1;
    @Basic(optional = false)
    @NotNull
    @Column(name = "plng1")
    private int plng1;
    @Size(max = 36)
    @Column(name = "pguid2")
    private String pguid2;
    @Basic(optional = false)
    @NotNull
    @Column(name = "plat2")
    private int plat2;
    @Basic(optional = false)
    @NotNull
    @Column(name = "plng2")
    private int plng2;
    @Size(max = 36)
    @Column(name = "pguid3")
    private String pguid3;
    @Basic(optional = false)
    @NotNull
    @Column(name = "plat3")
    private int plat3;
    @Basic(optional = false)
    @NotNull
    @Column(name = "plng3")
    private int plng3;
    @Column(name = "valid")
    private Boolean valid;

    public Mufields() {
    }

    public Mufields(String guid) {
        this.guid = guid;
    }

    public Mufields(String guid, String agent, int mu, int timestamp, int plat1, int plng1, int plat2, int plng2, int plat3, int plng3) {
        this.guid = guid;
        this.agent = agent;
        this.mu = mu;
        this.timestamp = timestamp;
        this.plat1 = plat1;
        this.plng1 = plng1;
        this.plat2 = plat2;
        this.plng2 = plng2;
        this.plat3 = plat3;
        this.plng3 = plng3;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public int getMu() {
        return mu;
    }

    public void setMu(int mu) {
        this.mu = mu;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public Character getTeam() {
        return team;
    }

    public void setTeam(Character team) {
        this.team = team;
    }

    public String getPguid1() {
        return pguid1;
    }

    public void setPguid1(String pguid1) {
        this.pguid1 = pguid1;
    }

    public int getPlat1() {
        return plat1;
    }

    public void setPlat1(int plat1) {
        this.plat1 = plat1;
    }

    public int getPlng1() {
        return plng1;
    }

    public void setPlng1(int plng1) {
        this.plng1 = plng1;
    }

    public String getPguid2() {
        return pguid2;
    }

    public void setPguid2(String pguid2) {
        this.pguid2 = pguid2;
    }

    public int getPlat2() {
        return plat2;
    }

    public void setPlat2(int plat2) {
        this.plat2 = plat2;
    }

    public int getPlng2() {
        return plng2;
    }

    public void setPlng2(int plng2) {
        this.plng2 = plng2;
    }

    public String getPguid3() {
        return pguid3;
    }

    public void setPguid3(String pguid3) {
        this.pguid3 = pguid3;
    }

    public int getPlat3() {
        return plat3;
    }

    public void setPlat3(int plat3) {
        this.plat3 = plat3;
    }

    public int getPlng3() {
        return plng3;
    }

    public void setPlng3(int plng3) {
        this.plng3 = plng3;
    }

    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
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
        if (!(object instanceof Mufields)) {
            return false;
        }
        Mufields other = (Mufields) object;
        if ((this.guid == null && other.guid != null) || (this.guid != null && !this.guid.equals(other.guid))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "net.silicontrip.ingress.Mufields[ guid=" + guid + " ]";
    }
    
}
