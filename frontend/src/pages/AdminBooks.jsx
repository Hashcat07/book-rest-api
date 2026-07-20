import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { bookApi, categoryApi } from "../api/endpoints";
import { useAuth } from "../context/AuthContext";

export default function AdminBooks() {
    const { isAdmin } = useAuth();
    const navigate = useNavigate();

    const [categories, setCategories] = useState([]);
    const [newCategory, setNewCategory] = useState("");
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [form, setForm] = useState({
        title: "",
        author: "",
        price: "",
        available: true,
        categoryId: "",
    });

    const loadCategories = () => {
        categoryApi.list().then(setCategories).catch(() => setCategories([]));
    };

    useEffect(() => {
        if (isAdmin) {
            loadCategories();
        }
    }, [isAdmin]);

    if (!isAdmin) {
        return (
            <div className="container">
                <div className="message message-error">
                    You need an admin account to open this page.
                </div>
            </div>
        );
    }

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setForm({ ...form, [name]: type === "checkbox" ? checked : value });
    };

    const handleAddCategory = async (e) => {
        e.preventDefault();
        setError("");
        try {
            await categoryApi.create({ name: newCategory });
            setNewCategory("");
            setMessage("Category added");
            loadCategories();
        } catch (err) {
            setError(err.response?.data || "Could not add category");
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");
        setMessage("");
        try {
            const created = await bookApi.create({
                title: form.title,
                author: form.author,
                price: Number(form.price),
                available: form.available,
                categoryId: form.categoryId ? Number(form.categoryId) : null,
            });
            navigate(`/books/${created.id}`);
        } catch (err) {
            setError(err.response?.data || "Could not create the book");
        }
    };

    return (
        <div className="container-small">
            <h1>Add a book</h1>

            {message && <div className="message message-success">{String(message)}</div>}
            {error && <div className="message message-error">{String(error)}</div>}

            <form onSubmit={handleSubmit}>
                <div className="form-group">
                    <label>Title</label>
                    <input name="title" required value={form.title} onChange={handleChange} />
                </div>

                <div className="form-group">
                    <label>Author</label>
                    <input name="author" required value={form.author} onChange={handleChange} />
                </div>

                <div className="form-group">
                    <label>Price</label>
                    <input
                        name="price"
                        type="number"
                        min="1"
                        step="0.01"
                        required
                        value={form.price}
                        onChange={handleChange}
                    />
                </div>

                <div className="form-group">
                    <label>Category</label>
                    <select name="categoryId" value={form.categoryId} onChange={handleChange}>
                        <option value="">No category</option>
                        {categories.map((category) => (
                            <option key={category.id} value={category.id}>
                                {category.name}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="form-group">
                    <label>
                        <input
                            name="available"
                            type="checkbox"
                            checked={form.available}
                            onChange={handleChange}
                            style={{ width: "auto", marginRight: "8px" }}
                        />
                        Available to borrow
                    </label>
                </div>

                <button type="submit" className="btn btn-full">Create book</button>
            </form>

            <div className="card" style={{ marginTop: "25px" }}>
                <h2>New category</h2>
                <form onSubmit={handleAddCategory}>
                    <div className="form-group">
                        <input
                            placeholder="Category name"
                            required
                            value={newCategory}
                            onChange={(e) => setNewCategory(e.target.value)}
                        />
                    </div>
                    <button type="submit" className="btn btn-secondary">Add category</button>
                </form>
            </div>
        </div>
    );
}
