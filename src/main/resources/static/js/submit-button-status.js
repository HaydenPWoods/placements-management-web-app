/*
Checks the status of form errors and determines whether the submission button should be disabled. The submission button
is disabled if at least one form field has an error.
 */
function submitButtonStatus() {
    // Get the current page location, remove any unique IDs from path to get a common path value
    const currentPage = new URL(window.location.href);
    const regex = new RegExp("\/[0-9]+\/*", 'g');
    let pathName = currentPage.pathname.replaceAll(regex, "/");

    let submitButton = document.getElementById("submitButton");
    let formInvalid = false;

    // Handle differently depending on the location/form and the errors in use
    switch (pathName) {
        case "/app-register": {
            let usernameError = document.getElementById("usernameError").innerText;
            let emailError = document.getElementById("emailError").innerText;
            let passwordError = document.getElementById("passwordError").innerText;
            let passwordStrengthError = document.getElementById("passwordStrengthError").innerText;
            formInvalid = (usernameError === "True" || emailError === "True" || passwordError === "True" || passwordStrengthError === "True");
            break;
        }
        case "/account/edit": {
            let emailError = document.getElementById("emailError").innerText;
            let passwordError = document.getElementById("passwordError").innerText;
            let passwordStrengthError = document.getElementById("passwordStrengthError").innerText;
            formInvalid = (emailError === "True" || passwordError === "True" || passwordStrengthError === "True");
            break;
        }
        case "/requests/new": {
            let tutorError = document.getElementById("tutorError").innerText;
            let filesError = document.getElementById("filesError").innerText;
            formInvalid = (tutorError === "True" || filesError === "True");
            break;
        }
        case "/requests/upload":
        case "/applications/upload":
        case "/placements/upload":
        case "/placements/apply": {
            let filesError = document.getElementById("filesError").innerText;
            formInvalid = (filesError === "True");
            break;
        }
        case "/placements/add": {
            let applicationDeadlineError = document.getElementById("applicationDeadlineError").innerText;
            let placementDatesError = document.getElementById("placementDatesError").innerText;
            let filesError = document.getElementById("filesError").innerText;
            formInvalid = (applicationDeadlineError === "True" || placementDatesError === "True" || filesError === "True");
            break;
        }
        case "/placements/visits/new":
        case "/placements/visits/edit" : {
            let visitDateError = document.getElementById("visitDateError").innerText;
            formInvalid = (visitDateError === "True");
            break;
        }
        default:
            // Page hasn't been set up for checking the submit button status - log error, consider form as valid.
            console.log("Current page not set up for checking form valid status");
            console.log(pathName);
    }

    if (formInvalid) {
        // There is at least one error - disable form submission.
        submitButton.setAttribute("disabled", "true");
    } else {
        // Form is OK - enable form submission.
        submitButton.removeAttribute("disabled");
    }
}