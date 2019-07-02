package tasks;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable{

	private ServerSocket serverSocket;
	private int port;

	public Server(int port){
		try{
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Server(){
		this(7000);
	}
	@Override
	public void run(){
		while(true){
			try {
				Socket clientSocket=serverSocket.accept();
				Thread temp = new Thread(new ServerWriteTask(clientSocket));
				temp.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}