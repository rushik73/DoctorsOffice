
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
// Main class for the Doctor's Office simulation
public class DoctorsOffice {
    // Define maximum clinic capacity and initialize semaphores for managing clinic flow
    private static final int MAX_CLINIC_CAPACITY = 15;
    private static Semaphore maxClinicCapacity = new Semaphore(MAX_CLINIC_CAPACITY);
    private static Semaphore waitingRoom = new Semaphore(0);
    private static Semaphore registrationDesk = new Semaphore(0); // Initially, no patient is ready to register
    private static Semaphore registrationComplete = new Semaphore(0); // Semaphore to signal registration completion
    private static Semaphore[] doctorOffices; // Semaphores for each doctor's office availability
    private static Semaphore[] nurseAvailable; // Semaphores to signal nurse availability for each doctor
    private static Semaphore[] patientDone; // Semaphores to signal when a patient's visit is complete
    private static AtomicInteger patientsServed = new AtomicInteger(0); // Counter for served patients
    private static int patientCount; // Total number of patients to be served

    // Main method to start the simulation
    public static void main(String[] args) {
        // Validate input arguments for number of doctors and patients
        if (args.length != 2) {
            System.err.println("Usage: java Project2 <number of doctors> <number of patients>");
            System.exit(1);
        }
        // Parse command-line arguments for number of doctors and patients
        int doctorCount = Integer.parseInt(args[0]);
        patientCount = Integer.parseInt(args[1]);
        // Initialize arrays for semaphores based on number of doctors and patients
        doctorOffices = new Semaphore[doctorCount];
        nurseAvailable = new Semaphore[doctorCount];
        patientDone = new Semaphore[patientCount];
        // Set up semaphores for each doctor's office and nurse availability
        for (int i = 0; i < doctorCount; i++) {
            doctorOffices[i] = new Semaphore(0); // Doctor's office initially occupied
            nurseAvailable[i] = new Semaphore(0); // Nurse initially unavailable
        }
        // Initialize patient done semaphores
        for (int i = 0; i < patientCount; i++) {
            patientDone[i] = new Semaphore(0); // Patient visit initially incomplete
        }
        // Print the simulation start message with numbers of patients, nurses, and doctors
        System.out.println("Run with " + patientCount + " patients, " + doctorCount + " nurses, " + doctorCount + " doctors");
        // Start the Receptionist thread
        new Thread(new Receptionist()).start();
        // Start threads for each doctor
        for (int i = 0; i < doctorCount; i++) {
            new Thread(new Doctor(i)).start();
        }
        // Start threads for each nurse
        for (int i = 0; i < doctorCount; i++) {
            new Thread(new Nurse(i)).start();
        }
        // Start threads for each patient
        for (int i = 0; i < patientCount; i++) {
            new Thread(new Patient(i)).start();
        }
        // Wait for all patients to be served before printing simulation completion message
        while (patientsServed.get() < patientCount) {
            try {
                Thread.sleep(100); // Check for completion periodically
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Simulation complete");
    }

    // Receptionist class representing the receptionist thread in the simulation
    static class Receptionist implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    registrationDesk.acquire(); // Wait for a patient to be ready for registration
                    System.out.println("Receptionist registers patient");
                    Thread.sleep(100); // Simulate time taken for registration
                    registrationComplete.release(); // Signal that registration is complete
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Doctor class representing each doctor thread in the simulation
    static class Doctor implements Runnable {
        private int id; // Doctor's ID

        Doctor(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    nurseAvailable[id].acquire(); // Wait for a nurse to indicate a patient is ready
                    System.out.println("Doctor " + id + " listens to symptoms from patient " + id);
                    Thread.sleep(100); // Simulate consultation time
                    System.out.println("Patient " + id + " receives advice from doctor " + id);
                    patientDone[patientsServed.getAndIncrement()].release(); // Mark the patient's visit as complete
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Nurse class representing each nurse thread in the simulation
    static class Nurse implements Runnable {
        private int id; // Nurse's ID

        Nurse(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    waitingRoom.acquire(); // Wait for a patient to enter the waiting room
                    System.out.println("Nurse " + id + " takes patient to doctor's office");
                    doctorOffices[id].release(); // Indicate the doctor's office is now occupied
                    nurseAvailable[id].release(); // Signal the doctor that a patient is ready
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Patient class representing each patient thread in the simulation
    static class Patient implements Runnable {
        private int id; // Patient's ID

        Patient(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                maxClinicCapacity.acquire(); // Ensure the clinic is not over capacity
                System.out.println("Patient " + id + " enters waiting room, waits for receptionist");
                registrationDesk.release(); // Signal readiness for registration
                registrationComplete.acquire(); // Wait for registration to complete
                System.out.println("Patient " + id + " completes registration and sits in waiting room");
                waitingRoom.release(); // Enter the waiting room
                patientDone[id].acquire(); // Wait for the visit to complete
                System.out.println("Patient " + id + " leaves");
                maxClinicCapacity.release(); // Leave the clinic, making space for new patients
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}