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
public class AmazonPrice2 extends HttpServlet {
	private Context androidContext;
	private PrintWriter out;
	
	private String Result;
	private Handler mHandler;
	public 	Handler iHandler;
	private Thread hthread;
    private static final int OUTPUT = 0xFFF;
	
	private WSComponentParser ExchangeRateUSJPWSCParser;
	private WSComponentParser ExchangeRateCAJPWSCParser;
	
	private String external_isbn;
	
	private Thread threadAmazonUS;
	private String AmazonUS_ProductTitle;
	private String AmazonUS_ProductPrice;
	private IntentBroadcastReceiver IBReceiver_AmazonUS;
	private static final int WA_AmazonUS_SUCCEED = 0x101;
	private static final int WA_AmazonUS_FAILED = 0x10F;
	
	private Thread threadAmazonCA;
	private String AmazonCA_ProductTitle;
	private String AmazonCA_ProductPrice;
	//public 	Handler iHandler;
	private IntentBroadcastReceiver IBReceiver_AmazonCA;
	private static final int WA_AmazonCA_SUCCEED = 0x201;
	private static final int WA_AmazonCA_FAILED = 0x20F;
	
	private Thread threadAmazonJP;
	private String AmazonJP_ProductTitle;
	private String AmazonJP_ProductPrice;
	//public 	Handler iHandler;
	private IntentBroadcastReceiver IBReceiver_AmazonJP;
	private static final int WA_AmazonJP_SUCCEED = 0x301;
	private static final int WA_AmazonJP_FAILED = 0x30F;
	
	private Thread threadExchangeRateUSJP;
	private Parcel replyExchangeRateUSJP;
	private String ExchangeRateUSJP_YenPrice;
	private static final int WS_ExchangeRateUSJP_SUCCEED = 0x401;
	private static final int WS_ExchangeRateUSJP_FAILED = 0x40F;
	
	private Thread threadExchangeRateCAJP;
	private Parcel replyExchangeRateCAJP;
	private String ExchangeRateCAJP_YenPrice;
	private static final int WS_ExchangeRateCAJP_SUCCEED = 0x501;
	private static final int WS_ExchangeRateCAJP_FAILED = 0x50F;
	
	private static final int SYNCHRONIZE_1 = 0xFF1;
    private boolean Synchronize_1_ExchangeRateUSJP = false;
    private boolean Synchronize_1_ExchangeRateCAJP = false;
    private boolean Synchronize_1_AmazonJP = false;
	
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
		
		external_isbn = request.getParameter("isbn"); //?isbn=
		
		hthread = new Thread() {
			public void run() {
				Looper.prepare();
				mHandler = new Handler() {
					@Override 
					public void handleMessage(Message msg){
						ThreadMonitor tm = ThreadMonitor.getInstance();
						JsonOutputBuilder job = new JsonOutputBuilder();
						Bundle tempBundle1,tempBundle2;
						String[] tempArray;
						switch (msg.what) {
						case WA_AmazonUS_SUCCEED:
							stopThread(threadAmazonUS);
							AmazonUS_ProductPrice = filterProductPrice(AmazonUS_ProductPrice);
							
							prepareAmazonCA();
							threadAmazonCA.run();

							break;
						case WA_AmazonUS_FAILED:
							stopThread(threadAmazonUS);
							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}

							break;
						case WA_AmazonCA_SUCCEED:
							stopThread(threadAmazonCA);
							AmazonCA_ProductPrice = filterProductPrice(AmazonCA_ProductPrice);
							
							prepareAmazonJP();
							
							tempBundle1 = new Bundle();
							tempBundle1.putString("ProductPrice", AmazonUS_ProductPrice);
							ExchangeRateUSJPWSCParser = new WSComponentParser();
							ExchangeRateUSJPWSCParser.setBundle(tempBundle1);
							ExchangeRateUSJPWSCParser.setXML(getExchangeRateUSJPXML());
							prepareExchangeRateUSJP();
							
							tempBundle2 = new Bundle();
							tempBundle2.putString("ProductPrice", AmazonCA_ProductPrice);
							ExchangeRateCAJPWSCParser = new WSComponentParser();
							ExchangeRateCAJPWSCParser.setBundle(tempBundle2);
							ExchangeRateCAJPWSCParser.setXML(getExchangeRateCAJPXML());
		                	prepareExchangeRateCAJP();
							
		                	System.out.println("T1_TIME:"+getTimeStamp());
							threadAmazonJP.run();
							threadExchangeRateUSJP.run();
							threadExchangeRateCAJP.run();
							
							break;
						case WA_AmazonCA_FAILED:
							stopThread(threadAmazonCA);
							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}

							break;	
						case WA_AmazonJP_SUCCEED:
							stopThread(threadAmazonJP);
							AmazonJP_ProductPrice = filterProductPrice(AmazonJP_ProductPrice);
							
							Synchronize_1_AmazonJP = true;
							
							if(Synchronize_1_AmazonJP&&Synchronize_1_ExchangeRateUSJP&&Synchronize_1_ExchangeRateCAJP)
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_1);
							else
								debug("AmazonJP finished, other=not yet");
							break;
							
							
						case WA_AmazonJP_FAILED:
							stopThread(threadAmazonJP);
							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();
							
							stopThread(hthread);
							synchronized (tm){tm.notify();}

							break;
						case WS_ExchangeRateUSJP_SUCCEED:
							stopThread(threadExchangeRateUSJP);
							
		                	tempBundle1 = replyExchangeRateUSJP.readBundle();
		                	tempArray = tempBundle1.getStringArray("YenPrice");
		                	ExchangeRateUSJP_YenPrice = tempArray[0];
		                	
		                	System.out.println("T2_TIME:"+getTimeStamp());
		                	Synchronize_1_ExchangeRateUSJP = true;
							
							if(Synchronize_1_AmazonJP&&Synchronize_1_ExchangeRateUSJP&&Synchronize_1_ExchangeRateCAJP)
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_1);
							else
								debug("ExchangeRateUSJP finished, other=not yet");
							break;
						case WS_ExchangeRateUSJP_FAILED:
							stopThread(threadExchangeRateUSJP);

							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}
							break;
						case WS_ExchangeRateCAJP_SUCCEED:
							stopThread(threadExchangeRateCAJP);
							
							tempBundle1 = replyExchangeRateCAJP.readBundle();
		                	tempArray = tempBundle1.getStringArray("YenPrice");
		                	ExchangeRateCAJP_YenPrice = tempArray[0];
							
		                	System.out.println("T3_TIME:"+getTimeStamp());
							Synchronize_1_ExchangeRateCAJP = true;
							
							if(Synchronize_1_AmazonJP&&Synchronize_1_ExchangeRateUSJP&&Synchronize_1_ExchangeRateCAJP)
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_1);
							else
								debug("ExchangeRateCAJP finished, other=not yet");
							break;
		                	//prepareBookShoppingAdd();
		                	//threadBookShoppingAdd.run();
		                	//mHandler.sendEmptyMessage(OUTPUT);
		                	//break;
						case WS_ExchangeRateCAJP_FAILED:
							stopThread(threadExchangeRateCAJP);

							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}
							break;
						case SYNCHRONIZE_1:
							Synchronize_1_AmazonJP = false;
							Synchronize_1_ExchangeRateUSJP = false;
							Synchronize_1_ExchangeRateCAJP = false;
			        		
							System.out.println("T4_TIME:"+getTimeStamp());
							mHandler.sendEmptyMessage(OUTPUT);
		                	
		                   	break;
						case OUTPUT:
							tempBundle1 = new Bundle();
							tempBundle1.putString("TITLE_US", AmazonUS_ProductTitle);
							tempBundle1.putString("PRICE_US", AmazonUS_ProductPrice);
							tempBundle1.putString("PRICE_USJP", ExchangeRateUSJP_YenPrice);
							
							tempBundle1.putString("TITLE_CA", AmazonCA_ProductTitle);
							tempBundle1.putString("PRICE_CA", AmazonCA_ProductPrice);
							tempBundle1.putString("PRICE_CAJP", ExchangeRateCAJP_YenPrice);
							
							tempBundle1.putString("TITLE_JP", AmazonJP_ProductTitle);
							tempBundle1.putString("PRICE_JP", AmazonJP_ProductPrice);
							
							
							job.setBundle(tempBundle1);
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
				prepareAmazonUS();
				threadAmazonUS.start();
				Looper.loop();
			}
		};
		
		stm.isFree = false;
		hthread.start();
		waitfinal();
		out.print(Result);
		
		synchronized (stm){stm.isFree=true;debug("stm.notified();"); stm.notify();}
		System.out.println("FINISH_TIME:"+getTimeStamp());
	}
	
	public void prepareAmazonUS(){
		threadAmazonUS = new Thread(new Runnable() {
			public void run() {
				String URL = "http://www.amazon.com/?force-full-site=1";
				String[] scripts = new String[2];
				scripts[0] = 
				    "prach = new Object;\n"+
				    "prach.input = '"+external_isbn+"';\n"+
				    "var tagArray1 = document.getElementsByTagName('TD');\n"+
				    "var parentElement;\n"+
				    "for(var i=0;i<tagArray1.length;i++){\n"+
				    "    if(i>=17&&i<25&&tagArray1[i].id=='searchTextBoxHolder'){\n"+
				    "        parentElement = tagArray1[i];\n"+
				    "        break;\n"+
				    "    }\n"+
				    "}\n"+
				    "if(parentElement==undefined\n)"+
				    "    window.prach.setfinishstate('false');\n"+
				    "/*case 5: single parent&child, single child-tag*/\n"+
				    "var tagArray2 = parentElement.getElementsByTagName('INPUT');\n"+
				    "var childElement;\n"+
				    "for(var i=0;i<tagArray2.length;i++){\n"+
				    "    if(i==0&&tagArray2[i].id=='twotabsearchtextbox'&&tagArray2[i].name=='field-keywords'&&tagArray2[i].className=='searchSelect'){\n"+
				    "        childElement = tagArray2[i];\n"+
				    "    }\n"+
				    "}\n"+
				    "childElement.value=prach.input;\n"+
				    "childElement.form.submit();\n";

				scripts[1] = 
					"var ProductTitle = new Array();"+
				    "var tagArray1 = document.getElementsByTagName('div');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=38&&i<55&&tagArray1[i].className=='title'){"+
				    "        parentElement = tagArray1[i];"+
				    "        break;"+
				    "    }"+
				    "}"+
				    "if(parentElement==undefined)"+
				    "    window.prach.setfinishstate('false');"+
				    "/*case 5: single parent&child, single child-tag*/"+
				    "var tagArray2 = parentElement.getElementsByTagName('a');"+
				    "var childElement;"+
				    "for(var i=0;i<tagArray2.length;i++){"+
				    "    if(i==0&&tagArray2[i].className=='title'){"+
				    "        childElement = tagArray2[i];"+
				    "        ProductTitle.push(childElement.textContent);"+
				    "    }"+
				    "}"+
				    "window.prach.addOutputArray(ProductTitle,'ProductTitle');"+

				    "var ProductPrice = new Array();"+
				    "var tagArray1 = document.getElementsByTagName('span');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=23&&i<31&&tagArray1[i].className=='subPrice'){"+
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

				IBReceiver_AmazonUS = new IntentBroadcastReceiver();
				Intent intent = new Intent("com.prach.mashup.SMA");
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

				String[] msg = {"com.prach.mashup.WAExtractor",
						"RESULTS:OUTPUTS", //0
						"RESULTS:NAMES", //1
						"EXTRA:MODE","EXTRACTION",
						"EXTRA:URL",URL,
						"EXTRAS:SCRIPTS",Function.genExtrasString(scripts)};
				IBReceiver_AmazonUS.ResultArrayNameVector.add("OUTPUTS"); //0
				IBReceiver_AmazonUS.ResultArrayNameVector.add("NAMES"); //1
				intent.putExtra("MSG", msg);

				IntentFilter IFfinished = new IntentFilter("com.prach.mashup.FINISHED");
				androidContext.registerReceiver(IBReceiver_AmazonUS,IFfinished,null,null);
				IBReceiver_AmazonUS.ProcessNumber = 0x001;
				IBReceiver_AmazonUS.handler = iHandler;
				IBReceiver_AmazonUS.finish = iFinished_AmazonPriceUS;

				debug("getTitle()->call intent:com.prach.mashup.SMA");
				for (int i = 0; i < msg.length; i++)
					debug("getTitle()->msg["+i+"]:"+msg[i]);

				androidContext.startActivity(intent);
			}
		});
	}
	
	public Runnable iFinished_AmazonPriceUS = new Runnable() {
		public void run(){
			if(IBReceiver_AmazonUS.ProcessNumber == 0x001){
				stopThread(threadAmazonUS);
				androidContext.unregisterReceiver(IBReceiver_AmazonUS);

				int count_resultname = IBReceiver_AmazonUS.ResultNameVector.size();
				int count_resultarrayname = IBReceiver_AmazonUS.ResultArrayNameVector.size();
				int allcount = count_resultarrayname + count_resultname;

				debug("iFinished.run(0x001)->count_resultname:"+count_resultname);
				debug("iFinished.run(0x001)->count_resultarrayname:"+count_resultarrayname);
				debug("iFinished.run(0x001)->allcount:"+allcount);

				String[][] resultstrings = null;

				if(IBReceiver_AmazonUS.ResultStringVector.get(0).equals("RESULT_OK")){
					debug("result OK");
					resultstrings = new String[allcount][];

					for (int i = 0; i < count_resultname; i++){
						resultstrings[i] = new String[1];
						resultstrings[i][0] = IBReceiver_AmazonUS.ResultStringVector.get(i+1);
					}
					for (int i = count_resultname; i < allcount; i++) {
						resultstrings[i] = IBReceiver_AmazonUS.ResultStringArrayVector.get(i-count_resultname);
					}

					AmazonUS_ProductPrice = Function.getStringByName(
							resultstrings[IBReceiver_AmazonUS.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_AmazonUS.getArrayIndexfromName("NAMES")], 
					"ProductPrice");
					AmazonUS_ProductTitle = Function.getStringByName(
							resultstrings[IBReceiver_AmazonUS.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_AmazonUS.getArrayIndexfromName("NAMES")], 
					"ProductTitle");
					mHandler.sendEmptyMessage(WA_AmazonUS_SUCCEED);
				}else if(IBReceiver_AmazonUS.ResultStringVector.get(0).equals("RESULT_CANCELED")){
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
					mHandler.sendEmptyMessage(WA_AmazonUS_FAILED);

				}else 
					mHandler.sendEmptyMessage(WA_AmazonUS_FAILED);
			}
		}
	};
	
	public void prepareAmazonCA(){
		threadAmazonCA = new Thread(new Runnable() {
			public void run() {
				String URL = "http://www.amazon.ca/?force-full-site=1";
				String[] scripts = new String[2];
				scripts[0] = 
				    "prach = new Object;\n"+
				    "prach.input = '"+external_isbn+"';\n"+
				    "var tagArray1 = document.getElementsByTagName('TD');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=17&&i<25&&tagArray1[i].id=='searchTextBoxHolder'){"+
				    "        parentElement = tagArray1[i];"+
				    "        break;"+
				    "    }"+
				    "}"+
				    "if(parentElement==undefined)"+
				    "    window.prach.setfinishstate('false');"+
				    "/*case 5: single parent&child, single child-tag*/"+
				    "var tagArray2 = parentElement.getElementsByTagName('INPUT');"+
				    "var childElement;"+
				    "for(var i=0;i<tagArray2.length;i++){"+
				    "    if(i==0&&tagArray2[i].id=='twotabsearchtextbox'&&tagArray2[i].name=='field-keywords'&&tagArray2[i].className=='searchSelect'){"+
				    "        childElement = tagArray2[i];"+
				    "    }"+
				    "}"+
				    "childElement.value = prach.input;"+
				    "childElement.form.submit();";
				    

				scripts[1] = 			    
				    "var ProductTitle = new Array();"+
				    "var tagArray1 = document.getElementsByTagName('DIV');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=38&&i<58&&tagArray1[i].className=='title'){"+
				    "        parentElement = tagArray1[i];"+
				    "        break;"+
				    "    }"+
				    "}"+
				    "if(parentElement==undefined)"+
				    "    window.prach.setfinishstate('false');"+
				    "/*case 5: single parent&child, single child-tag*/"+
				    "var tagArray2 = parentElement.getElementsByTagName('A');"+
				    "var childElement;"+
				    "for(var i=0;i<tagArray2.length;i++){"+
				    "    if(i==0&&tagArray2[i].className=='title'){"+
				    "        childElement = tagArray2[i];"+
				    "        ProductTitle.push(childElement.textContent);"+
				    "    }"+
				    "}"+
				    "window.prach.addOutputArray(ProductTitle,'ProductTitle');"+
				    
				    "var ProductPrice = new Array();"+
				    "var tagArray1 = document.getElementsByTagName('DIV');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=41&&i<59&&tagArray1[i].className=='newPrice'){"+
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
				    "    if(i==0&&tagArray2[i].className=='price'){"+
				    "        childElement = tagArray2[i];"+
				    "        ProductPrice.push(childElement.textContent);"+
				    "    }"+
				    "}"+
				    "window.prach.addOutputArray(ProductPrice,'ProductPrice');"+
				    "window.prach.setfinishstate('true');";

				IBReceiver_AmazonCA = new IntentBroadcastReceiver();
				Intent intent = new Intent("com.prach.mashup.SMA");
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

				String[] msg = {"com.prach.mashup.WAExtractor",
						"RESULTS:OUTPUTS", //0
						"RESULTS:NAMES", //1
						"EXTRA:MODE","EXTRACTION",
						"EXTRA:URL",URL,
						"EXTRAS:SCRIPTS",Function.genExtrasString(scripts)};
				IBReceiver_AmazonCA.ResultArrayNameVector.add("OUTPUTS"); //0
				IBReceiver_AmazonCA.ResultArrayNameVector.add("NAMES"); //1
				intent.putExtra("MSG", msg);

				IntentFilter IFfinished = new IntentFilter("com.prach.mashup.FINISHED");
				androidContext.registerReceiver(IBReceiver_AmazonCA,IFfinished,null,null);
				IBReceiver_AmazonCA.ProcessNumber = 0x001;
				IBReceiver_AmazonCA.handler = iHandler;
				IBReceiver_AmazonCA.finish = iFinished_AmazonPriceCA;

				debug("getTitle()->call intent:com.prach.mashup.SMA");
				for (int i = 0; i < msg.length; i++)
					debug("getTitle()->msg["+i+"]:"+msg[i]);

				androidContext.startActivity(intent);
			}
		});
	}
	
	public Runnable iFinished_AmazonPriceCA = new Runnable() {
		public void run(){
			if(IBReceiver_AmazonCA.ProcessNumber == 0x001){
				stopThread(threadAmazonCA);
				androidContext.unregisterReceiver(IBReceiver_AmazonCA);

				int count_resultname = IBReceiver_AmazonCA.ResultNameVector.size();
				int count_resultarrayname = IBReceiver_AmazonCA.ResultArrayNameVector.size();
				int allcount = count_resultarrayname + count_resultname;

				debug("iFinished.run(0x001)->count_resultname:"+count_resultname);
				debug("iFinished.run(0x001)->count_resultarrayname:"+count_resultarrayname);
				debug("iFinished.run(0x001)->allcount:"+allcount);

				String[][] resultstrings = null;

				if(IBReceiver_AmazonCA.ResultStringVector.get(0).equals("RESULT_OK")){
					debug("result OK");
					resultstrings = new String[allcount][];

					for (int i = 0; i < count_resultname; i++){
						resultstrings[i] = new String[1];
						resultstrings[i][0] = IBReceiver_AmazonCA.ResultStringVector.get(i+1);
					}
					for (int i = count_resultname; i < allcount; i++) {
						resultstrings[i] = IBReceiver_AmazonCA.ResultStringArrayVector.get(i-count_resultname);
					}

					AmazonCA_ProductPrice = Function.getStringByName(
							resultstrings[IBReceiver_AmazonCA.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_AmazonCA.getArrayIndexfromName("NAMES")], 
					"ProductPrice");
					AmazonCA_ProductTitle = Function.getStringByName(
							resultstrings[IBReceiver_AmazonCA.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_AmazonCA.getArrayIndexfromName("NAMES")], 
					"ProductTitle");
					mHandler.sendEmptyMessage(WA_AmazonCA_SUCCEED);
				}else if(IBReceiver_AmazonCA.ResultStringVector.get(0).equals("RESULT_CANCELED")){
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
					mHandler.sendEmptyMessage(WA_AmazonCA_FAILED);

				}else 
					mHandler.sendEmptyMessage(WA_AmazonCA_FAILED);
			}
		}
	};
	
	public void prepareAmazonJP(){
		threadAmazonJP = new Thread(new Runnable() {
			public void run() {
				String URL = "http://www.amazon.co.jp/?force-full-site=1";
				String[] scripts = new String[2];
				scripts[0] = 
				    "prach = new Object;\n"+
				    "prach.input = '"+external_isbn+"';\n"+
				    "var tagArray1 = document.getElementsByTagName('TD');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=16&&i<24&&tagArray1[i].id=='searchTextBoxHolder'){"+
				    "        parentElement = tagArray1[i];"+
				    "        break;"+
				    "    }"+
				    "}"+
				    "if(parentElement==undefined)"+
				    "    window.prach.setfinishstate('false');"+
				    "/*case 5: single parent&child, single child-tag*/"+
				    "var tagArray2 = parentElement.getElementsByTagName('INPUT');"+
				    "var childElement;"+
				    "for(var i=0;i<tagArray2.length;i++){"+
				    "    if(i==0&&tagArray2[i].id=='twotabsearchtextbox'&&tagArray2[i].name=='field-keywords'&&tagArray2[i].className=='searchSelect'){"+
				    "        childElement = tagArray2[i];"+
				    "    }"+
				    "}"+
				    "childElement.value = prach.input;"+
				    "childElement.form.submit();";
				    

				scripts[1] = 			    
					"var ProductTitle = new Array();"+
				    "var tagArray1 = document.getElementsByTagName('DIV');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=38&&i<57&&tagArray1[i].className=='title'){"+
				    "        parentElement = tagArray1[i];"+
				    "        break;"+
				    "    }"+
				    "}"+
				    "if(parentElement==undefined)"+
				    "    window.prach.setfinishstate('false');"+
				    "/*case 5: single parent&child, single child-tag*/"+
				    "var tagArray2 = parentElement.getElementsByTagName('A');"+
				    "var childElement;"+
				    "for(var i=0;i<tagArray2.length;i++){"+
				    "    if(i==0&&tagArray2[i].className=='title'){"+
				    "        childElement = tagArray2[i];"+
				    "        ProductTitle.push(childElement.textContent);"+
				    "    }"+
				    "}"+
				    "window.prach.addOutputArray(ProductTitle,'ProductTitle');"+
				    
				    "var ProductPrice = new Array();"+
				    "var tagArray1 = document.getElementsByTagName('DIV');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=45&&i<63&&(tagArray1[i].className.indexOf('newPrice')!=-1&&tagArray1[i].className.indexOf('bold')!=-1)){"+
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
				    "    if(i==0&&tagArray2[i].className=='price'){"+
				    "        childElement = tagArray2[i];"+
				    "        ProductPrice.push(childElement.textContent);"+
				    "    }"+
				    "}"+
				    "window.prach.addOutputArray(ProductPrice,'ProductPrice');"+
				    "window.prach.setfinishstate('true');";

				IBReceiver_AmazonJP = new IntentBroadcastReceiver();
				Intent intent = new Intent("com.prach.mashup.SMA");
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

				String[] msg = {"com.prach.mashup.WAExtractor",
						"RESULTS:OUTPUTS", //0
						"RESULTS:NAMES", //1
						"EXTRA:MODE","EXTRACTION",
						"EXTRA:URL",URL,
						"EXTRAS:SCRIPTS",Function.genExtrasString(scripts)};
				IBReceiver_AmazonJP.ResultArrayNameVector.add("OUTPUTS"); //0
				IBReceiver_AmazonJP.ResultArrayNameVector.add("NAMES"); //1
				intent.putExtra("MSG", msg);

				IntentFilter IFfinished = new IntentFilter("com.prach.mashup.FINISHED");
				androidContext.registerReceiver(IBReceiver_AmazonJP,IFfinished,null,null);
				IBReceiver_AmazonJP.ProcessNumber = 0x001;
				IBReceiver_AmazonJP.handler = iHandler;
				IBReceiver_AmazonJP.finish = iFinished_AmazonPriceJP;

				debug("getTitle()->call intent:com.prach.mashup.SMA");
				for (int i = 0; i < msg.length; i++)
					debug("getTitle()->msg["+i+"]:"+msg[i]);

				androidContext.startActivity(intent);
			}
		});
	}
	
	public Runnable iFinished_AmazonPriceJP = new Runnable() {
		public void run(){
			if(IBReceiver_AmazonJP.ProcessNumber == 0x001){
				stopThread(threadAmazonJP);
				androidContext.unregisterReceiver(IBReceiver_AmazonJP);

				int count_resultname = IBReceiver_AmazonJP.ResultNameVector.size();
				int count_resultarrayname = IBReceiver_AmazonJP.ResultArrayNameVector.size();
				int allcount = count_resultarrayname + count_resultname;

				debug("iFinished.run(0x001)->count_resultname:"+count_resultname);
				debug("iFinished.run(0x001)->count_resultarrayname:"+count_resultarrayname);
				debug("iFinished.run(0x001)->allcount:"+allcount);

				String[][] resultstrings = null;

				if(IBReceiver_AmazonJP.ResultStringVector.get(0).equals("RESULT_OK")){
					debug("result OK");
					resultstrings = new String[allcount][];

					for (int i = 0; i < count_resultname; i++){
						resultstrings[i] = new String[1];
						resultstrings[i][0] = IBReceiver_AmazonJP.ResultStringVector.get(i+1);
					}
					for (int i = count_resultname; i < allcount; i++) {
						resultstrings[i] = IBReceiver_AmazonJP.ResultStringArrayVector.get(i-count_resultname);
					}

					AmazonJP_ProductPrice = Function.getStringByName(
							resultstrings[IBReceiver_AmazonJP.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_AmazonJP.getArrayIndexfromName("NAMES")], 
					"ProductPrice");
					AmazonJP_ProductTitle = Function.getStringByName(
							resultstrings[IBReceiver_AmazonJP.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_AmazonJP.getArrayIndexfromName("NAMES")], 
					"ProductTitle");
					mHandler.sendEmptyMessage(WA_AmazonJP_SUCCEED);
				}else if(IBReceiver_AmazonJP.ResultStringVector.get(0).equals("RESULT_CANCELED")){
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
					mHandler.sendEmptyMessage(WA_AmazonJP_FAILED);

				}else 
					mHandler.sendEmptyMessage(WA_AmazonJP_FAILED);
			}
		}
	};
	
	public String filterProductPrice(String price){
		return price.replaceAll("[^0-9.]", "");
	}
	
	public synchronized void stopThread(Thread t) {
		if (t != null) {
			Thread moribund = t;
			t = null;
			moribund.interrupt();
		}
	}
	
	public void debug(String msg){
		Log.d("AmazonPrice",msg);
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
	
	/*public String getOutputXML(){
		return 
		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
		"<object>\n"+
		"	<name>ResultStatus</name>\n"+
		"	<value>BookDatabase.output.STATUS</value>\n"+
		"</object>";
	}*/
	
	public String getOutputXML(){
		return 
		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
		"<object>\n"+
		"	<name>ResultStatus</name>\n"+
		"	<value>succeed</value>\n"+
		"	<name>TitleUS</name>\n"+
		"	<value>AmazonPriceUS.output.TITLE_US</value>\n"+
		"	<name>PriceUS</name>\n"+
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
		"	<value>AmazonPriceJP.output.PRICE_JP</value>\n"+
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
	
	public String getExchangeRateUSJPXML(){
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
	
	public void prepareExchangeRateUSJP(){
		threadExchangeRateUSJP = new Thread(new Runnable() {
			public void run() {
				replyExchangeRateUSJP = Parcel.obtain();
				Intent i = new Intent("com.prach.mashup.WSCService");
				boolean isConnected = androidContext.bindService(i,new ServiceConnection(){
					final int serviceCode = 0x101;
					public void onServiceConnected(ComponentName cname,IBinder service) {
						debug("ExchangeRate WS Service connected: "+ cname.flattenToShortString());

						Parcel data = Parcel.obtain();
						Bundle bundle = new Bundle();

						bundle.putString("BASE", ExchangeRateUSJPWSCParser.base);
						bundle.putStringArray("PATHS",ExchangeRateUSJPWSCParser.paths);
						bundle.putStringArray("KEYS", ExchangeRateUSJPWSCParser.keys);
						bundle.putStringArray("VALUES",ExchangeRateUSJPWSCParser.values);
						bundle.putString("FORMAT", ExchangeRateUSJPWSCParser.format);
						
						bundle.putStringArray("NAME", ExchangeRateUSJPWSCParser.name);
						bundle.putStringArray("TYPE", ExchangeRateUSJPWSCParser.type);
						bundle.putStringArray("QUERY", ExchangeRateUSJPWSCParser.query);
						bundle.putStringArray("INDEX", ExchangeRateUSJPWSCParser.index);
						
						data.writeBundle(bundle);
						boolean res = false;
						try {
							res = service.transact(serviceCode, data,replyExchangeRateUSJP, 0);
						} catch (RemoteException ex) {
							Log.e("onServiceConnected",
									"Remote exception when calling service",ex);
							res = false;
						}
						if (res)
							mHandler.sendEmptyMessage(WS_ExchangeRateUSJP_SUCCEED);
						else
							mHandler.sendEmptyMessage(WS_ExchangeRateUSJP_FAILED);
					}

					public void onServiceDisconnected(ComponentName name) {
						debug("ExchangeRate WS Service disconnected: "+ name.flattenToShortString());

					}
				}, Context.BIND_AUTO_CREATE);

				if (!isConnected) {
					debug("ExchangeRate WS Service could not be connected ");
					mHandler.sendEmptyMessage(WS_ExchangeRateUSJP_FAILED);
				}
			}
		});
	}
	
	public String getExchangeRateCAJPXML(){
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
		"			<path>cad</path>\n"+
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
	
	public void prepareExchangeRateCAJP(){
		threadExchangeRateCAJP = new Thread(new Runnable() {
			public void run() {
				replyExchangeRateCAJP = Parcel.obtain();
				Intent i = new Intent("com.prach.mashup.WSCService");
				boolean isConnected = androidContext.bindService(i,new ServiceConnection(){
					final int serviceCode = 0x101;
					public void onServiceConnected(ComponentName cname,IBinder service) {
						debug("ExchangeRate WS Service connected: "+ cname.flattenToShortString());

						Parcel data = Parcel.obtain();
						Bundle bundle = new Bundle();

						bundle.putString("BASE", ExchangeRateCAJPWSCParser.base);
						bundle.putStringArray("PATHS",ExchangeRateCAJPWSCParser.paths);
						bundle.putStringArray("KEYS", ExchangeRateCAJPWSCParser.keys);
						bundle.putStringArray("VALUES",ExchangeRateCAJPWSCParser.values);
						bundle.putString("FORMAT", ExchangeRateCAJPWSCParser.format);
						
						bundle.putStringArray("NAME", ExchangeRateCAJPWSCParser.name);
						bundle.putStringArray("TYPE", ExchangeRateCAJPWSCParser.type);
						bundle.putStringArray("QUERY", ExchangeRateCAJPWSCParser.query);
						bundle.putStringArray("INDEX", ExchangeRateCAJPWSCParser.index);
						
						data.writeBundle(bundle);
						boolean res = false;
						try {
							res = service.transact(serviceCode, data,replyExchangeRateCAJP, 0);
						} catch (RemoteException ex) {
							Log.e("onServiceConnected",
									"Remote exception when calling service",ex);
							res = false;
						}
						if (res)
							mHandler.sendEmptyMessage(WS_ExchangeRateCAJP_SUCCEED);
						else
							mHandler.sendEmptyMessage(WS_ExchangeRateCAJP_FAILED);
					}

					public void onServiceDisconnected(ComponentName name) {
						debug("ExchangeRate WS Service disconnected: "+ name.flattenToShortString());

					}
				}, Context.BIND_AUTO_CREATE);

				if (!isConnected) {
					debug("ExchangeRate WS Service could not be connected ");
					mHandler.sendEmptyMessage(WS_ExchangeRateCAJP_FAILED);
				}
			}
		});
	}
	
	public String getTimeStamp(){
		Calendar c = Calendar.getInstance();
		
        int hours = c.get(Calendar.HOUR);
        int minutes = c.get(Calendar.MINUTE);
        int seconds = c.get(Calendar.SECOND);
        int mseconds = c.get(Calendar.MILLISECOND);
        return  DateFormat.getDateInstance().format(new Date())+" "+hours + ":"+minutes + ":"+ seconds+"."+mseconds;
	}
}
