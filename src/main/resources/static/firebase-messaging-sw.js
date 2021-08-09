/*
The service worker for messaging web notifications, registered if the user accepts to enable web notifications when
prompted as part of the firebase-messaging.js script. Allows notifications to be received and shown when the application
is not in an active browser tab.
 */

// Importing configuration values
importScripts('/firebase-config.js');

// Importing necessary Firebase scripts
importScripts('https://www.gstatic.com/firebasejs/8.2.4/firebase-app.js');
importScripts('https://www.gstatic.com/firebasejs/8.2.4/firebase-messaging.js');

// Initialising Firebase
firebase.initializeApp(firebaseConfig);
const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
    // A message was received - the Firebase app will generate and display a notification. Log to the console.
    console.log('FIREBASE MESSAGING SERVICE WORKER: Message received: ', payload);
})