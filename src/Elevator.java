import jade.core.Agent;
import jade.core.behaviours.*;
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
    //Elevator variables. Can change!
    private static int maxCapacity = 4; // Number of people the elevators can handle.
    private static int speed = 10000; // Time in milseconds that it takes for the elevator to change floors.

    //Strings used for the final state machine.
    private static final String EMPTY_LOGIC = "EMPTY_LOGIC";
    private static final String MOVE_ELEVATOR_LOGIC = "MOVE_ELEVATOR_LOGIC";
    private static final String MOVE_PEOPLE_LOGIC = "MOVE_PEOPLE_LOGIC";

    //For threading agents
    private ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();

    //Internal logic variables. Don't change!
    private ArrayList<Person> currentLoad = new ArrayList<>(); // Current people in the elevator.
    private ArrayList<Person> tasks = new ArrayList<>(); // Stores all the people that want to use the elevator here.
    private int currentFloor = 0; // What floor the elevator is currently in.
    private int destinationFloor = 0; // What floor the elevator is currently in.
    private int stopped = 1; // If it's stopped in a floor. This is an Integer because it's easier to answer the simulator (0 = false; 1 = true)


    @Override
    protected void setup() {
        //Setup the elevator.
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

        //Final state machine for all logic
        FSMBehaviour fsm = new FSMBehaviour(this);

        //Final state machine states regestry
        fsm.registerFirstState(new whenEmptyLogic(this), EMPTY_LOGIC);
        fsm.registerState(new moveElevatorLogic(this), MOVE_ELEVATOR_LOGIC);
        //fsm.registerState(new moveElevatorLogic(this, speed), MOVE_ELEVATOR_LOGIC);
        fsm.registerState(new movePeopleLogic(this), MOVE_PEOPLE_LOGIC);

        //Final state machine transition orders.
        fsm.registerTransition(EMPTY_LOGIC, MOVE_ELEVATOR_LOGIC, 0);
        fsm.registerTransition(MOVE_ELEVATOR_LOGIC, MOVE_ELEVATOR_LOGIC, 1);
        fsm.registerTransition(MOVE_ELEVATOR_LOGIC, MOVE_PEOPLE_LOGIC, 0);
        fsm.registerTransition(MOVE_PEOPLE_LOGIC, MOVE_ELEVATOR_LOGIC, 0);
        fsm.registerTransition(MOVE_PEOPLE_LOGIC, EMPTY_LOGIC, 1);

        //Answers with the number of tasks every time hes queried about the number of tasks.
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
                    if (msg.getContent().equals("finish")) {
                        takeDown();
                    }
                }
            }

            @Override
            public boolean done() {
                return false;
            }
        };

        // Accepts a new task and adds it to the global list of tasks.
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
                    //System.out.println(this + " " + tasks);
                }
            }
        };

        // Answers the simulator about this elevator current statistics
        Behaviour informStatistics = new CyclicBehaviour() {
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF);
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    if (msg.getContent().equals("statistics")) {
                        ACLMessage newMsg = new ACLMessage(ACLMessage.QUERY_REF);
                        newMsg.addReceiver(msg.getSender());

                        newMsg.setContent("statistics " +
                                stopped +
                                " " + currentLoad.size() +
                                " " + tasks.size());
                        send(newMsg);
                    }
                }
            }
        };

        tbf.wrap(askNumberOfTasks);
        addBehaviour(askNumberOfTasks);

        tbf.wrap(receiveRequests);
        addBehaviour(receiveRequests);

        tbf.wrap(informStatistics);
        addBehaviour(informStatistics);

        tbf.wrap(fsm);
        addBehaviour(fsm);


    }

    protected void takeDown() {
        this.tbf.interrupt();
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            //fe.printStackTrace();
        }
        System.out.println("Elevator agent " + getAID().getName() + " was taken down.");
    }

    private class moveElevatorLogic extends Behaviour {

        private boolean finished = false;
        private int exitValue = 0;
        private long wakeUpTime;

        public moveElevatorLogic(Agent a) {
            super(a);
            this.wakeUpTime = System.currentTimeMillis() + speed;

        }

        @Override
        public void action() {
            long currentTime = System.currentTimeMillis();


            while()

            if (destinationFloor > currentFloor && this.wakeUpTime <= currentTime) {
                currentFloor += 1;
                System.out.println("Current floor " + currentFloor);
                //block(speed);
                this.exitValue = 1;
                finished = true;

            } else if (destinationFloor < currentFloor && this.wakeUpTime <= System.currentTimeMillis()) {
                currentFloor -= 1;
                System.out.println("Current floor " + currentFloor);
                //block(speed);
                this.exitValue = 1;
                finished = true;

            } else if (destinationFloor == currentFloor && this.wakeUpTime <= System.currentTimeMillis()) {
                System.out.println("cheguei");
                this.exitValue = 0;
                finished = true;
            }

        }

        @Override
        public boolean done() {
            return finished;
        }

        @Override
        public int onEnd() {
            finished = false;
            return exitValue;
        }
    }

/*    private class moveElevatorLogic extends TickerBehaviour{

        private boolean finished = false;
        private int exitValue = 0;

        public moveElevatorLogic(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            if (destinationFloor > currentFloor) {
                currentFloor += 1;
                System.out.println("Current floor " + currentFloor);
                this.exitValue = 1;
                this.finished = true;

            } else if (destinationFloor < currentFloor) {
                currentFloor -= 1;
                System.out.println("Current floor " + currentFloor);
                this.exitValue = 1;
                this.finished = true;


            } else if (destinationFloor == currentFloor) {
                System.out.println("cheguei");
                this.exitValue = 0;
                this.finished = true;

            }
        }



        @Override
        public int onEnd() {
            this.finished = false;
            return exitValue;
        }

        @Override
        public void setFixedPeriod(boolean fixedPeriod) {
            super.setFixedPeriod(true);
        }
    }*/



    private class whenEmptyLogic extends Behaviour {

        private boolean finished = false;
        private int exitValue = 0;

        public whenEmptyLogic(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            stopped = 1;
            if (currentLoad.size() == 0 && tasks.size() > 0) {
                stopped = 0;
                destinationFloor = tasks.get(0).getOriginalFloor();

                System.out.println("Going to " + destinationFloor);

                this.finished = true;
                this.exitValue = 0;
            }
        }

        @Override
        public boolean done() {
            return finished;
        }

        @Override
        public int onEnd() {
            finished = false;
            return exitValue;
        }
    }

    private class movePeopleLogic extends Behaviour {

        private boolean finished = false;
        private int exitValue = 0;

        public movePeopleLogic(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            //System.out.println("Current load: " + currentLoad);
            //Gets the max ammount of people in that same floor
            ListIterator<Person> iter = tasks.listIterator();
            while(iter.hasNext()){
                Person person = iter.next();
                if(currentLoad.size() == maxCapacity) break;
                else if(person.getOriginalFloor() == currentFloor){
                    currentLoad.add(person);
                    iter.remove();
                }
            }

            //Checks if anyone wants to get out in that floor.
            if (!currentLoad.isEmpty()){
                currentLoad.removeIf(person -> person.getDestinationFloor() == currentFloor);
            }
            //If theres still people inside after the removel, will get the destiation floor of the first person in the queue.
            if (!currentLoad.isEmpty()){
                destinationFloor = currentLoad.get(0).getDestinationFloor();
            }


            System.out.println("Current load after swaps: " + currentLoad);
            System.out.println("Going to " + destinationFloor);

            this.exitValue = 0;
            //If it's empty, goes to the emptyLogic, if not, will just move floors.
            if (currentLoad.isEmpty()) {
                this.exitValue = 1;
            }

            this.finished = true;

        }

        @Override
        public boolean done() {
            return finished;
        }

        @Override
        public int onEnd() {
            finished = false;
            return exitValue;
        }
    }
}
