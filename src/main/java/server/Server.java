package server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface Server 
{
	public Object start();
	public Object stop();
	public Object restart();
	public String getName();
	public String getLastError();
	public long getLastErrorDate();
	public boolean isRunning();
	public boolean regesterRequiredServers();
	public boolean reload();
	public boolean save();
	public boolean load();
	public ObjectNode getConfig();
	public ObjectNode getStats();
	public boolean setConfig(JsonNode config);
	//TODO: add public Operation getLastOperation();, This returns a date and an "Operation".
	//TODO: add public Exception getLastException();
	public Long getStartTime();
}
