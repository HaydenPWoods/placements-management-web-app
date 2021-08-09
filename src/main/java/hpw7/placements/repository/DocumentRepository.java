package hpw7.placements.repository;

import hpw7.placements.domain.AppUser;
import hpw7.placements.domain.Document;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

/**
 * Provides methods to query data and return records stored in the document table of the database.
 * See: {@link Document}.
 */
public interface DocumentRepository extends CrudRepository<Document, Integer> {

    /**
     * Finds and retrieves a document with the given filename.
     *
     * @param file The filename to retrieve the associated document for. File paths are stored relevant to the current
     *             running directory (e.g. "/uploads/file.png").
     * @return An Optional object containing a document with the given associated filename, or empty otherwise.
     */
    Optional<Document> findByFile(String file);

    /**
     * Finds and retrieves all documents with the given user as the owner.
     *
     * @param owner The owner of the documents to return.
     * @return A list of all documents owned by the given user.
     */
    List<Document> findAllByOwner(AppUser owner);

}
