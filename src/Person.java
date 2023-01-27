import java.io.Serializable;
import java.util.Random;

public class Person implements Serializable {
    private int originalFloor;
    private int destinationFloor;

    public Person(int nFloors) {
        Random random = new Random();
        int origin = random.nextInt(nFloors);
        int destination = random.nextInt(nFloors);
        // Just makes sure the destination isn't the same floor as the origin.
        while(origin == destination){
            destination = random.nextInt(nFloors);
        }

        this.originalFloor = origin;
        this.destinationFloor = destination;
    }

    //For debugging. Creates a person defined start and finish floores
    public Person(int start, int finish) {
        this.originalFloor = start;
        this.destinationFloor = finish;
    }

    public int getDestinationFloor() {
        return this.destinationFloor;
    }

    public int getOriginalFloor() {
        return this.originalFloor;
    }
}
