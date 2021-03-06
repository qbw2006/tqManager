package com.candy.ssh;

import java.io.IOException;
import java.io.InputStream;

import javax.websocket.Session;

import com.candy.utils.TqLog;

/**
 * 创建日期:2018年1月11日<br/>
 * 创建时间:下午10:15:48<br/>
 * 创建者    :yellowcong<br/>
 * 机能概要:用于读取ssh输出的
 */
public class SshWriteThread implements Runnable{
	
	//定义一个flag, 来停止线程用
    private boolean isStop = false;

    //接入输入流数据
    private InputStream in; 

    //用于输出数据
    private Session session;

    //停止线程
    public void stopThread() {
        this.isStop = true;
    }

    public SshWriteThread(InputStream in, Session session) {
        this.in = in;
        this.session = session;
    }

    @Override
    public void run() {
        try {
            //读取数据
            while (!isStop  && 
                    session != null &&
                    session.isOpen()) {
                //写数据到客户端
                writeToWeb(in);
            }
        } catch (Exception e) {
        	TqLog.getErrorLog().error("", e);
        }

    }

    /**
     * 写数据到web控制界面
     * @param in
     */
    private void writeToWeb(InputStream in) {

        try {
            //定义一个缓存
            //一个UDP 的用户数据报的数据字段长度为8192字节
            byte [] buff = new byte[8192];  

            int len =0;
            StringBuffer sb = new StringBuffer();
            while((len = in.read(buff)) > 0) {
                //设定从0 开始
                sb.setLength(0);

                //读取数组里面的数据，进行补码
                for (int i = 0; i < len; i++){
                    //进行补码操作
                    char c = (char) (buff[i] & 0xff);
                    sb.append(c);
                }
                //写数据到服务器端
                session.getBasicRemote().sendText(new String(sb.toString().getBytes("ISO-8859-1"),"UTF-8"));
            }
        } catch (IOException e) {
        	TqLog.getErrorLog().error("", e);
        }
    }
}
