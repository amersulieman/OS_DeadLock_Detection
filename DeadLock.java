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
     * Assign the number of resource from the file as the key and give it a resource object
     */
    private  static HashMap<Integer,Resource> resourcesContainer;
    private static HashMap<Integer,Process> processesContainer;
    
   /**
    * The use of the following lists is because my deadlockCheck method is a recursive method so 
    * it is important to keep track of the processes and resources between each recurssive call
    * */ 
    private static List<Integer> deadLockResources;  //list will be used to store the resources that were in a cycle and caused deadlock
    private static List<Integer> deadLockProcesses; // list will be used to store the processes that were in a cycle and caused deadlock

    public static void main(String[] args)throws FileNotFoundException{
        //checks if the user inputs the file to parse
        if(args.length<1){
            System.out.println("You need to provide the file name to parse for deadLocks!");
            System.exit(0);
        }
        //File to read is taken from the command line.
        File myFile= new File("./"+args[0]);
        //Use scanner to read the file.
        Scanner input = new Scanner(myFile);
        //create the hash map for resources
        resourcesContainer = new HashMap<>();
        processesContainer = new HashMap<>();

        int processKey;                    //integer value that represent a process read from file
        String needORrelease;           //string character read from file, N or R
        int resourceKey;                //integer value that represent the resource read from file
        String[] lineSplit;             //Array that will be used to split each line in file into 3 cells

        //Loop the file, but it will be read a line at a time.
        while(input.hasNextLine()){
            //split the line by spaces, and store in the array
            lineSplit=input.nextLine().split(" ");
            //parse the first number in the line as an integer and it represent a process number
            processKey=Integer.parseInt(lineSplit[0]);
            //the second cell in the array represent letter "N" or "R" which is for need or release
            needORrelease=lineSplit[1];
            //The third cell in the array which will be the resource number, it is parsed an integer 
            resourceKey=Integer.parseInt(lineSplit[2]);

            if(processesContainer.get(processKey)==null){
                Process processObject = new Process(processKey);
                processesContainer.put(processKey, processObject);
            }
            /**
             * The idea is to create a resource object only if it was not created. By parsing the line, 
             * check the hashmap of the resources, if that key does not exist, then that resource was not
             * created. Then create it.
             */
            if(resourcesContainer.get(resourceKey)==null){
                //create resource object and give it the id that was parsed from file
                Resource resourceObject = new Resource(resourceKey);
                //add the object to the hash map paired with its key which is the id of the resource parsed from the file
                resourcesContainer.put(resourceKey,resourceObject);  
            }
            //If the line parsed is trying to allocate a resource for that process, "N" means need, then call method Need()
            if(needORrelease.equals("N")){
                System.out.print("Process "+ processKey + " Needs Resource "+resourceKey + " - ");
                //call the method by passing it the process and the resource from that line
                Needing(processesContainer.get(processKey),resourcesContainer.get(resourceKey));
            }
            //If the line says release the resource then call Release()
            else if(needORrelease.equals("R")){
                //call the method by passing it the process and the resource from that line
                System.out.print("Process "+ processKey + " releases resource "+resourceKey + " - ");
                Releasing(processKey,resourcesContainer.get(resourceKey));
            }
        }//End of while loop
        input.close();
        System.out.println("EXECUTION COMPLETED: No deadlock encountered.");
    }//End of main method

    /***
     * A method that handles allocating resources for processes.
     * After every allocation or after every request, dead lock will be checked for by calling a seperate method
     * @param process    The process requesting
     * @param resourceNeeded   The resource being requested
     */
    public static void Needing(Process process,Resource resourceNeeded){
        /***
         * I recreate the array lists for processes and resources for each line needed to be processed because 
         * if a line goes through and was checked for its deadlock then my arraylists needs to be resat for the next line
         * so when the next line deadlock is checked, the arraylists that will store processes and resources are empty initially
         * they don't carry data from previous checks. 
         * */
         
        deadLockProcesses = new ArrayList<>();  //Array list will hold the deadlock processes
        deadLockResources = new ArrayList<>();  //arraylist will hold the deadlock resources
        //if the resource is not occupied then update its occupier to be the process that needs it
        if(resourceNeeded.occupier==null){
            //update the resource occupier to the process that needs it
            resourceNeeded.occupier = process;
            //call method to Check for deadlock
            checkDeadLock(process,process, resourceNeeded);
            //If the method came here then there is no dead lock and assign the resource to the process
            System.out.println("Resource " + resourceNeeded.id+ " is allocated to Process "+process.id);
        }
        /***
         * Here the case where the resource is occupied by another process then i need to handle it
         * by adding the process to that resource waiting queue and add the process to the processTOResource hashmap where process
         * points to resource it is waiting for or pointing toward. Then check deadlock
         */
        else{  
            //add the process to that resource's queue
            resourceNeeded.waiting.add(process.id);
            //check the hashmap of processes pointing to resources
            process.waitingOn.add(resourceNeeded);
            //Print statement 
            System.out.println("Process "+process.id + " Must Wait");
            //check the deadLock
            checkDeadLock(process,process, resourceNeeded);
        }
    }//END OF NEEDING() METHOD

    /**
     * A method that releases resources from the processes they were occupied by.
     * If a resource has processes waiting on it then the one waiting longest gets to occupy it first.
     * That is accomplished using queues
     * @param process    The process releasing the resource
     * @param resource      The resource being released
     */
    public static void Releasing(int process, Resource resourceReleased){
        //If a resource's queue is empty, it means no process was weaiting on it so reset it to being free
        if(resourceReleased.waiting.isEmpty()){
            //reset the resource occupier to be no one
            resourceReleased.occupier=null;
            //Print that it is free
            System.out.println("Resource "+resourceReleased.id+" is now free");
        }
        //If the resource had some process waiting on it, then reset the occupier of it to -1 which is no one then call need method
        else{
            //reset occupier to -1 to set the resource free
            resourceReleased.occupier=null;
            //get the process that is waiting on the resource
            int processWaiting = resourceReleased.waiting.remove();
            processesContainer.get(processWaiting).waitingOn.remove(resourceReleased);        
            Needing(processesContainer.get(processWaiting), resourceReleased);
        }
    }//END OF RELEASING() METHOD

    /***
     * This method looks for deadlock recursively!
     * It checks basic cases that breaks DealLock, such as if the resource is not occupied or if the
     * resource is occupied and its occupier is not pointing anywhere. 
     * Then, it checks if the resource occupier is the process that is requesting the resource. This means a loop.
     * If not, then recursively change the resource paramter to be the resource that the current resource's process is pointing to.
     * This way we move to the next resource and check the same cases again, if its occupier is the same number as 
     * the process we started with then we have reached a loop
     * @param process   //Process requesting resource
     * @param resource  //The resource requested
     */
    public static void checkDeadLock(Process coreProcess,Process process,Resource resource){
        deadLockProcesses.add(resource.occupier.id);
        deadLockResources.add(resource.id);
        //check if the resource we are requesting is occupied, If not then return there is no deadlock
        if(resource.occupier==null){
            return;
        }
        //if the resource is occupied, check if its occupier points to a different process or requesting a different process
        //If not, then there is no deadlock
        else if(process.waitingOn.isEmpty()){
            return;
        }
        //Check if the occupier of the resource is the process that is asking for the resource. That is our deadLock case
        else if(resource.occupier.id==coreProcess.id){
            
            System.out.print("DEADLOCK DETECTED: ");
            printDeadLockCauses();
            System.exit(0);
        }
        //If haven't found deadlock, loop through each resource connected and call this method recursively 
        else{
            for(Resource r:processesContainer.get(resource.occupier.id).waitingOn){
                checkDeadLock(coreProcess,processesContainer.get(resource.occupier.id),r);
            }
        }
    }//END OF DEADLOCKCHECK() METHOD
   
    /**
     * A method to print the deadLock array lists for processes and resourcesmwhich are the reaosn there is a cycle
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

class Process{
    int id=-1;
    Resource occupying=null;
    List<Resource> waitingOn;

    public Process(int process){
        id=process;
        waitingOn=new ArrayList<>();
    }
}

/**
 * A class object that represents a resource.
 */
class Resource{
    int id=-1;//Resource ID
    Process occupier=null;   //which process occup it, initlliay -1 means no one.
    Queue <Integer> waiting;    //Queue to add the processes waiting for the resource to get empty
    
    /***
     * Constructor to create the resource object, by given it its id and create a waiting list for it.
     * @param resource  The resource number given through a file
     */
    public Resource(int resource){
        id=resource;                   //create the resource
        waiting=new LinkedList<>();    //create a linekd list for it
    }
}

