import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { bookApi } from "../api/endpoints";

export default function Books() {
    const [books, setBooks] = useState([]);
    const [search, setSearch] = useState("");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const loadBooks = async (title) => {
        setLoading(true);
        setError("");
        try {
            const page = title ? await bookApi.searchByTitle(title) : await bookApi.list();
            setBooks(page.content || []);
        } catch {
            setError("Could not load books. Is the API running on port 8080?");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadBooks("");
    }, []);

    const handleSearch = (e) => {
        e.preventDefault();
        loadBooks(search.trim());
    };

    return (
        <div className="container">
            <div className="page-header">
                <h1>Books</h1>
                <form className="search-form" onSubmit={handleSearch}>
                    <input
                        placeholder="Search by title"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                    />
                    <button type="submit" className="btn">Search</button>
                </form>
            </div>

            {loading && <p className="text-muted">Loading...</p>}

            {error && <div className="message message-error">{error}</div>}

            {!loading && !error && books.length === 0 && (
                <p className="text-muted">No books found.</p>
            )}

            <div className="book-grid">
                {books.map((book) => (
                    <Link key={book.id} to={`/books/${book.id}`} className="book-card">
                        <h3>{book.title}</h3>
                        <p className="book-author">{book.author}</p>

                        <div className="book-meta">
                            <span className="price">Rs. {book.price}</span>
                            <span
                                className={
                                    book.available
                                        ? "badge badge-available"
                                        : "badge badge-borrowed"
                                }
                            >
                                {book.available ? "Available" : "Borrowed"}
                            </span>
                        </div>

                        {book.categoryName && (
                            <span className="badge badge-category">{book.categoryName}</span>
                        )}
                    </Link>
                ))}
            </div>
        </div>
    );
}
