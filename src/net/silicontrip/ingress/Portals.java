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
import javax.persistence.Lob;
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
@Table(name = "portals")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Portals.findAll", query = "SELECT p FROM Portals p"),
    @NamedQuery(name = "Portals.findByGuid", query = "SELECT p FROM Portals p WHERE p.guid = :guid"),
    @NamedQuery(name = "Portals.findByLatE6", query = "SELECT p FROM Portals p WHERE p.latE6 = :latE6"),
    @NamedQuery(name = "Portals.findByLngE6", query = "SELECT p FROM Portals p WHERE p.lngE6 = :lngE6"),
    @NamedQuery(name = "Portals.findByTeam", query = "SELECT p FROM Portals p WHERE p.team = :team"),
    @NamedQuery(name = "Portals.findByLevel", query = "SELECT p FROM Portals p WHERE p.level = :level"),
    @NamedQuery(name = "Portals.findByResCount", query = "SELECT p FROM Portals p WHERE p.resCount = :resCount"),
    @NamedQuery(name = "Portals.findByHealth", query = "SELECT p FROM Portals p WHERE p.health = :health"),
    @NamedQuery(name = "Portals.findByTimeLastseen", query = "SELECT p FROM Portals p WHERE p.timeLastseen = :timeLastseen"),
    @NamedQuery(name = "Portals.findByDeleted", query = "SELECT p FROM Portals p WHERE p.deleted = :deleted"),
    @NamedQuery(name = "Portals.findByImage", query = "SELECT p FROM Portals p WHERE p.image = :image")})
public class Portals implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "guid")
    private String guid;
    @Lob
    @Size(max = 65535)
    @Column(name = "title")
    private String title;
    @Basic(optional = false)
    @NotNull
    @Column(name = "latE6")
    private int latE6;
    @Basic(optional = false)
    @NotNull
    @Column(name = "lngE6")
    private int lngE6;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 16)
    @Column(name = "team")
    private String team;
    @Column(name = "level")
    private Integer level;
    @Column(name = "res_count")
    private Integer resCount;
    @Column(name = "health")
    private Integer health;
    @Column(name = "time_lastseen")
    private Integer timeLastseen;
    @Column(name = "deleted")
    private Boolean deleted;
    @Size(max = 160)
    @Column(name = "image")
    private String image;

    public Portals() {
    }

    public Portals(String guid) {
        this.guid = guid;
    }

    public Portals(String guid, int latE6, int lngE6, String team) {
        this.guid = guid;
        this.latE6 = latE6;
        this.lngE6 = lngE6;
        this.team = team;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getLatE6() {
        return latE6;
    }

    public void setLatE6(int latE6) {
        this.latE6 = latE6;
    }

    public int getLngE6() {
        return lngE6;
    }

    public void setLngE6(int lngE6) {
        this.lngE6 = lngE6;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getResCount() {
        return resCount;
    }

    public void setResCount(Integer resCount) {
        this.resCount = resCount;
    }

    public Integer getHealth() {
        return health;
    }

    public void setHealth(Integer health) {
        this.health = health;
    }

    public Integer getTimeLastseen() {
        return timeLastseen;
    }

    public void setTimeLastseen(Integer timeLastseen) {
        this.timeLastseen = timeLastseen;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
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
        if (!(object instanceof Portals)) {
            return false;
        }
        Portals other = (Portals) object;
        if ((this.guid == null && other.guid != null) || (this.guid != null && !this.guid.equals(other.guid))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "net.silicontrip.ingress.Portals[ guid=" + guid + " ]";
    }
    
}
