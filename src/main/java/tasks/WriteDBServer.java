package tasks;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class WriteDBServer implements Runnable{

	private ServerSocket serverSocket;
	private int port;

	WriteDBServer(int port){
		try{
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public WriteDBServer(){
		this(7000);
	}
	@Override
	public void run(){
		while(true) try {
			Socket clientSocket = serverSocket.accept();
			Thread temp = new Thread(new ServerWriteTask(clientSocket));
			temp.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}