# Smart Clinic Management System — Schema Design

This document defines the hybrid data storage design for the Smart Clinic
Management System: **MySQL** for structured, relational data and
**MongoDB** for flexible, document-based data.

---

## MySQL Database Design

### Table: patients
- id: INT, Primary Key, Auto Increment
- first_name: VARCHAR(50), Not Null
- last_name: VARCHAR(50), Not Null
- email: VARCHAR(100), Not Null, Unique
- phone: VARCHAR(15), Not Null, Unique
- password: VARCHAR(255), Not Null
- date_of_birth: DATE, Not Null
- gender: VARCHAR(10)
- address: VARCHAR(255)
- created_at: TIMESTAMP, Default CURRENT_TIMESTAMP

<!-- email and phone are unique since they double as login/contact
     identifiers used for appointment reminders. -->

### Table: doctors
- id: INT, Primary Key, Auto Increment
- first_name: VARCHAR(50), Not Null
- last_name: VARCHAR(50), Not Null
- email: VARCHAR(100), Not Null, Unique
- phone: VARCHAR(15), Not Null, Unique
- password: VARCHAR(255), Not Null
- specialization: VARCHAR(100), Not Null
- available_from: TIME, Not Null
- available_to: TIME, Not Null
- created_at: TIMESTAMP, Default CURRENT_TIMESTAMP

<!-- available_from/available_to model a simple daily availability window.
     If per-day or per-slot availability is needed later, this can be
     split into a separate doctor_schedule table without breaking the
     rest of the design. -->

### Table: appointments
- id: INT, Primary Key, Auto Increment
- doctor_id: INT, Foreign Key → doctors(id)
- patient_id: INT, Foreign Key → patients(id)
- appointment_time: DATETIME, Not Null
- status: INT (0 = Scheduled, 1 = Completed, 2 = Cancelled)
- notes: VARCHAR(500)
- created_at: TIMESTAMP, Default CURRENT_TIMESTAMP

<!-- Foreign keys use ON DELETE CASCADE: if a patient or doctor record is
     removed, their appointment history is removed with them, so no
     orphaned rows remain. A unique constraint on (doctor_id,
     appointment_time) could be added later to prevent double-booking a
     doctor at the same exact time. -->

### Table: admin
- id: INT, Primary Key, Auto Increment
- username: VARCHAR(50), Not Null, Unique
- password: VARCHAR(255), Not Null
- role: VARCHAR(20), Default 'STAFF'
- created_at: TIMESTAMP, Default CURRENT_TIMESTAMP

<!-- role distinguishes staff who manage day-to-day records from a
     super-admin who can manage other admin accounts. -->

### Table: payments
- id: INT, Primary Key, Auto Increment
- appointment_id: INT, Foreign Key → appointments(id), Unique
- amount: DECIMAL(10,2), Not Null
- status: INT (0 = Pending, 1 = Paid, 2 = Refunded)
- paid_at: TIMESTAMP

<!-- Kept relational rather than in MongoDB because payment records need
     strong consistency and are naturally tabular/transactional. -->

**Design decisions:**
- Prescriptions are *not* stored in MySQL — they live in MongoDB (see below)
  because the number of medications and attached files varies per
  prescription, which doesn't map cleanly to fixed columns.
- Deleting a patient or doctor cascades to their appointments, since an
  appointment without a valid patient/doctor no longer makes sense to keep.
- Overlapping appointments for the same doctor are prevented at the
  application layer (checked before insert), since MySQL constraints alone
  can't easily express "no overlapping time ranges."

---

## MongoDB Collection Design

### Collection: prescriptions

```json
{
  "_id": "ObjectId('64abc123456')",
  "patientId": 305,
  "patientName": "John Smith",
  "appointmentId": 51,
  "doctorId": 12,
  "medications": [
    {
      "name": "Paracetamol",
      "dosage": "500mg",
      "frequency": "Every 6 hours",
      "durationDays": 5
    },
    {
      "name": "Cetirizine",
      "dosage": "10mg",
      "frequency": "Once daily",
      "durationDays": 7
    }
  ],
  "doctorNotes": "Take with food. Follow up if fever persists beyond 3 days.",
  "refillCount": 2,
  "pharmacy": {
    "name": "Walgreens SF",
    "location": "Market Street"
  },
  "attachments": [
    {
      "type": "lab_report",
      "url": "https://storage.smartclinic.com/reports/305-cbc.pdf"
    }
  ],
  "createdAt": "2026-09-10T09:15:00Z"
}
```

**Design decisions:**
- Only `patientId`, `doctorId`, and `appointmentId` are stored as references
  (not the full patient/doctor objects) to avoid duplicating data that can
  change in MySQL — `patientName` is kept as a lightweight denormalized
  copy purely for quick display without a join/lookup.
- `medications` is an array so a prescription can list one or many drugs
  without needing a separate join table, unlike in a relational schema.
- `attachments` is optional and empty by default — some prescriptions have
  no lab reports or files attached.
- Because MongoDB is schema-flexible, new fields (e.g. `tags`,
  `insuranceClaimId`) can be added to future documents at any time without
  migrating older ones — existing documents simply won't have that field
  until updated.

---

## Summary

| Data | Store | Reason |
|---|---|---|
| Patients, doctors, appointments, admin, payments | MySQL | Fixed structure, strong relationships, transactional integrity |
| Prescriptions | MongoDB | Variable number of medications/attachments, evolving schema |
