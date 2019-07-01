import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Server {
    public static void main(String[] args) throws IOException {
        
		ServerSocket serverSocket;
		Socket clientSocket;
		Scanner in;
		PrintWriter out;
		int PORT=7000;
		serverSocket=new ServerSocket(PORT);
        
		while(true){
			System.out.println("Waiting For Client Connection on PORT: "+PORT);
			clientSocket=serverSocket.accept();
			System.out.println("Connected");
			in=new Scanner(clientSocket.getInputStream());
			String data=in.nextLine();
			if(data!=null){
				System.out.println(data);
			}
		}
		
	}
}