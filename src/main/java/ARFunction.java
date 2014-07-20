import android.util.Log;


public class ARFunction {
	public static String summation(String[] input){
    	double toutput = 0;
    	
    	for (int i = 0; i < input.length; i++)
			toutput += Double.parseDouble(input[i]);
		
    	return Double.toString(toutput);
	}
	
	public static String mintoDec(String min){
		double decmin=Double.parseDouble(min);
		return Double.toString(decmin/60);
	}
	
	public static String sectoDec(String min){
		double decmin=Double.parseDouble(min);
		return Double.toString(decmin/3600);
	}
	
	public static String sum(String a,String b,String c){
		return Double.toString(Double.parseDouble(a)+Double.parseDouble(b)+Double.parseDouble(c));
		
	}
	
	public static String[] gpsdistance(String lat1,String lng1,String[] lat2, String[] lng2){
		String[] result = new String[lat2.length];
		
		for (int i = 0; i < result.length; i++) {
			String distance = gpsdistance(lat1,lng1,lat2[i],lng2[i]);
			result[i] = distance;
			Log.i("ARFunction","result["+i+"]:"+result[i]);
		}
		
		return result;
	}
	
	public static String gpsdistance(String lat1,String lng1,String lat2,String lng2){
		return Double.toString(gps2m(Double.parseDouble(lat1),Double.parseDouble(lng1),Double.parseDouble(lat2),Double.parseDouble(lng2)));
	}

	//copied libs
	private static double gps2m(double lat_a, double lng_a, double lat_b, double lng_b) {
		//Log.i("ARFunction","gps2m("+lat_a+","+lng_a+","+lat_b+","+lng_b+")");
		double pk = (double) (180/3.14169);

		double a1 = lat_a / pk;
		double a2 = lng_a / pk;
		double b1 = lat_b / pk;
		double b2 = lng_b / pk;

		double t1 = Math.cos(a1)*Math.cos(a2)*Math.cos(b1)*Math.cos(b2);
		double t2 = Math.cos(a1)*Math.sin(a2)*Math.cos(b1)*Math.sin(b2);
		double t3 = Math.sin(a1)*Math.sin(b1);
	    double tt = Math.acos(t1 + t2 + t3);
	   
	    return 6366000*tt;
	}
}
