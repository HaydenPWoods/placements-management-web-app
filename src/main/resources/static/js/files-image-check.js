/*
Checks the image file selected by the user when setting a profile picture or provider logo, and checks whether it is
suitable based on the file type and the file size.

Relies on jQuery - ensure this script is declared before this one.
 */
$('#imageFile').on('change', function () {
    // Get page elements for displaying/hiding error messages and enabling/disabling form submission
    let imageFileValidationSpacer = document.getElementById('imageFileValidationSpacer');
    let imageFileValidationAlert = document.getElementById('imageFileValidationAlert');
    let imageFileValidationMessage = document.getElementById('imageFileValidationMessage');
    let profilePictureSetSubmit = document.getElementById('profilePictureSetSubmit');

    // Hide any errors shown for previous function calls, and remove their 'state' (danger, warning, etc.)
    imageFileValidationAlert.hidden = true;
    imageFileValidationSpacer.hidden = true;
    imageFileValidationAlert.classList.remove("alert-danger", "alert-success", "alert-info");

    // Reset submission button status
    profilePictureSetSubmit.setAttribute('disabled', 'true');

    // Get file selection field to check the selected file
    let imageFile = document.getElementById('imageFile');

    if (imageFile.files[0].type !== 'image/png' &&
        imageFile.files[0].type !== 'image/jpeg' &&
        imageFile.files[0].type !== 'image/gif') {
        // Not a suitable image, disable form
        imageFileValidationMessage.innerText = 'The selected file is not of a valid file type: png, jpeg, gif'
        imageFileValidationAlert.classList.add('alert-danger');
        imageFileValidationSpacer.hidden = false;
        imageFileValidationAlert.hidden = false;
    } else if (imageFile.files[0].size / 1024 / 1024 >= 10) {
        // File exceeds 10MB (cannot be uploaded, will be blocked by server)
        imageFileValidationMessage.innerText = 'The selected file exceeds the maximum upload limit of 10MB!';
        imageFileValidationAlert.classList.add('alert-danger');
        imageFileValidationSpacer.hidden = false;
        imageFileValidationAlert.hidden = false;
    } else {
        // Ok - enable form submission.
        profilePictureSetSubmit.removeAttribute('disabled');
    }
})