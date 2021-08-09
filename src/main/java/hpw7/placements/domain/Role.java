package hpw7.placements.domain;

/**
 * Valid roles for users of the application. Users can only have one of these roles. Used for determining various
 * permissions and access rights. (e.g. only admins can access the users list)
 */
public enum Role {
    STUDENT,
    GRADUATE,
    TUTOR,
    PROVIDER,
    ADMINISTRATOR
}
