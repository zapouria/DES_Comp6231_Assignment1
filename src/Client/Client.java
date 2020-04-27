package Client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

import ServerInterface.EventManagementInterface;

public class Client {
	
	public static void main(String args[])
	{
		Run();
	}

	private static void Run() {
		System.out.println("Please enter your username:");
		Scanner sc = new Scanner(System.in);
		String input = sc.nextLine().toLowerCase();
		
		if(input.length() != 8) {
			System.out.println("Please write a proper ID!");
			Run();
		}
		
		String eventManager = input.substring(3,4);
		
		if(eventManager.equals(("m")))
		{
			try
			{
				manager(input);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		else if(eventManager.equals("c"))
		{
			try
			{
				customer(input);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			System.out.println("Your user access is not correct!");
			Run();
		}
	}

	private static void customer(String customerID) throws Exception{
		int portNumber = serverPort(customerID);
		if(portNumber == -1)
		{
			System.out.println("Invalid branch! Please write a proprer username!");
			return;
		}
		Registry registry = LocateRegistry.getRegistry(portNumber);
		EventManagementInterface obj = (EventManagementInterface) registry.lookup("ServerClass");
		System.out.println("Please enter number of the action: \n "
				+ "1.Book an event \n "
				+ "2.Get booking schedule \n "
				+ "3.Cancel event \n "
				+ "4.Exit \n"
				+ "5.Shutdown the server \n");
		Scanner sc = new Scanner(System.in);
		String action = sc.nextLine();
		if(action.equals("1")) 
		{
			String eventID = set_everntID(customerID.substring(0,4));
			String event_type = set_event_type();
			
			String response = obj.bookEvent(customerID, eventID, event_type);
			System.out.println(response);
			customer(customerID);
			
		}
		else if(action.equals("2"))
		{
			String response = obj.getBookingSchedule(customerID);
			System.out.println(response);
			customer(customerID);
		}
		else if(action.equals("3"))
		{
			String eventID = set_everntID(customerID.substring(0,4));
			String event_type = set_event_type();
			String response = obj.cancelEvent(customerID, eventID, event_type);
			System.out.println(response);
			customer(customerID);		}
		else if(action.equals("4"))
		{
			Run();
		}
		else if(action.equals("5"))
		{
			obj.shutDown();
		}
		else
		{
			customer(customerID);
			System.out.println("Invalid action!");
		}
	}

	private static void manager(String input) throws Exception{
		int portNumber = serverPort(input);
		if(portNumber == -1)
		{
			System.out.println("Invalid branch! Please write a proprer username!");
			return;
		}
		Registry registry = LocateRegistry.getRegistry(portNumber);
		EventManagementInterface obj = (EventManagementInterface) registry.lookup("ServerClass");
		System.out.println("Please enter number of the action: \n "
				+ "1.Add new event \n "
				+ "2.Remove an event \n "
				+ "3.Check availability of an event \n "
				+ "4.log in as a customer\n "
				+ "5.Exit \n");

		Scanner sc = new Scanner(System.in);
		String action = sc.nextLine();
		if(action.equals("1")) 
		{
			String eventID = set_everntID(input.substring(0,4));
			String event_type = set_event_type();
			System.out.println("Please choose a capacity:");
			int booking_capacity = sc.nextInt();
			String response = obj.addEvent(eventID, event_type,booking_capacity);
			System.out.println(response);
			manager(input);
			
		}
		else if(action.equals("2"))
		{	
			String event_type = set_event_type();
			System.out.println("Please enter the eventID: \n");
			String eventID = sc.nextLine();
			String response = obj.removeEvent(eventID, event_type);
			System.out.println(response);
			manager(input);
		}
		else if(action.equals("3"))
		{
			String event_type = set_event_type();
			String response = obj.listEventAvailability(event_type);
			System.out.println(response);
			manager(input);		
		}
		else if(action.equals("4"))
		{
			System.out.println("Please enter your username:");
			String data = sc.nextLine().toLowerCase();	
			customer(data);
		}
		else if(action.equals("5"))
		{
			Run();
		}
		else
		{
			manager(input);
			System.out.println("Invalid action!");
		}
		
	}
	
	private static String set_event_type() {
		Scanner sc = new Scanner(System.in);
		System.out.println("Please choose your event type:"
				+ "1.Conferences"
				+ "2.Trade Shows"
				+ "3.Seminars):"); 
		String event_type = sc.nextLine();
		if(event_type.equals("1")) 
		{
			event_type = "Conferences";
		}
		else if(event_type.equals("2")) 
		{
			event_type = "Trade Shows";
		}
		else if(event_type.equals("3")) 
		{
			event_type = "Seminars";
		}
		else 
		{
			System.out.println("Invalid event type!");
			Run();
		}
		return event_type;
	}

	private static String set_everntID(String data) {
		String eventID;
		if(data.substring(3,4).toUpperCase().equals("C"))
		{
			eventID = set_city();
		}
		else
		{
			eventID = data.substring(0,3).toUpperCase();
		}
		eventID += set_time_slot();
		eventID += set_event_date();
		
		return eventID;
	}

	private static String set_city() {
		Scanner sc = new Scanner(System.in);
		System.out.println("Please choose one of the city codes as belllow: \n"
				+ "Quebec (code: QUE) \n"
				+ "Montreal (code: MTL) \n"
				+ "Sherbrooke (code: SHE) \n");		
		String city = sc.nextLine().toUpperCase();
		if(city.length() != 3) {
			System.out.println("Please write a proper code for city!");
			Run();
		}
		return city;
	}

	private static String set_time_slot() {
		Scanner sc = new Scanner(System.in);
		System.out.println("Please enter the time slot "
				+ "Morning (M), "
				+ "Afternoon (A) and "
				+ "Evening (E):");
		String time_slot = sc.nextLine().toUpperCase();

		if(time_slot.length()!=1)
		{
			System.out.println("Please write a proper time slot!");
			Run();
		}
		return time_slot;
	}
	
	private static String set_event_date() {
		Scanner sc = new Scanner(System.in);
		System.out.println("Please enter the event date digits(ddmmyy)");
		String event_date = sc.nextLine().toUpperCase();
		if(event_date.length()!=6)
		{
			System.out.println("Please write a proper event date!");
			Run();
		}
		return event_date;
	}

	private static int serverPort(String input)
	{
		String branch = input.substring(0,3);
		int portNumber = -1;
		
		if(branch.equals("que"))
			portNumber=9991;
		else if(branch.equals("mtl"))
			portNumber=9992;
		else if(branch.equals("she"))
			portNumber=9993;
			
		return portNumber;
	}
}
