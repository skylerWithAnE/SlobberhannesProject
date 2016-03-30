package javagamethingclient;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class CardDepthMapper 
{
    private static HashMap<Integer, ArrayList<Card>> map = new HashMap<>();
    
    public static void addCard(Card c, int depth)
    {
        if (!map.containsKey(depth))
        {
            ArrayList<Card> al = new ArrayList<>();
            al.add(c);
            map.put(depth, al);
        }
        else
        {
            map.get(depth).add(c);
        }
    }
    //^
    public static void addCard(Card c)
    {
        addCard(c, c.getDepth());
    }
    
    
    public static void removeCard(Card c, int depth)
    {
        ArrayList al = map.get(depth);
        al.remove(c);
        if (al.isEmpty())
        {
            map.remove(depth);
        }
    }
    //^
    public static void removeCard(Card c)
    {
        removeCard(c, c.getDepth());
    }
    
    ///Will not set the card's depth, should be called before card switches depth
    public static void depthSwitch(int newDepth, Card c)
    {
        removeCard(c);
        addCard(c, newDepth);
    }
    
    public static void updateAndDraw(double dTime, Graphics2D g)
    {
        Set keys = map.keySet();
        Iterator it = keys.iterator();
        while (it.hasNext())
        {
            ArrayList<Card> al = map.get((Integer)it.next());
            for (int i = 0; i < al.size(); i++)
            {
                Card c = al.get(i);
                if (c.dead)
                {
                    al.remove(i);
                    i--;
                }
                else
                {
                    c.update(dTime);
                    c.draw(g);
                }                
            }
        }
    }
}
