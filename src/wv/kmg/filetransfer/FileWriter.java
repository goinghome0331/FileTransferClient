package wv.kmg.filetransfer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class FileWriter extends Thread{
	public static final int BUFFER_SIZE = 1024*8;
	File file;
	byte[] buffer;
	int size;
	int acc = 0;
	int percent = 0;
	public int getPercent() {
		return percent;
	}
	public void setPercent(int percent) {
		this.percent = percent;
	}
	public FileWriter(File file) {
		this.file = file;
	}
	public void setSize(int size) {
		this.size = size;
	}
	public int getSize() {
		return this.size;
	}
	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}
	public byte[] getBuffer() {
		return this.buffer;
	}
	public int getAcc() {
		return acc;
	}
	@Override
	public void run() {
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(this.file));
			while(true) {
				synchronized (this) {
					try {
						wait();	
						if(this.size == -1) break;
						this.acc += this.size;
						bos.write(this.buffer,0,this.size);
					}catch (Exception e) {
						e.printStackTrace();
					}
					notify();
				}
			}
			bos.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	

}
