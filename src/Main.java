
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecodeParam;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.TIFFDirectory;

public class Main 
{
	static DotTracker[] trackers;
	static RenderedImage[] images;
	static ImageFrame im;

	static String filePath;
	static boolean foundValidTrackers=false;
	
	/**
	 * process the arguments to the program
	 * @param args arguments, straight from the command line.
	 */
	public static void settupArgs(String[] args)
	{
		if(args.length>0)
		{
			filePath=args[0];
		}
		else
		{
			filePath="For2Apugi003.tif";
		}
	}
	/**
	 * Sets up all the images and trackers.
	 * @throws IOException if the image cannot be loaded
	 */
	public static void settupImages() throws IOException
	{
		FileSeekableStream stream=new FileSeekableStream(filePath);

		ImageDecodeParam param=null;

		ImageDecoder dec= ImageCodec.createImageDecoder("tiff", stream, param);

		images=new RenderedImage[TIFFDirectory.getNumDirectories(stream)];


		System.out.println(images.length);

		for(int i=0;i<images.length;i++)
		{
			images[i]= new NullOpImage(dec.decodeAsRenderedImage(i), null, null, OpImage.OP_IO_BOUND);
		}

		JFrame f=new JFrame("Trak");
		im=new ImageFrame(images);

		File save=new File(filePath+".trak");
		if(save.exists())
		{
			try
			{
				ObjectInputStream fromSave=new ObjectInputStream(new GZIPInputStream(new FileInputStream(save)));
				trackers=(DotTracker[]) fromSave.readObject();

				if(trackers.length!=images.length)
				{
					throw new IOException("Invalid Trackers");
				}
				foundValidTrackers=true;
				System.out.println("Valid trackers found, not re-refining data");
			}
			catch(Exception e)
			{

			}
		}

		if(!foundValidTrackers)
		{
			trackers=new DotTracker[images.length];
		}

		f.add(im);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.pack();
		f.setLocationRelativeTo(null);
		
		for(int i=0;i<images.length;i++)
		{
			if(!foundValidTrackers)
			{
				trackers[i]=new DotTracker(im.datas[i]);
			}
			trackers[i].colorRefined(im.refinedImages[i].getGraphics());
			System.out.println(i);
		}

		remakeDots();
		
		f.setVisible(true);
	}
	/**
	 * initializes all the UI elements
	 */
	public static void settupInput()
	{
		final JPopupMenu rightClick=new JPopupMenu();
		final JMenu paths=new JMenu("Paths"); rightClick.add(paths);
		ButtonGroup pathGroup=new ButtonGroup();
		final JRadioButtonMenuItem allPaths=new JRadioButtonMenuItem("Show all"); paths.add(allPaths); pathGroup.add(allPaths);
		final JRadioButtonMenuItem currentPaths=new JRadioButtonMenuItem("Show visible"); paths.add(currentPaths); pathGroup.add(currentPaths);
		final JRadioButtonMenuItem somePaths=new JRadioButtonMenuItem("Show range"); paths.add(somePaths); pathGroup.add(somePaths);
		final JRadioButtonMenuItem noPaths=new JRadioButtonMenuItem("Show none"); paths.add(noPaths); pathGroup.add(noPaths);
		final JCheckBoxMenuItem showDots=new JCheckBoxMenuItem("Show dots"); paths.add(showDots); showDots.setSelected(true);
		currentPaths.setSelected(true);
		final JMenu image=new JMenu("Image"); rightClick.add(paths); rightClick.add(image);
		ButtonGroup imageGroup=new ButtonGroup();
		final JRadioButtonMenuItem refined=new JRadioButtonMenuItem("Refined");   image.add(refined); imageGroup.add(refined);
		final JRadioButtonMenuItem blank=new JRadioButtonMenuItem("Blank"); image.add(blank); imageGroup.add(blank);
		refined.setSelected(true);


		im.addMouseListener(new MouseAdapter(){

			public void mouseClicked(final MouseEvent e)
			{
				if(e.getButton()==3)
				{
					rightClick.show(im, e.getX(), e.getY());
				}
				else if(e.getButton()==1)
				{
					for(DotTracker t:trackers)
					{
						t.highlighted=null;
					}
					
					ArrayList<Dot> generation=new ArrayList<Dot>();
					generation.add(trackers[im.frame].closestDot(e.getX()/im.scaleX(),e.getY()/im.scaleY()));
					int i=im.frame;
					while(!generation.isEmpty())
					{
						trackers[i].highlighted=generation;
						ArrayList<Dot> newGen=new ArrayList<Dot>();
						for(Dot d:generation)
						{
							for(Dot childDot:d.getChildren())
							{
								if(!newGen.contains(childDot))
								{
									newGen.add(childDot);
								}
							}
						}
						generation=newGen;
						i++;
					}
				}
			}

		});
		im.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e)
			{
				if(e.getKeyCode()==KeyEvent.VK_LEFT)
				{
					if(im.frame>0)
					{
						im.frame--;
						im.repaint();
					}
				}
				else if(e.getKeyCode()==KeyEvent.VK_RIGHT)
				{
					if(im.frame<im.frames-1)
					{
						im.frame++;
						im.repaint();
					}
				}

			}
		});

		JFrame controls=new JFrame("Controls");
		controls.getContentPane().setLayout(new GridLayout(0,2));
		JLabel intenLabel=new JLabel("Minimum dot intensity (0 for is black, 1 is white)"); controls.add(intenLabel);
		final JTextField intensity=new JTextField(DotTracker.DOT_PEAK_INTENSITY_CUTOFF+""); controls.add(intensity);
		JLabel distLabel=new JLabel("Maximum travel distance (pixels)"); controls.add(distLabel);
		final JTextField distance=new JTextField(DotTracker.DOT_DISTANCE_CUTOFF+""); controls.add(distance);

		controls.add(new JLabel(" "));
		controls.add(new JLabel(" "));

		JLabel rStartLabel=new JLabel("Range Start"); controls.add(rStartLabel);
		final JTextField rStart=new JTextField(im.rangeStart+""); controls.add(rStart); rStart.setEnabled(false);
		JLabel rEndLabel=new JLabel("Range End"); controls.add(rEndLabel);
		final JTextField rEnd=new JTextField(im.rangeEnd+""); controls.add(rEnd);  rEnd.setEnabled(false);
		controls.pack();
		controls.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		controls.setVisible(true);

		ActionListener l=new ActionListener()
		{

			public void actionPerformed(ActionEvent e)
			{
				Object source=e.getSource();
				if(source==allPaths)
				{
					im.paths=ImageFrame.ALL_PATHS;
				}
				else if(source==currentPaths)
				{
					im.paths=ImageFrame.SHOWN_PATHS;
				}
				else if(source==somePaths)
				{
					im.paths=ImageFrame.BETWEEN;
				}
				else if(source==noPaths)
				{
					im.paths=ImageFrame.NO_PATHS;
				}
				else if(source==showDots)
				{
					im.showDots=showDots.isSelected();
				}

				else if(source==refined)
				{
					im.image=ImageFrame.REFINED;
				}
				else if(source==blank)
				{
					im.image=ImageFrame.BLANK;
				}

				rStart.setEnabled(somePaths.isSelected());
				rEnd.setEnabled(somePaths.isSelected());
			}

		};

		allPaths.addActionListener(l);
		currentPaths.addActionListener(l);
		noPaths.addActionListener(l);
		somePaths.addActionListener(l);
		showDots.addActionListener(l);

		refined.addActionListener(l);
		blank.addActionListener(l);


		final ActionListener controlListener=new ActionListener(){

			public void actionPerformed(ActionEvent e)
			{
				try
				{
					Object source=e.getSource();
					if(source==intensity)
					{
						double newIntensity=Double.parseDouble(intensity.getText());
						if(newIntensity!=DotTracker.DOT_PEAK_INTENSITY_CUTOFF)
						{
							DotTracker.DOT_PEAK_INTENSITY_CUTOFF=newIntensity;
							remakeDots();
						}
					}
					else if(source==distance)
					{
						double newDist=Double.parseDouble(distance.getText());
						if(newDist!=DotTracker.DOT_DISTANCE_CUTOFF)
						{
							DotTracker.DOT_DISTANCE_CUTOFF=newDist;
							remakeConnections();
						}
					}
					else if(source==rStart)
					{
						im.rangeStart=Integer.parseInt(rStart.getText());
					}
					else if(source==rEnd)
					{
						im.rangeEnd=Integer.parseInt(rEnd.getText());
					}
				}
				catch(Exception er)
				{

				}
			}};
			FocusListener prentendJavascript=new FocusListener() //in javascript, a field losing focus is an action. I make it so here, so that changing the controls will have effect immediately 
			{

				public void focusGained(FocusEvent e)
				{}

				public void focusLost(FocusEvent e)
				{
					Object source=e.getSource();
					controlListener.actionPerformed(new ActionEvent(source, 0, "Focus Lost"));
				}

			};
			intensity.addActionListener(controlListener);
			distance.addActionListener(controlListener);
			rStart.addActionListener(controlListener);
			rEnd.addActionListener(controlListener);

			intensity.addFocusListener(prentendJavascript);
			distance.addFocusListener(prentendJavascript);
			rStart.addFocusListener(prentendJavascript);
			rEnd.addFocusListener(prentendJavascript);
	}
	
	/**
	 * saves all the trackers
	 * @throws IOException if for some reason it cannot save
	 */
	public static void save() throws IOException
	{
		if(!foundValidTrackers)
		{
			File autoSave=new File(filePath+".trak");
			ObjectOutputStream toSave=new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(autoSave)));
			toSave.flush();
			toSave.writeObject(trackers);
			toSave.close();
		}
	}
	
	public static void main(String[] args) throws IOException
	{

		settupArgs(args);

		settupImages();

		im.showDots=true;
		im.paths=ImageFrame.SHOWN_PATHS;

		settupInput();

		save();

		System.out.println("done");
	}
	
	/**
	 * remakes all the dots. Perhaps their was an algorithm change.
	 */
	public static void remakeDots()
	{
		int popPathState=im.paths; //Pop it like a stack (SPOILERS: IT IS)
		boolean popDotState=im.showDots;

		im.paths=ImageFrame.NO_PATHS;
		im.showDots=false;
		for(DotTracker tracker:trackers)
		{
			tracker.assignDots();
		}
		remakeConnections();

		im.paths=popPathState;
		im.showDots=popDotState;
	}
	/**
	 * remakes all the connections between dots.
	 */
	public static void remakeConnections()
	{
		int popPathState=im.paths; //Pop it like a stack
		boolean popDotState=im.showDots;

		im.paths=ImageFrame.NO_PATHS;
		im.showDots=false;

		for(int i=0;i<trackers.length-1;i++)
		{
			trackers[i].assignAllBecomes(trackers[i+1]);
		}

		im.paths=popPathState;
		im.showDots=popDotState;
	}
}
