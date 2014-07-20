import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

@SuppressWarnings("serial")
public class BookSummary extends HttpServlet {
	private Context androidContext;
	private PrintWriter out;
	
	private String 	Result;
	private Handler mHandler;
	private Thread 	mainthread;
    private static final int OUTPUT = 0xFFF;
	
	private Thread threadBookShoppingSummary;
	private Parcel 	replyBookShoppingSummary;
	private String[] BookShoppingSummary_Title;
	private String[] BookShoppingSummary_Price;
	private String[] BookShoppingSummary_ISBN;
	private static final int MS_BookShoppingSummary_SUCCEED = 0x101;
    private static final int MS_BookShoppingSummary_FAILED = 0x10F;
	
	private Thread threadTotalCalculator;
	private String TotalCalculator_TotalPrice;	
    private static final int AR_TotalCalculator_SUCCEED = 0x201;
    private static final int AR_TotalCalculator_FAILED = 0x20F;
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		androidContext = (android.content.Context) config.getServletContext().getAttribute("org.mortbay.ijetty.context");
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException {
		doGet(request, response);
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		ServerThreadMonitor stm = ServerThreadMonitor.getInstance();
		if(!stm.isFree) waitserver();
		
		response.setContentType("text/html; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		out = response.getWriter();
				
		mainthread = new Thread() {
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
		                case MS_BookShoppingSummary_SUCCEED:
		                	stopThread(threadBookShoppingSummary);
		                	
		                	tempBundle = replyBookShoppingSummary.readBundle();
		                	BookShoppingSummary_Title = tempBundle.getStringArray("TITLE");
		                	BookShoppingSummary_ISBN = tempBundle.getStringArray("ISBN");
		                	BookShoppingSummary_Price = tempBundle.getStringArray("PRICE");
		                	
		                	prepareTotalCalculator();
		                	threadTotalCalculator.run();
		                	
		                	break;
		                case MS_BookShoppingSummary_FAILED:
		                	stopThread(threadBookShoppingSummary);
		                	
		                	job.setErrorXML(getErrorXML());
		                   	Result = job.getErrorJSON();
		                	
		                	stopThread(mainthread);
		            		synchronized (tm){tm.notify();}

		            		break;
		                case AR_TotalCalculator_SUCCEED:
		                	stopThread(threadTotalCalculator);
		                	
		                	mHandler.sendEmptyMessage(OUTPUT);
		                	
		                	break;
		                case AR_TotalCalculator_FAILED:
		                	stopThread(threadTotalCalculator);
		                	
		                	job.setErrorXML(getErrorXML());
		                   	Result = job.getErrorJSON();
		                	
		                	stopThread(mainthread);
		            		synchronized (tm){tm.notify();}

		                	break;
		                case OUTPUT:
		                	tempBundle = new Bundle();
		                	tempBundle.putStringArray("TITLE", BookShoppingSummary_Title);
		                	tempBundle.putStringArray("ISBN", BookShoppingSummary_ISBN);
		                	tempBundle.putStringArray("PRICE", BookShoppingSummary_Price);
		                	tempBundle.putString("TOTAL", TotalCalculator_TotalPrice);
		                	
		                	job.setBundle(tempBundle);
		                	job.setXML(getOutputXML());
		                	Result = job.getJSON();
		                	
		            		stopThread(mainthread);
		            		synchronized (tm){debug("tm.notified();");tm.notify();}
		                	
		                	break;
		                default:
		                    super.handleMessage(msg);
			            }
			        }
			    };
				prepareBookShoppingSummary();
				threadBookShoppingSummary.start();
				Looper.loop();
			}
		};
		
		stm.isFree = false;
		mainthread.start();
		waitfinal();
		out.print(Result);
		 
		synchronized (stm){stm.isFree=true;debug("stm.notified();"); stm.notify();}
	}
	
	public void prepareBookShoppingSummary(){
		threadBookShoppingSummary = new Thread(new Runnable() {
			public void run() {
				replyBookShoppingSummary = Parcel.obtain();
				Intent i = new Intent("com.prach.mashup.BookDatabaseService");
				boolean isConnected = androidContext.bindService(i,new ServiceConnection(){
					final int serviceCode = 0x66686601;
					public void onServiceConnected(ComponentName name,IBinder service) {
						debug("BookShoppingSummary Service connected: "+ name.flattenToShortString());
		
						Parcel data = Parcel.obtain();
						Bundle bundle = new Bundle();
		
						String command = "SUM";
						bundle.putString("COMMAND",command);
		
						data.writeBundle(bundle);
						boolean res = false;
						try {
							res = service.transact(serviceCode, data,replyBookShoppingSummary, 0);
						} catch (RemoteException ex) {
							debug("BookShoppingSummary Service Remote exception when calling service:"+ex.toString());
							res = false;
						}
						
						if (res)
							mHandler.sendEmptyMessage(MS_BookShoppingSummary_SUCCEED);
						else
							mHandler.sendEmptyMessage(MS_BookShoppingSummary_FAILED);
					}
					public void onServiceDisconnected(ComponentName name) {
						debug("BookShoppingSummary Service disconnected: "+ name.flattenToShortString());		
					}
				}, Context.BIND_AUTO_CREATE);
		
				if (!isConnected) {
					debug("BookShoppingSummary Service could not be connected ");
					mHandler.sendEmptyMessage(MS_BookShoppingSummary_FAILED);
				}
			}
		});
		
	}
	
	public void prepareTotalCalculator(){
		threadTotalCalculator = new Thread(new Runnable() {
			public void run() {
				TotalCalculator_TotalPrice = ARFunction.summation(BookShoppingSummary_Price);
				mHandler.sendEmptyMessage(AR_TotalCalculator_SUCCEED);
			}
		});
	}
	
	public void debug(String msg){
		Log.d("BookShoppingSummary",msg);
	}
	
	public synchronized void stopThread(Thread t) {
		if (t != null) {
			Thread moribund = t;
			t = null;
			moribund.interrupt();
		}
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
    
    public String getOutputXML(){
    	return 
    	"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
		"<object>\n"+
		"	<name>ResultStatus</name>\n"+
		"	<value>Succeed</value>\n"+
		"	<name>title</name>\n"+
		"	<array>\n"+
		"		<loop>\n"+
		"			<value>BookDatabase.output.TITLE</value>\n"+
		"		</loop>\n"+
		"	</array>\n"+
		"	<name>isbn</name>\n"+
		"	<array>\n"+
		"		<loop>\n"+
		"			<value>BookDatabase.output.ISBN</value>\n"+
		"		</loop>\n"+
		"	</array>\n"+
		"	<name>price</name>\n"+
		"	<array>\n"+
		"		<loop>\n"+
		"			<value>BookDatabase.output.PRICE</value>\n"+
		"		</loop>\n"+
		"	</array>\n"+
		"	<name>total</name>\n"+
		"	<value>BookDatabase.output.TOTAL</value>\n"+
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
    
    public String getXML2(){
    	return 
    	"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
    	"<object>\n"+
    	"	<name>ResultStatus</name>\n"+
    	"	<value>BookDatabase.output.STATUS</value>\n"+
    	"	<name>Data</name>\n"+
    	"	<array>\n"+
    	"		<loop>\n"+
    	"			<object>\n"+
    	"				<name>title</name>\n"+
    	"				<value>BookDatabase.output.TITLE</value>\n"+
    	"				<name>isbn</name>\n"+
    	"				<value>BookDatabase.output.ISBN</value>\n"+
    	"				<name>price</name>\n"+
    	"				<value>BookDatabase.output.PRICE</value>\n"+
    	"			</object>\n"+
    	"		</loop>\n"+
    	"	</array>\n"+
    	"	<name>total</name>\n"+
    	"	<value>BookDatabase.output.TOTAL</value>\n"+
    	"</object>";
    }
}
