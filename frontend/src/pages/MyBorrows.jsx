import { useEffect, useState } from "react";
import { borrowApi } from "../api/endpoints";

export default function MyBorrows() {
    const [records, setRecords] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        borrowApi
            .myHistory()
            .then(setRecords)
            .catch(() => setError("Could not load your borrow history"))
            .finally(() => setLoading(false));
    }, []);

    const formatDate = (value) => {
        if (!value) return "-";
        return new Date(value).toLocaleDateString();
    };

    return (
        <div className="container">
            <h1>My borrows</h1>

            {loading && <p className="text-muted">Loading...</p>}
            {error && <div className="message message-error">{error}</div>}

            {!loading && !error && records.length === 0 && (
                <p className="text-muted">You have not borrowed any books yet.</p>
            )}

            {records.length > 0 && (
                <table>
                    <thead>
                        <tr>
                            <th>Book</th>
                            <th>Borrowed on</th>
                            <th>Returned on</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        {records.map((record) => (
                            <tr key={record.id}>
                                <td>{record.bookTitle}</td>
                                <td>{formatDate(record.borrowDate)}</td>
                                <td>{formatDate(record.returnDate)}</td>
                                <td>
                                    {record.returnDate ? (
                                        <span className="badge badge-available">Returned</span>
                                    ) : (
                                        <span className="badge badge-borrowed">With you</span>
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
