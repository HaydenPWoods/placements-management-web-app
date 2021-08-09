/*
Checks that the given placement start and end date are not an 'impossible' combination - i.e. the end date shouldn't be
before the start date.

Relies on jQuery and submit-button-status.js - ensure these scripts are declared before this one.
 */
$('#startDate, #endDate').on('change', function () {
    // Get page elements for displaying/hiding error messages
    let placementDatesError = document.getElementById('placementDatesError');
    let placementDatesValidationSpacer = document.getElementById('placementDatesValidationSpacer');
    let placementDatesValidationAlert = document.getElementById('placementDatesValidationAlert');
    let placementDatesValidationMessage = document.getElementById('placementDatesValidationMessage');

    // Hide any errors shown for previous function calls, and remove their 'state' (danger, warning, etc.)
    placementDatesValidationAlert.hidden = true;
    placementDatesValidationSpacer.hidden = true;
    placementDatesValidationAlert.classList.remove('alert-danger');

    // Get current value of start and end dates
    let startDateValue = document.getElementById('startDate').value
    let endDateValue = document.getElementById('endDate').value;

    console.log('HERE');
    // Only check if both start and end date have been given
    if (startDateValue !== '' && endDateValue !== '') {
        // Parse to date values
        let startDate = new Date(startDateValue);
        let endDate = new Date(endDateValue);

        if (startDate > endDate) {
            // Start date is after end date, invalid, reject
            placementDatesValidationMessage.innerText = "The start date cannot be after the end date!";
            placementDatesValidationAlert.classList.add('alert-danger');
            placementDatesValidationSpacer.hidden = false;
            placementDatesValidationAlert.hidden = false;
            placementDatesError.innerText = "True";
        } else {
            // Ok
            placementDatesError.innerText = "False";
        }
    }

    // Determine whether the form submission button should be enabled, based on whether any fields have errors.
    // See: submit-button-status.js
    submitButtonStatus();
})