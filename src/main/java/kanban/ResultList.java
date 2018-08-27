package kanban;

import java.util.ArrayList;
import java.util.HashMap;

public class ResultList extends ArrayList<HashMap<String,Object>>{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5821956676968273233L;

	public ResultList(java.util.List<HashMap<String, Object>> list)
	{
		addAll(list);
	}

	public ArrayList<String> getColumnNames() {
		Object[] it =  get(0).keySet().toArray();
		ArrayList<String> rt = new ArrayList<String>();
		for(Object o : it)
			rt.add(o.toString());
		return rt;
	}

	public boolean first(){
		return size()>0;
	}
}