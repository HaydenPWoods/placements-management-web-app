/*
Checks that the given password by a user meets the requirements for the defined password policy - at least 8 characters
long, with one alpha-numeric character and one special character.

Relies on jQuery and submit-button-status.js - ensure these scripts are declared before this one.
 */
$('#password').on("input", function () {
    // Get value of password field
    let passwordValue = $("#password").val();

    // Get page elements for displaying/hiding error messages
    let passwordStrengthValidationAlert = document.getElementById("passwordStrengthValidationAlert");
    let passwordStrengthValidationMessage = document.getElementById("passwordStrengthValidationMessage");
    let passwordStrengthError = document.getElementById("passwordStrengthError");

    // Hide any errors shown for previous function calls, and remove their 'state' (danger, warning, etc.)
    passwordStrengthValidationAlert.hidden = true;
    passwordStrengthValidationAlert.classList.remove("alert-danger", "alert-success", "alert-info");

    let valid = true;

    if (!/[A-z0-9]/.test(passwordValue)) {
        // Password does not contain an alpha-numeric character
        passwordStrengthValidationMessage.innerText = "Password must contain at least one alpha-numeric character!";
        valid = false;
    } else if (!/[^A-z0-9]/.test(passwordValue)) {
        // Password does not contain a special character
        passwordStrengthValidationMessage.innerText = "Password must contain at least one special character!";
        valid = false;
    } else if (passwordValue.length < 8) {
        // Password is not at least 8 characters long
        passwordStrengthValidationMessage.innerText = "Password must be at least 8 characters long!";
        valid = false;
    }

    if (valid) {
        // Password is OK.
        passwordStrengthError.innerText = "False";
    } else {
        // Password failed a check, show alert and record error.
        passwordStrengthValidationAlert.classList.add("alert-danger");
        passwordStrengthValidationAlert.hidden = false;
        passwordStrengthError.innerText = "True";
    }

    // Determine whether the form submission button should be enabled, based on whether any fields have errors.
    // See: submit-button-status.js
    submitButtonStatus();
})