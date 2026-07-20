import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function Register() {
    const { register } = useAuth();
    const navigate = useNavigate();
    const [form, setForm] = useState({ name: "", email: "", password: "" });
    const [error, setError] = useState("");
    const [busy, setBusy] = useState(false);

    const handleChange = (e) => {
        setForm({ ...form, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");
        setBusy(true);
        try {
            await register(form);
            navigate("/");
        } catch (err) {
            setError(err.response?.data || "Registration failed");
        } finally {
            setBusy(false);
        }
    };

    return (
        <div className="container-small">
            <h1>Create account</h1>

            {error && <div className="message message-error">{String(error)}</div>}

            <form onSubmit={handleSubmit}>
                <div className="form-group">
                    <label>Name</label>
                    <input name="name" required value={form.name} onChange={handleChange} />
                </div>

                <div className="form-group">
                    <label>Email</label>
                    <input
                        name="email"
                        type="email"
                        required
                        value={form.email}
                        onChange={handleChange}
                    />
                </div>

                <div className="form-group">
                    <label>Password</label>
                    <input
                        name="password"
                        type="password"
                        required
                        minLength={6}
                        value={form.password}
                        onChange={handleChange}
                    />
                </div>

                <button type="submit" className="btn btn-full" disabled={busy}>
                    {busy ? "Creating..." : "Create account"}
                </button>
            </form>

            <p className="text-muted" style={{ marginTop: "15px" }}>
                Already registered? <Link to="/login">Sign in</Link>
            </p>
        </div>
    );
}
