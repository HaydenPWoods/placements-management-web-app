/*
Checks a given visit date when scheduling a visit, and compares it with the start and end date of the placement the
visit is being scheduled for to check whether it is within these boundaries and, hence, is a valid date for a visit.

Relies on jQuery and submit-button-status.js - ensure these scripts are declared before this one.
 */
$('#visitDate').on('change', function () {
    // Get page elements for displaying/hiding error messages
    let visitDateError = document.getElementById('visitDateError');
    let visitDateValidationSpacer = document.getElementById('visitDateValidationSpacer');
    let visitDateValidationAlert = document.getElementById('visitDateValidationAlert');
    let visitDateValidationMessage = document.getElementById('visitDateValidationMessage');

    // Hide any errors shown for previous function calls, and remove their 'state' (danger, warning, etc.)
    visitDateValidationAlert.hidden = true;
    visitDateValidationSpacer.hidden = true;
    visitDateValidationAlert.classList.remove("alert-danger");

    let visitDate = new Date(document.getElementById('visitDate').value); // Get the value to check

    // Comparing with the placement start and end date
    let placementStartDate = new Date(document.getElementById('placementStartDate').innerText);
    let placementEndDate = new Date(document.getElementById('placementEndDate').innerText);

    if (visitDate < placementStartDate) {
        // Visit date is before placement start date
        visitDateValidationMessage.innerText = "The chosen visit date is before the placement start date!";
        visitDateValidationAlert.classList.add("alert-danger");
        visitDateValidationSpacer.hidden = false;
        visitDateValidationAlert.hidden = false;
        visitDateError.innerText = "True";
    } else if (visitDate > placementEndDate) {
        // Visit date is after placement end date
        visitDateValidationMessage.innerText = "The chosen visit date is after the placement end date!";
        visitDateValidationAlert.classList.add("alert-danger");
        visitDateValidationSpacer.hidden = false;
        visitDateValidationAlert.hidden = false;
        visitDateError.innerText = "True";
    } else {
        // Ok - within the placement start and end dates.
        visitDateError.innerText = 'False';
    }

    // Determine whether the form submission button should be enabled, based on whether any fields have errors.
    // See: submit-button-status.js
    submitButtonStatus();
})