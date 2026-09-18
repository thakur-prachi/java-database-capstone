package com.project.back_end.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

@org.springframework.stereotype.Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final Service service;
    private final TokenService tokenService;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentService(AppointmentRepository appointmentRepository, Service service, TokenService tokenService,
                               PatientRepository patientRepository, DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.service = service;
        this.tokenService = tokenService;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    @Transactional
    public int bookAppointment(Appointment appointment) {
        try {
            appointmentRepository.save(appointment);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment) {
        Map<String, String> response = new HashMap<>();
        Optional<Appointment> existingOpt = appointmentRepository.findById(appointment.getId());

        if (existingOpt.isEmpty()) {
            response.put("message", "Appointment not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        Appointment existing = existingOpt.get();
        if (!existing.getPatient().getId().equals(appointment.getPatient().getId())) {
            response.put("message", "Patient ID mismatch");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        int valid = service.validateAppointment(appointment);
        if (valid == -1) {
            response.put("message", "Doctor does not exist");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } else if (valid == 0) {
            response.put("message", "Requested appointment time is unavailable");
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        appointmentRepository.save(appointment);
        response.put("message", "Appointment updated successfully");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        Map<String, String> response = new HashMap<>();
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(id);

        if (appointmentOpt.isEmpty()) {
            response.put("message", "Appointment not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        Appointment appointment = appointmentOpt.get();
        String email = tokenService.extractIdentifier(token);
        Patient patient = patientRepository.findByEmail(email);

        if (patient == null || !appointment.getPatient().getId().equals(patient.getId())) {
            response.put("message", "Unauthorized to cancel this appointment");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        appointmentRepository.delete(appointment);
        response.put("message", "Appointment cancelled successfully");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Transactional
    public Map<String, Object> getAppointment(String pname, LocalDate date, String token) {
        Map<String, Object> result = new HashMap<>();
        String email = tokenService.extractIdentifier(token);
        Doctor doctor = doctorRepository.findByEmail(email);

        if (doctor == null) {
            result.put("message", "Doctor not found");
            return result;
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        List<Appointment> appointments;

        if (pname == null || pname.equalsIgnoreCase("null") || pname.isBlank()) {
            appointments = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctor.getId(), start, end);
        } else {
            appointments = appointmentRepository.findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(doctor.getId(), pname, start, end);
        }

        List<AppointmentDTO> dtoList = new ArrayList<>();
        for (Appointment a : appointments) {
            dtoList.add(new AppointmentDTO(
                    a.getId(),
                    a.getDoctor().getId(),
                    a.getDoctor().getName(),
                    a.getPatient().getId(),
                    a.getPatient().getName(),
                    a.getPatient().getEmail(),
                    a.getPatient().getPhone(),
                    a.getPatient().getAddress(),
                    a.getAppointmentTime(),
                    a.getStatus()
            ));
        }

        result.put("appointments", dtoList);
        return result;
    }

    @Transactional
    public void changeStatus(long id, int status) {
        appointmentRepository.updateStatus(status, id);
    }
}
// 1. **Add @Service Annotation**:
//    - To indicate that this class is a service layer class for handling business logic.
//    - The `@Service` annotation should be added before the class declaration to mark it as a Spring service component.
//    - Instruction: Add `@Service` above the class definition.

// 2. **Constructor Injection for Dependencies**:
//    - The `AppointmentService` class requires several dependencies like `AppointmentRepository`, `Service`, `TokenService`, `PatientRepository`, and `DoctorRepository`.
//    - These dependencies should be injected through the constructor.
//    - Instruction: Ensure constructor injection is used for proper dependency management in Spring.

// 3. **Add @Transactional Annotation for Methods that Modify Database**:
//    - The methods that modify or update the database should be annotated with `@Transactional` to ensure atomicity and consistency of the operations.
//    - Instruction: Add the `@Transactional` annotation above methods that interact with the database, especially those modifying data.

// 4. **Book Appointment Method**:
//    - Responsible for saving the new appointment to the database.
//    - If the save operation fails, it returns `0`; otherwise, it returns `1`.
//    - Instruction: Ensure that the method handles any exceptions and returns an appropriate result code.

// 5. **Update Appointment Method**:
//    - This method is used to update an existing appointment based on its ID.
//    - It validates whether the patient ID matches, checks if the appointment is available for updating, and ensures that the doctor is available at the specified time.
//    - If the update is successful, it saves the appointment; otherwise, it returns an appropriate error message.
//    - Instruction: Ensure proper validation and error handling is included for appointment updates.

// 6. **Cancel Appointment Method**:
//    - This method cancels an appointment by deleting it from the database.
//    - It ensures the patient who owns the appointment is trying to cancel it and handles possible errors.
//    - Instruction: Make sure that the method checks for the patient ID match before deleting the appointment.

// 7. **Get Appointments Method**:
//    - This method retrieves a list of appointments for a specific doctor on a particular day, optionally filtered by the patient's name.
//    - It uses `@Transactional` to ensure that database operations are consistent and handled in a single transaction.
//    - Instruction: Ensure the correct use of transaction boundaries, especially when querying the database for appointments.

// 8. **Change Status Method**:
//    - This method updates the status of an appointment by changing its value in the database.
//    - It should be annotated with `@Transactional` to ensure the operation is executed in a single transaction.
//    - Instruction: Add `@Transactional` before this method to ensure atomicity when updating appointment status.


