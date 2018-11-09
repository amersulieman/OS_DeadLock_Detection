/***
 * @AUTHOR Amer Sulieman
 * @version 10/25/2018
 * 
 * Class that detecs deadlock parsed by file.
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class DeadLock{
    /**
     * A hash map that will hold the resources read from a file,
     * Assign the number of resource from the file as the key and give it a resource object as a value
     */
    private  static HashMap<Integer,Resource> resourcesContainer;
    private static HashMap<Integer,Process> processesContainer;
    
   /**
    * The use of the following lists is because my deadlockCheck method is a recursive method so 
    * it is important to keep track of the processes and resources between each recurssive call so i can print 
    * */ 
    private static List<Integer> deadLockResources;  //list will be used to store the resources that were in a cycle and caused deadlock
    private static List<Integer> deadLockProcesses; // list will be used to store the processes that were in a cycle and caused deadlock

    public static void main(String[] args)throws FileNotFoundException{
        //checks if the user inputs the file to parse
        if(args.length<1){
            System.out.println("You need to provide the file name to parse for deadLocks!");
            System.exit(0);
        }

        File myFile= new File("./"+args[0]);         //File to read is taken from the command line.
        Scanner input = new Scanner(myFile);         //Use scanner to read the file.

        resourcesContainer = new HashMap<>(200);        //create the hash map for resources with size 200
        processesContainer = new HashMap<>(200);        //create the hashmap for processes  with size 200

        int processID;                              //integer value that represent a process read from file
        String needORrelease;                        //string character read from file, N or R
        int resourceID;                              //integer value that represent the resource read from file
        String[] lineSplit;                          //Array that will be used to split each line read from file into 3 cells

        //Loop the file, but it will be read a line at a time and does the resource and process allocation and deadlock checking as every line read at a time.
        while(input.hasNextLine()){
            lineSplit=input.nextLine().split(" ");              //split the line by spaces, and store in the array
            processID=Integer.parseInt(lineSplit[0]);           //parse the first number in the line as an integer and it represent a process ID
            needORrelease=lineSplit[1];                         //the second cell in the array represent letter "N" or "R" which is for need or release
            resourceID=Integer.parseInt(lineSplit[2]);          //parse the second number in the line as an integer and it represent a resource ID 

            //if the hash map does not have the key for the process then create an object for it and insert it in the hashmap
            if(processesContainer.get(processID)==null){
                Process processObject = new Process(processID);
                processesContainer.put(processID, processObject);
            }
            //if the hash map does not have the key for the resource then create an object for it and insert it in the hashmap
            if(resourcesContainer.get(resourceID)==null){
                Resource resourceObject = new Resource(resourceID);
                resourcesContainer.put(resourceID,resourceObject);  
            }
            //If the line parsed is trying to allocate a resource for that process, "N" means need, then call method Need()
            if(needORrelease.equals("N")){
                System.out.print("Process "+ processID + " Needs Resource "+resourceID + " - ");
                Needing(processesContainer.get(processID),resourcesContainer.get(resourceID));
            }
            //If the line says release the resource then call Release()
            else if(needORrelease.equals("R")){
                System.out.print("Process "+ processID + " releases resource "+resourceID + " - ");
                Releasing(processID,resourcesContainer.get(resourceID));
            }
        }//End of while loop
        input.close();//close scanner
        //if we reach here after reading the whole file then there was no deadlock!!!!
        System.out.println("EXECUTION COMPLETED: No deadlock encountered.");
    }//End of main method

    /***
     * A method that handles allocating resources for processes.
     * After every allocation or after every request, dead lock will be checked for by calling a seperate method
     * @param process           The process requesting
     * @param resourceNeeded    The resource being requested
     */
    public static void Needing(Process process,Resource resourceNeeded){
        //initillay next process is sat to the same process we began with because in checkdeadlock, nmext process is changing when the recursive call is made
        Process nextProcess=process;
        /***
         * I recreate the array lists for processes and resources for each line needed to be processed because 
         * if a line goes through and was checked for its deadlock then my arraylists needs to be resat for the next line
         * so when the next line deadlock is checked, the arraylists that will store processes and resources are empty initially
         * they don't carry data from previous checks. 
         * */
        deadLockProcesses = new ArrayList<>();  //Arraylist will hold the deadlock processes
        deadLockResources = new ArrayList<>();  //arraylist will hold the deadlock resources
        //if the resource is not occupied then update its occupier to be the process that needs it
        if(resourceNeeded.occupier==null){
            resourceNeeded.occupier = process;              
            //call method to Check for deadlock
            checkDeadLock(process,nextProcess, resourceNeeded);
            //If this line is reached here then there is no dead lock and assign the resource to the process printed
            System.out.println("Resource " + resourceNeeded.id+ " is allocated to Process "+process.id);
        }
        //if the resource is already occupied, handle the case by adding the resource ro the process list, and add the process to the resource queue
        else{  
            resourceNeeded.processesWaitingOnIt.add(process.id);                //add the process to that resource's queue
            process.resourcesWaitingToOccupy.add(resourceNeeded);               //add the resource to that process's list
            System.out.println("Process "+process.id + " Must Wait");
            //check the deadLock
            checkDeadLock(process,nextProcess,resourceNeeded);
        }
    }//END OF NEEDING() METHOD

    /**
     * A method that releases resources from the processes they were occupied by.
     * If a resource has processes waiting on it then the one waiting longest gets to occupy it first.
     * That is accomplished using queues
     * @param process       The process releasing the resource
     * @param resource      The resource being released
     */
    public static void Releasing(int process, Resource resourceReleased){
        //If a resource's queue is empty, it means no process was weaiting on it so reset it to being free
        if(resourceReleased.processesWaitingOnIt.isEmpty()){
            //reset the resource occupier to be no one
            resourceReleased.occupier=null;
            //Print that it is free
            System.out.println("Resource "+resourceReleased.id+" is now free");
        }
        //If the resource had some process waiting on it, then reset the occupier of it to null which is no one, then call need() method
        else{
            //reset occupier to null
            resourceReleased.occupier=null;
            //get the process that is waiting on the resource for the longest
            int processWaiting = resourceReleased.processesWaitingOnIt.remove();
            //also remove that resource from the list that the process keeps of who it is waiting on
            processesContainer.get(processWaiting).resourcesWaitingToOccupy.remove(resourceReleased);     
            //call method needing to allocate the process waiting on that resource   
            Needing(processesContainer.get(processWaiting), resourceReleased);
        }
    }//END OF RELEASING() METHOD

    /***
     * This method looks for deadlock recursively!
     * It checks base cases that breaks DealLock, such as if the resource is not occupied or if the
     * resource is occupied and its occupier is not pointing requesting any other resources. 
     * Then, it checks if the resource occupier is the process i started with. This means a loop.
     * If not, then recursively change the resource paramter to be the resource that the current resource's process is pointing to.
     * This way we move to the next resource and check the same cases again, if its occupier is the same number as 
     * the process we started with then we have reached a loop
     * 
     * @param startingProcess   //process I started with, does not change through the recursive method
     * @param nextProcess       //next process in the chain
     * @param resource          //The resource requested, chnages during recursive call
     */
    public static void checkDeadLock(Process startingProcess,Process nextProcess,Resource resource){

        deadLockProcesses.add(nextProcess.id);            //add the process to the deadlock list of processes
        deadLockResources.add(resource.id);                     //add the resource to the deadlock list of resources

        //check if the resource we are requesting is occupied, If not then return there is no deadlock
        if(resource.occupier==null){
            return;
        }
        //if the resource is occupied, check if its occupier is requesting other resources. If not, then there is no deadlock
        else if(nextProcess.resourcesWaitingToOccupy.isEmpty()){
            return;
        }
        //Check if the occupier of the resource is the process i started with. That is our deadLock case
        else if(resource.occupier.id==startingProcess.id){
            System.out.print("DEADLOCK DETECTED: ");
            printDeadLockCauses();  //print the resources and processes associated with the deadlock which are in the lists i made
            System.exit(0);         //exit the program
        }
        //for the occupier of the resource being requested, loop through each resource it is requesting and call this method recursively with the next resource and process in chain 
        else{
            nextProcess= processesContainer.get(resource.occupier.id);  //gets the process occupying the resource being requested which will be next process to search with
            //this for loop is to check each resource the next process has been waiting on, check if deadlock occurs through any of them
            for(Resource nextResource : processesContainer.get(resource.occupier.id).resourcesWaitingToOccupy){
                checkDeadLock(startingProcess,nextProcess,nextResource);
            }
        }
    }//END OF DEADLOCKCHECK() METHOD
   
    /**
     * A method to print the deadLock array lists for processes and resources  which are the reaosn there is a cycle
     */
    public static void printDeadLockCauses(){
        Collections.sort(deadLockProcesses);    //Sort the deadLock process
        Collections.sort(deadLockResources);    //Sort the deadLock resources

        //printing foramt
        System.out.print("Processes ");
        //Print every element in process that caused the deadlock
        for(int i:deadLockProcesses){
            System.out.print(i+", ");
        }
        //print every element in resources that caused deadlock
        System.out.print("and Resources ");
        for(int i:deadLockResources){
            System.out.print(i+", ");
        }
        System.out.println("are found in a cycle");
    }
}

/**
 * A class that will represent a process as an object
 */
class Process{
    int id=-1;                              //The ID of a Process, initially sat to -1 before process is created
    Resource occupying=null;                //Resource object that the process occupies
    List<Resource> resourcesWaitingToOccupy;//The resources that the process is waiting on until they get freed

    /***
     * Constructor that creates the process object, sets the id to the process given to it
     * and create an array List that will hold the resources the process waiting on 
     * because they are occupied!
     * @param process   The process number which is the id of the process
     */
    public Process(int process){
        id=process;                 //set the id to the number given to the process
        resourcesWaitingToOccupy=new ArrayList<>();     //create array list to hold resources waits on for later
    }
}

/**
 * A class object that represents a resource.
 */
class Resource{
    int id=-1;                  //Resource ID, initially sat to -1 before it is created
    Process occupier=null;      //Process object that occupies the resource, initially null because no one occupy it yet
    Queue <Integer> processesWaitingOnIt;    //Queue to add the processes waiting for the resource to get empty
    
    /***
     * Constructor to create the resource object, by given it its id and create a waiting list for it.
     * @param resource  The resource number given through a file
     */
    public Resource(int resource){
        id=resource;                   //id of the resource given when it is created
        processesWaitingOnIt=new LinkedList<>();    //queue of linked list for the processes waiting on the resource so as soon as the resource free the first one needed it gets it.
                                        
    }
}

