/*
Checks a provider name given when creating or editing a provider, and performs an AJAX request to check whether a
provider already exists with the given name, and shows an error if so.

Relies on jQuery - ensure this script is declared before this one.
 */
$("#name").on("input", function () {
    let nameValue = $("#name").val(); // Get the value to check
    let token = $("meta[name='_csrf']").attr("content"); // Get the CSRF token to use for the AJAX request

    // Get page elements for displaying/hiding error messages and enabling/disabling submission button.
    let nameValidationSpacer = document.getElementById("nameValidationSpacer");
    let nameValidationAlert = document.getElementById("nameValidationAlert");
    let nameValidationMessage = document.getElementById("nameValidationMessage");
    let submitButton = document.getElementById("submitButton");

    // Hide any errors shown for previous function calls, and remove their 'state' (danger, warning, etc.)
    nameValidationAlert.hidden = true;
    nameValidationSpacer.hidden = true;
    nameValidationAlert.classList.remove("alert-danger", "alert-success", "alert-info", "alert-warning");

    const currentPage = new URL(window.location.href); // Get current page

    if (nameValue.length > 0) {
        if (currentPage.pathname.includes('edit') && nameValue === document.getElementById("providerOriginalName").innerText) {
            // Editing provider name, value matches the original name so is acceptable.
            submitButton.removeAttribute("disabled");
        } else {
            // Perform AJAX request, and react to returned result.
            $.ajax({
                type: "POST",
                url: "/ajax/provider-name-check",
                headers: {
                    "X-CSRF-TOKEN": token
                },
                data: encodeURIComponent(nameValue), // Encoding provider name string to ensure special characters are not lost
                success: function (data) {
                    // SUCCESS: some string response has been returned. Get the response.
                    let dataString = data.toString();

                    if (dataString === "Ok") {
                        // Ok - a provider does not exist with the given name.
                        submitButton.removeAttribute("disabled");
                    } else if (dataString === "Exists") {
                        // A provider was found with the given name, show error and disable form submission.
                        nameValidationMessage.innerText = "A provider already exists with the given name!";
                        nameValidationAlert.classList.add("alert-danger");
                        nameValidationSpacer.hidden = false;
                        nameValidationAlert.hidden = false;
                        submitButton.setAttribute("disabled", "true");
                    } else {
                        // Unexpected response - cannot check if a provider exists with the given name, so assume no
                        // error - any problems will be picked up by the controller method when the form is submitted.
                        nameValidationMessage.innerText = "Couldn't check if name has been taken (unexpected return value)";
                        nameValidationAlert.classList.add("alert-info");
                        nameValidationSpacer.hidden = false;
                        nameValidationAlert.hidden = false;
                        submitButton.removeAttribute("disabled");
                    }
                },
                error: function (data) {
                    // ERROR: something went wrong with the AJAX call, log to console.
                    console.error(data);
                }
            })
        }
    }
})