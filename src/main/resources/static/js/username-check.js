/*
Checks a username given at registration, and performs an AJAX request to check if the username is already in use by
another user.

Relies on jQuery and submit-button-status.js - ensure these scripts are declared before this one.
 */
$("#username").on("input", function () {
    let usernameValue = $("#username").val(); // Get the value to check
    let token = $("meta[name='_csrf']").attr("content"); // Get the CSRF token to use for the AJAX request

    // Get page elements for displaying/hiding error messages
    let usernameValidationSpacer = document.getElementById("usernameValidationSpacer");
    let usernameValidationAlert = document.getElementById("usernameValidationAlert");
    let usernameValidationMessage = document.getElementById("usernameValidationMessage");
    let usernameError = document.getElementById("usernameError");

    // Hide any errors shown for previous function calls, and remove their 'state' (danger, warning, etc.)
    usernameValidationAlert.hidden = true;
    usernameValidationSpacer.hidden = true;
    usernameValidationAlert.classList.remove("alert-danger", "alert-success", "alert-info");

    if (usernameValue.length === 0) {
        // No username specified - no need to check.
        usernameValidationSpacer.hidden = true;
        usernameValidationAlert.hidden = true;
        usernameError.innerText = "True"; // Either way, form cannot be submitted while username is blank
    } else {
        // Perform AJAX request, and react to response.
        $.ajax({
            type: "POST",
            url: "/ajax/username-check",
            headers: {
                "X-CSRF-TOKEN": token
            },
            data: encodeURIComponent(usernameValue), // Encoding username string to ensure special characters are not lost
            success: function (data) {
                // SUCCESS: some string response has been returned. Get the response.
                let dataString = data.toString();

                if (dataString === "Ok") {
                    // Username is acceptable, not in use
                    usernameError.innerText = "False";
                } else if (dataString === "Exists") {
                    // Username is already in use by another user - show alert and record error.
                    usernameValidationMessage.innerText = "Username has been taken!";
                    usernameValidationAlert.classList.add("alert-danger");
                    usernameValidationSpacer.hidden = false;
                    usernameValidationAlert.hidden = false;
                    usernameError.innerText = "True";
                } else {
                    // Unexpected response - cannot check if username is in use, so assume no error. If there is a
                    // problem, this will be picked up by the controller method when the form is submitted.
                    usernameValidationMessage.innerText = "Couldn't check if username has been taken (unexpected return value)";
                    usernameValidationAlert.classList.add("alert-info");
                    usernameValidationSpacer.hidden = false;
                    usernameValidationAlert.hidden = false;
                    usernameError.innerText = "False";
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