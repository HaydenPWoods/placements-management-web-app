/*
Providing a fallback approach to checking for new messages if the user has not enabled web notifications. The function
periodically performs an AJAX call to check for new messages and attempts to reload the page to show any new messages.

Relies on jQuery - ensure this script is declared before this one.
 */

if (Notification.permission === 'denied' || Notification.permission === 'default') {
    // User hasn't enabled notifications, so we have to check for new messages manually
    var interval = window.setInterval(checkNewMessages, 30000);
}

function checkNewMessages() {
    let csrfToken = $("meta[name='_csrf']").attr('content'); // Get the CSRF token to use for the AJAX request

    // Perform AJAX request, and react to response
    $.ajax({
        type: "GET",
        url: window.location.href + '/check',
        headers: {
            "X-CSRF-TOKEN": csrfToken
        },
        success: function (data) {
            // SUCCESS: some string response has been returned.
            if (data.toString() === 'true') {
                // There is a new message in the last 30 seconds - check if a reply is being constructed.
                let reply = document.getElementById('content').value;

                if (reply.length > 0) {
                    // Reply is being constructed - cannot reload the page, since reply would be lost. Show alert.
                    let newMessageAlert = document.getElementById('newMessageAlert');
                    newMessageAlert.removeAttribute('hidden');
                    clearInterval(interval); // At least one new message, stop checking
                } else {
                    // No reply being constructed, just reload the page.
                    location.reload();
                }
            }
        },
        error: function (data) {
            // ERROR: something went wrong with the AJAX call, cannot check for new messages. Log to console.
            console.error(data);
        }
    })
}