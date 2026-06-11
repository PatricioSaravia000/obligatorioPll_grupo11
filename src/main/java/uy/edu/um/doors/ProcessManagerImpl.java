package uy.edu.um.doors;

import uy.edu.um.tad.hash.MyHash;
import uy.edu.um.tad.hash.MyHashImpl;
import uy.edu.um.tad.heap.MyHeapImpl;
import uy.edu.um.tad.heap.MyHeap;
import uy.edu.um.tad.list.MyLinkedListImpl;
import uy.edu.um.tad.list.MyList;
import uy.edu.um.tad.queue.MyQueue;
import uy.edu.um.tad.queue.MyQueueImpl;
import uy.edu.um.tad.stack.MyStack;
import uy.edu.um.tad.stack.MyStackImpl;

public class ProcessManagerImpl implements ProcessManager {

    private MyQueue<Process> newProcesses;
    private MyHeap<Process> pendingProcesses;
    private Process currentProcess;
    private MyStack<Process> finishedProcesses;
    private MyHash<Integer, User> users;
    private MyHash<Integer, Process> allProcesses;

    public ProcessManagerImpl() {
        this.newProcesses = new MyQueueImpl<>();
        this.pendingProcesses = new MyHeapImpl<>(false); // false asi es el max-heap
        this.finishedProcesses = new MyStackImpl<>();
        this.users = new MyHashImpl<>();
        this.allProcesses = new MyHashImpl<>();
        this.currentProcess = null;
    }

    @Override
    public void loadProcessAndUserData(String processCsvPath, String usersCsvPath) {

        // cargar usuarios
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(usersCsvPath))) {
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(";");
                int uid = Integer.parseInt(parts[0]);
                String alias = parts[1];
                UserType type = UserType.valueOf(parts[2]);
                User user = new User(uid, alias, type);
                users.put(uid, user);
            }
        } catch (Exception e) {
            System.out.println("Error leyendo usuarios: " + e.getMessage());
            return;
        }

        // cargar procesos

        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(processCsvPath))) {
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                int firstSemi = line.indexOf(';');
                int secondSemi = line.indexOf(';', firstSemi + 1);
                int thirdSemi = line.indexOf(';', secondSemi + 1);

                int pid = Integer.parseInt(line.substring(0, firstSemi));
                int uid = Integer.parseInt(line.substring(firstSemi + 1, secondSemi));
                String name = line.substring(secondSemi + 1, thirdSemi);
                String eventsRaw = line.substring(thirdSemi + 1);

                User owner = users.get(uid); // fix: era ==
                if (owner == null) {
                    System.out.println("Usuario no encontrado uid=" + uid + ", pid=" + pid + " ignorado.");
                    continue;
                }


                // desarmar el formato del csv de los procesos

                Process process = new Process(pid, name, owner);

                eventsRaw = eventsRaw.trim();
                eventsRaw = eventsRaw.substring(1, eventsRaw.length() - 1);
                String[] eventParts = eventsRaw.split("#");

                for (String eventStr : eventParts) {
                    eventStr = eventStr.trim();
                    int bracket = eventStr.indexOf('[');
                    String typePart = eventStr.substring(0, bracket).trim().replace(":", "");
                    EventType eventType = EventType.valueOf(typePart);
                    Event event = new Event(eventType);

                    String instrPart = eventStr.substring(bracket + 1, eventStr.length() - 1);
                    String[] instructions = instrPart.split(",");
                    for (String instr : instructions) {
                        event.addInstruction(instr.trim());
                    }
                    process.addEvent(event);
                }

                newProcesses.enqueue(process);
                allProcesses.put(pid, process);
            }
            System.out.println("Datos cargados correctamente.");

        } catch (Exception e) {
            System.out.println("Error leyendo procesos: " + e.getMessage());
        }

    }

    @Override
    public void prepareProcesses() {

        while (!newProcesses.isEmpty()) {

            try {

                // sacando el proceso de la queue y lo agrego a pendientes

                Process process = newProcesses.dequeue();
                process.calculatePriority();
                process.setState(ProcessState.PENDING);
                pendingProcesses.insert(process);

                // este log es temporario hasta que creemos el archivo

                System.out.println("[" + getTimestamp() + "]: NEW PENDING PROCESS: PID ="
                        + process.getPid() + " | " + process.getName()
                        + " | " + process.getOwner()
                        + " | P =" + process.getPriority());



            } catch (Exception e) {

                System.out.println("Error preparando el proceso: " + e.getMessage());

            }

        }



    }

    @Override
    public void executeNextProcess() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void finishProcessOk() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void finishProcessError() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void terminateProcess(int uid) {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatus() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatusVerbose() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatusByUser(int uid) {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatusByProcess(int pid) {
        System.out.println("IMPLEMENTAR");
    }


    // metodo para calcular timestamp

    private String getTimestamp() {

        return java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    }
}
