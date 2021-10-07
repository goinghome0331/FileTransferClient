package wv.kmg.filetransfer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class Client {
	public static final int DEFAULT_PORT = 11111;
	public static String DEFAULT_DOWN_PATH = "";
	private static int REQ_LIMIT = 20;
	boolean exit = true;

	public static void main(String args[]) {
		if (args.length == 2) {
			DEFAULT_DOWN_PATH = args[0];
			try {
				REQ_LIMIT = Integer.parseInt(args[1]);
			}catch(NumberFormatException e) {
				REQ_LIMIT = 20;
			}
		} else if (args.length == 1) {
			DEFAULT_DOWN_PATH = args[0];
		} else {
			DEFAULT_DOWN_PATH = System.getProperty("user.home");
		}
		new Client().start();

	}

	public void start() {
		InputStreamReader reader = new InputStreamReader(System.in);
		BufferedReader in = new BufferedReader(reader);

		while (exit) {
			try {
				if (Controller.getInstance().isConnected() && !Controller.getInstance().isClosed()) {
					System.out.print(
							"[" + Controller.getInstance().getIp() + "/" + Controller.getInstance().getPwd() + "]> ");
				} else {
					System.out.print("[none]> ");
				}
				String cmdStr = in.readLine();
				String[] cmd = cmdStr.split(" ");
				Object[] par = { cmd };
				Class<?> cls = Class.forName(this.getClass().getName());
				if (!Controller.getInstance().isConnected() && !cmd[0].equals("connect") && !cmd[0].equals("exit")) {
					System.out.println("please connect before");
					continue;
				}
				Method m = cls.getDeclaredMethod(cmd[0], String[].class);
				m.setAccessible(true);
				m.invoke(this, par);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void disconnect(String[] cmd) {
		Controller.getInstance().disconnected();
	}

	public void exit(String[] cmd) {
		if (Controller.getInstance().isConnected() && !Controller.getInstance().isClosed()) {
			Controller.getInstance().disconnected();
		}
		exit = false;
	}

	public void connect(String[] cmd) {
		int port = 0;
		if (cmd.length == 1) {
			System.out.println("please input ip");
			return;
		} else if (cmd.length == 2) {
			port = DEFAULT_PORT;
		} else {
			port = Integer.parseInt(cmd[2]);
		}
		Controller.getInstance().connect(cmd[1], port);
		System.out.println("connect " + cmd[1] + " complete");
		JsonObject jo = Controller.getInstance().recv();
		Controller.getInstance().setPwd(jo.get("path").getAsString());
	}

	public void path(String[] cmd) {
		if (cmd.length == 1) {
			System.out.println(DEFAULT_DOWN_PATH);
		} else {
			DEFAULT_DOWN_PATH = cmd[1];
		}
	}

	public void cd(String[] cmd) {
		JsonObject jo = new JsonObject();
		jo.addProperty("request", "cd");
		jo.addProperty("path", Controller.getInstance().getPwd());
		jo.addProperty("add", cmd[1]);
		Controller.getInstance().send(jo);
		JsonObject ret = Controller.getInstance().recv();
		if (ret.get("error") == null) {
			Controller.getInstance().setPwd(ret.get("path").getAsString());
		} else {
			System.out.println(ret.get("error").getAsString());
		}

	}

	public void pwd(String[] cmd) {
		System.out.println(Controller.getInstance().getPwd());
	}

	public void dir(String[] cmd) {
		ll(cmd);
	}

	private JsonArray fileList(String path) {
		JsonObject jo = new JsonObject();
		jo.addProperty("request", "fileList");
		jo.addProperty("path", path);

		Controller.getInstance().send(jo);
		JsonObject ret = Controller.getInstance().recv();
		JsonArray ja = ret.get("result").getAsJsonArray();
		return ja;
	}

	public void ll(String[] cmd) {
		JsonArray ja = fileList(Controller.getInstance().getPwd());
		int len = ja.size();
		for (int i = 0; i < len; i++) {
			JsonObject o = ja.get(i).getAsJsonObject();
			System.out.println(o.get("name").getAsString() + "\t" + o.get("size").getAsString() + "\t"
					+ o.get("ext").getAsString() + "\t" + o.get("lastModified").getAsString());
		}

	}

	public void get(String[] cmd) {
		JsonArray ja = fileList(Controller.getInstance().getPwd() + "\\" + cmd[1]);
		System.out.println(ja.toString());
		int len = ja.size();
		int ac = 0;
		for (int i = 0; i < len; i++) {
			JsonObject o = ja.get(i).getAsJsonObject();
			if (o.get("ext").getAsString().indexOf("폴더") == -1) {
				JsonObject jo = new JsonObject();
				jo.addProperty("request", "data");
				jo.addProperty("down-path", Client.DEFAULT_DOWN_PATH);
				jo.addProperty("path", Controller.getInstance().getPwd() + "\\" + o.get("name").getAsString());
				if (ac++ < REQ_LIMIT) {
					Controller.getInstance().requestFile(jo);
				} else {
					Controller.getInstance().getReceiveThread().getQ().offer(jo);
				}
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		while (!Controller.getInstance().getReceiveThread().isEnd()
				|| !Controller.getInstance().getReceiveThread().isStart()) {
		}
		Controller.getInstance().getReceiveThread().setStart(false);
	}
}
