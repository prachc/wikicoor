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
import android.content.IntentFilter;
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
public class BookAdd extends HttpServlet {
	private Context androidContext;
	private PrintWriter out;
	
	private String Result;
	private Handler mHandler;
	private Thread hthread;
    private static final int OUTPUT = 0xFFF;
	
	private WSComponentParser ExchangeRateWSCParser;
	
	private String external_isbn;
	
	private Thread threadAmazon;
	private String Amazon_ProductTitle;
	private String Amazon_ProductPrice;
	public 	Handler iHandler;
	private IntentBroadcastReceiver IBReceiver;
	private static final int WA_Amazon_SUCCEED = 0x101;
	private static final int WA_Amazon_FAILED = 0x10F;
	
	private Thread threadExchangeRate;
	private Parcel replyExchangeRate;
	private String ExchangeRate_YenPrice;
	private static final int WS_ExchangeRate_SUCCEED = 0x201;
	private static final int WS_ExchangeRate_FAILED = 0x20F;
	
	private Thread threadBookShoppingAdd;
	private Parcel replyBookShoppingAdd;
	private String BookShoppingAdd_Status;	
	private static final int MS_BookShoppingAdd_SUCCEED = 0x301;
	private static final int MS_BookShoppingAdd_FAILED = 0x30F;


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
		
		external_isbn = request.getParameter("isbn"); //?isbn=
		
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
						case WA_Amazon_SUCCEED:
							Amazon_ProductPrice = filterProductPrice(Amazon_ProductPrice);
							
							tempBundle = new Bundle();
							tempBundle.putString("ProductPrice", Amazon_ProductPrice);
							ExchangeRateWSCParser = new WSComponentParser();
							ExchangeRateWSCParser.setBundle(tempBundle);
							ExchangeRateWSCParser.setXML(getExchangeRateXML());
							
							prepareExchangeRate();
							threadExchangeRate.run();

							break;
						case WA_Amazon_FAILED:
							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}

							break;
						case WS_ExchangeRate_SUCCEED:
							stopThread(threadExchangeRate);
							
		                	tempBundle = replyExchangeRate.readBundle();
		                	tempArray = tempBundle.getStringArray("YenPrice");
		                	ExchangeRate_YenPrice = tempArray[0];
		                	
		                	prepareBookShoppingAdd();
		                	threadBookShoppingAdd.run();
		                	
		                	break;
						case WS_ExchangeRate_FAILED:
							stopThread(threadExchangeRate);

							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}
							break;
						case MS_BookShoppingAdd_SUCCEED:
							stopThread(threadBookShoppingAdd);
							
							tempBundle = replyBookShoppingAdd.readBundle();
							BookShoppingAdd_Status = tempBundle.getString("STATUS");
							
							mHandler.sendEmptyMessage(OUTPUT);
		                	
							break;
						case MS_BookShoppingAdd_FAILED:
							stopThread(threadBookShoppingAdd);

							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}
							break;
						case OUTPUT:
							tempBundle = new Bundle();
							tempBundle.putString("STATUS", BookShoppingAdd_Status);
							
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
				prepareAmazon();
				threadAmazon.start();
				Looper.loop();
			}
		};
		
		stm.isFree = false;
		hthread.start();
		waitfinal();
		out.print(Result);
		
		synchronized (stm){stm.isFree=true;debug("stm.notified();"); stm.notify();}
	}

	public void prepareAmazon(){
		threadAmazon = new Thread(new Runnable() {
			public void run() {
				String URL = "http://www.amazon.com/?ie=UTF8&force-full-site=1";
				String[] scripts = new String[2];
				scripts[0] = 
					"prach = new Object;\n"+
					"prach.input = '"+external_isbn+"';\n"+
					"var tagArray1 = document.getElementsByTagName('table');\n"+
					"var parentElement;\n"+
					"for(var i=0;i<tagArray1.length;i++){\n"+
					"    if(i>4&&i<12&&tagArray1[i].id=='subDropdownTable'){\n"+
					"        parentElement = tagArray1[i];\n"+
					"        break;\n"+
					"    }\n"+
					"}\n"+
					"var tagArray2 = parentElement.getElementsByTagName('input');\n"+
					"var childElement;\n"+
					"for(var i=0;i<tagArray2.length;i++)\n"+
					"    if(i==0&&tagArray2[i].id=='twotabsearchtextbox'&&tagArray2[i].name=='field-keywords'&&tagArray2[i].className=='searchSelect')\n"+
					"        childElement = tagArray2[i];\n"+
					"childElement.focus();\n"+
					"childElement.value=prach.input;\n"+
					"childElement.form.submit();";
				scripts[1] = 
					"var tagArray1 = document.getElementsByTagName('div');\n"+
					"var parentElement;\n"+
					"for(var i=0;i<tagArray1.length;i++){\n"+
					"    if(i>38&&i<55&&tagArray1[i].className=='title'){\n"+
					"        parentElement = tagArray1[i];\n"+
					"        break;\n"+
					"    }\n"+
					"}\n"+
					"var tagArray2 = parentElement.getElementsByTagName('a');\n"+
					"var childElement;\n"+
					"for(var i=0;i<tagArray2.length;i++)\n"+
					"    if(i==0&&tagArray2[i].className=='title')\n"+
					"        childElement = tagArray2[i];\n"+
					"var ProductTitle = childElement.textContent;"+
					"window.prach.addOutput(ProductTitle,'ProductTitle');" +

					"var ProductPrice = new Array();"+
					"var tagArray1 = document.getElementsByTagName('div');"+
					"var parentElement;"+
					"for(var i=0;i<tagArray1.length;i++){"+
					"    if(i>=40&&i<60&&tagArray1[i].className=='newPrice'){"+
					"        parentElement = tagArray1[i];"+
					"        break;"+
					"    }"+
					"}"+
					"if(parentElement==undefined)"+
					"    window.prach.setfinishstate('false');"+
					"/*case 5: single parent&child, single child-tag*/"+
					"var tagArray2 = parentElement.getElementsByTagName('span');"+
					"var childElement;"+
					"for(var i=0;i<tagArray2.length;i++){"+
					"    if(i==0&&tagArray2[i].className=='price'){"+
					"        childElement = tagArray2[i];"+
					"        ProductPrice.push(childElement.textContent);"+
					"    }"+
					"}"+
					"window.prach.addOutputArray(ProductPrice,'ProductPrice');"+
					"window.prach.setfinishstate('true');";

				IBReceiver = new IntentBroadcastReceiver();
				Intent intent = new Intent("com.prach.mashup.SMA");
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

				String[] msg = {"com.prach.mashup.WAExtractor",
						"RESULTS:OUTPUTS", //0
						"RESULTS:NAMES", //1
						"EXTRA:MODE","EXTRACTION",
						"EXTRA:URL",URL,
						"EXTRAS:SCRIPTS",Function.genExtrasString(scripts)};
				IBReceiver.ResultArrayNameVector.add("OUTPUTS"); //0
				IBReceiver.ResultArrayNameVector.add("NAMES"); //1
				intent.putExtra("MSG", msg);

				IntentFilter IFfinished = new IntentFilter("com.prach.mashup.FINISHED");
				androidContext.registerReceiver(IBReceiver,IFfinished,null,null);
				IBReceiver.ProcessNumber = 0x001;
				IBReceiver.handler = iHandler;
				IBReceiver.finish = iFinished;

				debug("getTitle()->call intent:com.prach.mashup.SMA");
				for (int i = 0; i < msg.length; i++)
					debug("getTitle()->msg["+i+"]:"+msg[i]);

				androidContext.startActivity(intent);
			}
		});
	}

	public void prepareExchangeRate(){
		threadExchangeRate = new Thread(new Runnable() {
			public void run() {
				replyExchangeRate = Parcel.obtain();
				Intent i = new Intent("com.prach.mashup.WSCService");
				boolean isConnected = androidContext.bindService(i,new ServiceConnection(){
					final int serviceCode = 0x101;
					public void onServiceConnected(ComponentName cname,IBinder service) {
						debug("ExchangeRate WS Service connected: "+ cname.flattenToShortString());

						Parcel data = Parcel.obtain();
						Bundle bundle = new Bundle();

						bundle.putString("BASE", ExchangeRateWSCParser.base);
						bundle.putStringArray("PATHS",ExchangeRateWSCParser.paths);
						bundle.putStringArray("KEYS", ExchangeRateWSCParser.keys);
						bundle.putStringArray("VALUES",ExchangeRateWSCParser.values);
						bundle.putString("FORMAT", ExchangeRateWSCParser.format);
						
						bundle.putStringArray("NAME", ExchangeRateWSCParser.name);
						bundle.putStringArray("TYPE", ExchangeRateWSCParser.type);
						bundle.putStringArray("QUERY", ExchangeRateWSCParser.query);
						bundle.putStringArray("INDEX", ExchangeRateWSCParser.index);
						
						data.writeBundle(bundle);
						boolean res = false;
						try {
							res = service.transact(serviceCode, data,replyExchangeRate, 0);
						} catch (RemoteException ex) {
							Log.e("onServiceConnected",
									"Remote exception when calling service",ex);
							res = false;
						}
						if (res)
							mHandler.sendEmptyMessage(WS_ExchangeRate_SUCCEED);
						else
							mHandler.sendEmptyMessage(WS_ExchangeRate_FAILED);
					}

					public void onServiceDisconnected(ComponentName name) {
						debug("ExchangeRate WS Service disconnected: "+ name.flattenToShortString());

					}
				}, Context.BIND_AUTO_CREATE);

				if (!isConnected) {
					debug("ExchangeRate WS Service could not be connected ");
					mHandler.sendEmptyMessage(MS_BookShoppingAdd_FAILED);
				}
			}
		});
	}

	public void prepareBookShoppingAdd(){
		threadBookShoppingAdd = new Thread(new Runnable() {
			public void run() {
				replyBookShoppingAdd = Parcel.obtain();
				Intent i = new Intent("com.prach.mashup.BookDatabaseService");
				boolean isConnected = androidContext.bindService(i,new ServiceConnection(){
					final int serviceCode = 0x66686601;
					public void onServiceConnected(ComponentName name,IBinder service) {
						debug("BookShoppingAdd Service connected: "+ name.flattenToShortString());

						Parcel data = Parcel.obtain();
						Bundle bundle = new Bundle();

						String command = "ADD";
						String title = Amazon_ProductTitle;
						String isbn = external_isbn;
						String price = ExchangeRate_YenPrice;

						bundle.putString("COMMAND",command);
						bundle.putString("TITLE",title);
						bundle.putString("ISBN",isbn);
						bundle.putString("PRICE",price);

						data.writeBundle(bundle);
						boolean res = false;
						try {
							res = service.transact(serviceCode, data,replyBookShoppingAdd, 0);
						} catch (RemoteException ex) {
							debug("BookShoppingSummary Service Remote exception when calling service:"+ex.toString());
							res = false;
						}

						if (res)
							mHandler.sendEmptyMessage(MS_BookShoppingAdd_SUCCEED);
						else
							mHandler.sendEmptyMessage(MS_BookShoppingAdd_FAILED);
					}
					public void onServiceDisconnected(ComponentName name) {
						debug("BookShoppingSummary Service disconnected: "+ name.flattenToShortString());		
					}
				}, Context.BIND_AUTO_CREATE);

				if (!isConnected) {
					debug("BookShoppingSummary Service could not be connected ");
					mHandler.sendEmptyMessage(MS_BookShoppingAdd_FAILED);
				}
			}
		});

	}

	public Runnable iFinished = new Runnable() {
		public void run(){
			if(IBReceiver.ProcessNumber == 0x001){
				stopThread(threadAmazon);
				androidContext.unregisterReceiver(IBReceiver);

				int count_resultname = IBReceiver.ResultNameVector.size();
				int count_resultarrayname = IBReceiver.ResultArrayNameVector.size();
				int allcount = count_resultarrayname + count_resultname;

				debug("iFinished.run(0x001)->count_resultname:"+count_resultname);
				debug("iFinished.run(0x001)->count_resultarrayname:"+count_resultarrayname);
				debug("iFinished.run(0x001)->allcount:"+allcount);

				String[][] resultstrings = null;

				if(IBReceiver.ResultStringVector.get(0).equals("RESULT_OK")){
					debug("result OK");
					resultstrings = new String[allcount][];

					for (int i = 0; i < count_resultname; i++){
						resultstrings[i] = new String[1];
						resultstrings[i][0] = IBReceiver.ResultStringVector.get(i+1);
					}
					for (int i = count_resultname; i < allcount; i++) {
						resultstrings[i] = IBReceiver.ResultStringArrayVector.get(i-count_resultname);
					}

					Amazon_ProductPrice = Function.getStringByName(
							resultstrings[IBReceiver.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver.getArrayIndexfromName("NAMES")], 
					"ProductPrice");
					Amazon_ProductTitle = Function.getStringByName(
							resultstrings[IBReceiver.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver.getArrayIndexfromName("NAMES")], 
					"ProductTitle");
					mHandler.sendEmptyMessage(WA_Amazon_SUCCEED);
				}else if(IBReceiver.ResultStringVector.get(0).equals("RESULT_CANCELED")){
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
					mHandler.sendEmptyMessage(WA_Amazon_FAILED);

				}else 
					mHandler.sendEmptyMessage(WA_Amazon_FAILED);
			}
		}
	};

	public void debug(String msg){
		Log.d("BookSummary",msg);
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

	public String filterProductPrice(String price){
		return price.replaceAll("[^0-9.]", "");
	}

	public String getOutputXML(){
		return 
		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
		"<object>\n"+
		"	<name>ResultStatus</name>\n"+
		"	<value>BookDatabase.output.STATUS</value>\n"+
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
	
	public String getExchangeRateXML(){
		return 
		"<component>\n"+
		"	<name>ExchageRate</name>\n"+
		"	<role>\n"+
		"		<medium>\n"+
		"			<subscriber-id>001</subscriber-id>\n"+
		"			<publisher-id>002</publisher-id>\n"+
		"		</medium>\n"+
		"	</role>\n"+
		"	<execution>single</execution>\n"+
		"	<webservice>\n"+
		"		<base>http://www.exchangerate-api.com/</base>\n"+
		"		<paths>\n"+
		"			<path>usd</path>\n"+
		"			<path>jpy</path>\n"+
		"			<path>publisher.results.ProductPrice</path>\n"+
		"		</paths>\n"+
		"		<keys>\n"+
		"			<key>k</key>\n"+
		"		</keys>\n"+
		"		<values>\n"+
		"			<value>PQkn3-quzTZ-PNDav</value>\n"+
		"		</values>\n"+
		"		<format>SELF</format>\n"+
		"		<results>\n"+
		"			<result>\n"+
		"				<result-name>YenPrice</result-name>\n"+
		"				<type>single</type>\n"+
		"				<query>null</query>\n"+
		"				<index>null</index>\n"+
		"				<filter>null</filter>\n"+
		"			</result>\n"+
		"		</results>\n"+
		"	</webservice>\n"+
		"</component>";
	}
}
