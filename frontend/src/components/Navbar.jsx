import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function Navbar() {
    const { user, logout, isAdmin } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate("/login");
    };

    return (
        <div className="navbar">
            <div className="navbar-inner">
                <div className="navbar-links">
                    <Link to="/" className="navbar-brand">BookShelf</Link>
                    <Link to="/">Books</Link>
                    <Link to="/summaries">Ratings</Link>
                    {user && <Link to="/my-borrows">My Borrows</Link>}
                    {isAdmin && <Link to="/admin">Add Book</Link>}
                </div>

                <div className="navbar-user">
                    {user ? (
                        <>
                            <span>
                                {user.email}
                                {isAdmin && <span className="badge badge-admin"> ADMIN</span>}
                            </span>
                            <button onClick={handleLogout} className="btn btn-small btn-secondary">
                                Logout
                            </button>
                        </>
                    ) : (
                        <>
                            <Link to="/login">Login</Link>
                            <Link to="/register">Register</Link>
                        </>
                    )}
                </div>
            </div>
        </div>
    );
}
