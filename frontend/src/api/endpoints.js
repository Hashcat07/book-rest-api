import client from "./client";

export const authApi = {
    register: (data) => client.post("/auth/register", data).then((r) => r.data),
    login: (data) => client.post("/auth/login", data).then((r) => r.data),
};

export const bookApi = {
    list: (page = 0, size = 12) =>
        client.get("/books", { params: { page, size } }).then((r) => r.data),
    get: (id) => client.get(`/books/${id}`).then((r) => r.data),
    summaries: () => client.get("/books/summaries").then((r) => r.data),
    searchByTitle: (title) =>
        client.get("/books/title", { params: { title } }).then((r) => r.data),
    create: (data) => client.post("/books", data).then((r) => r.data),
    update: (id, data) => client.put(`/books/${id}`, data).then((r) => r.data),
    remove: (id) => client.delete(`/books/${id}`).then((r) => r.data),
};

export const reviewApi = {
    list: (bookId) => client.get(`/books/${bookId}/reviews`).then((r) => r.data),
    add: (bookId, data) =>
        client.post(`/books/${bookId}/reviews`, data).then((r) => r.data),
};

export const borrowApi = {
    borrow: (bookId) => client.post("/borrow", { bookId }).then((r) => r.data),
    returnBook: (bookId) => client.post(`/return/${bookId}`).then((r) => r.data),
    myHistory: () => client.get("/borrow/me").then((r) => r.data),
};

export const categoryApi = {
    list: () => client.get("/categories").then((r) => r.data),
    create: (data) => client.post("/categories", data).then((r) => r.data),
};
