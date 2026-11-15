import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Task 2 application logic: doctors by department (loaded ONCE into a map), validation and booking. */
public final class AppointmentService {
    public record Doctor(String id, String name, String department, List<String> slots) {
        @Override public String toString() { return name + " (" + id + ")"; }
    }
    public record Result(boolean ok, String message) { }

    private final Map<String, List<Doctor>> byDepartment = new LinkedHashMap<>();
    private final Set<String> booked = new HashSet<>();          // "D101@09:00"
    private int nextNo = 5001;

    public AppointmentService(Path csv) throws IOException {
        for (String l : Files.readAllLines(csv)) {
            if (l.isBlank() || l.startsWith("#")) continue;
            String[] p = l.split(",");
            byDepartment.computeIfAbsent(p[0], k -> new ArrayList<>())
                        .add(new Doctor(p[1], p[2], p[0], List.of(p[3].split(";"))));
        }
    }

    public List<String> departments() { return new ArrayList<>(byDepartment.keySet()); }

    /** O(1) map lookup - no database round trip when the department changes. */
    public List<Doctor> doctorsOf(String department) {
        return Collections.unmodifiableList(byDepartment.getOrDefault(department, List.of()));
    }

    public List<String> freeSlots(Doctor d) {
        List<String> free = new ArrayList<>();
        for (String s : d.slots()) if (!booked.contains(d.id() + "@" + s)) free.add(s);
        return free;
    }

    public Result book(String patientId, String department, Doctor doctor, String slot) {
        String id = patientId == null ? "" : patientId.trim().toUpperCase();
        if (id.isEmpty()) return new Result(false, "Enter the patient ID.");
        if (!id.matches("P\\d{6}")) return new Result(false, "Patient ID must be P followed by 6 digits (e.g. P104233).");
        if (department == null) return new Result(false, "Select a department.");
        if (doctor == null) return new Result(false, "Select a doctor.");
        if (!doctor.department().equals(department)) return new Result(false, "Doctor does not belong to " + department + ".");
        if (slot == null) return new Result(false, "Select a time slot.");
        if (!booked.add(doctor.id() + "@" + slot)) return new Result(false, slot + " with " + doctor.name() + " is already booked.");
        return new Result(true, "Booked APT-" + nextNo++ + ": " + id + " with " + doctor.name() + " (" + department + ") at " + slot + ".");
    }
}
