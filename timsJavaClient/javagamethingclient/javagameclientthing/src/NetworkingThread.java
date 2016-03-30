package javagamethingclient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

public class NetworkingThread extends Thread {

    volatile boolean done = false;
    static ArrayList<String> inMessageQueue = new ArrayList<>();
    static ArrayList<String> outMessageQueue = new ArrayList<>();
    static String inStringCache = new String("");

    public void endThread() {
        done = true;
    }

    @Override
    public void run() {
        done = false;
        Selector selector = null;

        try {
            selector = Selector.open();
            JavaGameThingClient.networkSocketChannel.configureBlocking(false);
        } catch (IOException e) {
            System.out.printf("IOException upon creating a selector: %s\n", e.getLocalizedMessage());
            System.exit(0);
        }

        try {
            JavaGameThingClient.networkSocketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        } catch (ClosedChannelException e) {
            System.out.printf("ClosedChannelException upon registering a selector: %s\n", e.getLocalizedMessage());
            System.exit(0);
        }

        while (!done) {

            try {
                int num = selector.select(8000);
                if (num == 0) {
                    System.out.println("Things Are Locked UP!!!");
                }
            } catch (Exception e) {
                System.out.printf("Exception upon selecting on a selector: %s\n", e.getLocalizedMessage());
                System.exit(0);
            }

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> it = selectedKeys.iterator();

            //System.out.println(selectedKeys.size());
            while (it.hasNext()) {
                SelectionKey key = it.next();

                if (key.isReadable()) {
                    //System.out.println("Readable");
                    try {
                        ByteBuffer buf = ByteBuffer.allocate(256);
                        int bytesRead = JavaGameThingClient.networkSocketChannel.read(buf);
                        if (bytesRead == -1) {
                            char c = 1;
                            try {
                                JavaGameThingClient.threadLock.lock();
                                inMessageQueue.add(String.valueOf(c));
                            } finally {
                                JavaGameThingClient.threadLock.unlock();
                                //System.out.println("Queue Size: " + JavaGameThingClient.messageQueue.size());                    
                                done = true;
                            }
                        }

                        //REMEMBER: at the end of a string incoming is a null byte followed by the outputed newline or return char!!!!!!!  Will probally cause issues and compatibility issues
                        String s = new String(buf.array(), "UTF-8").substring(0, bytesRead);
                        boolean messageIncomplete = false;
                        System.out.println("Received Input: " + s);
                        if (s.length() == 256) {
                            if (s.charAt(255) != 10) {
                                //System.out.println("Char at 255: " + s.charAt(255));
                                //System.out.println("MESSAGE INCOMPLETE!!!!");
                                messageIncomplete = true;
                            }
                        }
                        if (s.charAt(s.length()-1) != '\n')
                        {
                            messageIncomplete = true;
                        }

                        s = inStringCache.concat(s);
                        inStringCache = "";

                        String[] splitStrings = s.split("\n");

                        for (int i = 0; i < splitStrings.length; i++) {
                            //System.out.println("SplitStringContent " + i + " : " + splitStrings[i]);
                            if (messageIncomplete && i == splitStrings.length - 1) {
                                inStringCache = splitStrings[i];//No substringing here since this shouldnt have a null at the end
                                //System.out.println("Cached message part: " + inStringCache);
                            } else {
                                String message = splitStrings[i].trim();

                                try {
                                    JavaGameThingClient.threadLock.lock();
                                    inMessageQueue.add(message);
                                } finally {
                                    JavaGameThingClient.threadLock.unlock();
                                }
                            }
                        }
                    } catch (Exception e) {

                    }

                }

                if (!outMessageQueue.isEmpty() && key.isWritable()) {
                    //System.out.println("Writable");
                    while (!outMessageQueue.isEmpty()) {
                        //System.out.println("Trying OUTPUT");
                        String outMessage = "";
                        try {
                            JavaGameThingClient.threadLock.lock();
                            outMessage = outMessageQueue.remove(0);
                        } finally {
                            JavaGameThingClient.threadLock.unlock();
                        }

                        if (!outMessage.equals("")) {
                            //System.out.println("Testing out: " + outMessage);
                            ByteBuffer buf = ByteBuffer.allocate(outMessage.length());
                            buf.clear();
                            buf.put(outMessage.getBytes());
                            buf.flip();
                            //System.out.println("Testing out: " + buf.hasRemaining());
                            try {
                                System.out.println("Writing Out: " + new String(buf.array(), "UTF-8").trim());
                                while (buf.hasRemaining()) {
                                    JavaGameThingClient.networkSocketChannel.write(buf);
                                }
                            } catch (IOException e) {

                            }
                        }
                    }
                    //System.out.println("Finished OUTPUT");
                }

                it.remove();
            }
        }

        System.out.println("THREAD EXITED!");

    }
}
