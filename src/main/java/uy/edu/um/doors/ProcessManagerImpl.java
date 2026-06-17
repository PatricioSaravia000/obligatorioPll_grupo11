package uy.edu.um.doors;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import uy.edu.um.tad.hash.MyHash;
import uy.edu.um.tad.hash.MyHashImpl;
import uy.edu.um.tad.heap.EmptyHeapException;
import uy.edu.um.tad.heap.MyHeap;
import uy.edu.um.tad.heap.MyHeapImpl;
import uy.edu.um.tad.queue.MyQueue;
import uy.edu.um.tad.queue.MyQueueImpl;
import uy.edu.um.tad.stack.EmptyStackException;
import uy.edu.um.tad.stack.MyStack;
import uy.edu.um.tad.stack.MyStackImpl;

public class ProcessManagerImpl implements ProcessManager {

    private static final int max_finished_capacity = 10;

    private MyQueue<Process> newProcesses;
    private MyHeap<Process> pendingProcesses;
    private Process currentProcess;
    private MyStack<Process> finishedProcesses;
    private MyHash<Integer, User> users;
    private MyHash<Integer, Process> allProcesses;

    private BufferedWriter logWriter;
    private final DateTimeFormatter tsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ProcessManagerImpl() {
        this.newProcesses = new MyQueueImpl<>();
        this.pendingProcesses = new MyHeapImpl<>(false); // false asi es el max-heap
        this.finishedProcesses = new MyStackImpl<>();
        this.users = new MyHashImpl<>();
        this.allProcesses = new MyHashImpl<>();
        this.currentProcess = null;
        initLog();
    }

    private void initLog() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String filename = "DOORS_PROCESS_LOG_" + date;
        try {
            logWriter = new BufferedWriter(new FileWriter(filename, true));
        } catch (IOException e) {
            System.out.println("Error iniciando log: " + e.getMessage());
        }
    }

    private void log(String message) {
        String ts = LocalDateTime.now().format(tsFormatter);
        String line = "[" + ts + "]: " + message;
        System.out.println(line);
        if (logWriter != null) {
            try {
                logWriter.write(line);
                logWriter.newLine();
                logWriter.flush();
            } catch (IOException e) {
                System.out.println("Error escribiendo log: " + e.getMessage());
            }
        }
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

        if (newProcesses.isEmpty()) {
            System.out.println("No hay procesos nuevos para preparar.");
            return;
        }

        while (!newProcesses.isEmpty()) {
            Process p;
            try {
                p = newProcesses.dequeue();
            } catch (Exception e) {
                break;
            }
            p.calculatePriority();
            p.setState(ProcessState.PENDING);
            pendingProcesses.insert(p);

            log("NEW PENDING PROCESS: PID=" + p.getPid()
                    + " | " + p.getName()
                    + " | USER:" + p.getOwner().getAlias()
                    + " UID:" + p.getOwner().getUid()
                    + " | P=" + p.getPriority());
        }

    }

    @Override
    public void executeNextProcess() {
        
        if (currentProcess != null) {
            System.out.println("Ya hay un proceso en ejecución: PID=" + currentProcess.getPid());
            return;
        }

        if (pendingProcesses.isEmpty()) {
            System.out.println("No hay procesos pendientes para ejecutar.");
            return;
        }


        Process p = pendingProcesses.remove();

        p.setState(ProcessState.RUNNING);
        currentProcess = p;

        StringBuilder sb = new StringBuilder();
        sb.append("EXECUTING PROCESS: PID=").append(p.getPid())
            .append(" | USER:").append(p.getOwner().getAlias())
            .append(" UID:").append(p.getOwner().getUid());

        for (int i = 0; i < p.getEvents().size(); i++) {
            sb.append("\n").append(p.getEvents().get(i));
        }

        log(sb.toString());



    }

    @Override
    public void finishProcessOk() {
        finishProcess(FinishType.OK, null);
    }

    @Override
    public void finishProcessError() {
        finishProcess(FinishType.ERROR, null);
    }

    @Override
    public void terminateProcess(int uid) {

        if (currentProcess == null) {
            System.out.println("No hay proceso en ejecucion para terminar.");
            return;
        }

        User terminator = users.get(uid);

        if (terminator == null) {
            System.out.println("Usuario UID = " + uid + " no encontrado. No se puede terminar el proceso.");
            return;
        }

        finishProcess(FinishType.TERMINATED, terminator);

    }

    @Override
    public void printStatus() {
        
        StringBuilder sb = new StringBuilder();
        sb.append("PROCESS STATUS");

        sb.append("\nEXECUTING:");
        if (currentProcess != null) {
            sb.append("\n\t").append(formatBasic(currentProcess));
        }
    

        sb.append("\nPENDING:");
        int nPending = pendingProcesses.size();
        Process[] pendingArr = new Process[nPending];

        for (int i = 0; i < nPending; i++) {
            pendingArr[i] = pendingProcesses.remove();
        }

        for (int i = 0; i < nPending; i++) {
            sb.append("\n\t").append(formatBasic(pendingArr[i]));
            pendingProcesses.insert(pendingArr[i]);
        }

        sb.append("\nFINISHED:");
        int nFinished = finishedProcesses.size();
        Process[] finishedArr = new Process[nFinished];

        try {
            for (int i = 0; i < nPending; i++) {
            pendingArr[i] = pendingProcesses.remove();
            }
        } catch (EmptyHeapException e) {
            System.out.println("Error inesperado al recorrer pendientes: " + e.getMessage());
        }

        for (int i = 0; i < nFinished; i++) {
            sb.append("\n\t").append(formatFinished(finishedArr[i]));
        finishedProcesses.push(finishedArr[i]);
        }

        System.out.println(sb.toString());

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





      ///////////////////////////////////////////////////////////////
     //                 METODOS NUESTROS                      /////
    //////////////////////////////////////////////////////////////



    private String getTimestamp() {

        return java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    }

    private void finishProcess(FinishType type, User terminatedBy) {

        if (currentProcess == null) {
            System.out.println("No hay proceso en ejecucion para finalizar.");
            return;
        }

        Process p = currentProcess;

        p.setState(ProcessState.FINISHED);
        p.setFinishType(type);
        p.setTerminatedBy(terminatedBy);
        currentProcess = null;

        if (finishedProcesses.size() == max_finished_capacity) {

            StringBuilder sb = new StringBuilder();
            sb.append("Finished process stack overflow");

            int n = finishedProcesses.size();
            Process[] popped = new Process[n];

            try {
                for (int i = 0; i < n; i++) {
                    popped[i] = finishedProcesses.pop();
                }
            } catch (EmptyStackException e) {
                System.out.println("Error inesperado al vaciar stack de finalizados: " + e.getMessage());
            }

            for (int i = 0; i < n; i++) {
                sb.append("\n").append(formatFinished(popped[i]));
                finishedProcesses.push(popped[i]);
            }


        }
    }

    private String formatFinished(Process p) {

        return  "PID=" + p.getPid() + " " + p.getName()
                + " | STATE: " + p.getFinishType()
                + " | " + p.getOwner();

    }

    private String formatBasic(Process p) {
    return "PID=" + p.getPid() + " | " + p.getName()
            + " | " + p.getOwner()
            + " | P=" + p.getPriority();
    }


}
