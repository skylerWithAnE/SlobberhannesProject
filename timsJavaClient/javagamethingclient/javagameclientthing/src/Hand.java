package javagamethingclient;

import java.util.ArrayList;

public class Hand 
{
    public int[] position = {0,0};
    
    private ArrayList<Card> cards = new ArrayList<>();
    
    Hand(int x, int y)
    {
        position[0] = x;
        position[1] = y;
    }
    
    public void repositionCards(float speed)
    {
        int adj = 0;
        int n = cards.size()+1;//One extra for the full card
        if (n%2 == 1)
        {
            adj -= 56/2;
        }
        adj -= n/2*56;
        
        int j = cards.size()-1;
        for (int i = 0; i < cards.size(); i++)
        {
            cards.get(i).moveTowards(position[0] + adj +  i * 56, position[1], speed);
            cards.get(i).switchDepth(j);
            j--;
//myHand.get(i).setPosition(myHandPos[0] + adj +  i * 56, myHandPos[1]);
        }        
    }
    
    public void addCard(Card c, float repositionSpeed)
    {
        cards.add(0, c);
        repositionCards(repositionSpeed);
    }
    //^
    public void addCard(Card c)
    {
         addCard(c, 430);
    }
    
    public Card removeCard(int index, float repositionSpeed)
    {
        Card c = cards.remove(index);
        repositionCards(repositionSpeed);
        return c;
    }
    //^
    public Card removeCard(int index)
    {
        return removeCard(index, 400);
    }    
    //^^
    public Card removeCard(Card c, float repositionSpeed)
    {
        return removeCard(cards.indexOf(c), repositionSpeed);
    }
    //^
    public Card removeCard(Card c)
    {
       return removeCard(c, 400);
    }
    
    public Card getClickedCard(int mX, int mY)
    {
        for (int i = cards.size()-1; i >= 0; i--)
        {
            if (cards.get(i).containsPoint(mX, mY))
            {
                return cards.get(i);
            }
        }
        return null;
    }
    
    public Card getCard(int index)
    {
        return cards.get(index);
    }
    
    public int getNumberOfCards()
    {
        return cards.size();
    }
    
    public void throwAwayCards(int destX, int destY, float moveSpeed, float disapearSpeed)
    {
        while (!cards.isEmpty())
        {
            cards.get(0).flyAway(disapearSpeed);
            cards.get(0).moveTowards(destX, destY, moveSpeed);
            cards.remove(0);
        }        
    }
}
