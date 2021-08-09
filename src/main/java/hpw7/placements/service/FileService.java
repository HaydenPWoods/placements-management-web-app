package hpw7.placements.service;

import hpw7.placements.domain.AppUser;
import hpw7.placements.domain.Document;
import hpw7.placements.domain.Provider;
import hpw7.placements.repository.DocumentRepository;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * The <code>FileService</code> handles filesystem interactions for the storing of files related to {@link Document}s.
 * Methods are provided to store and delete files, create specific directories on the local filesystem, and check if
 * a file is "acceptable", according to its file extension.
 */
@Service
public class FileService {

    /**
     * A list of file mime types that should be blocked from being uploaded through the application. See for common
     * MIME types:
     * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types">
     * Common MIME types - HTTP | MDN
     * </a>
     */
    private final List<String> BLOCKED_MIME_TYPES = List.of("application/octet-stream", "application/java-archive",
            "application/x-dosexec", "application/x-ms-installer", "application/x-bat");

    /**
     * A list of allowed image file mime types.
     */
    private final List<String> IMAGE_MIME_TYPES = List.of("image/gif", "image/jpeg", "image/png");

    /**
     * The file separator character, as reported by the system properties. Used for constructing paths - Windows uses \,
     * while Mac and Linux use /.
     */
    public final String FS = System.getProperty("file.separator");

    private final DocumentRepository dRepo;

    public FileService(DocumentRepository dRepo) {
        this.dRepo = dRepo;
    }

    /**
     * Checks the file extension of a given file, and if the file is of a blocked type, as specified in
     * {@code BLOCKED_MIME_TYPES}, determines that the file is not acceptable.
     *
     * @param file The MultipartFile object to check
     * @return {@code true} if the file is acceptable, {@code false} if the file is of a blocked file type.
     */
    public boolean checkFileAcceptable(MultipartFile file) {
        Tika tika = new Tika();
        String typeDetect = tika.detect(file.getOriginalFilename());
        return !BLOCKED_MIME_TYPES.contains(typeDetect);
    }

    /**
     * Checks the file extension of a given file, and if the file is of an accpetable image type, as specified in
     * <code>IMAGE_MIME_TYPES</code>, determines that the file is an (acceptable) image.
     *
     * @param file The MultipartFile object to check
     * @return {@code true} if the file is acceptable, {@code false} if the file is not an acceptable image.
     */
    public boolean checkFileIsImage(MultipartFile file) {
        Tika tika = new Tika();
        String typeDetect = tika.detect(file.getOriginalFilename());
        return IMAGE_MIME_TYPES.contains(typeDetect);
    }

    /**
     * Creates the uploads directory, at /uploads, on the local filesystem, if it does not already exist.
     *
     * @throws IOException File system operations failed in some manner (no permission to check if the directory exists,
     *                     no permission to create a directory)
     */
    public void createUploadsDirectory() throws IOException {
        if (!Files.exists(Path.of(System.getProperty("user.dir") + FS + "uploads"))) {
            Files.createDirectory(Path.of(System.getProperty("user.dir") + FS + "uploads"));
        }
    }

    /**
     * Creates the profile pictures directory, at /uploads/profiles, on the local filesystem, if it does not already
     * exist.
     *
     * @throws IOException File system operations failed in some manner (no permission to check if the directory exists,
     *                     no permission to create a directory)
     */
    public void createProfilePicturesDirectory() throws IOException {
        if (!Files.exists(Path.of(System.getProperty("user.dir") + FS + "uploads" + FS + "profiles"))) {
            Files.createDirectory(Path.of(System.getProperty("user.dir") + FS + "uploads" + FS + "profiles"));
        }
    }

    /**
     * Creates the provider logos directory, at /uploads/logos, on the local filesystem, if it does not already exist.
     *
     * @throws IOException File system operations failed in some manner (no permission to check if the directory exists,
     *                     no permission to create a directory)
     */
    public void createProviderLogosDirectory() throws IOException {
        if (!Files.exists(Path.of(System.getProperty("user.dir") + FS + "uploads" + FS + "logos"))) {
            Files.createDirectory(Path.of(System.getProperty("user.dir") + FS + "uploads" + FS + "logos"));
        }
    }

    /**
     * Creates the events directory, at /events, on the local filesystem, if it does not already exist.
     *
     * @throws IOException File system operations failed in some manner (no permission to check if the directory exists,
     *                     no permission to create a directory)
     */
    public void createEventsDirectory() throws IOException {
        if (!Files.exists(Path.of(System.getProperty("user.dir") + FS + "events"))) {
            Files.createDirectory(Path.of(System.getProperty("user.dir") + FS + "events"));
        }
    }

    /**
     * Creates the exports directory, at /exports, on the local filesystem, if it does not already exist.
     *
     * @throws IOException File system operations failed in some manner (no permission to check if the directory exists,
     *                     no permission to create a directory)
     */
    public void createExportsDirectory() throws IOException {
        if (!Files.exists(Path.of(System.getProperty("user.dir") + FS + "exports"))) {
            Files.createDirectory(Path.of(System.getProperty("user.dir") + FS + "exports"));
        }
    }

    /**
     * Deletes all the files in the events directory ('cleans' the directory), if it exists, and if there is at least
     * 10 files.
     *
     * @param checkOverride Override the file count check - set to {@code true}, will delete all files, regardless of
     *                      the number of files actually in the directory.
     */
    public void cleanEventsDirectory(boolean checkOverride) {
        File dir = new File(System.getProperty("user.dir") + FS + "events");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null && (checkOverride || files.length >= 10)) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

    /**
     * Deletes all the files in the exports directory ('cleans' the directory), if it exists, and if there is at least
     * 10 files.
     *
     * @param checkOverride Override the file count check - set to {@code true}, will delete all files, regardless of
     *                      the number of files actually in the directory.
     */
    public void cleanExportsDirectory(boolean checkOverride) {
        File dir = new File(System.getProperty("user.dir") + FS + "exports");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null && (checkOverride || files.length >= 10)) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }


    /**
     * Stores a given file on the local filesystem, in the /uploads directory relevant to the location the application
     * has been run from. Prevents duplicate file names, by slightly changing the filename where the given filename
     * already exists on the local filesystem.
     *
     * @param file A MultipartFile object
     * @return A {@link Document} object with no owner, referencing the location and details of the stored file.
     * @throws IOException File system operations failed in some manner (no permission to store files, no permission to
     *                     check if files exist)
     */
    public Document uploadFile(MultipartFile file) throws IOException {
        StringBuilder fileName = new StringBuilder(Objects.requireNonNull(file.getOriginalFilename()));
        while (Files.exists(Path.of(System.getProperty("user.dir") + FS + "uploads" + FS + fileName))) {
            fileName.append('_');
        }
        file.transferTo(new File(System.getProperty("user.dir") + FS + "uploads" + FS + fileName.toString()));
        return new Document(fileName.toString(), "File description",
                "\\uploads\\" + fileName.toString());
    }

    /**
     * Stores a given file on the local filesystem, in the /uploads directory relevant to the location the application
     * has been run from, and sets the owner of the Document. Calls {@code uploadFile(file)}.
     *
     * @param file  A MultipartFile object
     * @param owner The {@link AppUser} owner of the to-be-created object.
     * @return A {@link Document} object with the specified owner, referencing the location and details of the stored
     * file.
     * @throws IOException File system operations failed in some manner (no permission to store files, no permission to
     *                     check if files exist)
     */
    public Document uploadFile(MultipartFile file, AppUser owner) throws IOException {
        Document document = uploadFile(file);
        document.setOwner(owner);
        return document;
    }

    /**
     * Stores a given image file on the local filesystem, in the /uploads/profiles directory relevant to the location
     * the application has been run from. Sets the file name to the owner's username.
     *
     * @param imageFile The MultipartFile object to store
     * @param owner     The {@link AppUser} owner of the image
     * @return A String of the location of the file on the local filesystem, relevant to the running directory.
     * @throws IOException File system operations failed in some manner (no permission to delete or store files)
     */
    public String uploadProfilePicture(MultipartFile imageFile, AppUser owner) throws IOException {
        deleteProfilePicture(owner);

        // Store the new profile picture in a "username" file with the relevant extension
        String fileName = owner.getUsername() +
                Objects.requireNonNull(imageFile.getOriginalFilename()).substring(imageFile.getOriginalFilename().lastIndexOf("."));
        imageFile.transferTo(new File(System.getProperty("user.dir") + FS + "uploads" + FS + "profiles" + FS + fileName));

        return "\\uploads\\profiles\\" + fileName;
    }

    /**
     * Stores a given image file on the local filesystem, in the /uploads/logos directory relevant to the location
     * the application has been run from. Sets the file name to the provider's id.
     *
     * @param imageFile The MultipartFile object to store
     * @param provider  The {@link Provider} for which the logo is related to
     * @return A String of the location of the file on the local filesystem, relevant to the running directory.
     * @throws IOException File system operations failed in some manner (no permission to delete or store files)
     */
    public String uploadProviderLogo(MultipartFile imageFile, Provider provider) throws IOException {
        deleteProviderLogo(provider);

        // Store the new logo in a file named with the provider id, with the relevant extension
        String fileName = provider.getId() +
                Objects.requireNonNull(imageFile.getOriginalFilename()).substring(imageFile.getOriginalFilename().lastIndexOf("."));
        imageFile.transferTo(new File(System.getProperty("user.dir") + FS + "uploads" + FS + "logos" + FS + fileName));

        return "\\uploads\\logos\\" + fileName;
    }

    /**
     * Delete a file uploaded as a document, and delete the document entry from the database. Release the document of
     * its' dependencies (e.g. remove it from the list of documents for a placement application) before calling this
     * method.
     *
     * @param document A {@link Document} object
     * @throws IOException Deleting the file from the filesystem has failed
     */
    public void deleteDocument(Document document) throws IOException {
        // Check separator, change if it is not the correct file separator for the current system
        if (!document.getFile().substring(0, 1).equals(FS)) {
            String altPathString = document.getFile().replace('\\', FS.charAt(0));
            Files.deleteIfExists(Path.of(System.getProperty("user.dir") + altPathString));
        } else {
            Files.deleteIfExists(Path.of(System.getProperty("user.dir") + document.getFile()));
        }
        dRepo.delete(document);
    }

    /**
     * Delete any profile pictures stored for the user. Deletes files with the user's username, and a suffix of either
     * '.png', '.jpg', '.jpeg', or '.gif'.
     *
     * @param user The user to delete stored profile pictures for.
     * @throws IOException File system operations failed in some manner (no permission to delete files)
     */
    public void deleteProfilePicture(AppUser user) throws IOException {
        // Clear any previous profile pictures stored for the user
        Files.deleteIfExists(Path.of(System.getProperty("user.dir") + FS + "uploads"+ FS + "profiles" + FS + user.getUsername() + ".png"));
        Files.deleteIfExists(Path.of(System.getProperty("user.dir") + FS + "uploads"+ FS + "profiles" + FS + user.getUsername() + ".jpg"));
        Files.deleteIfExists(Path.of(System.getProperty("user.dir") + FS + "uploads"+ FS + "profiles" + FS + user.getUsername() + ".jpeg"));
        Files.deleteIfExists(Path.of(System.getProperty("user.dir") + FS + "uploads"+ FS + "profiles" + FS + user.getUsername() + ".gif"));
    }

    /**
     * Delete any logos stored for the provider. Deletes files with the provider's ID, and a suffix of either '.png',
     * '.jpg', '.jpeg', or '.gif'.
     *
     * @param provider The provider to delete stored logos for.
     * @throws IOException File system operations failed in some manner (no permission to delete files)
     */
    public void deleteProviderLogo(Provider provider) throws IOException {
        // Clear any previous logos stored for the provider
        Files.deleteIfExists(Path.of(System.getProperty("user.dir") + FS + "uploads" + FS + "logos" + FS + provider.getId() + ".png"));
        Files.deleteIfExists(Path.of(System.getProperty("user.dir") + FS + "uploads" + FS + "logos" + FS + provider.getId() + ".jpg"));
        Files.deleteIfExists(Path.of(System.getProperty("user.dir") + FS + "uploads" + FS + "logos" + FS + provider.getId() + ".jpeg"));
        Files.deleteIfExists(Path.of(System.getProperty("user.dir") + FS + "uploads" + FS + "logos" + FS + provider.getId() + ".gif"));
    }

}
