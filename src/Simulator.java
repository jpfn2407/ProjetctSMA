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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Simulator extends Agent {

    private static int nFloors = 10; // Number of floors in the building
    private static int nElevators = 1; // Number of elevators to simulate

    private ArrayList<AID> elevatorAgents = new ArrayList<>();
    private HashMap<AID, Integer> elevatorAgentsCurrentCapacity = new HashMap<>();

    private ArrayList<Person> tasksQueue = new ArrayList<>();
    private boolean isSimulating = false;

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
                // Update the list of agents
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("Elevator");
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    for (int i = 0; i < result.length; ++i) {
                        if (!elevatorAgents.contains(result[i].getName())) {
                            //elevatorAgents.remove(result[i].getName().getLocalName());
                            elevatorAgents.add(result[i].getName());
                            elevatorAgentsCurrentCapacity.put(result[i].getName(), 0);
                        }
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
                //System.out.println(elevatorAgents);
            }
        });

        this.isSimulating = true;

        //Gera novas pessoas, e ao mesmo tempo pede aos elevadores que informem quantas tasks têm
        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                Person person = new Person(nFloors);
                //System.out.println(person.getOriginalFloor() + " to " + person.getDestinationFloor());

                //Manda mensagem a todos os elevator agents
                ACLMessage newMsg = new ACLMessage(ACLMessage.QUERY_IF);
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd2 = new ServiceDescription();
                sd2.setType("Elevator");
                template.addServices(sd2);
                for (int i = 0; i < elevatorAgents.size(); ++i) {
                    //System.out.println(result[i].getName());
                    newMsg.addReceiver(elevatorAgents.get(i));
                }
                newMsg.setContent("n_of_tasks");
                send(newMsg);

                tasksQueue.add(person);
                //System.out.println(queue);
            }
        });


        //Está sempre à espera de mensagens a informar quantas tasks cada elevador tem
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
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
        });

        //Está sempre a atribuir tasks ao elevador com menos tasks
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                Person person = new Person(nFloors);
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

        });


    }

}
