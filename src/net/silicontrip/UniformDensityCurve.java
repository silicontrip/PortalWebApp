package net.silicontrip;

import java.util.ArrayList;
import java.util.TreeSet;

import net.silicontrip.UniformDistribution;

class UniformDensityCurve {
    ArrayList<UniformDistribution> distributions;
    ArrayList<double[]> segments;
    UniformDistribution peakDistribution;
   // TreeSet<Double> boundaries;
    int peakValue;

    public UniformDensityCurve(ArrayList<UniformDistribution> dists) {
        this.distributions = dists;
       // boundaries = buildBoundaries(distributions);
        segments = buildSegments(dists);
        peakValue = getPeakValue(segments);
        peakDistribution = buildPeakDistribution(dists);
    }

    private ArrayList<double[]> buildSegments(ArrayList<UniformDistribution> dists) {
        TreeSet<Double> bound = new TreeSet<>();
        for (UniformDistribution ud : dists) {
            bound.add(ud.getLower());
            bound.add(ud.getUpper());
        }   
        ArrayList<Double> points = new ArrayList<>(bound);
        ArrayList<double[]> seg = new ArrayList<>();

        for (int i = 0; i < points.size() - 1; i++) {
            double start = points.get(i);
            double end = points.get(i + 1);
            int count = 0;
            for (UniformDistribution ud : dists) {
                if (ud.getLower() <= start && end <= ud.getUpper()) {
                    count++;
                }
            }
            seg.add(new double[] { start, end, count });
        }
        return seg;
    }

    // Optionally return the segment with the peak
    private UniformDistribution buildPeakDistribution(ArrayList<UniformDistribution> d) {
        ArrayList<UniformDistribution> dists = new ArrayList<UniformDistribution>(d);
        ArrayList<double[]> segm = buildSegments(dists);
        int max = getPeakValue(segm);

        if (max == 0)
            return new UniformDistribution(0,Float.MAX_VALUE);

        while (max > 0) {
            boolean first = true;
            double lower=0;
            double upper=Float.MAX_VALUE;
            for (double[] seg : segm) {
                if (seg[2] == max) {
                    if (first)
                    {
                        lower = seg[0];
                        upper = seg[1];
                        first = false;
                    }
                    if (seg[1] > upper)
                        upper = seg[1];
                    if (seg[0] < lower)
                        lower = seg[0]; // this is not likely to happen as segments are sorted.
                }
            }
            boolean valley = false;
            double valley1 = 0;
            double valley2 = 0 ;
            for (double[] seg : segm)
            {
                if ( seg[2] < max && seg[0] > lower && seg[1] < upper)
                {
                    valley = true;
                    valley1 = seg[0];
                    valley2 = seg[1];
                    break;
                }
            }
            if (!valley)
            {
                return new UniformDistribution(lower,upper);
            } else {
                for (int i=0; i < dists.size(); i++)
                {
                    UniformDistribution seg = dists.get(i);
                    if (seg.getLower() == valley1 ||  seg.getUpper() == valley1)
                    {
                        //System.out.println("Removing: " + dists.get(i));
                        dists.remove(i);
                        break;
                    }
                }
                for (int i=0; i < dists.size(); i++)
                {
                    UniformDistribution seg = dists.get(i);
                    if (seg.getLower() == valley2 ||  seg.getUpper() == valley2)
                    {
                        //System.out.println("Removing: " + dists.get(i));
                        dists.remove(i);
                        break;
                    }
                }
                segm = buildSegments(dists);
                //for (double[] g : getXYPlot(segm))
                //    System.out.println("" + g[0] + " " + g[1]);
                //peakDistribution = buildPeakDistribution(segm);
            }
            max = getPeakValue(segm);
            peakValue = max;
            //System.out.println("Build Peak error. Max: " + max);
        }
        return new UniformDistribution(0,Float.MAX_VALUE);

    }

    public int getPeakValue() {
        return peakValue;
    }

    private int getPeakValue(ArrayList<double[]> segm)
    {
        double max = 0;
        for (double[] seg : segm) {
            max = Math.max(max, seg[2]);
        }
        return (int)max;
    }

    public boolean allValid() {
        for (int i=0; i < distributions.size(); i++)
        {
            UniformDistribution io = distributions.get(i);
            for (int j=i+1; j < distributions.size(); j++)
            {
                UniformDistribution ii = distributions.get(j);
                if (!io.intersects(ii))
                    return false;
            }
        }
        return true;
    }

    public int countInvalid() {
        int count = 0;
        for (UniformDistribution ud : distributions)
            if (!isValid(ud))
                count ++;
        return count;
    }

    public boolean isValid(UniformDistribution ud)
    {
        return ud.contains(peakDistribution);
    }

    public boolean isPeak(UniformDistribution ud)
    {
        return ud.getLower() == peakDistribution.getLower() || ud.getUpper() == peakDistribution.getUpper();
    }

    public boolean isDualPeak(UniformDistribution ud)
    {
        return ud.getLower() == peakDistribution.getLower() && ud.getUpper() == peakDistribution.getUpper();
    }

    public boolean isLowerPeak(UniformDistribution ud)
    {
        return ud.getLower() == peakDistribution.getLower();
    }

    public boolean isUpperPeak(UniformDistribution ud)
    {
        return ud.getUpper() == peakDistribution.getUpper();
    }

    public UniformDistribution getPeakDistribution() {
        return peakDistribution;
    }

    // meaning that a single UD from the array covers both
    public boolean hasDualPeak(ArrayList<UniformDistribution> udl)
    {
        for (UniformDistribution ud : udl)
            if (isDualPeak(ud))
                return true;
        return false;
    }

    public ArrayList<double[]> getXYPlot()
    {
        return getXYPlot(segments);
    }

    private ArrayList<double[]> getXYPlot(ArrayList<double[]> segm)
    {
        ArrayList<double[]> plot = new ArrayList<double[]>();
        double[] seg;
        double x;
        double y;

        for (int i=0; i < segm.size(); i++)
        {
            seg = segm.get(i);
            y = seg[2];

            x = seg[0];
            plot.add(new double[] { x,y } );
            x = seg[1];
            plot.add(new double[] { x,y } );

        }
        return plot;
    }

}
