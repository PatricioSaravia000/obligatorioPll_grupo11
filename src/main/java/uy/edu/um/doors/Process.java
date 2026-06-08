package uy.edu.um.doors;

import uy.edu.um.tad.list.MyLinkedListImpl;
import uy.edu.um.tad.list.MyList;
//
public class Process implements Comparable<Process> {

    private int pid;
    private String name;
    private User owner;
    private int priority;
    private ProcessState state;
    private MyList<Event> events;

    private FinishType finishType;
    private User terminatedBy;

    public Process(int pid, String name, User owner) {
        this.pid = pid;
        this.name = name;
        this.owner = owner;
        this.state = ProcessState.NEW;
        this.events = new MyLinkedListImpl<>();
        this.priority = 0;
        this.finishType = null;
        this.terminatedBy = null;
    }

    public void addEvent(Event event) {
        events.add(event);
    }

    public MyList<Event> getEvents() {
        return events;
    }

    public void calculatePriority() {
        int nCpu = 0, nRam = 0, nDisk = 0;
        int nEvents = events.size();

        for (int i = 0; i < nEvents; i++) {
            Event e = events.get(i);
            switch (e.getType()) {
                case CPU:  nCpu++;  break;
                case RAM:  nRam++;  break;
                case DISK: nDisk++; break;
            }
        }

        if (nEvents == 0) {
            this.priority = 0;
            return;
        }

        int numerator = 8 * nCpu + 2 * nRam + 2 * nDisk;
        this.priority = (numerator / nEvents) + owner.getWeight() * nEvents;
    }

    @Override
    public int compareTo(Process other) {
        return Integer.compare(this.priority, other.priority);
    }

    public int getPid() { return pid; }
    public String getName() { return name; }
    public User getOwner() { return owner; }
    public int getPriority() { return priority; }
    public ProcessState getState() { return state; }
    public void setState(ProcessState state) { this.state = state; }
    public FinishType getFinishType() { return finishType; }
    public void setFinishType(FinishType finishType) { this.finishType = finishType; }
    public User getTerminatedBy() { return terminatedBy; }
    public void setTerminatedBy(User terminatedBy) { this.terminatedBy = terminatedBy; }
}
