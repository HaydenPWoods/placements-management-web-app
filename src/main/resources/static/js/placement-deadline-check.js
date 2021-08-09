/*
Checks a given application deadline when creating a placement, and checks that it has not already been surpassed.

Relies on jQuery and submit-button-status.js - ensure these scripts are declared before this one.
 */
$('#applicationDeadline').on('change', function () {
    // Get page elements for displaying/hiding error messages
    let applicationDeadlineError = document.getElementById('applicationDeadlineError');
    let applicationDeadlineValidationSpacer = document.getElementById('applicationDeadlineValidationSpacer');
    let applicationDeadlineValidationAlert = document.getElementById('applicationDeadlineValidationAlert');
    let applicationDeadlineValidationMessage = document.getElementById('applicationDeadlineValidationMessage');

    // Hide any errors shown for previous function calls, and remove their 'state' (danger, warning, etc.)
    applicationDeadlineValidationAlert.hidden = true;
    applicationDeadlineValidationSpacer.hidden = true;
    applicationDeadlineValidationAlert.classList.remove('alert-danger');

    // Get value to check
    let applicationDeadline = new Date(document.getElementById('applicationDeadline').value);

    if (applicationDeadline < Date.now()) {
        // Application deadline has already been surpassed, reject
        applicationDeadlineValidationMessage.innerText = "The chosen application deadline has already been surpassed!";
        applicationDeadlineValidationAlert.classList.add('alert-danger');
        applicationDeadlineValidationSpacer.hidden = false;
        applicationDeadlineValidationAlert.hidden = false;
        applicationDeadlineError.innerText = "True";
    } else {
        // Date is after today, ok
        applicationDeadlineError.innerText = "False";
    }

    // Determine whether the form submission button should be enabled, based on whether any fields have errors.
    // See: submit-button-status.js
    submitButtonStatus();
})