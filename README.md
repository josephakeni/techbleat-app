# techbleat-app

.env (new file):
VITE_USER_API=http://localhost:8000
VITE_TX_API=http://localhost:8080
VITE_ACTIVITY_API=http://localhost:8001

src/App.jsx lines 3â5 â replaced hardcoded strings with import.meta.env.VITE_* (Vite's way to expose env vars to the browser).

To use different API URLs in a different environment, just create a .env.local or .env.production file with the new values. You should also add .env to .gitignore if this repo has sensitive values in the future.
