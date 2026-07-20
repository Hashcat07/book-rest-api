import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { bookApi, borrowApi, reviewApi } from "../api/endpoints";
import { useAuth } from "../context/AuthContext";

export default function BookDetail() {
    const { id } = useParams();
    const { user, isAdmin } = useAuth();

    const [book, setBook] = useState(null);
    const [reviews, setReviews] = useState([]);
    const [error, setError] = useState("");
    const [message, setMessage] = useState("");
    const [form, setForm] = useState({ rating: 5, comment: "" });

    const loadBook = async () => {
        try {
            setBook(await bookApi.get(id));
            setReviews(await reviewApi.list(id));
        } catch {
            setError("Book not found");
        }
    };

    useEffect(() => {
        loadBook();
    }, [id]);

    const handleBorrow = async () => {
        setMessage("");
        try {
            await borrowApi.borrow(Number(id));
            setMessage("Book borrowed successfully");
            loadBook();
        } catch (err) {
            setMessage(err.response?.data || "Could not borrow this book");
        }
    };

    const handleReturn = async () => {
        setMessage("");
        try {
            await borrowApi.returnBook(id);
            setMessage("Book returned successfully");
            loadBook();
        } catch (err) {
            setMessage(err.response?.data || "Could not return this book");
        }
    };

    const handleDelete = async () => {
        if (!window.confirm("Delete this book?")) return;
        try {
            await bookApi.remove(id);
            window.location.href = "/";
        } catch (err) {
            setMessage(err.response?.data || "Could not delete this book");
        }
    };

    const submitReview = async (e) => {
        e.preventDefault();
        setMessage("");
        try {
            await reviewApi.add(id, {
                rating: Number(form.rating),
                comment: form.comment,
            });
            setForm({ rating: 5, comment: "" });
            setMessage("Review added");
            setReviews(await reviewApi.list(id));
        } catch (err) {
            setMessage(err.response?.data || "Could not add review");
        }
    };

    if (error) {
        return (
            <div className="container">
                <div className="message message-error">{error}</div>
            </div>
        );
    }

    if (!book) {
        return (
            <div className="container">
                <p className="text-muted">Loading...</p>
            </div>
        );
    }

    return (
        <div className="container">
            <p><Link to="/">&larr; Back to books</Link></p>

            {message && <div className="message message-success">{String(message)}</div>}

            <div className="card">
                <h1>{book.title}</h1>
                <p className="book-author">{book.author}</p>

                <p>
                    <span className="price">Rs. {book.price}</span>{" "}
                    <span
                        className={
                            book.available ? "badge badge-available" : "badge badge-borrowed"
                        }
                    >
                        {book.available ? "Available" : "Borrowed"}
                    </span>{" "}
                    {book.categoryName && (
                        <span className="badge badge-category">{book.categoryName}</span>
                    )}
                </p>

                {book.createdBy && (
                    <p className="text-muted">Added by {book.createdBy}</p>
                )}

                <div className="button-row">
                    {user && book.available && (
                        <button className="btn" onClick={handleBorrow}>Borrow</button>
                    )}
                    {user && !book.available && (
                        <button className="btn btn-secondary" onClick={handleReturn}>
                            Return
                        </button>
                    )}
                    {isAdmin && (
                        <button className="btn btn-danger" onClick={handleDelete}>
                            Delete
                        </button>
                    )}
                    {!user && <p className="text-muted">Sign in to borrow this book.</p>}
                </div>
            </div>

            <div className="card">
                <h2>Reviews ({reviews.length})</h2>

                {reviews.length === 0 && <p className="text-muted">No reviews yet.</p>}

                {reviews.map((review) => (
                    <div key={review.id} className="review">
                        <div className="review-stars">
                            {"★".repeat(review.rating)}{"☆".repeat(5 - review.rating)}
                        </div>
                        <p className="review-comment">{review.comment}</p>
                    </div>
                ))}
            </div>

            <div className="card">
                <h2>Write a review</h2>

                {user ? (
                    <form onSubmit={submitReview}>
                        <div className="form-group">
                            <label>Rating</label>
                            <select
                                value={form.rating}
                                onChange={(e) => setForm({ ...form, rating: e.target.value })}
                            >
                                {[5, 4, 3, 2, 1].map((n) => (
                                    <option key={n} value={n}>{n} star{n > 1 ? "s" : ""}</option>
                                ))}
                            </select>
                        </div>

                        <div className="form-group">
                            <label>Comment</label>
                            <textarea
                                rows={3}
                                value={form.comment}
                                onChange={(e) => setForm({ ...form, comment: e.target.value })}
                            />
                        </div>

                        <button type="submit" className="btn">Submit review</button>
                    </form>
                ) : (
                    <p className="text-muted">
                        <Link to="/login">Sign in</Link> to leave a review.
                    </p>
                )}
            </div>
        </div>
    );
}
