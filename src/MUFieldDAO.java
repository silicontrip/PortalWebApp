package net.silicontrip.ingress;
public interface MUFieldDAO {
        public boolean exists(String guid) throws MUFieldDAOException;
        public void updateMU(String guid,int mu) throws MUFieldDAOException;
        public void delete(String guid) throws MUFieldDAOException;
        public void insert(String creator,String agent,int mu, String guid,long timestamp,String team, String pguid1, long plat1, long plng1, String pguid2, long plat2, long plng2, String pguid3, long plat3, long plng3) throws MUFieldDAOException;
}
