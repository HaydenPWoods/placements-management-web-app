/*
Simple function to allow table cells to be clickable, redirecting the user to the url defined with the href attribute of
the cell.
 */
$('.clickable').click(function () {
    window.location = $(this).attr('href');
})