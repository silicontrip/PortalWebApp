package net.silicontrip.ingress;
import net.silicontrip.UniformDistribution;
import java.util.HashMap;
import com.google.common.geometry.S2CellId;
public interface MUCellDAO{
	public HashMap<S2CellId,UniformDistribution> getAll() throws MUCellDAOException;
	public void updateAll(HashMap<S2CellId,UniformDistribution> cellmu) throws MUCellDAOException;
}
