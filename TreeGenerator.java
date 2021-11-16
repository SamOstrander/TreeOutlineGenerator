import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.ArrayList;

import javax.swing.*;    

class TreeCanvas extends JComponent {
    //
    List<Point2D.Double> point_list;  //

    int window_width;
    int window_height;
    int offset_x;
    int offset_y;

    public void set_window_size(int w, int h)
    {
        //updates window size and offset vals for use in the paint function.
        window_width = w;
        window_height = h;
        offset_x = window_width/2;
        offset_y = window_height-42;
    }

    public void set_list(List<Point2D.Double> l)
    {
        point_list = l;
        repaint();
    }

    public void paint(Graphics g)
    {   
            //I'll need to add adjustments here. little offsets to flip the y as well as start drawing from the bottom center of the screen. 
        if (point_list == null)
        {
            return;
        }
        int sz = point_list.size();
        
        for (int i=0; i < point_list.size(); ++i)  //draws lines connecting the points potulating vertex_list. (last point is connected to first point)
            g.drawLine((int)Math.round(point_list.get(i).getX())+offset_x,-1*(int)Math.round(point_list.get(i).getY())+offset_y,(int)Math.round(point_list.get((i+1)%sz).getX())+offset_x,-1*(int)Math.round(point_list.get((i+1)%sz).getY())+offset_y);
    }
}

public class TreeGenerator {
    //Generates a randomized 2d tree, first as a series of lines, which are then converted to a tree outline.

    static List<Point2D.Double> tree_verticies = new ArrayList<Point2D.Double>();

    static TreeLimb base;
    static TreeCanvas tCanv;

    static double max_ang = 60; //base range of angles
    static double min_ang = 55; //

    static double max_height;
    
    public static void main(String[] args)
    {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
        int window_h = int window_h = 1024;//(int)screenSize.getHeight();   //switched to fixed height
        int window_w = window_h;

        max_height = (double)window_h-240;  //used in capping height of tree.

        tCanv = new TreeCanvas();

        JFrame window = new JFrame("Tree Generator");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setBounds((int)(window_w/6), 0, (int)(5*window_w/6), (int)(5*window_h/6));
        tCanv.set_window_size((int)window.getSize().getWidth(),(int)window.getSize().getHeight());

        JButton button = new JButton("Generate Tree");
        button.setBounds(50,50,150,100);
        button.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                reset_tree();
                Timer t1 = new Timer();
                
                t1.scheduleAtFixedRate(new TimerTask() {
                    TreeLimb b1 = TreeGenerator.base;
                    public void run(){
                        TreeGenerator.clear_list();
                        TreeGenerator.update_tree(TreeGenerator.base, 2.0);
                        TreeGenerator.tCanv.set_list(TreeGenerator.tree_verticies);
                        if(b1 != base || TreeGenerator.base.get_length() >= TreeGenerator.max_height)
                            cancel();
                            return;
                    }
                },0,1000/200);
            }
        });
        window.add(button);
        window.getContentPane().add(tCanv);
        window.setVisible(true);

        //TreeLimb base
        base = new TreeLimb(new Point2D.Double(0,0),90.0,new Random().nextInt(2),new TreeLimb(new Point2D.Double(0,0),0.0,0,null));
        base.set_subbranch(0);

        tree_verticies.clear();
        tCanv.set_list(tree_verticies); //update vertex list.
    }

    
    public static void clear_list()
    {
        tree_verticies.clear();
    }

    public static void reset_tree()
    {
        base = new TreeLimb(new Point2D.Double(0,0),90.0,new Random().nextInt(2),new TreeLimb(new Point2D.Double(0,0),0.0,0,null));//new TreeLimb(new Point2D.Double(0,0),0.0,0,null));
        base.set_subbranch(0);  //bugfix
    }

    public static void update_tree(TreeLimb limb, double time)
    {   
        limb.grow_by_time(time);

        //add base left point.
        //meaning, intersection between limb's left line, and parent's [parent_side] line.
        tree_verticies.add(limb.get_intersection(0));    //takes in side (0 or 1), and uses parent side.

        if(limb.is_branching() && limb.get_length() >= limb.get_nexttime())
        {
            //if can_branch, and length passed grow threshold, then create new branch.

            //modifying a randomized element to get an angle shifting for a new branch. altered by both length of branch, as well as how deep a sub_branch it is.
            double rn = new Random().nextDouble() * (max_ang - min_ang) + min_ang - ((limb.get_subbranch()-1)*20) - (15*limb.get_length()/max_height);  //randomize angle between bounds.
            
            double next_ang = ((double)limb.get_nextside()*2 - 1)*rn + limb.get_angle();
            Point2D.Double pt = new Point2D.Double(limb.get_position().getX() + limb.get_length()*Math.cos(Math.toRadians(limb.get_angle())),limb.get_position().getY() + limb.get_length()*Math.sin(Math.toRadians(limb.get_angle())));

            limb.add_branch(new TreeLimb(pt, next_ang, limb.get_nextside(), limb), limb.get_nextside());
            limb.update_next();
        }

        for (int i=0; i < limb.left_branches.size(); ++i)
        {
            //call update
            update_tree(limb.left_branches.get(i),time);
        }
        
        //add tip point
        tree_verticies.add(limb.get_tip()); //generated using angle, length and base point.

        //recursively loops through right branches
        for (int i=0; i < limb.right_branches.size(); ++i)
        {
            //call update
            update_tree(limb.right_branches.get(i),time);
        }

        //add base right point
        tree_verticies.add(limb.get_intersection(1));

    }

    public static void print_points(List<Point2D.Double> list)
    {
        //print ordered list of points which create the tree.

        if(list == null)
        {
            return;
        }
        int sz = list.size();
        for(int i=0;i<sz;++i)
        {
            list.get(i).getX();
            System.out.println(list.get(i).getX() + " , " + list.get(i).getY());
        }
        return;
    }
}
