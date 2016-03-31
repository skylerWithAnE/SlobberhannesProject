package javagamethingclient;

import java.net.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.locks.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

enum MessageID
{
    // Client Specific
    NOT_A_MESSAGE,
    LOST_CONNECTION,
    // Network Specific
    CONNECTION_ACCEPTED,
    CONNECTION_REJECTED,
    PLAYER_JOINED,
    PLAYER_QUIT,
    GAME_STATE_CHANGE,
    CARDS_DELT,
     PLAYER_TURN,
    CARD_PLAYED,
    ADD_POINTS,
    PLAYER_WON_TRICK,
    PLAYER_WON_GAME;
    
    
    public static MessageID convert(int value)
    {
        switch (value)
        {
            case 1:
                return MessageID.LOST_CONNECTION;
            case 48:
                return MessageID.CONNECTION_ACCEPTED;
            case 49:
                return MessageID.CONNECTION_REJECTED;
            case 50:
                return MessageID.PLAYER_JOINED;
            case 51:
                return MessageID.PLAYER_QUIT;
            case 52:
                return MessageID.GAME_STATE_CHANGE;      
            case 53:
                return MessageID.CARDS_DELT;      
            case 54:
                return MessageID.PLAYER_TURN;
            case 55:
                return MessageID.CARD_PLAYED;
            case 56:
                return MessageID.ADD_POINTS;
            case 57:
                return MessageID.PLAYER_WON_TRICK;
            case 97:
                return MessageID.PLAYER_WON_GAME;
            default:
                return MessageID.NOT_A_MESSAGE;
        }
    }
    
    public static int convert(MessageID id)
    {
        switch(id)
        {
            case LOST_CONNECTION:
                return 1;
            case CONNECTION_ACCEPTED:
                return 48;
            case CONNECTION_REJECTED:
                return 49;
            case PLAYER_JOINED:
                return 50;
            case PLAYER_QUIT:
                return  51;
            case GAME_STATE_CHANGE:
                return 52;
            case CARDS_DELT:
                return 53;
            case PLAYER_TURN:
                return 54;
            case CARD_PLAYED:
                return 55;
            case ADD_POINTS:
                return 56;
            case PLAYER_WON_TRICK:
                return 57;
            case PLAYER_WON_GAME:
                return 97;    
            default:
                return 0;//Not A Message ID
        }
    }
}

public class JavaGameThingClient
{   
    static SocketChannel networkSocketChannel;
    static Socket networkSocket;
    static PrintWriter networkWriter;
    static ArrayList<String> transferMessageQueue = new ArrayList<>();
    static NetworkingThread networkThread = new NetworkingThread();
    static ReentrantLock threadLock = new ReentrantLock();
    static boolean end = false;
    static Scanner networkScanner;
    
    static ArrayList<Integer> currentCards = new ArrayList<>();
    
    static int myPosition = -1;
    //static ArrayList<Card> myHand = new ArrayList<>();
    //static int[] myHandPos = {408, 400};
    
    //static ArrayList<Card> deck = new ArrayList<>();
    
    static boolean myTurn = false;
    
    static int[][] namePositions = {{400,560}, {10,290}, {400,70}, {790,290}};
    static int[][] cardFlyPositions = {{400, 700}, {-100, 290}, {400, -100}, {900, 290}};    
    static int[][] adjustedNamePositions = {{0,0}, {0,0}, {0,0}, {0,0}};
    static int[][] pointsPositions = {{400, 583}, {10, 310}, {400, 90}, {790, 310}};
    static int[][] adjustedPointsPositions = {{0,0}, {0,0}, {0,0}, {0,0}};
    static int[] playerPoints = {0,0,0,0};
    static double[] adjustmentDirections = {-0.5,0.0,-0.5,-1.0};
    static String[] names = {"","","",""};
    static int fontSize = 24;
    static FontMetrics currentFontMetrics = null;
    //static ArrayList<Card> flyingAwayCards = new ArrayList<>();
    static ArrayList<Card> allCards = new ArrayList<>();
    
    static Hand myHand = new Hand(408, 400);
    static Hand deck = new Hand(400,260);
    
    static String gameWonString = "";
    static int[] gameWonPosition = {400,300};
    static int[] gameWonAdjustedPosition = {0,0};
    static boolean gameWon = false;
//static int[] deckPosition = {320,230};
    
    static String myName = "";
    
    static Graphics2D g = null;
    
    static JFrame win = null;
    
    static BlockingQueue<MouseEvent> eventqueue = new LinkedBlockingQueue<>();
    
    public static double dTime = 0.0;
    
    public static void main(String[] args)
    {
        //initializeConnection("206.21.94.127", 6661);   
        initializeConnection("127.0.0.1", 6661);
        
        win = new JFrame("SlabbierHauns Client");
        win.setSize(800,600);
        win.setIgnoreRepaint(true);
        win.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        win.setVisible(true);
        win.createBufferStrategy(2);                  
        
        win.addMouseListener(new MouseAdapter() 
        {
            @Override
            public void mousePressed(MouseEvent ev) 
            {
                eventqueue.add(ev);
            }
        });         

        g = (Graphics2D) win.getBufferStrategy().getDrawGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setFont(new Font("Segoe UI Emoji 12",Font.PLAIN,fontSize));
        currentFontMetrics = g.getFontMetrics();

        g.setPaint(new Color(16,140,32));
        g.fillRect(0,0,win.getWidth(),win.getHeight());

        myName = JOptionPane.showInputDialog(null, "Name?");
        
        networkThread.start();
        
        setPlayerName(myName, 0);        
        
        createMessage( myName);
        //createMessage( MessageID.PLAYER_JOINED, "PooperScooper");
        
        setPoints(0,0);setPoints(1,0);setPoints(2,0);setPoints(3,0);
        
        double sTime = (double)System.currentTimeMillis()/1000.0;
        
        while (!end)
        {
            
            double cTime = (double)System.currentTimeMillis()/1000.0;
            dTime = cTime - sTime;
            sTime = cTime;
            
            /*if (dTime >= 0.5)
                System.out.println("dTime: " + dTime);*/
            //Attempt to send any queued out messages to networkthread
            transferMessages();
            
            //Lets do only one at a time
            //String message = null;
            String message = popNextMessage();
            if (message != null)
            {   
                System.out.printf("MID: %s : %s\n", message.charAt(0), parseMessage(message));
    
                switch (getMessageID(message))
                {
                    case CONNECTION_ACCEPTED:
                        recConnectionAccepted(message);
                        break;
                        
                    case CONNECTION_REJECTED:
                        System.out.println("Connection Rejected: " + parseMessage(message));                        
                        break;
                        
                    case PLAYER_JOINED:
                        recPlayerJoined(message);
                        break;
                        
                    case PLAYER_QUIT:
                        recPlayerQuit(message);
                        break;
                        
                    case GAME_STATE_CHANGE:
                        System.out.println("Game State Change: " + parseMessage(message));
                        break;
                            
                    case PLAYER_TURN:
                        recPlayerTurn(message); 
                        break;
                        
                    case CARDS_DELT:       
                        recCardsDelt(message);
                        break;
                        
                    case LOST_CONNECTION:
                        System.out.println("Lost Connection to Host, exiting application");
                        exitProgram();
                        break;
                        
                    case ADD_POINTS:
                        recAddPoints(message);
                        break;
                        
                    case PLAYER_WON_TRICK:
                        recPlayerWonTrick(message);
                        break;
                        
                    case PLAYER_WON_GAME:
                        recPlayerWonGame(message);
                        break;
                        
                    case CARD_PLAYED:
                        recCardPlayed(message);
                        break;
                        
                    default:
                        System.out.println("Warning: Got a non-message: " + parseMessage(message));
                        break;
                }
            }
            
            //Rendering
            
            g = (Graphics2D) win.getBufferStrategy().getDrawGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setFont(new Font("Segoe UI Emoji 12",Font.PLAIN,fontSize));
            currentFontMetrics = g.getFontMetrics();
            
            g.setPaint(new Color(16,140,32));
            g.fillRect(0,0,win.getWidth(),win.getHeight());
            
            g.setPaint(new Color(0,0,0));
            
            if (myTurn)
            {
                g.setPaint(new Color(255,255,0));
                g.drawString(names[0], adjustedNamePositions[0][0], adjustedNamePositions[0][1]);
                g.setPaint(new Color(0,0,0));
            }
            else
            {
                g.drawString(names[0], adjustedNamePositions[0][0], adjustedNamePositions[0][1]);
            }
            
            g.drawString(names[1], adjustedNamePositions[1][0], adjustedNamePositions[1][1]);
            g.drawString(names[2], adjustedNamePositions[2][0], adjustedNamePositions[2][1]);
            g.drawString(names[3], adjustedNamePositions[3][0], adjustedNamePositions[3][1]);
             
            g.drawString(Integer.toString(playerPoints[0]), adjustedPointsPositions[0][0], adjustedPointsPositions[0][1]);
            g.drawString(Integer.toString(playerPoints[1]), adjustedPointsPositions[1][0], adjustedPointsPositions[1][1]);
            g.drawString(Integer.toString(playerPoints[2]), adjustedPointsPositions[2][0], adjustedPointsPositions[2][1]);
            g.drawString(Integer.toString(playerPoints[3]), adjustedPointsPositions[3][0], adjustedPointsPositions[3][1]);
            
            if (gameWon)
            {
                g.setPaint(Color.YELLOW);
                g.drawString(gameWonString, gameWonAdjustedPosition[0], gameWonAdjustedPosition[1]);
                g.setColor(Color.BLACK);
            }
            
            MouseEvent me = eventqueue.poll();
            if (me != null)
            {
                if (me.getButton() == 1)
                {
                    if (myTurn)
                    //if (myTurn || !myTurn)
                    {
                        Card c = myHand.getClickedCard(me.getX(), me.getY());
                        if (c != null)
                        {
                            //c.moveTowards(deckPosition[0], deckPosition[1], 530.0f);
                            deck.addCard(myHand.removeCard(c), 530.0f);
                            int rank = c.rank-1;
                            int suit = c.suit.getValue()*13;

                            createMessage(MessageID.CARD_PLAYED, myPosition + "," +  Integer.toString(rank+suit));      

                            //deck.add(myHand.removeCard(c));
                            //repositionHand();
                            myTurn = false;
                            //break;
                        }
                    }
                       /* //System.out.println("MouseButton Clicked");
                        for (int i = myHand.size()-1; i >= 0; i--)
                        {
                            if (myHand.get(i).containsPoint(me.getX(), me.getY()))
                            {
                                myHand.get(i).moveTowards(deckPosition[0], deckPosition[1], 530.0f);
                                //
                                int rank = myHand.get(i).rank-1;
                                int suit = myHand.get(i).suit.getValue()*13;

                                createMessage(MessageID.CARD_PLAYED, myPosition + "," +  Integer.toString(rank+suit));      
                                //
                                
                                deck.add(myHand.remove(i));
                                repositionHand();
                                myTurn = false;
                                break;
                            }
                        }
                    }*/
                }
            }
            
            CardDepthMapper.updateAndDraw(dTime, g);
            
            /*for (int i = 0; i < allCards.size(); i++)
            {
                if (allCards.get(i).dead)
                {
                    allCards.remove(i);
                    i--;
                }
                else
                {
                    allCards.get(i).update(dTime);
                    allCards.get(i).draw(g);
                }
            }*/
            
           /* for(Card c: myHand )
            {
                c.update(dTime);
                //setPosition
                //c.moveTowards(100, 100, 40.0f);
                c.draw(g);
            }
            
            for (Card c: deck)
            {
                c.update(dTime);
                c.draw(g);
            }
            
            for (int i = 0; i < flyingAwayCards.size(); i++)
            {
                if (flyingAwayCards.get(i).dead)
                {
                    flyingAwayCards.remove(i);
                    i--;
                }
                else
                {
                    flyingAwayCards.get(i).update(dTime);
                    flyingAwayCards.get(i).draw(g);
                }
            }*/
                       
            g.dispose();
            win.getBufferStrategy().show();
           
            //Reduces busy waiting
            try
            {
                Thread.sleep(30);
            }
            catch(InterruptedException e)
            {
                System.exit(0);
            }
            
        }

        exitProgram();
    }

    
    public static void exitProgram()
    {
        networkThread.endThread();
        try
        {
            networkThread.join();
        }
        catch (InterruptedException e)
        {
            System.out.println("Something wrong happened to the Network Thread: " + e.getLocalizedMessage());
        }
        closeConnection();
        
        System.exit(0);
    }
    
    public static String popNextMessage()
    {
        if (!NetworkingThread.inMessageQueue.isEmpty() && threadLock.tryLock())
        {
            try
            {
                return NetworkingThread.inMessageQueue.remove(0);
            }
            finally
            {
                threadLock.unlock();
            }            
        }
        else
        {
            return null;
        }
    }
          
    public static void initializeConnection(String address ,int portNumber)
    {   
        try
        {
            //networkSocketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 6661));
            networkSocketChannel = SocketChannel.open(new InetSocketAddress(address, portNumber));
            networkSocket = networkSocketChannel.socket();
            networkScanner = new Scanner(networkSocket.getInputStream());
            networkWriter = new PrintWriter(networkSocket.getOutputStream());
        }
        catch(IOException e)
        {
            System.out.printf("IOException upon connection initialization: %s", e.getLocalizedMessage());
            System.exit(0);
        }
    }
    
    public static void closeConnection()
    {
        try
        {
            //Probally should tell the host that user has dissconnected// ONLY if it wasnt a server disconnect doi
            networkWriter.close();
            networkScanner.close();
            networkSocket.close();
        }
        catch(IOException e)
        {
            System.out.printf("IOException upon closing connection: %s\n", e.getLocalizedMessage());
            System.exit(0);
        }        
    }
    
    public static void createMessage(MessageID messageID, String message)
    {
        System.out.println("Created Message: " + new String((char)MessageID.convert(messageID) + message));
        transferMessageQueue.add(new String((char)MessageID.convert(messageID) + message) + "\n");
    }

    public static void createMessage(String message)
    {
        System.out.println("Created Message: " + message);        
        transferMessageQueue.add(message + "\n");
    }
    
    public static boolean transferMessages()
    {
        //System.out.println("Transfering ish");
        if (!transferMessageQueue.isEmpty() && threadLock.tryLock())
        {
            try
            {
                while (!transferMessageQueue.isEmpty())
                {
                    NetworkingThread.outMessageQueue.add(transferMessageQueue.remove(0));
                }
                return true;
            }
            finally
            {
                threadLock.unlock();
            }
        }
        return false;
    }
    
    //Returns -1 if message is not a valid message
    public static MessageID getMessageID(String message)
    {
        //First char is the message ID
        //Checking to see if there actually is a message or not
        return MessageID.convert((int)(message.charAt(0)));
    }
    
    //Returns the message without its message ID, doesnt check message ID at all
    public static String parseMessage(String message)
    {
        return message.substring(1);
    }
    
    
    public static Card spawnCard(int cardNumber, int x, int y)
    {
        int suitNum = (cardNumber-1)/13;
        int cardNum = (cardNumber-1)%13;
        
        Card c = new Card(cardNum+1, Card.Suit.valueOf(suitNum));
        //allCards.add(c);
        CardDepthMapper.addCard(c);
        c.setPosition(x, y);
        
        return c;
    }
        
    /*public static Card addNewCardToHand(int cardNumber)
    {           
        Card c = spawnCard(cardNumber, myHand.position[0], myHand.position[1]-100);
        c.flip(200);
        
        myHand.addCard(c);
        
        //repositionHand();
        
        return c;
    }*/
    
    /*static void repositionHand()
    {
        int adj = 0;
        int n = myHand.size()+1;//One extra for the full card
        if (n%2 == 1)
        {
            adj -= 56/2;
        }
        adj -= n/2*56;
        
        for (int i = 0; i < myHand.size(); i++)
        {
            myHand.get(i).moveTowards(myHandPos[0] + adj +  i * 56, myHandPos[1], 430);
            //myHand.get(i).setPosition(myHandPos[0] + adj +  i * 56, myHandPos[1]);
        }
    }*/
    
    static void setPlayerName(String name, int pos)
    {
        names[pos] = name;
        
        /*int[] xy = {0,0};
        
        //int n = name.length();
        
        for (int i = 0; i < name.length(); i++)
        {
            xy[0] += currentFontMetrics.charWidth(name.charAt(i)) * adjustmentDirections[pos][0];
        }
        
        xy[1] -= fontSize/2;*/
        
        adjustedNamePositions[pos][0] = namePositions[pos][0] + getStringAdjustmentX(name, adjustmentDirections[pos]);
        adjustedNamePositions[pos][1] = namePositions[pos][1];
        //adjustedNamePositions[pos][1] = xy[1];
    }
    
    static int getStringAdjustmentX(String stringToAdjust, double adjustmentDirectionX)
    {        
        int x = 0;
        
        //int n = name.length();
        
        for (int i = 0; i < stringToAdjust.length(); i++)
        {
            x += currentFontMetrics.charWidth(stringToAdjust.charAt(i)) * adjustmentDirectionX;
        }
        
        //x -= fontSize/2;
        return x;
    }
    
    static void addPoints(int pos, int points)
    {
        setPoints(pos, points);//playerPoints[pos] + points);
    }
    
    static void setPoints(int pos, int points)
    {
        playerPoints[pos] = points;
        adjustedPointsPositions[pos][0] = pointsPositions[pos][0] + getStringAdjustmentX(Integer.toString(points), adjustmentDirections[pos]);
        adjustedPointsPositions[pos][1] = pointsPositions[pos][1];
    }
    
    static int toRelativePos(int pos)
    {
        pos -= myPosition;
        if (pos < 0)
        {
            pos += 4;
        }
        return pos;
    }
    
    
    
    ///////////////////////////////////////
    //      Connection Accepted
    ///////////////////////////////////////
    static void recConnectionAccepted(String message)
    {
        String s = parseMessage(message);
        String[] splitStrings = s.split(",");

        if (splitStrings.length != 2)
        {
            System.out.println("Message Error, Message with ID 0 does not have exactly 2 parts [pos, message]");
            return;
        }

        int pos = -1;
        try
        {
            pos = Integer.parseInt(splitStrings[0]);
        }
        catch (NumberFormatException e)
        {
            System.out.println("Message Error, Message with ID 0 doesnot have an integer to parse as first value");
            return;
        }                        
        if (pos < 0 || pos > 3)
        {
            System.out.println("Message Error, Message with ID 0 has an invalid position");
            return;
        }

        myPosition = pos;

        System.out.println("Your position is: " + pos);
        System.out.println(splitStrings[1]);        
    }
    
    
    ///////////////////////////////////////
    //      Player Joined
    ///////////////////////////////////////    
    public static void recPlayerJoined(String message)
    {
        String s = parseMessage(message);
        String[] splitStrings = s.split(",");

        if (splitStrings.length != 2)
        {
            System.out.println("Message Error, Message with ID 2 does not have exactly 2 parts [pos, name]");
            return;
        }

        int pos = -1;
        try
        {
            pos = Integer.parseInt(splitStrings[0]);
        }
        catch (NumberFormatException e)
        {
            System.out.println("Message Error, Message with ID 2 doesnot have an integer to parse as first value");
            return;
        }                        
        if (pos < 0 || pos > 3)
        {
            System.out.println("Message Error, Message with ID 2 has an invalid position");
            return;
        }

        if (pos == myPosition)
        {
            System.out.println("Somethigns wrong, player " + splitStrings[1] + " joined in my position");
        }


        int oPos = pos;
        //Adjust to relative position
        pos = toRelativePos(pos);

        if (names[pos] != "")
        {
            System.out.println("Somethings wrong, player " + splitStrings[1] + " attempted to join an already filled out position by " + pos);
            return;
        }

        setPlayerName(splitStrings[1], pos);

        System.out.println("Player " + splitStrings[1] + " joined at position " + oPos + " Placing at position " + pos);
    }
    
    ///////////////////////////////////////
    //      Player Quit
    ///////////////////////////////////////
    public static void recPlayerQuit(String message)
    {
        String s = parseMessage(message);
        String[] splitStrings = s.split(",");

        if (splitStrings.length != 2)
        {
            System.out.println("Message Error, Message with ID 3 does not have exactly 2 parts [pos, name]");
            return;
        }

        int pos = -1;
        try
        {
            pos = Integer.parseInt(splitStrings[0]);
                                    //System.out.println("POS" + pos);
        }
        catch (NumberFormatException e)
        {
            System.out.println("Message Error, Message with ID 3 doesnot have an integer to parse as first value");
            return;
        }                        
        if (pos < 0 || pos > 3)
        {
            System.out.println("Message Error, Message with ID 3 has an invalid position");
            return;
        }

        //Adjust to relative position
        int posO = pos;
        pos = toRelativePos(pos);

        if (names[pos] == "")
        {
            System.out.println("Message Error, Message with and ID of 3 attempted to remove a no-existing player from position " + posO );
            return;
        }

        setPlayerName("",pos);
    }
    
    ///////////////////////////////////////
    //      Player Turn
    ///////////////////////////////////////    
    public static void recPlayerTurn(String message)
    {
        String s = parseMessage(message);
        String[] splitStrings = s.split(",");

        if (splitStrings.length != 1)
        {
            System.out.println("Message Error, Message with ID 6 has not exactly 1 item of content");
            return;
        }

        int pos = Integer.parseInt(splitStrings[0]);

        if (pos < 0 || pos > 3)
        {
            System.out.println("Issue in message id of 6, postion " + pos + " not a valid position");
            return;
        }

        if (pos == myPosition)
        {
            myTurn = true;
            System.out.println("Its my turn!!!");
        }
        else
        {
            myTurn = false; //insurance
            System.out.println("Its " + names[toRelativePos(pos)] + "'s turn");
        }
    }
    
    ///////////////////////////////////////
    //      Cards Delt
    ///////////////////////////////////////    
    public static void recCardsDelt(String message)
    {
        String s = parseMessage(message);
        String[] splitStrings = s.split(",");
        int[] cards = new int[splitStrings.length];

        boolean endLoop = false;
        for (int i = 0; i < splitStrings.length; i++)
        {
            int cardNum = -1;
            try
            {
                cardNum = Integer.parseInt(splitStrings[i]);
            }
            catch (NumberFormatException e)
            {
                //System.out.println("Message with ID of 5 had a value that could not be converted into a number: " + e.getLocalizedMessage());
                endLoop = true;
                return;
            }
            //System.out.println("CardNum: " + cardNum);

            if (cardNum < 0 || cardNum > 51)
            {
                System.out.println("Message Error, Message with ID 5 has an unreadable card number of " + cardNum);
                endLoop = true;
                return;
            }
            cards[i] = cardNum + 1;//Check with group
        }
        if (endLoop)
        {
            return;
        }
        
       /*while (!myHand.isEmpty())
        {
            myHand.get(0).flyAway(400.0f);
            myHand.get(0).moveTowards(cardFlyPositions[0][0], cardFlyPositions[0][1], 400.0f);
            myHand.remove(0);
        }*/
        myHand.throwAwayCards(cardFlyPositions[0][0], cardFlyPositions[0][1],400.0f,400.0f);

        for (int i = 0; i < cards.length; i++)
        {
            System.out.println("Added card number " + (cards[i]-1) + " to my hand");
            Card c = spawnCard(cards[i], myHand.position[0], myHand.position[1]-100);
            c.flip(200);

            myHand.addCard(c);            

//addNewCardToHand(cards[i]);
        }
    }
    
    ///////////////////////////////////////
    //      Card Played
    ///////////////////////////////////////    
    public static void recCardPlayed(String message)
    {
        String s = parseMessage(message);
        String[] splitStrings = s.split(",");

        int cardNum = Integer.parseInt(splitStrings[1]);

        boolean endLoop = false;

        if (cardNum < 0 || cardNum > 51)
        {
            System.out.println("Issue in message id of 7, card number " + cardNum + " not a valid card number");
            endLoop = true;
            return;
        }

        int pos = Integer.parseInt(splitStrings[0]);

        if (pos < 0 || pos > 3)
        {
            System.out.println("Issue in message id of 7, postion " + pos + " not a valid position");
            endLoop = true;
            return;
        }

        if (pos == myPosition)
        {
            //Confirmed
            return;
        }

        pos = toRelativePos(pos);

        Card c = spawnCard(cardNum+1, namePositions[pos][0], namePositions[pos][1]);
        c.flip(300);
        deck.addCard(c, 530.0f);
//c.moveTowards(deckPosition[0], deckPosition[1], 530.0f);

        //deck.add(c);

        System.out.println("Card " + cardNum + " was played");
    }
                           
    
    ///////////////////////////////////////
    //      Add Points
    ///////////////////////////////////////    
    public static void recAddPoints(String message)
    {
        String s = parseMessage(message);
        String[] splitStrings = s.split(",");

        if (splitStrings.length != 2)
        {
            System.out.println("Message Error, Message with ID 8 has not exactly 2 items of content");
            return;
        }        
        
        int pos = Integer.parseInt(splitStrings[0]);
        int points = Integer.parseInt(splitStrings[1]);

        if (pos < 0 || pos > 3)
        {
            System.out.println("Issue in message id of 8, postion " + pos + " not a valid position");
            return;
        }
        
        pos = toRelativePos(pos);
     
        addPoints(pos, points);

        System.out.println("Player " + names[pos] + " got " + points + " points");      
   
    } 
    
    ///////////////////////////////////////
    //      Player Won Trick
    ///////////////////////////////////////    
    public static void recPlayerWonTrick(String message)
    {
        String s = parseMessage(message);
        String[] splitStrings = s.split(",");

        if (splitStrings.length != 1)
        {
            System.out.println("Message Error, Message with ID 9 has not exactly 1 item of content");
            return;
        }        
        
        int pos = Integer.parseInt(splitStrings[0]);

        if (pos < 0 || pos > 3)
        {
            System.out.println("Issue in message id of 9, postion " + pos + " not a valid position");
            return;
        }
        
        pos = toRelativePos(pos);
        
        deck.throwAwayCards(cardFlyPositions[pos][0], cardFlyPositions[pos][1], 380.0f, 100.0f);
        
        System.out.println("Player " + names[pos] + " won the trick");      
   
    }
    
    ///////////////////////////////////////
    //      Player Won Game
    ///////////////////////////////////////    
    public static void recPlayerWonGame(String message)
    {
        String s = parseMessage(message);
        String[] splitStrings = s.split(",");

        if (splitStrings.length != 1)
        {
            System.out.println("Message Error, Message with ID a has not exactly 1 item of content");
            return;
        }        
        
        int pos = Integer.parseInt(splitStrings[0]);

        if (pos < 0 || pos > 3)
        {
            System.out.println("Issue in message id of a, postion " + pos + " not a valid position");
            return;
        }
        
        pos = toRelativePos(pos);
        
        gameWon = true;
        gameWonString = names[pos] + " Wins!!!";
        
        gameWonAdjustedPosition[0] = gameWonPosition[0] + getStringAdjustmentX(gameWonString, -0.5);
        gameWonAdjustedPosition[1] = gameWonPosition[1];
        
        
        
        //deck.throwAwayCards(cardFlyPositions[pos][0], cardFlyPositions[pos][1], 380.0f, 100.0f);
        
        System.out.println("Player " + names[pos] + " won the game!!!");      
   
    }    
    
}
