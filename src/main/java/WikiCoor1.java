import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

@SuppressWarnings("serial")
public class WikiCoor1 extends HttpServlet {
	private Context androidContext;
	private PrintWriter out;
	
	private String Result;
	private Handler mHandler;
	public 	Handler iHandler;
	private Thread hthread;
    private static final int OUTPUT = 0xFFF;
    
    private Thread threadWiki1;
	private String Wiki1_Lat1H;
	private String Wiki1_Lat1M;
	private String Wiki1_Lat1S;
	private String Wiki1_Lng1H;
	private String Wiki1_Lng1M;
	private String Wiki1_Lng1S;
	private String Wiki1_Lat2H;
	private String Wiki1_Lat2M;
	private String Wiki1_Lat2S;
	private String Wiki1_Lng2H;
	private String Wiki1_Lng2M;
	private String Wiki1_Lng2S;
	//private String Wiki1_Lat;
	private IntentBroadcastReceiver IBReceiver_Wiki1;
	private static final int WA_Wiki1_SUCCEED = 0x101;
	private static final int WA_Wiki1_FAILED = 0x10F;
	
	private Thread threadToLat1MDec;
	private String Lat1MDec;	
    private static final int AR_ToLat1MDec_SUCCEED = 0x201;
    private static final int AR_ToLat1MDec_FAILED = 0x20F;
	
	private Thread threadToLng1MDec;
	private String Lng1MDec;	
    private static final int AR_ToLng1MDec_SUCCEED = 0x301;
    private static final int AR_ToLng1MDec_FAILED = 0x30F;
    
    private Thread threadToLat1SDec;
	private String Lat1SDec;	
    private static final int AR_ToLat1SDec_SUCCEED = 0x401;
    private static final int AR_ToLat1SDec_FAILED = 0x40F;
	
	private Thread threadToLng1SDec;
	private String Lng1SDec;	
    private static final int AR_ToLng1SDec_SUCCEED = 0x501;
    private static final int AR_ToLng1SDec_FAILED = 0x50F;
    
    private Thread threadSumLat1;
    private String SumLat1;
    private static final int AR_SumLat1_SUCCEED = 0x601;
    private static final int AR_SumLat1_FAILED = 0x60F;
    
    private Thread threadSumLng1;
    private String SumLng1;
    private static final int AR_SumLng1_SUCCEED = 0x701;
    private static final int AR_SumLng1_FAILED = 0x70F;
    
	private Thread threadToLat2MDec;
	private String Lat2MDec;	
    private static final int AR_ToLat2MDec_SUCCEED = 0x801;
    private static final int AR_ToLat2MDec_FAILED = 0x80F;
	
	private Thread threadToLng2MDec;
	private String Lng2MDec;	
    private static final int AR_ToLng2MDec_SUCCEED = 0x901;
    private static final int AR_ToLng2MDec_FAILED = 0x90F;
    
    private Thread threadToLat2SDec;
	private String Lat2SDec;	
    private static final int AR_ToLat2SDec_SUCCEED = 0xA01;
    private static final int AR_ToLat2SDec_FAILED = 0xA0F;
	
	private Thread threadToLng2SDec;
	private String Lng2SDec;	
    private static final int AR_ToLng2SDec_SUCCEED = 0xB01;
    private static final int AR_ToLng2SDec_FAILED = 0xB0F;
    
    private Thread threadSumLat2;
    private String SumLat2;
    private static final int AR_SumLat2_SUCCEED = 0xC01;
    private static final int AR_SumLat2_FAILED = 0xC0F;
    
    private Thread threadSumLng2;
    private String SumLng2;
    private static final int AR_SumLng2_SUCCEED = 0xD01;
    private static final int AR_SumLng2_FAILED = 0xD0F;
    
    private Thread threadGPSDistance;
    private String GPSDistance_Distance;
    private static final int AR_GPSDistance_SUCCEED = 0xE01;
    private static final int AR_GPSDistance_FAILED = 0xE0F;
    
    private static final int SYNCHRONIZE_1 = 0xFF1;
    private boolean Synchronize_1_ToLat1MDec = false;
    private boolean Synchronize_1_ToLat1SDec = false;
    
    private static final int SYNCHRONIZE_2 = 0xFF2;
    private boolean Synchronize_2_ToLat2MDec = false;
    private boolean Synchronize_2_ToLat2SDec = false;
    
    private static final int SYNCHRONIZE_3 = 0xFF3;
    private boolean Synchronize_3_ToLng1MDec = false;
    private boolean Synchronize_3_ToLng1SDec = false;
    
    private static final int SYNCHRONIZE_4 = 0xFF4;
    private boolean Synchronize_4_ToLng2MDec = false;
    private boolean Synchronize_4_ToLng2SDec = false;
    
    private static final int SYNCHRONIZE_5 = 0xFF5;
    private boolean Synchronize_5_SumLat1 = false;
    private boolean Synchronize_5_SumLng1 = false;
    private boolean Synchronize_5_SumLat2 = false;
    private boolean Synchronize_5_SumLng2 = false;
    
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		androidContext = (android.content.Context) config.getServletContext().getAttribute("org.mortbay.ijetty.context");
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException {
		doGet(request, response);
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		System.out.println("START_TIME:"+getTimeStamp());
		ServerThreadMonitor stm = ServerThreadMonitor.getInstance();
		if(!stm.isFree) waitserver();
		
		response.setContentType("text/html; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		out = response.getWriter();
		
		
		hthread = new Thread() {
			public void run() {
				Looper.prepare();
				mHandler = new Handler() {
					@Override 
					public void handleMessage(Message msg){
						ThreadMonitor tm = ThreadMonitor.getInstance();
						JsonOutputBuilder job = new JsonOutputBuilder();
						Bundle tempBundle;
						String[] tempArray;
						switch (msg.what) {
						case WA_Wiki1_SUCCEED:
							stopThread(threadWiki1);
							
							prepareToLat1MDec();
							prepareToLat1SDec();
							prepareToLat2MDec();
							prepareToLat2SDec();
							
							prepareToLng1MDec();
							prepareToLng1SDec();
							prepareToLng2MDec();
							prepareToLng2SDec();
							//prepareAmazonCA();
							//threadAmazonCA.run();
							System.out.println("T1_TIME:"+getTimeStamp());
							threadToLat1MDec.run();
							threadToLat1SDec.run();
							threadToLat2MDec.run();
							threadToLat2SDec.run();
							
							threadToLng1MDec.run();
							threadToLng1SDec.run();
							threadToLng2MDec.run();
							threadToLng2SDec.run();
							
							//mHandler.sendEmptyMessage(OUTPUT);

							break;
						case WA_Wiki1_FAILED:
							stopThread(threadWiki1);

							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}
							break;
						case AR_ToLat1MDec_SUCCEED:
							stopThread(threadToLat1MDec);
							debug("ToLat1MDec finished");
							Synchronize_1_ToLat1MDec = true;
							
							if(Synchronize_1_ToLat1MDec&&Synchronize_1_ToLat1SDec){
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_1);
			        			break;
							}else
								debug("ToLat1MDec finished, other=not yet");
							break;
						case AR_ToLat1SDec_SUCCEED:
							stopThread(threadToLat1SDec);
							debug("ToLat1SDec finished");
							Synchronize_1_ToLat1SDec = true;
							
							if(Synchronize_1_ToLat1MDec&&Synchronize_1_ToLat1SDec){
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_1);
								break;
							}else
								debug("ToLat1SDec finished, other=not yet");
							break;
						case SYNCHRONIZE_1:
							debug("SYNCHRONIZE_1");
							Synchronize_1_ToLat1MDec = false;
							Synchronize_1_ToLat1SDec = false;
							
							prepareSumLat1();
							threadSumLat1.run();
							
							break;
						case AR_ToLat2MDec_SUCCEED:
							stopThread(threadToLat2MDec);
							debug("ToLat2MDec finished");
							Synchronize_2_ToLat2MDec = true;
							
							if(Synchronize_2_ToLat2MDec&&Synchronize_2_ToLat2SDec){
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_2);
			        			break;
							}else
								debug("ToLat2MDec finished, other=not yet");
							break;
						case AR_ToLat2SDec_SUCCEED:
							stopThread(threadToLat2SDec);
							debug("ToLat2SDec finished");
							Synchronize_2_ToLat2SDec = true;
							
							if(Synchronize_2_ToLat2MDec&&Synchronize_2_ToLat2SDec){
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_2);
			        			break;
							}else
								debug("ToLat2SDec finished, other=not yet");
							break;
						
						case SYNCHRONIZE_2:
							debug("SYNCHRONIZE_2");
							Synchronize_2_ToLat2MDec = false;
							Synchronize_2_ToLat2SDec = false;
							
							prepareSumLat2();
							threadSumLat2.run();
							
							break;
						/***/
						case AR_ToLng1MDec_SUCCEED:
							stopThread(threadToLng1MDec);
							debug("ToLng1MDec finished");
							Synchronize_3_ToLng1MDec = true;
							
							if(Synchronize_3_ToLng1MDec&&Synchronize_3_ToLng1SDec){
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_3);
			        			break;
							}else
								debug("ToLng1MDec finished, other=not yet");
							break;
						case AR_ToLng1SDec_SUCCEED:
							stopThread(threadToLng1SDec);
							debug("ToLng1SDec finished");
							Synchronize_3_ToLng1SDec = true;
							
							if(Synchronize_3_ToLng1MDec&&Synchronize_3_ToLng1SDec){
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_3);
			        			break;
							}else
								debug("ToLng1SDec finished, other=not yet");
							break;
						case SYNCHRONIZE_3:
							debug("SYNCHRONIZE_3");
							Synchronize_3_ToLng1MDec = false;
							Synchronize_3_ToLng1SDec = false;
							
							prepareSumLng1();
							threadSumLng1.run();
							
							break;
						case AR_ToLng2MDec_SUCCEED:
							stopThread(threadToLng2MDec);
							debug("ToLng2MDec finished");
							Synchronize_4_ToLng2MDec = true;
							
							if(Synchronize_4_ToLng2MDec&&Synchronize_4_ToLng2SDec){
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_4);
			        			break;
							}else
								debug("ToLng2MDec finished, other=not yet");
							break;
						case AR_ToLng2SDec_SUCCEED:
							stopThread(threadToLng2SDec);
							debug("ToLng2SDec finished");
							Synchronize_4_ToLng2SDec = true;
							
							if(Synchronize_4_ToLng2MDec&&Synchronize_4_ToLng2SDec){
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_4);
			        			break;
							}else
								debug("ToLng2SDec finished, other=not yet");
							break;
						
						case SYNCHRONIZE_4:
							debug("SYNCHRONIZE_4");
							Synchronize_4_ToLng2MDec = false;
							Synchronize_4_ToLng2SDec = false;
							
							prepareSumLng2();
							threadSumLng2.run();
							
							break;
						case AR_SumLat1_SUCCEED:
							stopThread(threadSumLat1);
							debug("SumLat1 finished");
							Synchronize_5_SumLat1 = true;
							if(Synchronize_5_SumLat1&&Synchronize_5_SumLng1&&Synchronize_5_SumLat2&&Synchronize_5_SumLng2){
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_5);
			        			break;
							}else
								debug("SumLat1 finished, other=not yet");
							
							break;
						case AR_SumLat2_SUCCEED:
							stopThread(threadSumLat2);
							debug("SumLat2 finished");
							Synchronize_5_SumLat2 = true;
							if(Synchronize_5_SumLat1&&Synchronize_5_SumLng1&&Synchronize_5_SumLat2&&Synchronize_5_SumLng2){
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_5);
			        			break;
							}else
								debug("SumLat2 finished, other=not yet");
							
							break;
						case AR_SumLng1_SUCCEED:
							stopThread(threadSumLng1);
							debug("SumLng1 finished");
							Synchronize_5_SumLng1 = true;
							if(Synchronize_5_SumLat1&&Synchronize_5_SumLng1&&Synchronize_5_SumLat2&&Synchronize_5_SumLng2){
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_5);
			        			break;
							}else
								debug("SumLng1 finished, other=not yet");
							
							break;
						case AR_SumLng2_SUCCEED:
							stopThread(threadSumLng2);
							debug("SumLng2 finished");
							Synchronize_5_SumLng2 = true;
							if(Synchronize_5_SumLat1&&Synchronize_5_SumLng1&&Synchronize_5_SumLat2&&Synchronize_5_SumLng2){
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_5);
			        			break;
							}else
								debug("SumLng2 finished, other=not yet");
							
							break;
						/***/
						
						case SYNCHRONIZE_5:
							debug("SYNCHRONIZE_5");
							Synchronize_5_SumLat1 = false;
							Synchronize_5_SumLng1 = false;
							Synchronize_5_SumLat2 = false;
							Synchronize_5_SumLng2 = false;
							
							prepareGPSDistance();
							threadGPSDistance.run();
							break;
						
						case AR_GPSDistance_SUCCEED:
							stopThread(threadGPSDistance);
							
							mHandler.sendEmptyMessage(OUTPUT);
							
							break;
							
						case OUTPUT:
							tempBundle = new Bundle();
							tempBundle.putString("Lat1H", Wiki1_Lat1H);
							tempBundle.putString("Lat1M", Wiki1_Lat1M);
							tempBundle.putString("Lat1S", Wiki1_Lat1S);
							tempBundle.putString("Lng1H", Wiki1_Lng1H);
							tempBundle.putString("Lng1M", Wiki1_Lng1M);
							tempBundle.putString("Lng1S", Wiki1_Lng1S);
							tempBundle.putString("Lat2H", Wiki1_Lat2H);
							tempBundle.putString("Lat2M", Wiki1_Lat2M);
							tempBundle.putString("Lat2S", Wiki1_Lat2S);
							tempBundle.putString("Lng2H", Wiki1_Lng2H);
							tempBundle.putString("Lng2M", Wiki1_Lng2M);
							tempBundle.putString("Lng2S", Wiki1_Lng2S);
							tempBundle.putString("Lat1", SumLat1);
							tempBundle.putString("Lng1", SumLng1);
							tempBundle.putString("Lat2", SumLat2);
							tempBundle.putString("Lng2", SumLng2);
							tempBundle.putString("GPSDistance", GPSDistance_Distance);
							
							job.setBundle(tempBundle);
		                	job.setXML(getOutputXML());
		                	Result = job.getJSON();
							
		                	stopThread(hthread);
							synchronized (tm){debug("tm.notified();");tm.notify();}
							break;
						default:
							super.handleMessage(msg);
						}
					}
				};
				iHandler = new Handler();
				prepareWiki1();
				threadWiki1.start();
				Looper.loop();
			}
		};
		
		stm.isFree = false;
		hthread.start();
		waitfinal();
		out.print(Result);
		
		synchronized (stm){stm.isFree=true;debug("stm.notified();"); stm.notify();}
		System.out.println("FINISH_TIME:"+getTimeStamp());
		//System.out.println(Integer.parseInt("01"));
	}
	
	public void prepareToLat1MDec(){
		threadToLat1MDec = new Thread(new Runnable() {
			public void run() {
				Lat1MDec = ARFunction.mintoDec(Wiki1_Lat1M);
				mHandler.sendEmptyMessage(AR_ToLat1MDec_SUCCEED);
			}
		});
	}
	
	public void prepareToLng1MDec(){
		threadToLng1MDec = new Thread(new Runnable() {
			public void run() {
				Lng1MDec = ARFunction.mintoDec(Wiki1_Lng1M);
				mHandler.sendEmptyMessage(AR_ToLng1MDec_SUCCEED);
			}
		});
	}
	
	public void prepareToLat1SDec(){
		threadToLat1SDec = new Thread(new Runnable() {
			public void run() {
				Lat1SDec = ARFunction.sectoDec(Wiki1_Lat1S);
				mHandler.sendEmptyMessage(AR_ToLat1SDec_SUCCEED);
			}
		});
	}
	
	public void prepareToLng1SDec(){
		threadToLng1SDec = new Thread(new Runnable() {
			public void run() {
				Lng1SDec = ARFunction.sectoDec(Wiki1_Lng1S);
				mHandler.sendEmptyMessage(AR_ToLng1SDec_SUCCEED);
			}
		});
	}
	
	public void prepareSumLat1(){
		threadSumLat1 = new Thread(new Runnable() {
			public void run() {
				SumLat1 = ARFunction.sum(Wiki1_Lat1H,Lat1MDec,Lat1SDec);
				mHandler.sendEmptyMessage(AR_SumLat1_SUCCEED);
			}
		});
	}
	
	public void prepareSumLng1(){
		threadSumLng1 = new Thread(new Runnable() {
			public void run() {
				SumLng1 = ARFunction.sum(Wiki1_Lng1H,Lng1MDec,Lng1SDec);
				mHandler.sendEmptyMessage(AR_SumLng1_SUCCEED);
			}
		});
	}
	
	public void prepareToLat2MDec(){
		threadToLat2MDec = new Thread(new Runnable() {
			public void run() {
				Lat2MDec = ARFunction.mintoDec(Wiki1_Lat2M);
				mHandler.sendEmptyMessage(AR_ToLat2MDec_SUCCEED);
			}
		});
	}
	
	public void prepareToLng2MDec(){
		threadToLng2MDec = new Thread(new Runnable() {
			public void run() {
				Lng2MDec = ARFunction.mintoDec(Wiki1_Lng2M);
				mHandler.sendEmptyMessage(AR_ToLng2MDec_SUCCEED);
			}
		});
	}
	
	public void prepareToLat2SDec(){
		threadToLat2SDec = new Thread(new Runnable() {
			public void run() {
				Lat2SDec = ARFunction.sectoDec(Wiki1_Lat2S);
				mHandler.sendEmptyMessage(AR_ToLat2SDec_SUCCEED);
			}
		});
	}
	
	public void prepareToLng2SDec(){
		threadToLng2SDec = new Thread(new Runnable() {
			public void run() {
				Lng2SDec = ARFunction.sectoDec(Wiki1_Lng2S);
				mHandler.sendEmptyMessage(AR_ToLng2SDec_SUCCEED);
			}
		});
	}
	
	public void prepareSumLat2(){
		threadSumLat2 = new Thread(new Runnable() {
			public void run() {
				SumLat2 = ARFunction.sum(Wiki1_Lat2H,Lat2MDec,Lat2SDec);
				mHandler.sendEmptyMessage(AR_SumLat2_SUCCEED);
			}
		});
	}
	
	public void prepareSumLng2(){
		threadSumLng2 = new Thread(new Runnable() {
			public void run() {
				SumLng2 = ARFunction.sum(Wiki1_Lng2H,Lng2MDec,Lng2SDec);
				mHandler.sendEmptyMessage(AR_SumLng2_SUCCEED);
			}
		});
	}
	
	public void prepareGPSDistance(){
		threadGPSDistance = new Thread(new Runnable() {
			public void run() {
				GPSDistance_Distance = ARFunction.gpsdistance(SumLat1,SumLng1,SumLat2,SumLng2);
				mHandler.sendEmptyMessage(AR_GPSDistance_SUCCEED);
			}
		});
	}
	
	
	
	public void prepareWiki1(){
		threadWiki1 = new Thread(new Runnable() {
			public void run() {
				String URL = "http://en.m.wikipedia.org/w/index.php?title=Wikipedia:WikiProject_Geographical_coordinates&mobileaction=view_normal_site";
				String[] scripts = new String[1];

				
				scripts[0] = 
					"var Lat1 = new Array();"+
					"var Lng1 = new Array();"+
					"var Lat1H = new Array();"+
					"var Lat1M = new Array();"+
					"var Lat1S = new Array();"+
					"var Lng1H = new Array();"+
					"var Lng1M = new Array();"+
					"var Lng1S = new Array();"+
					"var tagArray1 = document.getElementsByTagName('SPAN');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=138&&i<156&&tagArray1[i].className=='geo-dms'){"+
				    "        parentElement = tagArray1[i];"+
				    "        break;"+
				    "    }"+
				    "}"+
				    "if(parentElement==undefined)"+
				    "    window.prach.setfinishstate('false');"+
				    "/*case 5: single parent&child, single child-tag*/"+
				    "var tagArray2 = parentElement.getElementsByTagName('SPAN');"+
				    "var childElement;"+
				    "for(var i=0;i<tagArray2.length;i++){"+
				    "    if(tagArray2[i].className=='latitude'){"+
				    "        childElement = tagArray2[i];"+
				    "        Lat1.push(childElement.innerHTML);"+
				    "    }"+
				    "}"+
				    "for(var i=0;i<tagArray2.length;i++){"+
				    "    if(tagArray2[i].className=='longitude'){"+
				    "        childElement = tagArray2[i];"+
				    "        Lng1.push(childElement.innerHTML);"+
				    "    }"+
				    "}"+
				    "Lat1H.push(Lat1[0].split('‹')[0]);"+
				    "Lat1M.push(Lat1[0].split('‹')[1].split('Œ')[0]);"+
				    "Lat1S.push(Lat1[0].split('‹')[1].split('Œ')[1].split('')[0]);"+
				    "window.prach.addOutputArray(Lat1H,'Lat1H');"+
				    "window.prach.addOutputArray(Lat1M,'Lat1M');"+
				    "window.prach.addOutputArray(Lat1S,'Lat1S');\n"+
				    "Lng1H.push(Lng1[0].split('‹')[0]);"+
				    "Lng1M.push(Lng1[0].split('‹')[1].split('Œ')[0]);"+
				    "Lng1S.push(Lng1[0].split('‹')[1].split('Œ')[1].split('')[0]);"+
				    "window.prach.addOutputArray(Lng1H,'Lng1H');"+
				    "window.prach.addOutputArray(Lng1M,'Lng1M');"+
				    "window.prach.addOutputArray(Lng1S,'Lng1S');"+
				    "window.prach.info(Lat1[0]);"+
				    "window.prach.info(Lng1[0]);"+
				    
				    "var Lat2 = new Array();"+
					"var Lng2 = new Array();"+
					"var Lat2H = new Array();"+
					"var Lat2M = new Array();"+
					"var Lat2S = new Array();"+
					"var Lng2H = new Array();"+
					"var Lng2M = new Array();"+
					"var Lng2S = new Array();"+
					"var tagArray1 = document.getElementsByTagName('SPAN');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=160&&i<180&&tagArray1[i].className=='geo-dms'){"+
				    "        parentElement = tagArray1[i];"+
				    "        break;"+
				    "    }"+
				    "}"+
				    "if(parentElement==undefined)"+
				    "    window.prach.setfinishstate('false');"+
				    "/*case 5: single parent&child, single child-tag*/"+
				    "var tagArray2 = parentElement.getElementsByTagName('SPAN');"+
				    "var childElement;"+
				    "for(var i=0;i<tagArray2.length;i++){"+
				    "    if(tagArray2[i].className=='latitude'){"+
				    "        childElement = tagArray2[i];"+
				    "        Lat2.push(childElement.innerHTML);"+
				    "    }"+
				    "}"+
				    "for(var i=0;i<tagArray2.length;i++){"+
				    "    if(tagArray2[i].className=='longitude'){"+
				    "        childElement = tagArray2[i];"+
				    "        Lng2.push(childElement.innerHTML);"+
				    "    }"+
				    "}"+
				    "Lat2H.push(Lat2[0].split('‹')[0]);"+
				    "Lat2M.push(Lat2[0].split('‹')[1].split('Œ')[0]);"+
				    "Lat2S.push('0');"+
				    "window.prach.addOutputArray(Lat2H,'Lat2H');"+
				    "window.prach.addOutputArray(Lat2M,'Lat2M');"+
				    "window.prach.addOutputArray(Lat2S,'Lat2S');\n"+
				    "Lng2H.push(Lng2[0].split('‹')[0]);"+
				    "Lng2M.push(Lng2[0].split('‹')[1].split('Œ')[0]);"+
				    "Lng2S.push('0');"+
				    "window.prach.addOutputArray(Lng2H,'Lng2H');"+
				    "window.prach.addOutputArray(Lng2M,'Lng2M');"+
				    "window.prach.addOutputArray(Lng2S,'Lng2S');"+
				    "window.prach.info(Lat2[0]);"+
				    "window.prach.info(Lng2[0]);"+
				    "window.prach.setfinishstate('true');";

				IBReceiver_Wiki1 = new IntentBroadcastReceiver();
				Intent intent = new Intent("com.prach.mashup.SMA");
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

				String[] msg = {"com.prach.mashup.WAExtractor",
						"RESULTS:OUTPUTS", //0
						"RESULTS:NAMES", //1
						"EXTRA:MODE","EXTRACTION",
						"EXTRA:URL",URL,
						"EXTRAS:SCRIPTS",Function.genExtrasString(scripts)};
				IBReceiver_Wiki1.ResultArrayNameVector.add("OUTPUTS"); //0
				IBReceiver_Wiki1.ResultArrayNameVector.add("NAMES"); //1
				intent.putExtra("MSG", msg);

				IntentFilter IFfinished = new IntentFilter("com.prach.mashup.FINISHED");
				androidContext.registerReceiver(IBReceiver_Wiki1,IFfinished,null,null);
				IBReceiver_Wiki1.ProcessNumber = 0x001;
				IBReceiver_Wiki1.handler = iHandler;
				IBReceiver_Wiki1.finish = iFinished_Wiki1;

				debug("getTitle()->call intent:com.prach.mashup.SMA");
				for (int i = 0; i < msg.length; i++)
					debug("getTitle()->msg["+i+"]:"+msg[i]);

				androidContext.startActivity(intent);
			}
		});
	}
	
	public Runnable iFinished_Wiki1 = new Runnable() {
		public void run(){
			if(IBReceiver_Wiki1.ProcessNumber == 0x001){
				stopThread(threadWiki1);
				androidContext.unregisterReceiver(IBReceiver_Wiki1);

				int count_resultname = IBReceiver_Wiki1.ResultNameVector.size();
				int count_resultarrayname = IBReceiver_Wiki1.ResultArrayNameVector.size();
				int allcount = count_resultarrayname + count_resultname;

				debug("iFinished.run(0x001)->count_resultname:"+count_resultname);
				debug("iFinished.run(0x001)->count_resultarrayname:"+count_resultarrayname);
				debug("iFinished.run(0x001)->allcount:"+allcount);

				String[][] resultstrings = null;

				if(IBReceiver_Wiki1.ResultStringVector.get(0).equals("RESULT_OK")){
					debug("result OK");
					resultstrings = new String[allcount][];

					for (int i = 0; i < count_resultname; i++){
						resultstrings[i] = new String[1];
						resultstrings[i][0] = IBReceiver_Wiki1.ResultStringVector.get(i+1);
					}
					for (int i = count_resultname; i < allcount; i++) {
						resultstrings[i] = IBReceiver_Wiki1.ResultStringArrayVector.get(i-count_resultname);
					}

					Wiki1_Lat1H = Function.getStringByName(
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("NAMES")], 
					"Lat1H");
					Wiki1_Lat1M = Function.getStringByName(
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("NAMES")], 
					"Lat1M");
					Wiki1_Lat1S = Function.getStringByName(
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("NAMES")], 
					"Lat1S");
					Wiki1_Lng1H = Function.getStringByName(
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("NAMES")], 
					"Lng1H");
					Wiki1_Lng1M = Function.getStringByName(
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("NAMES")], 
					"Lng1M");
					Wiki1_Lng1S = Function.getStringByName(
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("NAMES")], 
					"Lng1S");
					
					
					Wiki1_Lat2H = Function.getStringByName(
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("NAMES")], 
					"Lat2H");
					Wiki1_Lat2M = Function.getStringByName(
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("NAMES")], 
					"Lat2M");
					Wiki1_Lat2S = Function.getStringByName(
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("NAMES")], 
					"Lat2S");
					Wiki1_Lng2H = Function.getStringByName(
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("NAMES")], 
					"Lng2H");
					Wiki1_Lng2M = Function.getStringByName(
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("NAMES")], 
					"Lng2M");
					Wiki1_Lng2S = Function.getStringByName(
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Wiki1.getArrayIndexfromName("NAMES")], 
					"Lng2S");
					mHandler.sendEmptyMessage(WA_Wiki1_SUCCEED);
					
				}else if(IBReceiver_Wiki1.ResultStringVector.get(0).equals("RESULT_CANCELED")){
					debug("result CXL");
					resultstrings = new String[allcount][];
					for (int i = 0; i < count_resultname; i++){
						resultstrings[i] = new String[1];
						resultstrings[i][0] = "null";
					}
					for (int i = count_resultname; i < allcount; i++) {
						resultstrings[i] = new String[1];
						resultstrings[i][0] = "null";
					}
					mHandler.sendEmptyMessage(WA_Wiki1_FAILED);

				}else 
					mHandler.sendEmptyMessage(WA_Wiki1_FAILED);
			}
		}
	};
	
	public synchronized void stopThread(Thread t) {
		if (t != null) {
			Thread moribund = t;
			t = null;
			moribund.interrupt();
		}
	}
	
	public void debug(String msg){
		Log.d("WikiCoor1",msg);
	}
	
	public void waitfinal(){
		ThreadMonitor tm = ThreadMonitor.getInstance();
		synchronized (tm) {
			try {
				tm.wait();
			} catch (InterruptedException e) {
				debug("waitfinal()->error="+e.toString());
			}
		}
	}
	
	public void waitserver(){
		ServerThreadMonitor stm = ServerThreadMonitor.getInstance();
		synchronized (stm) {
			try {
				stm.wait();
			} catch (InterruptedException e) {
				debug("waitserver()->error="+e.toString());
			}
		}
	}
	
	public String getTimeStamp(){
		Calendar c = Calendar.getInstance();
		
        int hours = c.get(Calendar.HOUR);
        int minutes = c.get(Calendar.MINUTE);
        int seconds = c.get(Calendar.SECOND);
        int mseconds = c.get(Calendar.MILLISECOND);
        return  DateFormat.getDateInstance().format(new Date())+" "+hours + ":"+minutes + ":"+ seconds+"."+mseconds;
	}
	
	public String getOutputXML(){
		return 
		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
		"<object>\n"+
		"	<name>ResultStatus</name>\n"+
		"	<value>succeed</value>\n"+
		"	<name>Lat1H</name>\n"+
		"	<value>Wikipedia1.output.Lat1H</value>\n"+
		"	<name>Lat1M</name>\n"+
		"	<value>Wikipedia1.output.Lat1M</value>\n"+
		"	<name>Lat1S</name>\n"+
		"	<value>Wikipedia1.output.Lat1S</value>\n"+
		"	<name>Lng1H</name>\n"+
		"	<value>Wikipedia1.output.Lng1H</value>\n"+
		"	<name>Lng1M</name>\n"+
		"	<value>Wikipedia1.output.Lng1M</value>\n"+
		"	<name>Lng1S</name>\n"+
		"	<value>Wikipedia1.output.Lng1S</value>\n"+
		"	<name>Lat2H</name>\n"+
		"	<value>Wikipedia1.output.Lat2H</value>\n"+
		"	<name>Lat2M</name>\n"+
		"	<value>Wikipedia1.output.Lat2M</value>\n"+
		"	<name>Lat2S</name>\n"+
		"	<value>Wikipedia1.output.Lat2S</value>\n"+
		"	<name>Lng2H</name>\n"+
		"	<value>Wikipedia1.output.Lng2H</value>\n"+
		"	<name>Lng2M</name>\n"+
		"	<value>Wikipedia1.output.Lng2M</value>\n"+
		"	<name>Lng2S</name>\n"+
		"	<value>Wikipedia1.output.Lng2S</value>\n"+
		"	<name>Lat1</name>\n"+
		"	<value>SumLat1.output.Lat1</value>\n"+
		"	<name>Lng1</name>\n"+
		"	<value>SumLng1.output.Lng1</value>\n"+
		"	<name>Lat2</name>\n"+
		"	<value>SumLat2.output.Lat2</value>\n"+
		"	<name>Lng2</name>\n"+
		"	<value>SumLng2.output.Lng2</value>\n"+
		"	<name>GPSDistance</name>\n"+
		"	<value>GPSDistance.output.GPSDistance</value>\n"+
		//"	<name>Lng1</name>\n"+
		//"	<value>Wikipedia1.output.Lng1</value>\n"+
		/*"	<name>PriceUS</name>\n"+
		"	<value>AmazonPriceUS.output.PRICE_US</value>\n"+
		"	<name>PriceUSJP</name>\n"+
		"	<value>ExchangeRateUSJP.output.PRICE_USJP</value>\n"+
		"	<name>TitleCA</name>\n"+
		"	<value>AmazonPriceCA.output.TITLE_CA</value>\n"+
		"	<name>PriceCA</name>\n"+
		"	<value>AmazonPriceCA.output.PRICE_CA</value>\n"+
		"	<name>PriceCAJP</name>\n"+
		"	<value>ExchangeRateCAJP.output.PRICE_CAJP</value>\n"+
		"	<name>TitleJP</name>\n"+
		"	<value>AmazonPriceJP.output.TITLE_JP</value>\n"+
		"	<name>PriceJP</name>\n"+
		"	<value>AmazonPriceJP.output.PRICE_JP</value>\n"+*/
		"</object>";
	}

	public String getErrorXML(){
		return 
		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
		"<error>\n"+
		"	<name>ResultStatus</name>\n"+
		"	<value>failed</value>\n"+
		"</error>";
	}
}
