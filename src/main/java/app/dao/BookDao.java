package app.dao;

import app.model.Book;
import app.model.BookCopy;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface BookDao {
    Optional<Book> findById(int bookId) throws SQLException;
    Optional<Book> findByIsbn(String isbn) throws SQLException;
    List<Book> findAll() throws SQLException;
    List<Book> search(String keyword) throws SQLException;
    void save(Book book) throws SQLException;
    List<BookCopy> findCopiesByBookId(int bookId) throws SQLException;
    Optional<BookCopy> findCopyByBarcode(String barcode) throws SQLException;
    void addCopy(int bookId, String barcode, String condition) throws SQLException;
}
