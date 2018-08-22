package server;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface Server 
{
	public Object start();
	public Object stop();
	public Object restart();
	public String getName();
	public String getLastError();
	public String getLastErrorDate();
	public boolean isRunning();
	public boolean regesterRequiredServers();
	public void reload();
	public void save();
	public void load();
	public ObjectNode getConfig();
	//TODO: add public Operation getLastOperation();, This returns a date and an "Operation".
	//TODO: add public Exception getLastException();
}
