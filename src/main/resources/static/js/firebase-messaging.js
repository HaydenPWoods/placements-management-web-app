/*
This script attempts to register the Firebase Messaging service worker (firebase-messaging-sw.js), prompting the user to
enable notifications and registering the service worker when enabled. This script also handles the receiving of
notifications while the application is in an active browser tab - conversation pages can then be dynamically updated
where appropriate.

Relies on jQuery and firebase-config.js - ensure these scripts are declared before this one.
 */

// Initialising Firebase
firebase.initializeApp(firebaseConfig);
const messaging = firebase.messaging();

// Attempt to get the device token - will prompt the user to enable notifications if not enabled already.
messaging.getToken({
    vapidKey: vapidKey
}).then((currentToken) => {
    if (currentToken) {
        // A device token was retrieved - send in AJAX request to store against the user in the database.
        let csrfToken = $("meta[name='_csrf']").attr("content"); // Get the CSRF token to use for the AJAX request
        $.ajax({
            type: "POST",
            url: "/messaging/token-store",
            headers: {
                "X-CSRF-TOKEN": csrfToken
            },
            data: JSON.stringify(currentToken),
            success: function (data) {
                // SUCCESS: we can assume the token has reached the server, and is probably stored if there are no issues.
            },
            error: function (data) {
                // ERROR: something went wrong with the AJAX request, log error to console.
                console.error(data);
            }
        })
    } else {
        // Device token was not retrieved - notifications permission has been denied.
        const currentPage = new URL(window.location.href);
        if (currentPage.pathname.startsWith('/messaging')) {
            // Show alert to enable notifications for 'best experience'
            let enableNotificationsAlert = document.getElementById('enableNotificationsAlert');
            enableNotificationsAlert.removeAttribute('hidden');
        }
    }
}).catch((error) => {
    // There was an error while retrieving the device token - this will happen on Chrome with a self-signed certificate,
    // since it is not properly trusted. This can be bypassed following the instructions given in the javadoc for the
    // sendNewMessageNotification() method of the NotificationService class.
    const currentPage = new URL(window.location.href);
    if (currentPage.pathname.startsWith('/messaging')) {
        // Show alert to enable notifications for 'best experience'
        let enableNotificationsAlert = document.getElementById('enableNotificationsAlert');
        enableNotificationsAlert.removeAttribute('hidden');
    }
});

/*
When a notification is received while the application is in an active browser tab, this is called. If we're on the
conversation page where the notification is relevant, the page can be dynamically updated to add the new message or
delete any message.
 */
messaging.onMessage((payload) => {
    const currentPage = new URL(window.location.href);
    if (payload['notification']['title'].startsWith('Message deleted from conversation with user')) {
        // Notification for message deleted
        const senderUsername = payload['notification']['title'].substring(44);

        if (currentPage.pathname === '/messaging/user/' + senderUsername) {
            // On the relevant conversation page, check if reply is being constructed
            let reply = document.getElementById('content').value;

            if (reply.length > 0) {
                // Reply being constructed, show update message.
                let conversationUpdateAlert = document.getElementById('conversationUpdateAlert');
                conversationUpdateAlert.removeAttribute('hidden');
            } else {
                // No reply being constructed, just reload the page.
                location.reload();
            }
        }
    } else {
        // Notification for new message
        const senderUsername = payload['notification']['title'].substring(26);

        if (currentPage.pathname === '/messaging/user/' + senderUsername && (currentPage.searchParams.get('page') == null || currentPage.searchParams.get('page') === '1')) {
            // On the relevant conversation page, append message to bottom of conversation history (the latest message)
            let tableBody = document.getElementById('messageRows');

            while (tableBody.childElementCount >= 10) {
                // Remove older messages from the top where more than 10 messages on the page
                tableBody.removeChild(tableBody.firstChild);
            }

            // Message Row
            let messageRow = document.createElement('tr');

            // Sender Cell
            let thSender = document.createElement('th');
            thSender.setAttribute('scope', 'row');
            thSender.innerText = senderUsername;

            // Message Cell
            let tdContent = document.createElement('td');

            // Message contents in Message Cell
            let tdContentSpan = document.createElement('span');
            tdContentSpan.innerText = payload['notification']['body'] + ' ';

            // Timestamp in Message Cell (in same style as generated when template parsing)
            let currentDate = new Date();
            let tdContentDateStamp = document.createElement('span');
            tdContentDateStamp.classList.add('font-weight-lighter', 'font-italic', 'date-stamp');
            tdContentDateStamp.innerText = currentDate.getFullYear() + '-' +
                (currentDate.getMonth() + 1) + '-' +
                currentDate.getDate() + ', ' +
                currentDate.getHours() + ':' +
                currentDate.getMinutes() + ':' +
                currentDate.getSeconds();

            tdContent.appendChild(tdContentSpan);
            tdContent.appendChild(tdContentDateStamp);

            // Button Cell (only for spacing, since recipient != sender)
            let tdButtonSpace = document.createElement('td');

            messageRow.appendChild(thSender);
            messageRow.appendChild(tdContent);
            messageRow.appendChild(tdButtonSpace);

            tableBody.insertAdjacentElement('beforeend', messageRow);
        } else {
            // Not on the relevant conversation page - build and show notification
            let notificationOptions = {
                body: payload['notification']['body']
            }
            new Notification(payload['notification']['title'], notificationOptions);
        }
    }

});