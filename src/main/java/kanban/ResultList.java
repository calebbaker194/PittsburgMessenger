package kanban;

import java.util.ArrayList;
import java.util.HashMap;

public class ResultList extends ArrayList<HashMap<String,Object>>{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5821956676968273233L;
	private int lastRow;
	
	
	public ResultList(java.util.List<HashMap<String, Object>> list)
	{
		addAll(list);
	}

	/**
	 * Creates an empty result list. that we can build up
	 * Mainly used for json
	 */
	public ResultList()
	{
		
	}

	public int addRow()
	{
		add(new HashMap<String,Object>());
		lastRow = size()-1;
		return size()-1;
	}
	
	/**
	 * gets the value of the field specified by prop in the first elemet of the result set
	 * @param field the column that you want the value from.
	 */
	public Object get(String field)
	{
		if(size()>0)
			return get(0).get(field);
		else 
			return null;
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

	/**
	 * This alllows you to add json items into every item with a query.
	 * The nested group will be accessed as items. 
	 * @param query This is the query that it will use. 
	 * @param linker This is the field you will use to link the items. make sure that it is the same name in both querys
	 * @see NOTE: make sure that you have a link, to link the second set to the first set.
	 */
	public void addLevel(String query,String field)
	{
		ResultList r = SQLEngine.executeDBQuery(query);
		
		for(HashMap<String, Object> row: this)
		{
			row.put("items",new ResultList());
			for(HashMap<String, Object> indRow:r)
			{
				if(indRow.get(field).equals(row.get(field)))
				{
					int rid = ((ResultList) row.get("items")).addRow();
					((ResultList) row.get("items")).set(rid, indRow);
				}
			}
		}
	}

	public void put(String string, Object value)
	{
		get(lastRow).put(string, value);
	}
}