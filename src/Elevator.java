import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.util.ArrayList;
import java.util.ListIterator;

public class Elevator extends Agent {
    private static int maxCapacity = 3; // Number of people the elevators can handle.
    private static int speed = 1000; // Time in milseconds that it takes for the elevator to change floors.
    private ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    private int currentFloor = 0; // What floor the elevator is currently in.
    private boolean stopped = true; // If it's stopped in a floor.
    private ArrayList<Person> currentLoad = new ArrayList<>(); // Current people in the elevator.
    private ArrayList<Person> tasks = new ArrayList<>(); // Stores all the people that want to use the elevator here.

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " agent started.");
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Elevator");
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        Behaviour askNumberOfTasks = new Behaviour() {
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    if (msg.getContent().equals("n_of_tasks")) {
                        ACLMessage newMsg = new ACLMessage(ACLMessage.INFORM);
                        newMsg.addReceiver(msg.getSender());
                        newMsg.setContent(String.valueOf(tasks.size()) + " n_of_tasks");
                        send(newMsg);
                    }
                }
            }

            @Override
            public boolean done() {
                return false;
            }
        };

        Behaviour receiveRequests = new CyclicBehaviour() {
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    Person person = null;
                    try {
                        person = (Person) msg.getContentObject();
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                        System.out.println(tasks);
                    }
                    tasks.add(person);
                    System.out.println(this + " " + tasks);
                }
            }

        };



        Behaviour moveLogic = new Behaviour() {
            public void action() {

                // If there's no one in the elevator, and there are tasks,
                // it will move to the floor for the first taks in the list.
                if (currentLoad.size()==0 && tasks.size() > 0) {
                    int destinationFloor = tasks.get(0).getOriginalFloor();
                    System.out.println("Going to " + destinationFloor);

                    // Moves the elevator up or down
                    //moveToFloor(destinationFloor);

                    addBehaviour(new TickerBehaviour(this.myAgent, speed) {
                                     @Override
                                     protected void onTick() {
                                         if (destinationFloor > currentFloor) {

                                                 currentFloor += 1;
                                                 System.out.println("Current floor " + currentFloor);


                                         } else if (destinationFloor < currentFloor) {


                                                 currentFloor -= 1;
                                                 System.out.println("Current floor " + currentFloor);

                                         }
                                         else if(destinationFloor == currentFloor){
                                             done();
                                         }

                                     }
                                 }
                    );


                }


                // Does the first taks inside the elevator.
                /*if(!currentLoad.isEmpty()) {
                    int destinationFloor = currentLoad.get(0).getDestinationFloor();
                    //moveToFloor(destinationFloor);
                    if (destinationFloor > currentFloor) {
                        while (currentFloor != destinationFloor) {

                            //Thread.sleep(speed);
                            block(speed);

                            currentFloor += 1;
                            System.out.println("Current floor " + currentFloor);
                        }

                    } else if (destinationFloor < currentFloor) {
                        while (currentFloor != destinationFloor) {
                            block(speed);
                            currentFloor -= 1;
                            System.out.println("Current floor " + currentFloor);
                        }
                    }

                    currentLoad.remove(0);
                    //Checks if there are more people inside the elevator that will get out in this same floor.
                    currentLoad.removeIf(currentPerson -> currentPerson.getDestinationFloor() == currentFloor);
                    System.out.println("Finished unloading. Current load " + currentLoad);


                }*/

            }


            @Override
            public boolean done() {
                return false;
            }

        };


        tbf.wrap(askNumberOfTasks);
        addBehaviour(askNumberOfTasks);

        tbf.wrap(receiveRequests);
        addBehaviour(receiveRequests);

        tbf.wrap(moveLogic);
        addBehaviour(moveLogic);

    }

    private void moveToFloor(int floor){
        if (floor > currentFloor) {
            while (currentFloor != floor) {
                try {
                    Thread.sleep(speed);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                currentFloor += 1;
                System.out.println("Current floor " + currentFloor);
            }

        } else if (floor < currentFloor) {
            while (currentFloor != floor) {
                try {
                    Thread.sleep(speed);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                currentFloor -= 1;
                System.out.println("Current floor " + currentFloor);
            }
        }
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println("Elevator agent " + getAID().getName() + " was taken down.");
    }
}
