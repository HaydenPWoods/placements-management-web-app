/*
Checks that the files selected by the user when uploading documents for supported entities do not exceed the maximum
upload size accepted by the server. If the files are accepted, the script dynamically creates description fields for
each selected file - these are submitted with the form, and the values are used for the file descriptions.

Relies on jQuery and submit-button-status.js - ensure these scripts are declared before this one.
*/
$('#files').on('change', function () {
    // Get page elements for displaying/hiding error messages
    let filesError = document.getElementById('filesError');
    let filesValidationSpacer = document.getElementById("filesValidationSpacer");
    let filesValidationAlert = document.getElementById("filesValidationAlert");
    let filesValidationMessage = document.getElementById("filesValidationMessage");

    // Hide any errors shown for previous function calls, and remove their 'state' (danger, warning, etc.)
    filesValidationAlert.hidden = true;
    filesValidationSpacer.hidden = true;
    filesValidationAlert.classList.remove("alert-danger", "alert-success", "alert-info");

    // Remove any existing description form fields created for previous file selections
    while (document.getElementsByName("descriptionGroup").length > 0) {
        let descriptionGroups = document.getElementsByName("descriptionGroup");
        descriptionGroups.forEach(function (field) {
            field.remove();
        })
    }

    // Work out the total file size for all selected files
    let filesSize = 0;
    let filesElement = document.getElementById('files');
    for (let i = 0; i < filesElement.files.length; i++) {
        filesSize += filesElement.files.item(i).size;
    }

    if (filesSize / 1024 / 1024 >= 10) {
        // Selected files exceed 10MB total - the maximum upload limit. Reject and show error.
        filesValidationMessage.innerText = "The selected files exceed the maximum upload limit of 10MB!";
        filesValidationAlert.classList.add("alert-danger");
        filesValidationSpacer.hidden = false;
        filesValidationAlert.hidden = false;
        filesError.innerText = "True";
    } else {
        // Selected files are below the maximum upload limit - deemed OK. (If any of the files are of a blocked file
        // type, this is handled in the controller method and the files will get rejected.)
        filesError.innerText = "False";

        // Dynamically create description form fields for each selected file
        for (let i = filesElement.files.length - 1; i >= 0; i--) {
            // Create field element
            let descriptionField = document.createElement("input");
            descriptionField.setAttribute("type", "text");
            descriptionField.setAttribute("name", "description");
            descriptionField.required = true;
            descriptionField.classList.add("form-control");

            // Set label for field with file name
            let descriptionLabel = document.createElement("label");
            descriptionLabel.innerText = "Description for " + filesElement.files[i].name;

            // Append field to form
            let descriptionGroup = document.createElement("div");
            descriptionGroup.classList.add("form-group");
            descriptionGroup.setAttribute("name", "descriptionGroup");
            descriptionGroup.insertAdjacentElement("afterbegin", descriptionLabel);
            descriptionGroup.insertAdjacentElement("beforeend", descriptionField);
            filesElement.insertAdjacentElement("afterend", descriptionGroup);
        }
    }

    // Determine whether the form submission button should be enabled, based on whether any fields have errors.
    // See: submit-button-status.js
    submitButtonStatus();
})