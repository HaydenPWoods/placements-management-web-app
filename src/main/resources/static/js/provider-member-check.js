/*
Checks a username given when assigning a new member to a provider, and performs an AJAX request to check whether the
user exists, and if so, whether they have the correct role (PROVIDER) and whether they are already a member of another
provider.

Relies on jQuery - ensure this script is declared before this one.
 */
$("#username, #addMemberButton").on("change click", function () {
    let usernameValue = $("#username").val(); // Get the value to check
    let token = $("meta[name='_csrf']").attr("content"); // Get the CSRF token to use for the AJAX request

    // Get page elements for displaying/hiding error messages
    let usernameValidationSpacer = document.getElementById("usernameValidationSpacer");
    let usernameValidationAlert = document.getElementById("usernameValidationAlert");
    let usernameValidationMessage = document.getElementById("usernameValidationMessage");

    // Hide any errors shown for previous function calls, and remove their 'state' (danger, warning, etc.)
    usernameValidationAlert.hidden = true;
    usernameValidationSpacer.hidden = true;
    usernameValidationAlert.classList.remove("alert-danger", "alert-success", "alert-info", "alert-warning");

    // Perform AJAX request, and react to returned result.
    $.ajax({
        type: "POST",
        url: "/ajax/provider-member-check",
        headers: {
            "X-CSRF-TOKEN": token
        },
        data: encodeURIComponent(usernameValue), // Encoding username string to ensure special characters are not lost
        success: function (data) {
            // SUCCESS: some string response has been returned. Get the response.
            let dataString = data.toString();

            if (dataString === "Ok") {
                // Ok - given user has the 'PROVIDER' role, and is not already a member of another provider.
                // Do nothing.
            } else if (dataString === "Not found") {
                // User wasn't found with the given username - show error.
                usernameValidationMessage.innerText = "Selected user was not found!";
                usernameValidationAlert.classList.add("alert-danger");
                usernameValidationSpacer.hidden = false;
                usernameValidationAlert.hidden = false;
            } else if (dataString.startsWith("MEMBER: ")) {
                // User exists and has the correct role, but is a member of another provider. Show warning that
                // assigning this user to the new provider will remove them from the previous one.
                usernameValidationMessage.innerText = "The selected user is already a member of " + dataString.substr(8) +
                    " - adding them to the selected provider will remove them from their old one.";
                usernameValidationAlert.classList.add("alert-warning");
                usernameValidationSpacer.hidden = false;
                usernameValidationAlert.hidden = false;
            } else {
                // Unexpected response - cannot check whether the user exists, etc, so assume no error - any
                // problems will be picked up by the controller method when the form is submitted.
                usernameValidationMessage.innerText = "Couldn't check user status (unexpected return value)";
                usernameValidationAlert.classList.add("alert-info");
                usernameValidationSpacer.hidden = false;
                usernameValidationAlert.hidden = false;
            }
        },
        error: function (data) {
            // ERROR: something went wrong with the AJAX call, log to console. Cannot check whether the user exists,
            // etc.
            console.error(data);
        }
    })
})