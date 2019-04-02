package net.silicontrip.ingress;

import java.util.ArrayList;
import com.google.common.geometry.*;

public interface LinkDAO {
        public ArrayList<Link> getInRect (S2LatLngRect reg) throws LinkDAOException;
        public ArrayList<Link> getAll () throws LinkDAOException;
        public void purge() throws LinkDAOException;
        public void delete(String guid) throws LinkDAOException;
        public void insert(String guid,String dguid, long dlatE6, long dlngE6,String oguid, long olatE6, long olngE6, String team) throws LinkDAOException;
}
