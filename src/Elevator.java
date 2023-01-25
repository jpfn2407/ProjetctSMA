import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class Elevator extends Agent {

    private static int maxCapacity = 4; // Number of people the elevators can handle.
    private int currentFloor = 0;
    private ArrayList<Person> currentLoad = new ArrayList<>();
    private ArrayList<Person> tasks = new ArrayList<>();


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

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    if(msg.getContent().equals("n_of_tasks")){
                        ACLMessage newMsg = new ACLMessage(ACLMessage.INFORM);
                        newMsg.addReceiver(msg.getSender());
                        newMsg.setContent(String.valueOf(tasks.size()) + " n_of_tasks");
                        send(newMsg);
                    } else {
                        Person person = new Person(1);
                        try {
                            person = (Person) msg.getContentObject();
                        } catch (UnreadableException e) {
                            e.printStackTrace();
                        }
                        tasks.add(person);
                    }
                }
            }
        });

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
