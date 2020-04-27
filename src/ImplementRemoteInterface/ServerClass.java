package ImplementRemoteInterface;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import ServerInterface.EventManagementInterface;
import DataBase.*;

public class ServerClass extends UnicastRemoteObject implements EventManagementInterface{
	private Map<String, Map<String, EventDetail>> EventMap;
	private Map<String, Map<String, ClientDetail>> ClientMap;
	private int quebec_port, montreal_port, sherbrooke_port;
	private String serverName;
	public ServerClass(int quebec_port, int montreal_port, int sherbrooke_port, String serverName) throws RemoteException {
		super();
		
		this.quebec_port = quebec_port;
		this.montreal_port = montreal_port;
		this.sherbrooke_port = sherbrooke_port;
		this.serverName = serverName.toUpperCase();
		EventMap = new HashMap<>();
		ClientMap = new HashMap<>();
		
	}

	@Override
	public String addEvent(String eventID, String eventType, int bookingCapacity) throws RemoteException {
		if(EventMap.containsKey(eventType.toUpperCase()) && EventMap.get(eventType.toUpperCase()).containsKey(eventID.toUpperCase()))
		{
			int currentCapacity = EventMap.get(eventType.toUpperCase()).get(eventID.toUpperCase()).bookingCapacity;
			EventMap.get(eventType.toUpperCase()).replace(eventID,new EventDetail(eventType.toUpperCase(), eventID.toUpperCase(), currentCapacity + bookingCapacity));

			try 
			{
				serverLog("Add event", " EventType:"+eventType+ " EventID:"+eventID +
						"bookingCapacity:"+ bookingCapacity,"successfully completed", "Capacity added to event");				
			} catch (IOException e) {
				e.printStackTrace();
			}
			return "Event added capacity";
		}
		else if(EventMap.containsKey(eventType.toUpperCase()))
		{
			EventMap.get(eventType.toUpperCase()).put(eventID.toUpperCase(), new EventDetail(eventType.toUpperCase(), eventID.toUpperCase(), bookingCapacity));
			try 
			{
				serverLog("Add event", " EventType:"+eventType+ " EventID:"+eventID +
						"bookingCapacity:"+ bookingCapacity,"successfully completed", "Event added to" + serverName.toUpperCase());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return "Event added to" + serverName.toUpperCase();
		}	
		else
		{
			Map <String, EventDetail> subHashMap = new HashMap<>();
			subHashMap.put(eventID.toUpperCase(), new EventDetail(eventType.toUpperCase(), eventID.toUpperCase(), bookingCapacity));
			EventMap.put(eventType.toUpperCase(), subHashMap);
			try 
			{
				serverLog("Add event", " EventType:"+eventType+ " EventID:"+eventID +
						"bookingCapacity:"+ bookingCapacity,"successfully completed", "Event added to" + serverName.toUpperCase());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return "Event added to" + serverName.toUpperCase();
		}
	}

	@Override
	public String removeEvent(String eventID, String eventType) throws RemoteException {
		if(EventMap.containsKey(eventType.toUpperCase()) && EventMap.get(eventType.toUpperCase()).containsKey(eventID.toUpperCase()))
		{			
			String response="";
			String branch = eventID.substring(0,3).toUpperCase();
			EventMap.get(eventType.toUpperCase()).remove(eventID.toUpperCase());
			try {
				serverLog("Remove event", " EventType:"+eventType+ " EventID:"+eventID
						,"successfully completed", "Event removed from server" + serverName.toUpperCase());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			response = remove_client_event(eventID.toUpperCase(), eventType.toUpperCase());
			
			if(branch.equals("QUE"))
			{
				send_data_request(montreal_port, "remove_client_event", eventID.toUpperCase(), eventType.toUpperCase(),"-");
				send_data_request(sherbrooke_port, "remove_client_event",eventID.toUpperCase(), eventType.toUpperCase(),"-");
			}
			else if(branch.equals("MTL"))
			{
				send_data_request(quebec_port, "remove_client_event", eventID.toUpperCase(), eventType.toUpperCase(),"-");
				send_data_request(sherbrooke_port, "remove_client_event", eventID.toUpperCase(), eventType.toUpperCase(),"-");

			}
			else if(branch.equals("SHE"))
			{
				send_data_request(montreal_port, "remove_client_event", eventID.toUpperCase(), eventType.toUpperCase(),"-");
				send_data_request(quebec_port, "remove_client_event", eventID.toUpperCase(), eventType.toUpperCase(),"-");
			}
						
			return response;
		}
		else
		{
			return "Error: There is no such a record to remove";
		}
	}
	
	public String remove_client_event(String eventID, String eventType) 
	{
		String data = "";
		String new_eventID = "";
		for(Entry<String, Map<String, ClientDetail>> customer : ClientMap.entrySet())
		{
			Map<String, ClientDetail> eventDetail = customer.getValue();
			String branch = eventID.substring(0,3).toUpperCase();

			if(eventDetail.containsKey(eventType.toUpperCase() +";"+ eventID.toUpperCase()+""))
			{	
				eventDetail.remove(eventType.toUpperCase() +";"+ eventID.toUpperCase());
				for (Map.Entry<String,ClientDetail> entry : customer.getValue().entrySet()) 
				{
					data +=(entry.getValue().eventID.toUpperCase()+":");
				}
				if(branch.equals("QUE"))
				{
					new_eventID = send_data_request(quebec_port, "boook_next_event", data, eventType.toUpperCase(),"-");
				}
				else if(branch.equals("MTL"))
				{
					new_eventID = send_data_request(montreal_port, "boook_next_event", data, eventType.toUpperCase(),"-");

				}
				else if(branch.equals("SHE"))
				{
					new_eventID = send_data_request(sherbrooke_port, "boook_next_event", data, eventType.toUpperCase(),"-");
				}

				try 
				{
					if(new_eventID.equals(""))
					{
						serverLog("Remove event", " EventType:"+eventType+ " EventID:"+eventID,"successfully completed", 
							"Event removed for client:" + customer.getKey().toUpperCase());
					
						clientLog(customer.getKey().toUpperCase(), "Remove event", "eventType:" + eventType+" eventID:"+eventID+" has been removed");
					}
					else
					{
						add_book_customer(customer.getKey().toUpperCase(),new_eventID, eventType);
						serverLog("Remove event", " EventType:"+eventType+ " EventID:"+new_eventID,"successfully completed", 
								"Event has been replaced for client:" + customer.getKey().toUpperCase());
						
							clientLog(customer.getKey().toUpperCase(), "Remove event", "eventType:" + eventType+" eventID:"+new_eventID+" has been replaced");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return "Event with eventID:"+ eventID.toUpperCase() +" and eventType: "+eventType.toUpperCase() +" for clients has been removed for server\n";
	}
	
	public String boook_next_event(String temp, String eventType) {
		
		String response="";
		String [] data = temp.split(":");
		String eventID="";
		int capacity =0;

		if(EventMap.containsKey(eventType.toUpperCase()) && EventMap.get(eventType.toUpperCase()).values().size() != 0)
		{
			if(data.length!= 0)
			{
				for (Map.Entry<String,EventDetail> entry : EventMap.get(eventType).entrySet()) 
				{
					boolean check = false;
					for (int i =0; i< data.length; ++i)
					{
						capacity = entry.getValue().bookingCapacity;

						if(!(data[i].indexOf(entry.getKey().toUpperCase())!=-1) && capacity!=0)
						{
							check = true;
						}
						else
						{
							check = false;
							break;
						}
					}
					if(check == true)
					{
						eventID = entry.getKey().toUpperCase();
						EventMap.get(eventType.toUpperCase()).replace(eventID,new EventDetail(eventType.toUpperCase(), eventID.toUpperCase(), capacity - 1));
						try 
						{
							serverLog("Remove event", " EventType:"+eventType+ " EventID:"+eventID,"successfully completed", 
									"Next available event replaced for client:");
						} catch (IOException e) {
							e.printStackTrace();
						}
						return eventID;
					}
				}
			}
			else
			{
				Map.Entry<String,EventDetail> entry = EventMap.get(eventType).entrySet().iterator().next();
				capacity = EventMap.get(eventType.toUpperCase()).get(entry.getValue().eventID.toUpperCase()).bookingCapacity;
				if(capacity!=0)
				{
					eventID = entry.getValue().eventID.toUpperCase();
					EventMap.get(eventType.toUpperCase()).replace(eventID,new EventDetail(eventType.toUpperCase(), eventID.toUpperCase(), capacity - 1));
					try 
					{
						serverLog("Remove event", " EventType:"+eventType+ " EventID:"+eventID,"successfully completed", 
								"Next available event replaced for client:");
					} catch (IOException e) {
						e.printStackTrace();
					}
					return eventID;
				}
			}
		}

		return response;	
	}

	@Override
	public String listEventAvailability(String eventType) throws RemoteException {
		String response = "List of availability for "+ eventType +":\n";
		if(EventMap.containsKey(eventType.toUpperCase()))
		{
			for (Map.Entry<String, EventDetail> entry : EventMap.get(eventType.toUpperCase()).entrySet()) 
			{
				response += entry.getKey() + " " + entry.getValue().bookingCapacity +",  ";
			}
			if(serverName.equals("QUE"))
			{
				response += send_data_request(montreal_port, "list_events", "-", eventType.toUpperCase(),"-");
				response += send_data_request(sherbrooke_port, "list_events", "-", eventType.toUpperCase(),"-");
	
			}
			else if(serverName.equals("MTL"))
			{
				response += send_data_request(quebec_port, "list_events", "-", eventType.toUpperCase(),"-");
				response += send_data_request(sherbrooke_port, "list_events", "-", eventType.toUpperCase(),"-");
			}
			else if(serverName.equals("SHE"))
			{
				response += send_data_request(montreal_port, "list_events", "-", eventType.toUpperCase(),"-");
				response += send_data_request(quebec_port, "list_events", "-", eventType.toUpperCase(),"-");
			}
		}
		else
			response += "0";
		return response;
	}
	public String list_events(String eventType) 
	{
		String response = "";

		if(EventMap.containsKey(eventType.toUpperCase()))
		{	
			for (Map.Entry<String, EventDetail> entry : EventMap.get(eventType).entrySet()) 
			{
				response += entry.getKey() + " " + entry.getValue().bookingCapacity +",  ";
			}
		}
		return response;
	}
	
	@Override
	public String bookEvent(String customerID, String eventID, String eventType) throws RemoteException {
		String response="";
		String city = eventID.substring(0,3).toUpperCase();
		String eventDetail = eventType.toUpperCase()+ ";" + eventID.toUpperCase();
		int count = 0;
		if(city.equals(serverName))
		{
			response = book_accepted_event(customerID, eventID, eventType);
			add_book_customer(customerID, eventID, eventType);
			if(response.indexOf("ERR_NO_CAPACITY")!=-1)
			{
				try 
				{
					serverLog("Book an event", " EventType:"+eventType+ " EventID:"+eventID +" CustomerID:"+ customerID,"failed","There is no capacity for this event");
					clientLog(customerID, "Book an event", "There is no capacity for eventType:" + eventType+" eventID:"+eventID);

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else if(response.indexOf("ERR_NO_RECORD")!=-1)
			{
				try 
				{
					serverLog("Book an event", " EventType:"+eventType+ " EventID:"+eventID+" CustomerID:"+ customerID,"failed","There is no such an event");
					clientLog(customerID, "Book an event", "There is no such an event --> eventType:" + eventType+" eventID:"+eventID);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else
			{
				try 
				{
					serverLog("Book an event", " EventType:"+eventType+ " EventID:"+eventID+" CustomerID:"+ customerID,"successfully completed","Booking request has been approved");
					clientLog(customerID, "Book an event", "Booking request has been approved --> eventType:" + eventType+" eventID:"+eventID);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		else
		{
			if(city.equals("QUE"))
			{
				if(ClientMap.containsKey(customerID.toUpperCase()) && ClientMap.get(customerID.toUpperCase()).containsKey(eventDetail))			
				{
					try 
					{
						serverLog("Book an event", " EventType:"+eventType+ " EventID:"+eventID +" CustomerID:"+ customerID,"failed","This event has already been booked");
						clientLog(customerID, "Book an event", "This event has already been booked --> eventType:" + eventType+" eventID:"+eventID);

					} catch (IOException e) {
						e.printStackTrace();
					}
					
					return "ERR_RECORD_EXISTS";
				}
				if(ClientMap.containsKey(customerID.toUpperCase()))
				{
					for (Map.Entry<String, ClientDetail> entry : ClientMap.get(customerID.toUpperCase()).entrySet()) 
					{
						String [] data = entry.getKey().split(";");
						int limit = entry.getValue().outer_city_limit;
						if( data[1].substring(0, 3).equals(city)&& week_number() == limit)
						{
							count ++;
						}
					}
					if(count == 3)
					{
						try 
						{
							serverLog("Book an event", " EventType:"+eventType+ " EventID:"+eventID+" CustomerID:"+ customerID,"failed","This customer has already booked 3 times from other cities!");
							clientLog(customerID, "Book an event", "This customer has already booked 3 times from other cities --> eventType:" + eventType+" eventID:"+eventID);
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						return "This customer has already booked 3 times from other cities!";
					}
				}
				response = send_data_request(quebec_port, "bookEvent", eventID.toUpperCase(), eventType.toUpperCase(),customerID.toUpperCase());
				if(response.indexOf("BOOKING_APPROVED")!=-1)
				{
					try 
					{
						serverLog("Book an event", " EventType:"+eventType+ " EventID:"+eventID+" CustomerID:"+ customerID,"successfully completed","Booking request has been approved");
						clientLog(customerID, "Book an event", "Booking request has been approved --> eventType:" + eventType+" eventID:"+eventID);
					} catch (IOException e) {
						e.printStackTrace();
					}
					add_book_customer(customerID, eventID, eventType);
				}
				else if(response.indexOf("ERR_NO_CAPACITY")!=-1)
				{
					try 
					{
						serverLog("Book an event", " EventType:"+eventType+ " EventID:"+eventID +" CustomerID:"+ customerID,"failed","There is no capacity for this event");
						clientLog(customerID, "Book an event", "There is no capacity for eventType:" + eventType+" eventID:"+eventID);

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if(response.indexOf("ERR_NO_RECORD")!=-1)
				{
					try 
					{
						serverLog("Book an event", " EventType:"+eventType+ " EventID:"+eventID+" CustomerID:"+ customerID,"failed","There is no such an event");
						clientLog(customerID, "Book an event", "There is no such an event --> eventType:" + eventType+" eventID:"+eventID);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			else if(city.equals("MTL"))
			{
				if(ClientMap.containsKey(customerID.toUpperCase()) && ClientMap.get(customerID.toUpperCase()).containsKey(eventDetail))			
				{
					try 
					{
						serverLog("Book an event", " EventType:"+eventType+ " EventID:"+eventID +" CustomerID:"+ customerID,"failed","This event has already been booked");
						clientLog(customerID, "Book an event", "This event has already been booked --> eventType:" + eventType+" eventID:"+eventID);
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					return "ERR_RECORD_EXISTS";
				}
				if(ClientMap.containsKey(customerID.toUpperCase()))
				{
					for (Map.Entry<String, ClientDetail> entry : ClientMap.get(customerID.toUpperCase()).entrySet()) 
					{
						String [] data = entry.getKey().split(";");
						int limit = entry.getValue().outer_city_limit;
						if( data[1].substring(0, 3).equals(city)&& week_number() == limit)
						{
							count ++;
						}
					}
					if(count == 3)
					{
						try 
						{
							serverLog("Book an event", " EventType:"+eventType+ " EventID:"+eventID+" CustomerID:"+ customerID,"failed","This customer has already booked 3 times from other cities!");
							clientLog(customerID, "Book an event", "This customer has already booked 3 times from other cities --> eventType:" + eventType+" eventID:"+eventID);
						} catch (IOException e) {
							e.printStackTrace();
						}
						return "This customer has already booked 3 times from other cities!";
					}
				}
				response = send_data_request(montreal_port, "bookEvent", eventID.toUpperCase(), eventType.toUpperCase(),customerID.toUpperCase());
				if(response.indexOf("BOOKING_APPROVED")!=-1)
				{
					try 
					{
						serverLog("Book an event", " EventType:"+eventType+ " EventID:"+eventID+" CustomerID:"+ customerID,"successfully completed","Booking request has been approved");
						clientLog(customerID, "Book an event", "Booking request has been approved --> eventType:" + eventType+" eventID:"+eventID);
					} catch (IOException e) {
						e.printStackTrace();
					}
					add_book_customer(customerID, eventID, eventType);
				}
				else if(response.indexOf("ERR_NO_CAPACITY")!=-1)
				{
					try 
					{
						serverLog("Book an event", " EventType:"+eventType+ " EventID:"+eventID +" CustomerID:"+ customerID,"failed","There is no capacity for this event");
						clientLog(customerID, "Book an event", "There is no capacity for eventType:" + eventType+" eventID:"+eventID);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if(response.indexOf("ERR_NO_RECORD")!=-1)
				{
					try 
					{
						serverLog("Book an event", " EventType:"+eventType+ " EventID:"+eventID+" CustomerID:"+ customerID,"failed","There is no such an event");
						clientLog(customerID, "Book an event", "There is no such an event --> eventType:" + eventType+" eventID:"+eventID);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			else if(city.equals("SHE"))
			{
				if(ClientMap.containsKey(customerID.toUpperCase()) && ClientMap.get(customerID.toUpperCase()).containsKey(eventDetail))			
				{
					try 
					{
						serverLog("Book an event", " EventType:"+eventType+ " EventID:"+eventID +" CustomerID:"+ customerID,"failed","This event has already been booked");
						clientLog(customerID, "Book an event", "This event has already been booked --> eventType:" + eventType+" eventID:"+eventID);
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					return "ERR_RECORD_EXISTS";
				}
				if(ClientMap.containsKey(customerID.toUpperCase()))
				{
					for (Map.Entry<String, ClientDetail> entry : ClientMap.get(customerID.toUpperCase()).entrySet()) 
					{
						String [] data = entry.getKey().split(";");
						int limit = entry.getValue().outer_city_limit;
						if( data[1].substring(0, 3).equals(city)&& week_number() == limit)
						{
							count ++;
						}
					}
					if(count == 3)
					{
						try 
						{
							serverLog("Book an event", " EventType:"+eventType+ " EventID:"+eventID+" CustomerID:"+ customerID,"failed","This customer has already booked 3 times from other cities!");
							clientLog(customerID, "Book an event", "This customer has already booked 3 times from other cities --> eventType:" + eventType+" eventID:"+eventID);
						} catch (IOException e) {
							e.printStackTrace();
						}
						return "This customer has already booked 3 times from other cities!";
					}
				}
				response = send_data_request(sherbrooke_port, "bookEvent", eventID.toUpperCase(), eventType.toUpperCase(),customerID.toUpperCase());
				if(response.indexOf("BOOKING_APPROVED")!=-1)
				{
					try 
					{
						serverLog("Book an event", " EventType:"+eventType+ " EventID:"+eventID+" CustomerID:"+ customerID,"successfully completed","Booking request has been approved");
						clientLog(customerID, "Book an event", "Booking request has been approved --> eventType:" + eventType+" eventID:"+eventID);
					} catch (IOException e) {
						e.printStackTrace();
					}
					add_book_customer(customerID, eventID, eventType);
				}
				else if(response.indexOf("ERR_NO_CAPACITY")!=-1)
				{
					try 
					{
						serverLog("Book an event", " EventType:"+eventType+ " EventID:"+eventID +" CustomerID:"+ customerID,"failed","There is no capacity for this event");
						clientLog(customerID, "Book an event", "There is no capacity for eventType:" + eventType+" eventID:"+eventID);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if(response.indexOf("ERR_NO_RECORD")!=-1)
				{
					try 
					{
						serverLog("Book an event", " EventType:"+eventType+ " EventID:"+eventID+" CustomerID:"+ customerID,"failed","There is no such an event");
						clientLog(customerID, "Book an event", "There is no such an event --> eventType:" + eventType+" eventID:"+eventID);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
		}
		return response;
	}
	
	public int week_number()
	{
		Date date = new Date();
		LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		int day = localDate.getDayOfMonth();
		int month = localDate.getMonthValue();
		
		int week_number = (day + (month-1)*30)/7; 
			
		return week_number;
	}
	public String book_accepted_event(String customerID, String eventID, String eventType)
	{
		String response="";

		if(EventMap.containsKey(eventType.toUpperCase()) && EventMap.get(eventType.toUpperCase()).containsKey(eventID.toUpperCase()))
		{
			int capacity = EventMap.get(eventType.toUpperCase()).get(eventID.toUpperCase()).bookingCapacity;
			
			if( capacity == 0)
				return "ERR_NO_CAPACITY";
			else
			{
				EventMap.get(eventType.toUpperCase()).replace(eventID,new EventDetail(eventType.toUpperCase(), eventID.toUpperCase(), capacity - 1));
				return "BOOKING_APPROVED";
			}
		}
		else
		{
			response = "ERR_NO_RECORD!";
		}

		return response;
	}

	public String add_book_customer(String customerID, String eventID, String eventType)
	{
		String response = "";
		String eventDetail = eventType.toUpperCase()+ ";" + eventID.toUpperCase();

		if(ClientMap.containsKey(customerID.toUpperCase()))			
		{	
			ClientMap.get(customerID.toUpperCase()).put(eventDetail, new ClientDetail(customerID.toUpperCase(), eventType.toUpperCase(), eventID.toUpperCase(), week_number()));
			response = "BOOKED";
		}
		else
		{
			Map <String, ClientDetail> subHashMap = new HashMap<>();
			subHashMap.put(eventDetail, new ClientDetail(customerID.toUpperCase(), eventType.toUpperCase(), eventID.toUpperCase(), week_number()));
			ClientMap.put(customerID.toUpperCase(), subHashMap);
			response = "BOOKED";
		}
		return response;
	}

	@Override
	public String getBookingSchedule(String customerID) throws RemoteException {
		String response = "";
		
		if(ClientMap.containsKey(customerID.toUpperCase()))			
		{
			for (Map.Entry<String, ClientDetail> entry : ClientMap.get(customerID.toUpperCase()).entrySet()) 
			{
				String [] data = entry.getKey().split(";");
				response += "EventType:" + data[0] + " EventID:" + data[1]+"\n";
			}
			return response;
		}
		else
			return "No record for this customer";
	}

	@Override
	public String cancelEvent(String customerID, String eventID, String eventType) throws RemoteException {
		String eventDetail = eventType.toUpperCase()+ ";" + eventID.toUpperCase();
		String branch = eventID.substring(0,3).toUpperCase();

		if(ClientMap.containsKey(customerID.toUpperCase()) && ClientMap.get(customerID.toUpperCase()).containsKey(eventDetail))		
		{
			ClientMap.get(customerID.toUpperCase()).remove(eventDetail);

			if(branch.equals(serverName))
			{
				int currentCapacity = EventMap.get(eventType.toUpperCase()).get(eventID.toUpperCase()).bookingCapacity;
				EventMap.get(eventType.toUpperCase()).replace(eventID,new EventDetail(eventType.toUpperCase(), eventID.toUpperCase(), currentCapacity + 1));
				try 
				{
					serverLog("Cancel an event", " EventType:"+eventType+ " EventID:"+eventID+" CustomerID:"+ customerID,"successfully completed","Event has been canceled");
					clientLog(customerID, "Cancel an event", "Event has been canceled --> eventType:" + eventType+" eventID:"+eventID);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else if(branch.equals("QUE"))
			{
				send_data_request(quebec_port, "cancel_client_event", eventID.toUpperCase(), eventType.toUpperCase(),"-");
				try 
				{
					serverLog("Cancel an event", " EventType:"+eventType+ " EventID:"+eventID+" CustomerID:"+ customerID,"successfully completed","Event has been canceled");
					clientLog(customerID, "Cancel an event", "Event has been canceled --> eventType:" + eventType+" eventID:"+eventID);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
			else if(branch.equals("MTL"))
			{
				send_data_request(montreal_port, "cancel_client_event", eventID.toUpperCase(), eventType.toUpperCase(),"-");
				try 
				{
					serverLog("Cancel an event", " EventType:"+eventType+ " EventID:"+eventID+" CustomerID:"+ customerID,"successfully completed","Event has been canceled");
					clientLog(customerID, "Cancel an event", "Event has been canceled --> eventType:" + eventType+" eventID:"+eventID);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			else if(branch.equals("SHE"))
			{
				send_data_request(sherbrooke_port, "cancel_client_event", eventID.toUpperCase(), eventType.toUpperCase(),"-");
				try 
				{
					serverLog("Cancel an event", " EventType:"+eventType+ " EventID:"+eventID+" CustomerID:"+ customerID,"successfully completed","Event has been canceled");
					clientLog(customerID, "Cancel an event", "Event has been canceled --> eventType:" + eventType+" eventID:"+eventID);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return "Event for customer cancelled";
		}
		else
			return "No record event";
	}
	
	public String cancel_client_event(String eventID, String eventType)
	{
		if(EventMap.containsKey(eventType.toUpperCase()) && EventMap.get(eventType.toUpperCase()).containsKey(eventID.toUpperCase()))
		{
			int currentCapacity = EventMap.get(eventType.toUpperCase()).get(eventID.toUpperCase()).bookingCapacity;
			EventMap.get(eventType.toUpperCase()).replace(eventID,new EventDetail(eventType.toUpperCase(), eventID.toUpperCase(), currentCapacity + 1));
		}	
		return "CANCELED";
		
	}

	public void serverLog(String acion, String peram, String requestResult, String response) throws IOException {
		String city = serverName;
		final String dir = System.getProperty("user.dir");
		String fileName = dir;
		if(city.equals("QUE")) {
			fileName = dir+"Quebec.txt";
		}else if(city.equals("SHE")) 
		{
			fileName = dir+"Sherbrook.txt";
		}else if(city.equals("MTL")) 
		{
			fileName = dir+"Montreal.txt";
		}

		Date date = new Date();

		String strDateFormat = "yyyy-MM-dd hh:mm:ss a";

		DateFormat dateFormat = new SimpleDateFormat(strDateFormat);

		String formattedDate= dateFormat.format(date);


		FileWriter fileWriter = new FileWriter(fileName,true);
		PrintWriter printWriter = new PrintWriter(fileWriter);
		printWriter.println("DATE: "+formattedDate+"| Request type: "+acion+" | Request parameters: "+ peram +" | Request result: "+requestResult+" | Server resonse: "+ response);

		printWriter.close();

	}
	
	public void clientLog(String ID, String acion, String response) throws IOException {
		final String dir = System.getProperty("user.dir");
		String fileName = dir;
		fileName = dir+"\\"+ID+".txt";



		FileWriter fileWriter = new FileWriter(fileName,true);
		PrintWriter printWriter = new PrintWriter(fileWriter);
		printWriter.println("Request type: "+acion+" | Resonse: "+ response);

		printWriter.close();

	}
	
	private static String send_data_request(int serverPort,String function,String eventID, String eventType, String customerID) {
		DatagramSocket socket = null;
		String result ="";
		String clientRequest = function+";"+eventID.toUpperCase()+";"+eventType.toUpperCase()+";" + customerID.toUpperCase();
		try {
			socket = new DatagramSocket();
			byte[] data = clientRequest.getBytes();
			InetAddress host = InetAddress.getByName("localhost");
			DatagramPacket request = new DatagramPacket(data, clientRequest.length(), host, serverPort);
			socket.send(request);

			byte[] buffer = new byte[1000];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);

			socket.receive(reply);
			result = new String(reply.getData());
		} catch (SocketException e) {
			System.out.println("Socket exception: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("IO Error: " + e.getMessage());
		} finally {
			if (socket != null)
				socket.close();
		}
		return result;

	}

	@Override
	public String shutDown() throws RemoteException 
	{
		new Thread(new Runnable() {
			public void run() {
				try {
				   Thread.sleep(100);
				} catch (InterruptedException e) {
				   // ignored
				}
				System.exit(1);
			}
		});
		return "Shutting down";
	}
}
