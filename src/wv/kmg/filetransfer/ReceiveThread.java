package wv.kmg.filetransfer;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ReceiveThread extends Thread {
	
	Socket s;
	Map<String, FileWriter> m;
	boolean start;
	private Queue<JsonObject> q;
	
	public ReceiveThread(Socket s) {
		this.s = s;
		m = new HashMap<String, FileWriter>();
		start = false;
		q = new LinkedList<JsonObject>();
	}
	public boolean isEnd() {
		return m.size() == 0 ? true : false;
	}
	
	public Queue<JsonObject> getQ() {
		return q;
	}
	public boolean isStart() {
		return start;
	}
	public void setStart(boolean start) {
		this.start = start;
	}
	@Override
	public void run() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(),"UTF-8"));
			Gson gson = new Gson();
			
			while (true) {
				String input = br.readLine();
				
				
				JsonObject ret = gson.fromJson(input, JsonObject.class);
				FileWriter fw = null;
				String name = ret.get("name").getAsString();
				File f = new File(Client.DEFAULT_DOWN_PATH,name);

				if ((fw = m.get(f.getAbsolutePath())) == null) {

					fw = new FileWriter(f);
					m.put(f.getAbsolutePath(), fw);
					fw.start();
					start = true;
				} 
				
				fw.setSize(ret.get("result").getAsJsonObject().get("length").getAsInt());
				
				fw.setBuffer(Base64.getDecoder().decode(ret.get("result").getAsJsonObject().get("data").getAsString()));
				while (fw.getState() != Thread.State.WAITING) {

				}
				
				synchronized (fw) {
					try {

						fw.notify();

						fw.wait();

					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
					
				
//				Class<?> cls = Class.forName(this.getClass().getName());
//
//				Method m = cls.getMethod(jo.get("type").getAsString(), JsonObject.class);
//
//				JsonObject ret = (JsonObject) m.invoke(this, jo);
				// 데이터 기록 작성
				
				if(ret.get("result").getAsJsonObject().get("length").getAsInt() != -1) {
					JsonObject jo = new JsonObject();
					jo.addProperty("request", "data");
					jo.addProperty("down-path", Client.DEFAULT_DOWN_PATH);
					jo.addProperty("path", ret.get("path").getAsString());
					Controller.getInstance().requestFile(jo);

				}else {
					m.remove(f.getAbsolutePath());
					if (!q.isEmpty()) {
						JsonObject jo = q.poll();
						Controller.getInstance().requestFile(jo);
					}
				}
				int percent = (int)(((double)fw.getAcc())/ret.get("result").getAsJsonObject().get("total").getAsInt()*100);
				if(percent - fw.getPercent()  >= 1) {
					System.out.println(ret.get("path").getAsString()+"파일 "+percent+"% 다운로드 받음");
					fw.setPercent(percent);
				}
				if(m.size() == 0) {
					synchronized (this) {
						this.notify();
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
