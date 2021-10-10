package wv.kmg.filetransfer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.Queue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class Controller {
	private Socket s;
	private Socket ds;
	private String ip;
	private String pwd;
	private static Controller controller=  new Controller();;
	private ReceiveThread recv; 
	private Controller() {
		 s = new Socket();
		 ds = new Socket();
	}
	public boolean isConnected() {
		return s.isConnected();
	}
	public boolean isClosed() {
		return s.isClosed();
	}
	public void disconnected(){
		try {
		s.close();
		ds.close();
		this.recv.interrupt();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

	public ReceiveThread getReceiveThread(){
		return recv;
	}
//	public static synchronized Controller getInstance() {
//		if(controller == null) {
//			controller = new Controller();
//		}
//		return controller;
//	}
	
	public static Controller getInstance() {
		return controller;
	}
	public void connect(String address,int port) {
		if(this.s.isClosed()) {
			s = new Socket();
			ds = new Socket();
		}
		SocketAddress sd = new InetSocketAddress(address, port);
		SocketAddress sd1 = new InetSocketAddress(address, port+1);
		try {
			this.s.connect(sd);
			this.ds.connect(sd1);
			setIp(this.s.getInetAddress().toString());
			this.recv = new ReceiveThread(this.ds);
			this.recv.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public void send(JsonObject jo) {
		OutputStreamWriter output = null;
		BufferedReader br = null;
		try {
			output = new OutputStreamWriter(this.s.getOutputStream(),"UTF-8");
			br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			PrintWriter writer = new PrintWriter(output, true);
			writer.println(jo.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		 
	}
	public void requestFile(JsonObject jo) {
		OutputStreamWriter output = null;
		BufferedReader br = null;
		try {
			output = new OutputStreamWriter(this.ds.getOutputStream(),"UTF-8");
			br = new BufferedReader(new InputStreamReader(ds.getInputStream(),"UTF-8"));
			PrintWriter writer = new PrintWriter(output, true);
			writer.println(jo.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		 
	}
	public JsonObject recv() {
		BufferedReader br;
		try {
			br = new BufferedReader(new InputStreamReader(s.getInputStream(),"UTF-8"));
			
			Gson gson = new Gson();
			
			String returnMessage = br.readLine();
			return gson.fromJson(returnMessage, JsonObject.class);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			JsonObject ret = new JsonObject();
			ret.addProperty("error", "client error");
			return ret;
		} 
		 
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getPwd() {
		return pwd;
	}
	public void setPwd(String pwd) {
		this.pwd = pwd;
	}
	public Socket getDataSocket() {
		return this.ds;
	}
}
