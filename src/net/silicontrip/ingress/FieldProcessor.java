package net.silicontrip.ingress;

import com.google.common.geometry.*;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.silicontrip.UniformDistribution;
import net.silicontrip.UniformDistributionException;

@Stateless
@LocalBean
public class FieldProcessor {

    @EJB
    private MUSessionBean muBean;

    @EJB
    private SQLEntityDAO dao;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public HashSet<S2CellId> processField(String fieldGuid) {
        HashSet<S2CellId> modifiedCells = new HashSet<>();
        try {
            Field field = dao.getField(fieldGuid);
            if (field == null) {
                Logger.getLogger(FieldProcessor.class.getName()).log(Level.WARNING, "cannot find field guid: " + fieldGuid);
                return modifiedCells;
            }

            UniformDistribution initialMU;
            Integer score = field.getMU();

            if (score == 1) {
                initialMU = new UniformDistribution(0.0, 1.5);
            } else {
                initialMU = new UniformDistribution(score, 0.5);
            }

            final S2CellUnion cells = field.getCells();

            for (S2CellId cellOuter : cells) {
                StringBuilder cellLog = new StringBuilder();
                UniformDistribution mus = new UniformDistribution(initialMU);
                cellLog.append("( ");
                cellLog.append(mus.toString());
                for (S2CellId cellInner : cells) {
                    if (!cellOuter.equals(cellInner)) {
                        double areaInner = CellSessionBean.getIntersectionArea(field.getS2Polygon(), cellInner);
                        UniformDistribution cellInnerMU = muBean.getMU(cellInner.toToken());
                        cellLog.append(" - ");
                        cellLog.append(areaInner);
                        cellLog.append(" x ");
                        cellLog.append(cellInner.toToken());
                        cellLog.append(":");
                        if (cellInnerMU != null) {
                            cellLog.append(cellInnerMU);
                            UniformDistribution cma = cellInnerMU.mul(areaInner);
                            mus = mus.sub(cma);
                        } else {
                            cellLog.append("undefined");
                            mus.setLower(0.0);
                        }
                    }
                }
                cellLog.append(" ) / ");
                double areaOuter = CellSessionBean.getIntersectionArea(field.getS2Polygon(), cellOuter);
                cellLog.append(areaOuter);
                mus = mus.div(areaOuter);
                cellLog.insert(0, " = ");

                try {
                    mus.clampLower(0.0);
                    cellLog.insert(0, mus);

                    UniformDistribution cellOuterMU = muBean.getMU(cellOuter.toToken());

                    cellLog.insert(0, " => ");
                    if (cellOuterMU == null) {
                        cellLog.insert(0, "null");
                    } else {
                        cellLog.insert(0, cellOuterMU.toString());
                    }
                    cellLog.insert(0, ": ");
                    cellLog.insert(0, cellOuter.toToken());

                    if (muBean.refineMU(cellOuter.toToken(), mus)) {
                        modifiedCells.add(cellOuter);
                    }
                } catch (UniformDistributionException ae) {
                    Logger.getLogger(FieldProcessor.class.getName()).log(Level.WARNING, "Field: " + fieldGuid + " Cell: " + cellOuter.toToken() + " MU exception " + ae.getMessage());
                    System.out.println(ae.getMessage() + " " + cellLog.toString());
                }
            }
            return modifiedCells;

        } catch (Exception e) {
            Logger.getLogger(FieldProcessor.class.getName()).log(Level.SEVERE, null, e);
            System.out.println("processFieldException: " + e.getMessage());
            e.printStackTrace();
        }
        return modifiedCells;
    }
}
