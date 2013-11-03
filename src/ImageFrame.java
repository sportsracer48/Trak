
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Hashtable;

/**
 * 
 * @author Henry Rachootin
 *This is a class to display an animated image in a swing frame. It is all ui, no logic
 *If you like colormodels, feel free to read this class.
 *Otherwise, just know that it accepts an array of RenderedImages in the constructor, and will animate them when placed in a JFrame
 */
public class ImageFrame extends Component
{	
	private static final long serialVersionUID = 1L;
	
	public static final int REFINED=0;
	public static final int BLANK=1;
	
	
	public static final int ALL_PATHS=0;
	public static final int SHOWN_PATHS=1;
	public static final int BETWEEN=2;
	public static final int NO_PATHS=3;
	
	public volatile int image=REFINED;
	public volatile int paths=NO_PATHS;
	public volatile int rangeStart=0;
	public volatile int rangeEnd=5;
	
	public volatile boolean showDots=false;
	
	public int frame=0;
	public final int frames;
	private final int FPS=16;
	
	
	public BufferedImage refinedImages[];
	
	public int[][][] datas;
	
	public int startScale=6;
	
	private final int imwidth;
	private final int imheight;
	private Thread animator;
	public volatile boolean playing=false;
	
	/**
	 * Creates an image frame to hold a number of RenderedImages
	 * @param images the images to hold
	 */
	public ImageFrame(RenderedImage[] images)
	{
		super();
		setPreferredSize(new Dimension((int)(images[0].getWidth()*startScale),(int)(images[0].getHeight()*startScale)));
		imwidth=images[0].getWidth();
		imheight=images[0].getHeight();
		
		datas=new int[images.length][imwidth][imheight];
		
		refinedImages=new BufferedImage[images.length];
		BufferedImage[] rawImages=new BufferedImage[images.length];
		
		frames=images.length;
		
		for(int i=0;i<images.length;i++)
		{
			rawImages[i]=convertRenderedImage(images[i],i);
		}
		animator=new Thread(new Runnable()
		{
			long targetFrame=1000/FPS;
			long sleepTime=0;
			public void run() 
			{
				while(true)
				{
					if(playing)
					{
						frame=(frame+1)%frames;
					}
					try 
					{
						Thread.sleep(sleepTime);
					} 
					catch (InterruptedException e) 
					{
						e.printStackTrace();
					}
					long beforeTime=System.currentTimeMillis();
					repaint();
					sleepTime=targetFrame-(System.currentTimeMillis()-beforeTime);
					if(sleepTime<0)
					{
						sleepTime=0;
					}
				}
			}
			
		},"animator");
	}
	/**
	 * starts the animation
	 */
	public void start()
	{
		playing=true;
	}
	/**
	 * stops the animation
	 */
	public void stop()
	{
		playing=false;
	}
	
	/**
	 * 
	 * @return the width of one pixel from the tif on the screen in pixels
	 */
	public double scaleX()
	{
		return getWidth()/(double)imwidth;
	}
	/**
	 * 
	 * @return the height of one pixel from the tif on the screen in pixels
	 */
	public double scaleY()
	{
		return getHeight()/(double)imheight;
	}
	/**
	 * 
	 * @return the x-coordinate of the picX x-coordinate on the screen
	 */
	public int toScreenX(int picX)
	{
		return (int) Math.round(picX*scaleX()+scaleX()/2d);
	}
	/**
	 * 
	 * @return the y-coordinate of the picY y-coordinate on the screen
	 */
	public int toScreenY(int picY)
	{
		return (int) Math.round(picY*scaleY()+scaleY()/2d);
	}
	
	/**
	 * Images created in the convertRenderedImage function don't render very fast at all. Images created with the Component's createImage function work much better. However, they can only be made once the image is added to a frame.
	 * This method makes all the new BufferedImages, and starts the animation thread.
	 */
	public void addNotify()
	{
		super.addNotify();
		//Images created in the convertRenderedImage function don't render very fast at all. Images created with the Component's createImage function work much better.
		for(int i=0;i<refinedImages.length;i++)
		{
			BufferedImage refined=(BufferedImage) createImage(imwidth,imheight);
			
			refinedImages[i]=refined;
		}
		animator.start();
		setFocusable(true);
		requestFocus();
	}
	
	/**
	 * Converts a RenderedImage (img) from the Java Advanced Imaging API to a Java2D/AWT BufferedImage, as well as to an int[][]. The former is returned, the latter is saved by a side effect into datas[index]
	 * @param img the image to covert
	 * @param index the frame that the image will represent. This is necessary for saving the image as an int[][]
	 * @return the converted BufferedImage
	 */
	public BufferedImage convertRenderedImage(RenderedImage img,int index) 
	{
		if (img instanceof BufferedImage) 
		{
			return (BufferedImage)img;	
		}	
		ColorModel cm = img.getColorModel();
		int width = img.getWidth();
		int height = img.getHeight();
		WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
		
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		Hashtable<String,Object> properties = new Hashtable<String,Object>();
		String[] keys = img.getPropertyNames();
		if (keys!=null) 
		{
			for (int i = 0; i < keys.length; i++) 
			{
				properties.put(keys[i], img.getProperty(keys[i]));
			}
		}
		BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
		img.copyData(raster);
		
		
		int[][] data=new int[width][height];
		
		for(int x=0;x<width;x++)
		{
			for(int y=0;y<height;y++)
			{
				data[x][y]=raster.getSample(x, y, 0);
			}
		}
		datas[index]=data;
		
		return result;
	}
	/**
	 * Method to paint the correct image and then the correct paths. Called only by the AWT repaint manager.
	 */
	public void paint(Graphics g)
	{
		if(image==REFINED)
		{
			g.drawImage(refinedImages[frame], 0, 0,getWidth(),getHeight(), null);
		}
		else if(image==BLANK)
		{
			g.setColor(Color.black);
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		
		if(paths==SHOWN_PATHS)
		{
			for(Dot d:Main.trackers[frame].dots)
			{
				d.paint(g,showDots);
			}
			
			for(DotTracker tracker:Main.trackers)
			{
				for(Dot d:tracker.dots)
				{
					d.resetPaintStatus();
				}
			}
		}
		else if(paths==ALL_PATHS)
		{
			for(DotTracker tracker:Main.trackers)
			{
				for(Dot d:tracker.dots)
				{
					d.paintOneDown(g);
					if(showDots && tracker==Main.trackers[frame])
					{
						d.paintSelf(g);
					}
				}
			}
		}
		else if(paths==BETWEEN)
		{
			for(int i=rangeStart;i<=rangeEnd;i++)
			{
				try
				{
					for(Dot d:Main.trackers[i].dots)
					{
						d.paintOneDown(g);
						if(showDots && i==frame)
						{
							d.paintSelf(g);
						}
					}
				}
				catch(Exception e)
				{
					
				}
			}
		}
		else if(paths==NO_PATHS && showDots)
		{
			for(Dot d:Main.trackers[frame].dots)
			{
				d.paintSelf(g);
			}
		}
		for(int i=0;i<Main.trackers.length;i++)
		{
			if(i<frame)
			{
				Main.trackers[i].paintHighlighted(g, Color.blue);
			}
			else if(i>frame)
			{
				Main.trackers[i].paintHighlighted(g, Color.green);
			}
		}
		Main.trackers[frame].paintHighlighted(g, Color.red);
	}
}
