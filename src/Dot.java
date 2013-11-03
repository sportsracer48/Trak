import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;

/**
 * A dot represents where an object may be a a particular time. It can have information about where the object may go, saved as child dots, where it has been, saved as parent dots, and where else the object may be at the
 * moment, saved as sibling dots.
 * @author Henry Rachootin
 *
 */
public class Dot
{
	private Point pos;
	
	private ArrayList<Dot> children=new ArrayList<Dot>();
	private ArrayList<Dot> parents=new ArrayList<Dot>();
	private ArrayList<Dot> siblings=new ArrayList<Dot>();
	
	private boolean paintedDown;
	private boolean paintedUp;
	
	private boolean paintingUp;
	private boolean paintingDown;
	
	public final DotTracker tracker;
	
	/**
	 * 
	 * @param d the dot to find the distance to
	 * @return the distance from this dot to d (not including time or generation)
	 */
	public double distance(Dot d)
	{
		return pos.distance(d.pos);
	}
	
	/**
	 * Due to the recursive storage of dots and their painting, each dot must keep track of if it has been painted each frame. This Method resets that track, and should be called for every dot after every frame.
	 */
	public void resetPaintStatus()
	{
		paintedDown=false;
		paintedUp=false;
	}
	/**
	 * creates a dot with that position
	 */
	public Dot(Point pos,DotTracker t)
	{
		this.pos=pos;
		this.tracker=t;
	}
	/**
	 * Adds dot d to this dot's children. Side effect: adds this dot to d's parents.
	 * @param d the dot to add
	 */
	public void addChild(Dot d)
	{
		children.add(d);
		d.addParent(this);
	}
	/**
	 * Adds dot d to this dot's siblings. (Dots in the same frame which are close enough to be the same dot)
	 * @param d the dot to add
	 */
	public void addSibling(Dot d)
	{
		siblings.add(d);
	}
	private void addParent(Dot d)
	{
		parents.add(d);
	}
	
	/**
	 * Paints a green line from this dot to each of its children.
	 * @param g the Graphics to paint to
	 */
	public void paintOneDown(Graphics g)
	{
		g.setColor(new Color(0,255,0,100));
		for(Dot d:children)
		{
			g.drawLine(Main.im.toScreenX(pos.x), Main.im.toScreenY(pos.y), Main.im.toScreenX(d.pos.x), Main.im.toScreenY(d.pos.y));
		}
		for(Dot d:siblings)
		{
			g.drawLine(Main.im.toScreenX(pos.x), Main.im.toScreenY(pos.y), Main.im.toScreenX(d.pos.x), Main.im.toScreenY(d.pos.y));
		}
	}
	/**
	 * Paints a red circle at this dot and a red line to each of its siblings.
	 * @param g the Graphics to paint to
	 */
	public void paintSelf(Graphics g,Color c)
	{
		g.setColor(c);
		g.fillOval(Main.im.toScreenX(pos.x)-(int)Main.im.scaleX()/2-1, Main.im.toScreenY(pos.y)-(int)Main.im.scaleY()/2-1, (int)Main.im.scaleX()+1, (int)Main.im.scaleY()+1);
		for(Dot d:siblings)
		{
			g.drawLine(Main.im.toScreenX(pos.x), Main.im.toScreenY(pos.y), Main.im.toScreenX(d.pos.x), Main.im.toScreenY(d.pos.y));
		}
	}
	
	
	public void paintSelf(Graphics g)
	{
		paintSelf(g,new Color(255,0,0,100));
	}
	
	/**
	 * Paints this dot's history in blue and posterity in green in total.
	 * @param g the Graphics to paint to
	 * @param paintSelf this method will also paint the dot itself in red if true.
	 */
	public void paint(Graphics g,boolean paintSelf)
	{
		paintDown(g,new Color(0,255,0,100));
		paintUp(g,new Color(0,0,255,100));
		if(paintSelf)
		{
			paintSelf(g,new Color(255,0,0,100));
		}
	}
	/**
	 * This is where the dot recursively paints its ancestors. Only goes backwards in time. Should refrain from redundant drawing and StackOverflowExceptions.
	 * @param g
	 */
	private void paintUp(Graphics g,Color c)
	{
		paintingUp=true;
		g.setColor(c);
		for(Dot d:parents)
		{
			g.drawLine(Main.im.toScreenX(pos.x), Main.im.toScreenY(pos.y), Main.im.toScreenX(d.pos.x), Main.im.toScreenY(d.pos.y));
			if(!d.paintedUp)
				d.paintUp(g,c);
		}
		for(Dot d:siblings)
		{
			g.drawLine(Main.im.toScreenX(pos.x), Main.im.toScreenY(pos.y), Main.im.toScreenX(d.pos.x), Main.im.toScreenY(d.pos.y));
			if(!d.paintingUp && !d.paintedUp)
				d.paintUp(g,c);
		}
		paintedUp=true; //Gotta keep track to prevent redundancy. Other solution require massive amounts of graph theory.
		paintingUp=false;
	}
	/**
	 * This is where the dot recursively paints its posterity. Only goes forwards in time. Should refrain from redundant drawing and StackOverflowExceptions.
	 * @param g
	 */
	private void paintDown(Graphics g,Color c)
	{
		paintingDown=true;
		g.setColor(c);
		for(Dot d:children)
		{
			g.drawLine(Main.im.toScreenX(pos.x), Main.im.toScreenY(pos.y), Main.im.toScreenX(d.pos.x), Main.im.toScreenY(d.pos.y));
			if(!d.paintedDown)
				d.paintDown(g,c);
		}
		for(Dot d:siblings)
		{
			g.drawLine(Main.im.toScreenX(pos.x), Main.im.toScreenY(pos.y), Main.im.toScreenX(d.pos.x), Main.im.toScreenY(d.pos.y));
			if(!d.paintingDown && !d.paintedDown)
				d.paintDown(g,c);
		}
		paintedDown=true; //Gotta keep track.
		paintingDown=false;
	}
	
	/**
	 * 
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @return the distance from this dot to x,y
	 */
	public double distance(double x, double y)
	{
		return pos.distance(x, y);
	}
	
	/**
	 * Paints this dot, where it goes, where it came from, and, if paintSelf, a circle where it is.
	 * @param g the graphics to paint to.
	 * @param paintSelf whether or not to paint a circle at the current location
	 * @param c the color to paint in.
	 */
	public void paint(Graphics g, boolean paintSelf, Color c)
	{
		paintDown(g,c);
		paintUp(g,c);
		if(paintSelf)
		{
			paintSelf(g,c);
		}
	}
	
	/**
	 * 
	 * @return all the dots this dot becomes in the next frame.
	 */
	public ArrayList<Dot> getChildren()
	{
		return children;
	}
}
