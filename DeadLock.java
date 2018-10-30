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
    /**
     * A hashmap that will keep of the processes and where they point to.
     * If a process needs a resource:
     * if that resource is busy then that process is pointint to the resource and waiting.
     */
    private  static HashMap<Integer,Integer> processPointToResourceContainer;

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
        //create the hash map for processes
        processPointToResourceContainer = new HashMap<>();
        
        int process;                    //integer value that represent a process read from file
        String needORrelease;           //string character read from file, N or R
        int theResource;                //integer value that represent the resource read from file
        String[] lineSplit;             //Array that will be used to split each line in file into 3 cells

        //Loop the file, but it will be read a line at a time.
        while(input.hasNextLine()){
            //split the line by spaces, and store in the array
            lineSplit=input.nextLine().split(" ");
            //parse the first number in the line as an integer and it represent a process number
            process=Integer.parseInt(lineSplit[0]);
            //the second cell in the array represent letter "N" or "R" which is for need or release
            needORrelease=lineSplit[1];
            //The third cell in the array which will be the resource number, it is parsed an integer 
            theResource=Integer.parseInt(lineSplit[2]);

            /**
             * The idea is to create a resource object only if it was not created. By parsing the line, 
             * check the hashmap of the resources, if that key does not exist, then that resource was not
             * created. Then create it.
             */
            if(!resourcesContainer.containsKey(theResource)){
                //create resource object and give it the id that was parsed from file
                Resource myResource = new Resource(theResource);
                //add the object to the hash map paired with its key which is the id of the resource parsed from the file
                resourcesContainer.put(theResource,myResource);  
            }
            //If the line parsed is trying to allocate a resource for that process, "N" means need, then call method Need()
            if(needORrelease.equals("N")){
                System.out.print("Process "+ process + " Needs Resource "+theResource + " - ");
                //call the method by passing it the process and the resource from that line
                Needing(process,resourcesContainer.get(theResource));
            }
            //If the line says release the resource then call Release()
            else if(needORrelease.equals("R")){
                //call the method by passing it the process and the resource from that line
                System.out.print("Process "+ process + " releases resource "+theResource + " - ");
                Releasing(process,resourcesContainer.get(theResource));
            }
        }//End of while loop
        System.out.println("EXECUTION COMPLETED: No deadlock encountered.");
    }//End of main method

    /***
     * A method that handles allocating resources for processes.
     * After every allocation or after every request, dead lock will be checked for by calling a seperate method
     * @param theProcess    The process requesting
     * @param resource      The resource being requested
     */
    public static void Needing(int theProcess,Resource resource){
        /***
         * I recreate the array lists for processes and resources for each line needed to be processed because 
         * if a line goes through and was checked for its deadlock then my arraylists needs to be resat for the next line
         * so when the next line deadlock is checked, the arraylists that will store processes and resources are empty initially
         * they don't carry data from previous checks. 
         * */
         
        deadLockProcesses = new ArrayList<>();  //Array list will hold the deadlock processes
        deadLockResources = new ArrayList<>();  //arraylist will hold the deadlock resources
        //if the resource is not occupied then update its occupier to be the process that needs it
        if(resource.getOccupier()==-1){
            //update the resource occupier to the process that needs it
            resource.setOccupier(theProcess);
            //call method to Check for deadlock
            checkDeadLock(theProcess, resource);
            //If the method came here then there is no dead lock and assign the resource to the process
            System.out.println("Resource " + resource.getId()+ " is allocated to Process "+theProcess);
        }
        /***
         * Here the case where the resource is occupied by another process then i need to handle it
         * by adding the process to that resource waiting queue and add the process to the processTOResource hashmap where process
         * points to resource it is waiting for or pointing toward. Then check deadlock
         */
        else{  
            //add the process to that resource's queue
            resource.enqueueProcess(theProcess);
            //check the hashmap of processes pointing to resources
            if(!processPointToResourceContainer.containsKey(theProcess)){
                //if it does not have that process, then add it with the resource it needs as its value
                processPointToResourceContainer.put(theProcess,resource.getId());
            }
            //If the hashmap alread has that key for that process, then update where that process's value to the new resource it points to
            else{
                processPointToResourceContainer.replace(theProcess,resource.getId());
            }
            //Print statement 
            System.out.println("Process "+theProcess + " Must Wait");
            //check the deadLock
            checkDeadLock(theProcess, resource);
        }
    }//END OF NEEDING() METHOD

    /**
     * A method that releases resources from the processes they were occupied by.
     * If a resource has processes waiting on it then the one waiting longest gets to occupy it first.
     * That is accomplished using queues
     * @param theProcess    The process releasing the resource
     * @param resource      The resource being released
     */
    public static void Releasing(int theProcess, Resource resource){
        //If a resource's queue is empty, it means no process was weaiting on it so reset it to being free
        if(resource.getQueue().isEmpty()){
            //reset the resource occupier to be no one
            resource.setOccupier(-1);
            //Print that it is free
            System.out.println("Resource "+resource.getId()+" is now free");
        }
        //If the resource had some process waiting on it, then reset the occupier of it to -1 which is no one then call need method
        else{
            //reset occupier to -1 to set the resource free
            resource.setOccupier(-1);
            //get the process that is waiting on the resource
            int processWaiting = resource.dequeueProcess();
            //set the occupier of the current resource to the process waiting for it
            resource.setOccupier(processWaiting);
            //reset the process in the  hashMap to -1 because now it occupied the resource and not waiting anymore
            processPointToResourceContainer.replace(processWaiting,-1);
            System.out.println("Resource "+resource.getId()+" is allocated to process "+processWaiting);
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
    public static void checkDeadLock(int process,Resource resource){
        deadLockProcesses.add(resource.getOccupier());
        deadLockResources.add(resource.getId());
        //check if the resource we are requesting is occupied, If not then return there is no deadlock
        if(resource.getOccupier()==-1){
            return;
        }
        //if the resource is occupied, check if its occupier points to a different process or requesting a different process
        //If not, then there is no deadlock
        else if(processPointToResourceContainer.containsKey(resource.getOccupier())==false){
            return;
        }
        //Check if the occupier of the resource is the process that is asking for the resource. That is our deadLock case
        else if(resource.getOccupier()==process){
            
            System.out.print("DEADLOCK DETECTED: ");
            printDeadLockCauses();
            System.exit(0);
        }
        //If haven't found deadlock, loop through each resource connected and call this method recursively 
        else{

            //look in the hashmap, where does the occupier of that resource points to? so we can recursivley check that resource
            int point = processPointToResourceContainer.get(resource.getOccupier());
            //change the resource paramter
            //now it is the resource being pointed to by the previous resource occupier
            resource = resourcesContainer.get(point);
            //call deadlock again
            checkDeadLock(process,resource);
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

/**
 * A class object that represents a resource.
 */
class Resource{
    private  int id;//Resource ID
    private  int occupier=-1;   //which process occup it, initlliay -1 means no one.
    private  Queue <Integer> waiting;    //Queue to add the processes waiting for the resource to get empty
    
    /***
     * Constructor to create the resource object, by given it its id and create a waiting list for it.
     * @param resource  The resource number given through a file
     */
    public Resource(int resource){
        this.id=resource;                   //create the resource
        this.waiting=new LinkedList<>();    //create a linekd list for it
    }
    /**
     * Return the resource Id
     */
    public int getId(){
        return id;
    }
    /***
     * Return who occupies this resource
     */
    public int getOccupier(){
        return occupier;
    }
    /***
     * update the process that occupies the resource
     * @param process   The process number that occupies the resource
     */
    public void setOccupier(int process){
        occupier=process;
    }
    /***
     * A method that returns the queue of processes for the resource
     * @return  A queue of processes
     */
    public Queue<Integer> getQueue(){
        return waiting;
    }
    /**
     * A method to enqueue in the gueue of a resource
     * @param process   The process that is waiting on the resource to be freed
     */
    public void enqueueProcess(int process){
        waiting.add(process);
    }
    /**
     * A method to dequeue from the queue of resources
     */
    public int dequeueProcess(){
        return waiting.remove();
    }

}

