package net.silicontrip.ingress;

import net.silicontrip.*;
import java.util.HashMap;
import com.google.common.geometry.*;
import javax.ejb.Remote;
 
@Remote
public interface CellSessionBeanRemote {
	S2Polygon getS2Polygon (S2Cell cell);
	S2Polygon getS2Polygon (S2LatLng v1,S2LatLng v2,S2LatLng v3);
	S2CellUnion getCellsForField(S2Polygon thisField);
	HashMap<S2CellId,AreaDistribution> getIntersectionMU(S2CellUnion cells,S2Polygon thisField); 
        UniformDistribution muForField(S2Polygon s2Field);
}
