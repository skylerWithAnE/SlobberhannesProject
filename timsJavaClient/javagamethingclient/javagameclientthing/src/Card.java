package javagamethingclient;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;



public class Card{

    static int WIDTH = 97;
    static int HEIGHT = 129;
    static BufferedImage texture;
    static BufferedImage cardback;
    
    enum State{ FACE_DOWN, FACE_UP, TURNING_UP, TURNING_DOWN, FLYING_AWAY };
    
    public enum Suit{
        DIAMONDS(0), HEARTS(1), SPADES(2), CLUBS(3);
        private final int value;
        private Suit(int x){
            value=x;
        }
        public int getValue(){
            return value;
        }
        public static Suit valueOf(int i){
            switch (i) {
                case 0:
                    return Suit.DIAMONDS;
                case 1:
                    return Suit.HEARTS;
                case 2:
                    return Suit.SPADES;
                case 3:
                    return Suit.CLUBS;
                default:
                    throw new RuntimeException("Bad value for valueOf: "+i);
            }
        }
    };


    int rank;
    Suit suit;
    double x,y;
    State state;      //0=face down, 1=face up, 2=turning face up, 3=turning face down
    float progress;   //if in state 2 or 3: value from 0...100 giving percentage
    BufferedImage spr;
    
    boolean moving = false;
    int[] moveLocation = {0,0};
    float moveSpeed = 0.0f;
    
    boolean dead = false;
    
    float flipRate = 0.0f;
    float flyAwayRate = 0.0f;
    
    private int depth = 0;
    
    public Card(int rank, Suit suit){
        this.rank=rank;
        this.suit=suit;
        x=y=0;
        state=State.FACE_DOWN;
        progress=0;
        if(texture == null){
            try {
                texture = ImageIO.read(new File("allcards.png"));
            } catch (IOException ex) {
                throw new RuntimeException("Cannot load allcards.png");
            }
            cardback = new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB);
            cardback.getGraphics().drawImage(
                    texture,
                    0,0, WIDTH,HEIGHT,      //dest
                    4*WIDTH,4*HEIGHT,5*WIDTH,5*HEIGHT,   //src
                    new Color(0,0,0,0),
                    null);
        }
        spr = new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB);
        spr.getGraphics().drawImage(
                texture,
                0,0,WIDTH,HEIGHT,
                (rank == 14) ? 0: ((rank-1)*WIDTH),
                suit.getValue()*HEIGHT, 
                (rank == 14) ? WIDTH : (rank*WIDTH),
                (suit.getValue()+1)*HEIGHT,
                new Color(0,0,0,0),
                null);
    }
    
    @Override
    protected Card clone(){
        Card C = new Card(rank,suit);
        C.x=x;
        C.y=y;
        C.state=state;
        C.progress=progress;
        return C;
    }
    
    void setPosition(int x, int y){
        this.x=x;
        this.y=y;
    }
    
    void update(double dTime)
    {
        if (!dead)
        {
            if( state == State.TURNING_DOWN || state == State.TURNING_UP ){
                progress+= dTime*flipRate;
                if( progress >= 100 ){
                    progress = 0;
                    if( state == State.TURNING_UP )
                        state = State.FACE_UP;
                    else
                        state = State.FACE_DOWN;
                }
            }
            else if( state == State.FLYING_AWAY ){
                progress += dTime * flyAwayRate;
                if( progress >= 100 )
                {
                    //dead = true;
                    progress = 100;
                }
            }
            //x += 1;
            //x = x + 10*dTime;
            if (moving)
            {
                double[] q = {(double)(moveLocation[0] - x),(double)(moveLocation[1] - y)};
                double qLength = Math.sqrt(q[0]*q[0] + q[1]*q[1]);
                double[] qHat = {q[0]/qLength, q[1]/qLength};
                double displacement = moveSpeed*dTime;

                if (displacement >= qLength)
                {
                    x = moveLocation[0];
                    y = moveLocation[1];
                    moving = false;
                }
                else
                {
                    x += qHat[0]*displacement;
                    y += qHat[1]*displacement;
                }
            }
        }
    }
    
    void moveTowards(int x, int y, float speed)
    {
        int[] tmp = {x, y};
        moveLocation = tmp;
        moving = true;
        moveSpeed = speed;
    }
    
    boolean isFaceUp(){
        return state == State.FACE_UP || state == State.TURNING_UP ;
    }
    
    boolean containsPoint(int x, int y){
        if( state == State.FLYING_AWAY )
            return false;
            
        if( x >= this.x && y >= this.y && x <= this.x + WIDTH && y <= this.y + HEIGHT )
            return true;
        else
            return false;
    }
    
    void flip(float rate){
        if( state == State.FACE_DOWN ){
            state = State.TURNING_UP;
            progress = 0;
        }
        else if( state == State.FACE_UP ) {
            state = State.TURNING_DOWN;
            progress = 0;
        }
        else if( state == State.TURNING_UP ){
            state = State.TURNING_DOWN;
            progress = 100-progress;
        }
        else if( state == State.TURNING_DOWN ){
            state = State.TURNING_UP;
            progress = 100-progress;
        }
        flipRate = rate;
    }

    void flyAway(float rate){
        state = State.FLYING_AWAY;
        progress = 0;
        flyAwayRate = rate;
    }
    
    void draw(Graphics2D win){
        BufferedImage S;
        float sx=1.0f;
        float alpha=1.0f;
        if( state == State.FLYING_AWAY ){
            S = spr;
            alpha = (1.0f-progress/100.0f);
        }
        else{
            if( state == State.FACE_DOWN || (state == State.TURNING_UP && progress < 50 ) || (state == State.TURNING_DOWN && progress > 50)  )
                S = cardback;
            else
                S = spr;

            if( progress < 50 )
                sx = (50-progress)/50.0f;
            else
                sx = (progress-50)/50.0f;
        }
        
        if( alpha != 1.0f )
            S = new RescaleOp(new float[]{1.0f,1.0f,1.0f,alpha},new float[]{0.0f,0.0f,0.0f,0.0f},null).filter(S,null);
            
        win.drawImage(S,
            new AffineTransform(
                sx,     0.0f,       //x scale, y shear
                0.0f,   1.0f,       //x shear, y scale
                x+(1.0f-sx)*WIDTH/2,y   //x,y translate
            ),
            null
        );
    }
    
    public void switchDepth(int newDepth)
    {
        CardDepthMapper.depthSwitch(newDepth, this);
        this.depth = newDepth;
    }
    
    public int getDepth()
    {
        return depth;
    }
    
    public String toString(){
        return "[Card "+rank+" "+suit+"]";
    }
}
        
