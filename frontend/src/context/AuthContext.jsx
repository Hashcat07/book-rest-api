import { createContext, useContext, useEffect, useState } from "react";
import { authApi } from "../api/endpoints";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const stored = localStorage.getItem("user");
        if (stored) {
            setUser(JSON.parse(stored));
        }
        setLoading(false);
    }, []);

    // the API returns { token, email, role } for both login and register
    const persist = (data) => {
        localStorage.setItem("token", data.token);
        const current = { email: data.email, role: data.role };
        localStorage.setItem("user", JSON.stringify(current));
        setUser(current);
        return current;
    };

    const login = async (credentials) => persist(await authApi.login(credentials));

    const register = async (data) => persist(await authApi.register(data));

    const logout = () => {
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        setUser(null);
    };

    const value = {
        user,
        loading,
        login,
        register,
        logout,
        isAdmin: user?.role === "ADMIN",
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
    return useContext(AuthContext);
}
