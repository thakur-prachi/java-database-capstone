import { openModal } from "./components/modals.js";
import {
  getDoctors,
  filterDoctors,
  saveDoctor,
} from "./services/doctorServices.js";
import { createDoctorCard } from "./components/doctorCard.js";

document.addEventListener("DOMContentLoaded", () => {
  const addDocBtn = document.getElementById("addDocBtn");
  if (addDocBtn) {
    addDocBtn.addEventListener("click", () => {
      openModal("addDoctor");
    });
  }

  loadDoctorCards();

  const searchBar = document.getElementById("searchBar");
  if (searchBar) searchBar.addEventListener("input", filterDoctorsOnChange);

  const filterTime = document.getElementById("filterTime");
  if (filterTime) filterTime.addEventListener("change", filterDoctorsOnChange);

  const filterSpecialty = document.getElementById("filterSpecialty");
  if (filterSpecialty)
    filterSpecialty.addEventListener("change", filterDoctorsOnChange);
});

function loadDoctorCards() {
  getDoctors()
    .then((doctors) => {
      renderDoctorCards(doctors);
    })
    .catch((error) => {
      console.error("Failed to load doctors:", error);
    });
}

function filterDoctorsOnChange() {
  const searchBarValue = document.getElementById("searchBar").value.trim();
  const filterTimeValue = document.getElementById("filterTime").value;
  const filterSpecialtyValue = document.getElementById("filterSpecialty").value;

  const name = searchBarValue.length > 0 ? searchBarValue : null;
  const time = filterTimeValue.length > 0 ? filterTimeValue : null;
  const specialty =
    filterSpecialtyValue.length > 0 ? filterSpecialtyValue : null;

  filterDoctors(name, time, specialty)
    .then((response) => {
      const doctors = response.doctors;
      const contentDiv = document.getElementById("content");
      contentDiv.innerHTML = "";

      if (doctors && doctors.length > 0) {
        renderDoctorCards(doctors);
      } else {
        contentDiv.innerHTML =
          "<p>No doctors found with the given filters.</p>";
      }
    })
    .catch((error) => {
      console.error("Failed to filter doctors:", error);
      alert("An error occurred while filtering doctors.");
    });
}

function renderDoctorCards(doctors) {
  const contentDiv = document.getElementById("content");
  contentDiv.innerHTML = "";

  doctors.forEach((doctor) => {
    const card = createDoctorCard(doctor);
    contentDiv.appendChild(card);
  });
}

window.adminAddDoctor = async function () {
  try {
    const name = document.getElementById("doctorName").value;
    const specialty = document.getElementById("specialization").value;
    const email = document.getElementById("doctorEmail").value;
    const password = document.getElementById("doctorPassword").value;
    const phone = document.getElementById("doctorPhone").value;

    const checkboxes = document.querySelectorAll(
      'input[name="availability"]:checked',
    );
    const availableTimes = Array.from(checkboxes).map((cb) => cb.value);

    const token = localStorage.getItem("token");
    if (!token) {
      alert("You must be logged in as admin to add a doctor.");
      return;
    }

    const doctor = { name, specialty, email, password, phone, availableTimes };

    const { success, message } = await saveDoctor(doctor, token);

    if (success) {
      alert("Doctor added successfully.");
      document.getElementById("modal").style.display = "none";
      window.location.reload();
    } else {
      alert(message || "Failed to add doctor.");
    }
  } catch (error) {
    console.error("Error adding doctor:", error);
    alert("An error occurred while adding the doctor.");
  }
};

/*
  This script handles the admin dashboard functionality for managing doctors:
  - Loads all doctor cards
  - Filters doctors by name, time, or specialty
  - Adds a new doctor via modal form


  Attach a click listener to the "Add Doctor" button
  When clicked, it opens a modal form using openModal('addDoctor')


  When the DOM is fully loaded:
    - Call loadDoctorCards() to fetch and display all doctors


  Function: loadDoctorCards
  Purpose: Fetch all doctors and display them as cards

    Call getDoctors() from the service layer
    Clear the current content area
    For each doctor returned:
    - Create a doctor card using createDoctorCard()
    - Append it to the content div

    Handle any fetch errors by logging them


  Attach 'input' and 'change' event listeners to the search bar and filter dropdowns
  On any input change, call filterDoctorsOnChange()


  Function: filterDoctorsOnChange
  Purpose: Filter doctors based on name, available time, and specialty

    Read values from the search bar and filters
    Normalize empty values to null
    Call filterDoctors(name, time, specialty) from the service

    If doctors are found:
    - Render them using createDoctorCard()
    If no doctors match the filter:
    - Show a message: "No doctors found with the given filters."

    Catch and display any errors with an alert


  Function: renderDoctorCards
  Purpose: A helper function to render a list of doctors passed to it

    Clear the content area
    Loop through the doctors and append each card to the content area


  Function: adminAddDoctor
  Purpose: Collect form data and add a new doctor to the system

    Collect input values from the modal form
    - Includes name, email, phone, password, specialty, and available times

    Retrieve the authentication token from localStorage
    - If no token is found, show an alert and stop execution

    Build a doctor object with the form values

    Call saveDoctor(doctor, token) from the service

    If save is successful:
    - Show a success message
    - Close the modal and reload the page

    If saving fails, show an error message
*/
