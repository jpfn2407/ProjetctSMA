import jade.Boot;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.IOException;
import java.util.*;

public class Simulator extends Agent {
    //Simulator variables. Can change!
    private static int nFloors = 10; // Number of floors in the building
    private static int nElevators = 2; // Number of elevators to simulate
    private static int timeToSimulate = 30_000; // Time in mls to simulate. (1s = 1000mls)
    private static int timeBetweenPersonSpawm = 500; // Time in mls it takes to spawn in a new random person.

    //Internal logic variables. Don't change!
    private ArrayList<AID> elevatorAgents = new ArrayList<>(); // Stores all the elevator AIDs here.
    private HashMap<AID, Integer> elevatorAgentsCurrentCapacity = new HashMap<>(); // Stores the number of tasks each elevator has.
    private ArrayList<Person> tasksQueue = new ArrayList<>(); // Stores all tasks here before sending to each elevator.

    private HashMap<AID, ArrayList<Integer>> elevatorsStoppedStats = new HashMap<>(); // Stores if the elevator was stopped when asked.
    private HashMap<AID, ArrayList<Integer>> elevatorsLoadSizeStats = new HashMap<>(); // Stores the current load every time when asked.
    private HashMap<AID, ArrayList<Integer>> elevatorsTasksStats = new HashMap<>(); // Stores the number of people waiting every time when asked.

    private boolean isSimulating = true; // If the simulation is running or not.


    //Starts the simulator and generates elevators.
    public static void main(String[] args) {
        String[] services = {"-agents", "Simulator:Simulator;"};
        for (int i = 0; i < nElevators; i++) {
            services[1] = services[1].concat("Elevator" + (i + 1) + ":Elevator;");
        }
        Boot.main(services);
    }

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " agent started.");
        System.out.println("Simulation started ");

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Simulator");
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }


        //Searches and stashes agents of type "Elevator" in a HashMap
        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                if(isSimulating){
                    // Update the list of agents
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("Elevator");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        for (int i = 0; i < result.length; ++i) {
                            if (!elevatorAgents.contains(result[i].getName())) {
                                // Adds the agent ID in the list
                                elevatorAgents.add(result[i].getName());

                                // Creates initial array lists and values
                                elevatorAgentsCurrentCapacity.put(result[i].getName(), 0);
                                elevatorsStoppedStats.put(result[i].getName(), new ArrayList<Integer>());
                                elevatorsLoadSizeStats.put(result[i].getName(), new ArrayList<Integer>());
                                elevatorsTasksStats.put(result[i].getName(), new ArrayList<Integer>());
                            }
                        }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }
                    //System.out.println(elevatorAgents);
                }
            }
        });

        //Generates new peoeple and at the same time asks every elevator the number of tasks that they currently have.
        addBehaviour(new TickerBehaviour(this, timeBetweenPersonSpawm) {
            @Override
            protected void onTick() {
                if(isSimulating){
                    Person person = new Person(nFloors);

                    //Queries all elevators for their number of tasks.
                    ACLMessage newMsg = new ACLMessage(ACLMessage.QUERY_IF);
                    for (int i = 0; i < elevatorAgents.size(); ++i) {
                        //System.out.println(result[i].getName());
                        newMsg.addReceiver(elevatorAgents.get(i));
                    }
                    newMsg.setContent("n_of_tasks");
                    send(newMsg);

                    tasksQueue.add(person);
                }
            }
        });


        //Always listening for the elevators to update how many tasks they have.
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                if (isSimulating){
                    MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        if (msg.getContent().contains(" n_of_tasks")) {
                            String recivedString = msg.getContent().replace(" n_of_tasks", "");
                            Integer quantity = Integer.valueOf(recivedString);
                            elevatorAgentsCurrentCapacity.put(msg.getSender(), quantity);
                        }
                    }
                }
            }
        });

        //Sends tasks to the elevator with the least ammount of tasks.
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                if (isSimulating){
                    if(elevatorAgents.size() > 0 && elevatorAgentsCurrentCapacity.size()>0 && tasksQueue.size()>0){
                        //Person person = new Person(nFloors);
                        Person person = tasksQueue.remove(0);
                        AID elevatorAID = Collections.min(elevatorAgentsCurrentCapacity.entrySet(), Map.Entry.comparingByValue()).getKey();
                        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);

                        try {
                            msg.setContentObject(person);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        msg.addReceiver(elevatorAID);
                        send(msg);
                    }
                }
            }
        });


        // Asks each elevator their statistics every half second.
        addBehaviour(new TickerBehaviour(this, 500) {
            @Override
            protected void onTick() {
                if(isSimulating){
                    //Queries all elevators for their stats.
                    ACLMessage newMsg = new ACLMessage(ACLMessage.QUERY_REF);
                    for (int i = 0; i < elevatorAgents.size(); ++i) {
                        newMsg.addReceiver(elevatorAgents.get(i));
                    }
                    newMsg.setContent("statistics");
                    send(newMsg);
                }
            }
        });

        //Always listening for the elevators to update their statistics.
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                if (isSimulating){
                    MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF);
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        if (msg.getContent().contains("statistics ")) {
                            String recivedString = msg.getContent().replace("statistics ", "");
                            //Integer quantity = Integer.valueOf(recivedString);
                            //elevatorAgentsCurrentCapacity.put(msg.getSender(), quantity);
                            String[] splitArray = recivedString.split(" ");
                            elevatorsStoppedStats.get(msg.getSender()).add(Integer.parseInt(splitArray[0]));
                            elevatorsLoadSizeStats.get(msg.getSender()).add(Integer.parseInt(splitArray[1]));
                            elevatorsTasksStats.get(msg.getSender()).add(Integer.parseInt(splitArray[2]));
                        }
                    }
                }
            }
        });

        //Says for the simulation to end, and process statistics
        addBehaviour(new TickerBehaviour(this, timeToSimulate) {
            @Override
            protected void onTick() {
                isSimulating = false;

                //Asks all the elevators to stop
                ACLMessage newMsg = new ACLMessage(ACLMessage.QUERY_IF);
                for (int i = 0; i < elevatorAgents.size(); ++i) {
                    newMsg.addReceiver(elevatorAgents.get(i));
                }
                newMsg.setContent("finish");
                send(newMsg);

                for(AID aid : elevatorAgents){
                    double stopPercentage =  (Double.valueOf(Collections.frequency(elevatorsStoppedStats.get(aid), 1)) / Double.valueOf(elevatorsStoppedStats.get(aid).size())) * 100 ;
                    double loadSize = elevatorsLoadSizeStats.get(aid).stream().mapToDouble(d -> d).average().orElse(0.0);
                    double tasksSize = elevatorsTasksStats.get(aid).stream().mapToDouble(d -> d).average().orElse(0.0);
                    System.out.println("===================== " + aid.getLocalName() + " statistics ==================");
                    System.out.println("Percentage of times it was stopped (waiting for people): " + stopPercentage + "%");
                    System.out.println("Average load inside this elevator: " + loadSize);
                    System.out.println("Average number of people waiting for this elevator: " + tasksSize);
                }

            }
        });




    }

}
