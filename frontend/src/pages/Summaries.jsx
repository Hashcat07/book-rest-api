import { useEffect, useState } from "react";
import { bookApi } from "../api/endpoints";

export default function Summaries() {
    const [rows, setRows] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        bookApi
            .summaries()
            .then(setRows)
            .catch(() => setError("Could not load ratings"))
            .finally(() => setLoading(false));
    }, []);

    return (
        <div className="container">
            <h1>Average ratings</h1>
            <p className="text-muted">
                Calculated in the database using AVG() over each book's reviews.
            </p>

            {loading && <p className="text-muted">Loading...</p>}
            {error && <div className="message message-error">{error}</div>}

            {!loading && !error && (
                <table>
                    <thead>
                        <tr>
                            <th>Title</th>
                            <th>Author</th>
                            <th>Rating</th>
                        </tr>
                    </thead>
                    <tbody>
                        {rows.map((row) => (
                            <tr key={row.id}>
                                <td>{row.title}</td>
                                <td>{row.author}</td>
                                <td>
                                    {row.averageRating == null ? (
                                        <span className="text-muted">No reviews yet</span>
                                    ) : (
                                        <span className="rating">
                                            {Number(row.averageRating).toFixed(1)} / 5
                                        </span>
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}
        </div>
    );
}
