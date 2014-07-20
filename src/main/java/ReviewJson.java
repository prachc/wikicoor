import java.io.IOException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

@SuppressWarnings("serial")
public class ReviewJson extends HttpServlet {
	String packagename = null;
	IntentBroadcastReceiver IBReceiver;
	private Context androidContext;
	private ContentResolver resolver;
	private String ISBN;
	private String ProductPrice;
	private String ProductTitle;
	private String BookImageUrl;
    private String BookDescription;
    private String BookReview;
    private String Result;
    private volatile Thread runner;
    private ServletOutputStream out;
    private boolean finished = false;

	public synchronized void stopThread() {
		if (runner != null) {
			Thread moribund = runner;
			runner = null;
			moribund.interrupt();
		}
	}
    
	public Handler iHandler;
	
	
	public Runnable iFinished = new Runnable() {
		public void run(){
			if(IBReceiver.ProcessNumber == 0x001){
				androidContext.unregisterReceiver(IBReceiver);
		    	
		    	int count_resultname = IBReceiver.ResultNameVector.size();
		    	int count_resultarrayname = IBReceiver.ResultArrayNameVector.size();
		    	int allcount = count_resultarrayname + count_resultname;
		    	
		    	debug("iFinished.run(0x001)->count_resultname:"+count_resultname);
		    	debug("iFinished.run(0x001)->count_resultarrayname:"+count_resultarrayname);
		    	debug("iFinished.run(0x001)->allcount:"+allcount);
		    	    	
		    	String[][] resultstrings = null;
		    	
		    	if(IBReceiver.ResultStringVector.get(0).equals("RESULT_OK")){
		    		resultstrings = new String[allcount][];
		    		
		    		for (int i = 0; i < count_resultname; i++){
						resultstrings[i] = new String[1];
						resultstrings[i][0] = IBReceiver.ResultStringVector.get(i+1);
		    		}
		    		for (int i = count_resultname; i < allcount; i++) {
						resultstrings[i] = IBReceiver.ResultStringArrayVector.get(i-count_resultname);
					}
		    		
		    		ProductPrice = Function.getStringByName(
			    			resultstrings[IBReceiver.getArrayIndexfromName("OUTPUTS")], 
			    			resultstrings[IBReceiver.getArrayIndexfromName("NAMES")], 
			    			"ProductPrice");
			    	ProductTitle = Function.getStringByName(
			    			resultstrings[IBReceiver.getArrayIndexfromName("OUTPUTS")], 
			    			resultstrings[IBReceiver.getArrayIndexfromName("NAMES")], 
			    			"ProductTitle");
			    	getReview(ProductTitle);
		        }else if(IBReceiver.ResultStringVector.get(0).equals("RESULT_CANCELED")){
		        	resultstrings = new String[allcount][];
		        	for (int i = 0; i < count_resultname; i++){
						resultstrings[i] = new String[1];
						resultstrings[i][0] = "null";
		    		}
		    		for (int i = count_resultname; i < allcount; i++) {
		    			resultstrings[i] = new String[1];
						resultstrings[i][0] = "null";
					}
		    		dofinish(false);
		    		
		        }else 
		        	;
		    	
		    	//String ProductPrice = Function.getStringByName(resultstrings[0], resultstrings[1], "ProductPrice");
		    	//String ProductTitle = Function.getStringByName(resultstrings[0], resultstrings[1], "ProductTitle");
		    	
		    	
				//dofinish(true);
		    	
			}else if(IBReceiver.ProcessNumber == 0x002){
				androidContext.unregisterReceiver(IBReceiver);
		    	
		    	int count_resultname = IBReceiver.ResultNameVector.size();
		    	int count_resultarrayname = IBReceiver.ResultArrayNameVector.size();
		    	int allcount = count_resultarrayname + count_resultname;
		    	
		    	debug("iFinished.run(0x002)->count_resultname:"+count_resultname);
		    	debug("iFinished.run(0x002)->count_resultarrayname:"+count_resultarrayname);
		    	debug("iFinished.run(0x002)->allcount:"+allcount);
		    	    	
		    	String[][] resultstrings = null;
		    	
		    	if(IBReceiver.ResultStringVector.get(0).equals("RESULT_OK")){
		    		resultstrings = new String[allcount][];
		    		
		    		for (int i = 0; i < count_resultname; i++){
						resultstrings[i] = new String[1];
						resultstrings[i][0] = IBReceiver.ResultStringVector.get(i+1);
		    		}
		    		for (int i = count_resultname; i < allcount; i++) {
						resultstrings[i] = IBReceiver.ResultStringArrayVector.get(i-count_resultname);
					}
		    		
		    		BookImageUrl = Function.getStringByName(
			    			resultstrings[IBReceiver.getArrayIndexfromName("OUTPUTS")], 
			    			resultstrings[IBReceiver.getArrayIndexfromName("NAMES")], 
			    			"BookImage");
			    	BookDescription = Function.getStringByName(
			    			resultstrings[IBReceiver.getArrayIndexfromName("OUTPUTS")], 
			    			resultstrings[IBReceiver.getArrayIndexfromName("NAMES")], 
			    			"BookDescription");
			    	BookReview = Function.getStringByName(
			    			resultstrings[IBReceiver.getArrayIndexfromName("OUTPUTS")], 
			    			resultstrings[IBReceiver.getArrayIndexfromName("NAMES")], 
			    			"BookReview");
			    	dofinish(true);
			    	
		        }else if(IBReceiver.ResultStringVector.get(0).equals("RESULT_CANCELED")){
		        	resultstrings = new String[allcount][];
		        	for (int i = 0; i < count_resultname; i++){
						resultstrings[i] = new String[1];
						resultstrings[i][0] = "null";
		    		}
		    		for (int i = count_resultname; i < allcount; i++) {
		    			resultstrings[i] = new String[1];
						resultstrings[i][0] = "null";
					}
		    		dofinish(false);
		        }else 
		        	;
		    	
		    	//String ProductPrice = Function.getStringByName(resultstrings[0], resultstrings[1], "ProductPrice");
		    	//String ProductTitle = Function.getStringByName(resultstrings[0], resultstrings[1], "ProductTitle");
		    	
		    	
		    	
			}
		}
	};
	
	public void dofinish(boolean result_ok){
		if(result_ok){
			Result = Function.genSimpleJson(
					new String[] {"ISBN","ProductPrice","ProductTitle","BookImageUrl","BookDescription","BookReview","Status"}, 
					new String[] {ISBN,ProductPrice,ProductTitle,BookImageUrl,BookDescription,BookReview,"OK"}
				);
		}else{
			Result = "{\"Status\":\"FAILED\"}";
		}
		debug("dofinish()->result="+Result);
		runner.interrupt();
		stopThread();
		runner=null;
		ThreadMonitor tm = ThreadMonitor.getInstance(); 
    	synchronized (tm) {
			tm.notify();
		}
		
	}
	
	
	/* ------------------------------------------------------------ */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		// to demonstrate it is possible
		Object o = config.getServletContext().getAttribute("org.mortbay.ijetty.contentResolver");
		resolver = (android.content.ContentResolver) o;
		androidContext = (android.content.Context) config.getServletContext().getAttribute("org.mortbay.ijetty.context");
		packagename = androidContext.getApplicationInfo().packageName;
	}

	/* ------------------------------------------------------------ */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/* ------------------------------------------------------------ */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		finished = false;
		if (runner != null) {
			finished = true;
			killProcess("com.prach.mashup.webappextractor");
			killProcess("com.prach.mashup.sma");
			//IBReceiver.running=false;
			stopThread();
			runner = null;
			finished = false;
		}
		
		response.setContentType("text/html");
		out = response.getOutputStream();
		//out.println("ReviewJson<br>");
		final String isbn = request.getParameter("isbn");
		//out.println("isbn=" + isbn + "<br>");
		//String result = "";
		if (isbn == null)
			dofinish(false);
		else {
			runner = new Thread() {
				public void run() {
					//while (Thread.currentThread() == runner) {
						Looper.prepare();
						iHandler = new Handler();
						getTitle(isbn);
						Looper.loop();

						//if (interrupted()) {
						//	stopThread();
						//	return;
						//}
					//}

				}
			};
			runner.start();
		}
		ThreadMonitor tm = ThreadMonitor.getInstance();
		synchronized (tm) {
			try {
				tm.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				debug("doGet()->error="+e.toString());
			}
		}
		out.print(Result);
		//out.println("result=" + result + "<br>");
	}

	public void getTitle(String isbn) {
		
		ISBN = isbn;
		String URL = "http://www.amazon.com/?ie=UTF8&force-full-site=1";
		String[] scripts = new String[2];
		scripts[0] = 
			"prach = new Object;\n"+
			"prach.input = '"+isbn+"';\n"+
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
			"    if(i>38&&i<46&&tagArray1[i].className=='title'){\n"+
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
    	//IBReceiver.running = true;
    	IBReceiver.ProcessNumber = 0x001;
    	IBReceiver.handler = this.iHandler;
		IBReceiver.finish = this.iFinished;
    	
		debug("getTitle()->call intent:com.prach.mashup.SMA");
    	for (int i = 0; i < msg.length; i++)
    		debug("getTitle()->msg["+i+"]:"+msg[i]);
		
    	androidContext.startActivity(intent);
    	
    	//while(IBReceiver.running){}
    	
    	//return resultstrings;
    	
		//return "{\""+"ProductTitle"+"\":\""+ProductTitle+"\",\""+"ProductPrice"+"\":\""+ProductPrice+"\"}";
	}
	
	public void getReview(String booktitle){
		ProductTitle = filterProductTitle(booktitle);
		String URL = "http://www.goodreads.com/";
		String[] scripts = new String[2];
		scripts = new String[3];
		scripts[0] =
		"prach = new Object;\n"+
		"prach.input = '"+ProductTitle+"';\n"+
		"var tagArray1 = document.getElementsByTagName('form');"+
		"var parentElement;"+
		"for(var i=0;i<tagArray1.length;i++){"+
		"    if(i>=0&&i<6&&tagArray1[i].name=='headerSearchForm'){"+
		"        parentElement = tagArray1[i];"+
		"        break;"+
		"    }"+
		"}"+
		"var tagArray2 = parentElement.getElementsByTagName('input');"+
		"var childElement;"+
		"for(var i=0;i<tagArray2.length;i++)"+
		"    if(i==1&&tagArray2[i].id=='search_query'&&tagArray2[i].name=='search[query]')"+
		"        childElement = tagArray2[i];"+
		"childElement.focus();"+
		"childElement.value=prach.input;"+
		"childElement.form.submit();";
		scripts[1]=
		"var tagArray1 = document.getElementsByTagName('table');"+
		"var parentElement;"+
		"for(var i=0;i<tagArray1.length;i++){"+
		"    if(i>=0&&i<6&&tagArray1[i].className=='tableList'){"+
		"        parentElement = tagArray1[i];"+
		"        break;"+
		"    }"+
		"}"+
		"var tagArray2 = parentElement.getElementsByTagName('a');"+
		"var childElement;"+
		"for(var i=0;i<tagArray2.length;i++)"+
		"    if(i==2&&tagArray2[i].className=='bookTitle')"+
		"        childElement = tagArray2[i];"+
		"childElement.focus();"+
		"window.location = childElement.href;";
		scripts[2]=
		"var tagArray1 = document.getElementsByTagName('div');"+
		"var parentElement;"+
		"for(var i=0;i<tagArray1.length;i++){"+
		"    if(i>=11&&i<19&&tagArray1[i].id=='imagecol'&&tagArray1[i].className=='col'){"+
		"        parentElement = tagArray1[i];"+
		"        break;"+
		"    }"+
		"}"+
		"var tagArray2 = parentElement.getElementsByTagName('img');"+
		"var childElement;"+
		"for(var i=0;i<tagArray2.length;i++)"+
		"    if(i==0&&tagArray2[i].id=='coverImage')"+
		"        childElement = tagArray2[i];"+
		"childElement.focus();"+
		"var BookImage = childElement.src;"+
		
		"var tagArray1 = document.getElementsByTagName('div');"+
		"var parentElement;"+
		"for(var i=0;i<tagArray1.length;i++){"+
		"    if(i>=21&&i<29&&tagArray1[i].id=='description'&&(tagArray1[i].className.indexOf('readable')!=-1&&tagArray1[i].className.indexOf('stacked')!=-1)){"+
		"        parentElement = tagArray1[i];"+
		"        break;"+
		"    }"+
		"}"+
		"var tagArray2 = parentElement.getElementsByTagName('span');"+
		"var childElement;"+
		"for(var i=0;i<tagArray2.length;i++)"+
		"    if(i==1&&tagArray2[i].className=='reviewText')"+
		"        childElement = tagArray2[i];"+
		"var BookDescription = childElement.textContent;"+
		
		"var tagArray1 = document.getElementsByTagName('div');"+
		"var parentElement;"+
		"for(var i=0;i<tagArray1.length;i++){"+
		"    if(i>=62&&i<70&&tagArray1[i].className=='bigBoxContent'){"+
		"        parentElement = tagArray1[i];"+
		"        break;"+
		"    }"+
		"}"+
		"var tagArray2 = parentElement.getElementsByTagName('span');"+
		"var childElement;"+
		"var BookReview = '';"+
		"for(var i=0;i<tagArray2.length;i++){"+
		"    if(tagArray2[i].className=='reviewText'){"+
		"        childElement = tagArray2[i];"+
		"        BookReview += (childElement.textContent+'\\n');"+
		"    }"+
		"}"+
		"window.prach.addOutput(BookImage,'BookImage');" +
		"window.prach.addOutput(BookDescription,'BookDescription');" +
		"window.prach.addOutput(BookReview,'BookReview');" +
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
    	//IBReceiver.running = true;
    	IBReceiver.ProcessNumber = 0x002;
    	IBReceiver.handler = this.iHandler;
		IBReceiver.finish = this.iFinished;
    	
		debug("getReview()->call intent:com.prach.mashup.SMA");
    	for (int i = 0; i < msg.length; i++)
    		debug("getReview()->msg["+i+"]:"+msg[i]);
		
    	androidContext.startActivity(intent);
	}
	
	public String filterProductTitle(String ptitle){
		String[] temp = ptitle.split("\\(",2);
		return temp[0];
	}
	
	public String filterPrice(String price){
		return price.replace("$", "");
	}
	
	public String filterSpaces(String text){
		return text.replaceAll("\\s+", " ");
	}
	
	public void debug(String msg){
		Log.d("ReviewJson",msg);
	}
	
	@SuppressWarnings("static-access")
	public void killProcess(String pname){
		ActivityManager actManager = (ActivityManager) androidContext.getSystemService(androidContext.ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> processList = actManager.getRunningAppProcesses();
		for (int i = 0; i < processList.size(); i++) {
			debug("killProcess()->processName["+i+"]:"+processList.get(i).processName);
			if(processList.get(i).processName.contains(pname)){
				debug("killProcess()->killProces:("+processList.get(i).pid+")"+processList.get(i).processName);
				android.os.Process.killProcess(processList.get(i).pid);
			}
		}
	}
}
