import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;


/**
 * A DotTracker represents all the information about a frame. It stores the refined data and the dots which exist at that time. 
 * It has no information about what dots do in the future. It is a huge class, both in code, and in memory.
 * @author Henry Rachootin
 *
 */
public class DotTracker implements Serializable
{
	private static final long serialVersionUID = DotTracker.class.hashCode();
	
	
	public static double DOT_PEAK_INTENSITY_CUTOFF=.34; //These can change without hurting saves
	public static double DOT_DISTANCE_CUTOFF=4;
	
	public static double REFINE_RANGE_CUTOFF=.25; //these are used in the refining process. Changes should change the serialVersion
	public static double REFINE_INTENSITY_CUTOFF=.2;
	public static int LOCAL_SEARCH_RANGE=10;
	
	private final int width;
	private final int height;
	
	
	private double min=10000000;//Definitely the best way to do it.
	private double max=0;
	
	private int[][] data;
	private double[][] refined;
	private double[][] ranges;
	private double[][] entropy;
	
	/**
	 * All the dots on this frame. NOT AUTOMATICALLY ASSIGNED, FOR PERFORMACE
	 */
	transient ArrayList<Dot> dots=new ArrayList<Dot>();
	/**
	 * The list of highlighted dots, used for user interaction.
	 */
	transient ArrayList<Dot> highlighted;
	
	/**
	 * Creates a new DotTracker from the specified tiff
	 * @param data: the raw data from the tiff
	 */
	public DotTracker(int [][] data)
	{
		this.data=data;
		width=data.length;
		height=data[0].length;
		refined=new double[width][height];
		ranges=new double[width][height];
		entropy=new double[width][height];
		for(int x=0;x<width;x++)
		{
			for(int y=0;y<height;y++)
			{
				max=Math.max(data[x][y], max);
				min=Math.min(data[x][y], min);			
			}
		}
		refineAll();
	}
	
	/**
	 * 
	 * @param x x coordinate
	 * @param y y coordinate
	 * @return the refined value at x,y
	 */
	private double getRefined(int x, int y)
	{
		double localMin=getLocalMin(x,y);
		double localMax=getLocalMax(x,y);
		double rescaledLocalMin=rescale(localMin,min,max);
		double rescaledLocalMax=rescale(localMax,min,max);
		double localRange=rescaledLocalMax-rescaledLocalMin;
		ranges[x][y]=localRange;
		
		if(localRange<REFINE_RANGE_CUTOFF)
		{
			return 0;
		}
		
		double ref=rescale(data[x][y],localMin,localMax);
		
		ref=clamp(ref,REFINE_INTENSITY_CUTOFF,1);
		ref=rescale(ref,REFINE_INTENSITY_CUTOFF,1);
		
		return ref;
	}
	
	/**
	 * 
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @return the lowest value in the data within LOCAL_SEARCH_RANGE of x,y
	 */
	private double getLocalMin(int x, int y)
	{
		double min=10000000;
		for(int dx=-LOCAL_SEARCH_RANGE;dx<=LOCAL_SEARCH_RANGE;dx++)
		{
			for(int dy=-LOCAL_SEARCH_RANGE;dy<=LOCAL_SEARCH_RANGE;dy++)
			{
				try
				{
					if(Math.sqrt(dx*dx+dy*dy)<=LOCAL_SEARCH_RANGE)
					{
						min=Math.min(min,data[x+dx][y+dy]);
					}
				}
				catch(ArrayIndexOutOfBoundsException e)
				{
					
				}
			}
		}
		return min;
	}
	
	/**
	 * 
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @return the highest value in the data within LOCAL_SEARCH_RANGE of x,y
	 */
	private double getLocalMax(int x, int y)
	{
		double max=0;
		for(int dx=-LOCAL_SEARCH_RANGE;dx<=LOCAL_SEARCH_RANGE;dx++)
		{
			for(int dy=-LOCAL_SEARCH_RANGE;dy<=LOCAL_SEARCH_RANGE;dy++)
			{
				try
				{
					if(Math.sqrt(dx*dx+dy*dy)<=LOCAL_SEARCH_RANGE)
					{
						max=Math.max(max, data[x+dx][y+dy]);
					}
				}
				catch(ArrayIndexOutOfBoundsException e)
				{
					
				}
			}
		}
		return max;
	}
	
	/**
	 * 
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @return a measure of the disorder around x,y, based on the similarity between the data at x,y and its neighbors
	 */
	private double getEntropy(int x, int y)
	{
		double sumDifference=0;
		double numDifferences=0;
		for(int dx=-1;dx<=1;dx++)
		{
			for(int dy=-1;dy<=1;dy++)
			{
				try
				{
					double difference=refined[x][y]/refined[x-dx][y-dy];
					if(difference>1)
					{
						difference=1/difference;
					}
					sumDifference+=1-difference;
					numDifferences++;
				}
				catch(ArrayIndexOutOfBoundsException e)
				{
					
				}
			}
		}
		return sumDifference/numDifferences;
	}
	
	/**
	 * refines all the data, assigning the refined[][] array.
	 */
	private void refineAll()
	{
		for(int x=0;x<width;x++)
		{
			for(int y=0;y<height;y++)
			{
				refined[x][y]=getRefined(x,y);
			}
		}
		for(int x=0;x<width;x++)
		{
			for(int y=0;y<height;y++)
			{
				entropy[x][y]=getEntropy(x,y);
			}
		}
		for(int x=0;x<width;x++)
		{
			for(int y=0;y<height;y++)
			{
				refined[x][y]-=entropy[x][y];
				refined[x][y]=clamp(refined[x][y],0,1);
			}
		}
		
		data=new int[0][0];
		entropy=new double[0][0];
		ranges=new double[0][0];
	}
	/**
	 * utility method to fit data to the range [0,1]
	 * @param val the raw value
	 * @param min the known minimum, i.e a val of min will return zero
	 * @param max the known maximum, i.e a val of max will return one
	 * @return a linear interpolation between min and max of val
	 */
	private static double rescale(double val, double min, double max)
	{
		return (val-min)/(max-min);
	}
	/**
	 * utility method to cut off outliers in data
	 * @param val
	 * @param min
	 * @param max
	 * @return the closest value to val on the range [min,max]
	 */
	private static double clamp(double val, double min, double max)
	{
		if(val<min) return min;
		if(val>max) return max;
		return val;
	}
	/**
	 * Draws the refined data from this tracker to the graphics.
	 * @param graphics the graphics to draw to.
	 */
	public void colorRefined(Graphics graphics)
	{
		for(int x=0;x<width;x++)
		{
			for(int y=0;y<height;y++)
			{
				float color=(float) refined[x][y];
				graphics.setColor(new Color(color,color,color));
				graphics.fillRect(x, y, 1, 1);
			}
		}
	}
	
	/**
	 * Assigns all the dots in this frame their rightfull children in the next.
	 * @param next the DotTracker from the next frame of the tiff.
	 */
	public void assignAllBecomes(DotTracker next)
	{
		for(Dot d1:dots)
		{
			for(Dot d2:next.dots)
			{
				if(d1.distance(d2)<DOT_DISTANCE_CUTOFF)
				{
					d1.addChild(d2);
				}
			}
			for(Dot d2:dots)
			{
				if(d2!=d1 && d1.distance(d2)<DOT_DISTANCE_CUTOFF)
				{
					d1.addSibling(d2);
				}
			}
		}
	}
	
	/**
	 * Finds all the dots in this tracker.
	 */
	public void assignDots()
	{
		dots=getDots();
	}
	
	/**
	 * Gets all the valid dots it can find on this tracker, using a peak method.
	 * @return a list of all the dots on this tracker
	 */
	private ArrayList<Dot> getDots()
	{
		ArrayList<Dot> toReturn=new ArrayList<Dot>();
		for(int x=0;x<width;x++)
		{
			for(int y=0;y<height;y++)
			{
				boolean isPeak=true;
				for(int dx=-1;dx<=1 && isPeak;dx++)
				{
					for(int dy=-1;dy<=1 && isPeak;dy++)
					{
						if(!(dx==0 && dy==0))
						{
							try
							{
								isPeak=refined[x][y]>=refined[x+dx][y+dy];
							}
							catch(ArrayIndexOutOfBoundsException e)
							{
								
							}
						}
					}
				}
				if(refined[x][y]>DOT_PEAK_INTENSITY_CUTOFF && isPeak)
				{
					toReturn.add(new Dot(new Point(x,y),this));
				}
			}
		}
		return toReturn;
	}
	
	/**
	 * Gets the closest dot to a point, i.e. a mouse click, in this tracker.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @return the closest dot to the x,y coordinate
	 */
	public Dot closestDot(double x, double y)
	{
		Dot closest=null;
		double dist=1000000;
		for(Dot d:dots)
		{
			if(d.distance(x,y)<dist)
			{
				dist=d.distance(x,y);
				closest=d;
			}
		}
		return closest;
	}
	/**
	 * Paints the highlighted dot.
	 * @param g the graphics to paint to
	 * @param c the color to paint the dot
	 */
	public void paintHighlighted(Graphics g,Color c)
	{
		if(highlighted!=null)
		{
			for(Dot d:highlighted)
			{
				d.paintSelf(g,c);
			}
		}
	}
	
}
