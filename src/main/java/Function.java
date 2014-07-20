public class Function {
	public static String getStringByName(String[] outputs, String[] names, String key) {
		String result = "";
		for (int i = 0; i < names.length; i++) {
			if(names[i].equals(key)){
				result = outputs[i];
			}
		}
		return result;
	}

	public static String[] getArrayByName(String[] outputs, String[] names, String key) {
		String[] results = new String[0];
		for (int i = 0; i < names.length; i++) {
			if(names[i].equals(key)){
				concat(results,outputs[i]);
			}
		}
		return results;
	}
	
	public static String[] concat(String[] A, String B){
		if(A==null) 
			return new String[] {B};
		
		String[] C = new String[A.length+1];
		System.arraycopy(A, 0, C, 0, A.length);
		C[A.length] = B;
		return C;
	}
	
	public static String genExtrasString(String[] extras){
		StringBuffer temp = new StringBuffer();
		for (int i = 0; i < extras.length; i++) {
			temp.append(extras[i]);
			temp.append("<<>>");
		}
		return temp.substring(0,temp.length()-4).toString();
	}
	
	public static String genSimpleJson(String[] names, String[] values){
		StringBuffer json = new StringBuffer();
		json.append("{");
		
		for (int i = 0; i < names.length; i++) {
			json.append("\""+names[i]+"\"");
			json.append(":");
			json.append("\""+escapeJson(values[i])+"\"");
			json.append(",");
		}
		json.deleteCharAt(json.length()-1);
		json.append("}");
		
		return json.toString();
	}
	
	public static String escapeJson(String json){
		String result = json.replace("\"", "\\\"");
		result = result.replace(",", "\\,");
		result = result.replace("{", "\\{");
		result = result.replace("}", "\\}");
		result = result.replace("[", "\\[");
		result = result.replace("]", "\\]");
		result = result.replace("'", "\\'");
		return result;
		//, { } [ ] " '
	}
}
