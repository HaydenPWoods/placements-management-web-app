/*
Checks that the given passwords in the 'password' and 'confirm password' fields of the registration form, and the form
to edit account details, match.

Relies on jQuery and submit-button-status.js - ensure these scripts are declared before this one.
 */
$('#password, #passwordConfirm').on("input", function () {
    // Get value of password and password confirm fields
    let passwordValue = $("#password").val();
    let passwordConfirmValue = $("#passwordConfirm").val();

    // Get page elements for displaying/hiding error messages
    let passwordValidationSpacer = document.getElementById("passwordValidationSpacer");
    let passwordValidationAlert = document.getElementById("passwordValidationAlert");
    let passwordValidationMessage = document.getElementById("passwordValidationMessage");
    let passwordError = document.getElementById("passwordError");

    // Hide any errors shown for previous function calls, and remove their 'state' (danger, warning, etc.)
    passwordValidationAlert.hidden = true;
    passwordValidationSpacer.hidden = true;
    passwordValidationAlert.classList.remove("alert-danger", "alert-success", "alert-info");

    if (passwordValue === passwordConfirmValue) {
        // Ok - the passwords match.
        passwordError.innerText = "False";
    } else {
        // The passwords don't match, reject and show error.
        passwordValidationMessage.innerText = "Passwords do not match!";
        passwordValidationAlert.classList.add("alert-danger");
        passwordValidationSpacer.hidden = false;
        passwordValidationAlert.hidden = false;
        passwordError.innerText = "True";
    }

    // Determine whether the form submission button should be enabled, based on whether any fields have errors.
    // See: submit-button-status.js
    submitButtonStatus();
})