import { Route, Routes } from "react-router-dom";
import Navbar from "./components/Navbar";
import Books from "./pages/Books";
import BookDetail from "./pages/BookDetail";
import Summaries from "./pages/Summaries";
import MyBorrows from "./pages/MyBorrows";
import AdminBooks from "./pages/AdminBooks";
import Login from "./pages/Login";
import Register from "./pages/Register";

export default function App() {
    return (
        <>
            <Navbar />
            <Routes>
                <Route path="/" element={<Books />} />
                <Route path="/books/:id" element={<BookDetail />} />
                <Route path="/summaries" element={<Summaries />} />
                <Route path="/my-borrows" element={<MyBorrows />} />
                <Route path="/admin" element={<AdminBooks />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route
                    path="*"
                    element={
                        <div className="container">
                            <p className="text-muted">Page not found.</p>
                        </div>
                    }
                />
            </Routes>
        </>
    );
}
