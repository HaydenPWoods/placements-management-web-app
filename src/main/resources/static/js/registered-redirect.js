/*
Simple script to redirect to the login page after successful registration.
 */

window.setInterval(loginRedirect, 3000); // 3 seconds

function loginRedirect() {
    window.location = '/';
}