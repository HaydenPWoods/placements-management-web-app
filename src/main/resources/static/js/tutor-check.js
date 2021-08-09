/*
Checks a username given for an assigned tutor when creating an authorisation request, and performs an AJAX request to
check that a user exists with the given username, and that they are a tutor (have the TUTOR role).

Relies on jQuery and submit-button-status.js - ensure these scripts are declared before this one.
 */

$("#tutorUsername").on("input", function () {
    let tutorUsernameValue = $("#tutorUsername").val(); // Get the value to check
    let token = $("meta[name='_csrf']").attr("content"); // Get the CSRF token to use for the AJAX request

    // Get page elements for displaying/hiding error messages
    let tutorUsernameValidationSpacer = document.getElementById("tutorUsernameValidationSpacer");
    let tutorUsernameValidationAlert = document.getElementById("tutorUsernameValidationAlert");
    let tutorUsernameValidationMessage = document.getElementById("tutorUsernameValidationMessage");
    let tutorError = document.getElementById("tutorError");

    // Hide any errors shown for previous function calls, and remove their 'state' (danger, warning, etc.)
    tutorUsernameValidationAlert.hidden = true;
    tutorUsernameValidationSpacer.hidden = true;
    tutorUsernameValidationAlert.classList.remove("alert-danger", "alert-success", "alert-info");

    if (tutorUsernameValue.length === 0) {
        // No username specified - no need to check.
        tutorUsernameValidationSpacer.hidden = true;
        tutorUsernameValidationAlert.hidden = true;
        tutorError.innerText = "True"; // Either way, form cannot be submitted while assigned tutor is blank
    } else {
        // Perform AJAX request, and react to response
        $.ajax({
            type: "POST",
            url: "/ajax/tutor-check",
            headers: {
                "X-CSRF-TOKEN": token
            },
            data: encodeURIComponent(tutorUsernameValue), // Encoding username string to ensure special characters are not lost
            success: function (data) {
                // SUCCESS: some string response has been returned. Get the response.
                let dataString = data.toString();

                if (dataString === "Is tutor") {
                    // A user exists with the given username, and they are a tutor - accept.
                    tutorUsernameValidationMessage.innerText = "Tutor was found!";
                    tutorUsernameValidationAlert.classList.add("alert-success");
                    tutorUsernameValidationSpacer.hidden = false;
                    tutorUsernameValidationAlert.hidden = false;
                    tutorError.innerText = "False";
                } else if (dataString === "Is not tutor") {
                    // A user exists with the given username, but they are not a tutor - show alert and record error.
                    tutorUsernameValidationMessage.innerText = "User was found, but is not a tutor";
                    tutorUsernameValidationAlert.classList.add("alert-danger");
                    tutorUsernameValidationSpacer.hidden = false;
                    tutorUsernameValidationAlert.hidden = false;
                    tutorError.innerText = "True";
                } else if (dataString === "Not found") {
                    // A user does not exist with the given username - show alert and record error.
                    tutorUsernameValidationMessage.innerText = "User was not found with given username";
                    tutorUsernameValidationAlert.classList.add("alert-danger");
                    tutorUsernameValidationSpacer.hidden = false;
                    tutorUsernameValidationAlert.hidden = false;
                    tutorError.innerText = "True";
                } else {
                    // Unexpected response - assume no error. If there is a problem, this will be picked up by the
                    // controller method when the form is submitted.
                    console.log("Unexpected return value: " + dataString);
                    tutorUsernameValidationMessage.innerText = "Couldn't check if user is tutor (unexpected return value)";
                    tutorUsernameValidationAlert.classList.add("alert-info");
                    tutorUsernameValidationSpacer.hidden = false;
                    tutorUsernameValidationAlert.hidden = false;
                    tutorError.innerText = "False";
                }

                // Determine whether the form submission button should be enabled, based on whether any fields have errors.
                // See: submit-button-status.js
                submitButtonStatus();
            },
            error: function (data) {
                // ERROR: something went wrong with the AJAX call, log to console.
                console.error(data);
            }
        })
    }
})
