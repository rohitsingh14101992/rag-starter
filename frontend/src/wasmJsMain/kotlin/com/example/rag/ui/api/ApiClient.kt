package com.example.rag.ui.api

@JsFun("""
function performLogin(email, password, onSuccess, onError) {
    fetch('http://localhost:8081/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email, password: password })
    })
    .then(res => {
        if (res.ok) {
            res.json().then(data => onSuccess(data.token));
        } else {
            res.json().then(data => onError(data.error || "Login failed"));
        }
    })
    .catch(err => onError(err.toString()));
}
""")
external fun performLogin(email: String, password: String, onSuccess: (String) -> Unit, onError: (String) -> Unit)
