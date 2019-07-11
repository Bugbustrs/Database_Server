package tasks;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ReadDBServer implements Runnable {


    private ServerSocket serverSocket;
    private int port;

    public ReadDBServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ReadDBServer() {
        this(7001);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                Thread temp = new Thread(new ServerReadTask(clientSocket));
                temp.start();
                System.out.println("Started new thread");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
