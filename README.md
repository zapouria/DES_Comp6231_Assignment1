Distributed Event Management System (DEMS) using Java RMI

In this assignment, we implemented a distributed event management system (DEMS) for a leading corporate event management company: a distributed system used by event managers who manage the information about the events and customers who can book or cancel an event across the company’s different branches.
Managers are allowed to perform some functions such as: addEvent, removeEvent, listEventAvailability.
Additionally customers are allowed to perform some functions such as: bookEvent, getBookingSchedule , cancelEvent.
We have 3 servers, Montreal, Quebec and Sherbrook. Therefore, we have one class for implementation of servers and we instantiate each of servers, and pass the required parameters for running the servers through the constructor.