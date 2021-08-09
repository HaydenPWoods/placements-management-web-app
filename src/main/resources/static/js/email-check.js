/*
Checks the email provided by the user (for example, at registration), and performs an AJAX request to check whether a
user already exists with the given email. If so, we can show an error and disable form submission.

Relies on jQuery and submit-button-status.js - ensure these scripts are declared before this one.
*/
$("#email").on("input", function () {
    let emailValue = $("#email").val(); // Get the value to check
    let token = $("meta[name='_csrf']").attr("content"); // Get the CSRF token to use for the AJAX request

    // Get page elements for displaying/hiding error messages
    let emailValidationSpacer = document.getElementById("emailValidationSpacer");
    let emailValidationAlert = document.getElementById("emailValidationAlert");
    let emailValidationMessage = document.getElementById("emailValidationMessage");
    let emailError = document.getElementById("emailError");

    // Hide any errors shown for previous function calls, and remove their 'state' (danger, warning, etc.)
    emailValidationAlert.hidden = true;
    emailValidationSpacer.hidden = true;
    emailValidationAlert.classList.remove("alert-danger", "alert-success", "alert-info");

    if (emailValue.length === 0) {
        // No email specified - no need to check.
        emailError.innerText = "True"; // Either way, form cannot be submitted while email is blank
    } else {
        // Perform AJAX request, and react to returned result.
        $.ajax({
            type: "POST",
            url: "/ajax/email-check",
            headers: {
                "X-CSRF-TOKEN": token
            },
            data: encodeURIComponent(emailValue), // Encoding email string to ensure special characters are not lost
            success: function (data) {
                // SUCCESS: some string response has been returned. Get the response.
                let dataString = data.toString();

                if (dataString === "Ok") {
                    // No error - the email is not already in use.
                    emailError.innerText = "False";
                } else if (dataString === "Exists") {
                    // A user exists with the given email - show alert and record error.
                    emailValidationMessage.innerText = "Specified email has already been used";
                    emailValidationAlert.classList.add("alert-danger");
                    emailValidationSpacer.hidden = false;
                    emailValidationAlert.hidden = false;
                    emailError.innerText = "True";
                } else {
                    // Unexpected response - cannot determine if the email is used, so assume no error. If the email
                    // has actually been used, this will be picked up by the controller method when the form is
                    // submitted.
                    emailValidationMessage.innerText = "Couldn't check if email has been used (unexpected return value)";
                    emailValidationAlert.classList.add("alert-info");
                    emailValidationSpacer.hidden = false;
                    emailValidationAlert.hidden = false;
                    emailError.innerText = "False";
                }

                // Determine whether the form submission button should be enabled, based on whether any fields have errors.
                // See: submit-button-status.js
                submitButtonStatus();
            },
            error: function (data) {
                // ERROR: something went wrong with the AJAX call, log to console. Cannot determine if the email is
                // used.
                console.error(data);
            }
        })
    }
})