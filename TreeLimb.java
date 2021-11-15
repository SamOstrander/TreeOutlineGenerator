import java.util.LinkedList;
import java.awt.geom.Point2D;
import java.util.Random;

public class TreeLimb {
    //uses a line to create a limb
    
    //double creation_time;   //use this to calculate length on draw.
    Point2D.Double position;        //position of base of branch relative to base of tree. 
    double length;
    double angle;
    double half_branchtip_angle = .75;   //angle of tip of branch triangle /2 (in degrees)
    double max_angle_adjustment = 1.3;  //modifier bound for tip angle randomization.
    //might need to store components of the angle function here, instead of just the angle, as i'll need to recalculate it every step otherwise.

        //data for the lines, constants rb and lb will  be adjusted as length increases over time.
    double cm;  //center slope
    double cb;  //center constant
    double lm;  //left slope
    double lb;  //left constant
    double rm;  //right slope
    double rb;  //right constant

    //should i use this type of variable?
    double next_branch_timer; //I could almost just tie this to a point along the branch, rather than a time. 

    int sub_branch;
    //int last_side;   //0 for left, 1 for right. (last side branch spawned from)
    int parent_side; //0 for left, 1 for right. the side of the parent branch this limb was spawned from. used for determining the line to use for the intersection point finding. 
    
        //next time
    boolean isBranching = true; //should this branch trigger creation of additional sub branches.
    int next_side;      //next branch spawned from [next_side] side. (left-0, right-1)
    double next_time;   //compared against length to determine if it's time to grow another branch. 
    
    TreeLimb parent;    //ptr to parent.

    //next_time max and min.
    double nt_max = 120;
    double nt_min = 100;

    LinkedList<TreeLimb> left_branches;// = new LinkedList<>();     //new branches added to end.
    LinkedList<TreeLimb> right_branches;// = new LinkedList<>();    //new branches added to front.

    public TreeLimb(Point2D.Double pos, double ang, int prev, TreeLimb par)
    {
        position = pos;
        angle = ang;    //angle of branch
        half_branchtip_angle += new Random().nextDouble()*max_angle_adjustment;   //adding randomization to branch width.
        
        length = 1; //base length of branch
        calculate_lines();

        sub_branch = 0;
        if(par != null)
        {
            sub_branch = par.get_subbranch()+1;
        }
        else
        {
            //for the dummy branch.
            //ensuring that the bottom of the tree is straight.
            //however, this might be unneccessary due to length being 0, meaning there is no angle adjustment due to size.
            lb = cb;
            rb = cb;
            lm = cm;
            rm = cm;
        }

        if(sub_branch >= 3) //no 4th sub_branches
        {
            isBranching = false;
        }

        parent_side = prev;   //0 for left, 1 for right. growing from this [parent_side] of parent branch.
        parent = par;    //ptr to parent.

                                                        //Ordering is deliberate for conversion of branch lines to Points.
        left_branches = new LinkedList<TreeLimb>();     //new branches added to end.
        right_branches  = new LinkedList<TreeLimb>();   //new add to front.

        next_side = new Random().nextInt(2);
        
        if(isBranching)  //Stops branching at third sub_branch.
            update_next();
    }

    public void add_branch(TreeLimb branch,int side)
    {
        if(side == 0)
            left_branches.addLast(branch);      //add to left_branches
        else
            right_branches.addFirst(branch);    //add to right_branches
    }

    public Point2D.Double get_position()
    {
        //returns position as Point2D(double x, double y)
        return position;
    }

    public void set_angle(double new_angle)
    {
        //Sets angle of branch. would only be used at creation.
        angle = new_angle;
    }

    public double get_angle()
    {
        //Returns angle of branch in degree
        return angle;
    }

    public void set_length(double len)
    {
        //Sets length and updates lines to reflect the change. Probably low cohesion.
        length = len;
        update_line();
    }
    public double get_length()
    {
        //Returns length of branch.
        return length;
    }

    public double get_nexttime()
    {
        //Returns length at which new branch spawn will be triggered
        return next_time;
    }
    public int get_nextside()
    {
        //Returns the side that the next branch will grow from.
        return next_side;
    }

    public void update_next()
    {
        //updates next_side and next_time
        next_time = length + new Random().nextDouble() * (nt_max-nt_min) + nt_min + sub_branch*10;
        next_side = (new Random().nextDouble() > .90) ? next_side:(next_side+1)%2; //.7 chance that next side is opposite last.
    }

    public boolean is_branching()
    {
        return isBranching;
    }

    public int get_subbranch()
    {
        return sub_branch;
    }

    public void set_subbranch(int sub)
    {
        //sets sub_branch. used for base branch(trunk), as all others are generated off of their parent's value.
        sub_branch = sub;
    }

    public void update_line()
    {
        //call when length has been changed (or upon creation of the branch). Updates constants. Slopes remain unchanged.
        
            //get new tip point.
        Point2D.Double branch_tip = get_tip();
        
        lb = branch_tip.getY() - branch_tip.getX()*lm;
        rb = branch_tip.getY() - branch_tip.getX()*rm;
    }

    public void grow_by_time(double time)
    {
        //pass a time val which determines growth amount.
        double sub_mod = ((double)sub_branch + 1);
        set_length(get_length()+time/sub_mod);//sub_branch);  //just add flat for now.
    }

    public void calculate_lines()
    {
        //Solves for 3 lines and stores their values as class members. 
        //This function is called once per unique branchLimb object within the class constructor.
        //First is the central line, or the branch skeleton.
        //Left and right lines form the outline of the branch and are used when creating the verticies for drawing the tree.

        //base point is position along parent skeleton line that this branch comes off of. 
        //that is stored at creation as [position].
        
        //Central line or branch skeleton.
        cm = Math.tan(Math.toRadians(angle));
        cb = position.getY() - position.getX()*cm;
        
        //x = r cos(theta) where r is length and theta is angle from origin to target point.
        Point2D.Double branch_tip = get_tip();

        //branch_tip used to create 2 lines. angle offset by half of branch tip angle.
        lm = Math.tan(Math.toRadians(angle + half_branchtip_angle));
        lb = branch_tip.getY() - branch_tip.getX()*lm;

        rm = Math.tan(Math.toRadians(angle - half_branchtip_angle));
        rb = branch_tip.getY() - branch_tip.getX()*rm;
    }

    public Point2D.Double get_intersection(int side)//double m1, double b1, double m2, double b2)
    {
        //returns intersection point between current limb's [side] line, and parent limb's [parent_side] line.
        double m1 = side < 1 ? lm : rm;
        double m2 = parent_side < 1 ? parent.get_leftslope() : parent.get_rightslope();
        double b1 = side < 1 ? lb : rb;
        double b2 = parent_side < 1 ? parent.get_leftconstant() : parent.get_rightconstant();

        if(m1 == m2)
        {
            //slopes are equivalent aka parallel lines. this shoudn't happen. 
            return null;
        }
        else
            return new Point2D.Double((-1*b2 - -1*b1) / (m1*-1 - m2*-1),(b1*m2 - b2*m1) / (m1*-1 - m2*-1));
    }

    public Point2D.Double get_tip()
    {
        //uses central line to get branch_tip.
        return new Point2D.Double(position.getX() + length*Math.cos(Math.toRadians(angle)),position.getY() + length*Math.sin(Math.toRadians(angle)));
    }

    public void set_nextside(int side)
    {
        next_side = side;
    }

    public void set_nexttimer(double timer)
    {
        next_time = timer;
    }

    public TreeLimb get_parent()
    {
        return parent;
    }

    public int get_parent_side()
    {
        //returns parent_size, 0 or 1.  0-left, 1-right.
        return parent_side;
    }

    //getters. will be used to grab a parent branch's line for finding intersection points. 
    public double get_leftslope()
    {
        return lm;
    }

    public double get_leftconstant()
    {
        return lb;
    }

    public double get_rightslope()
    {
        return rm;
    }

    public double get_rightconstant()
    {
        return rb;
    }

    public double get_centerslope()
    {
        return cm;
    }

    public double get_centerconstant()
    {
        return cb;
    }

}
