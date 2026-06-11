import os

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, EmailStr
from sqlalchemy import create_engine, text

DATABASE_URL = os.getenv("DATABASE_URL")
FRONTEND_ORIGIN = os.getenv("FRONTEND_ORIGIN", "http://localhost:3000")

if not DATABASE_URL:
    raise RuntimeError("DATABASE_URL is required")

engine = create_engine(f"postgresql+psycopg2://{DATABASE_URL.split('://', 1)[1]}")

app = FastAPI(title="Techbleat Global Bank - User Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=[FRONTEND_ORIGIN, "http://127.0.0.1:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class UserCreate(BaseModel):
    id: str
    full_name: str
    email: EmailStr


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/users")
def create_user(user: UserCreate):
    with engine.begin() as conn:
        existing = conn.execute(
            text("SELECT id FROM users WHERE id = :id OR email = :email"),
            {"id": user.id, "email": user.email},
        ).fetchone()

        if existing:
            raise HTTPException(status_code=400, detail="User ID or email already exists")

        conn.execute(
            text(
                '''
                INSERT INTO users (id, full_name, email)
                VALUES (:id, :full_name, :email)
                '''
            ),
            {"id": user.id, "full_name": user.full_name.title(), "email": user.email},
        )

        conn.execute(
            text(
                '''
                INSERT INTO accounts (user_id, balance)
                VALUES (:user_id, 0)
                '''
            ),
            {"user_id": user.id},
        )

    return {"message": "User created successfully", "user_id": user.id}


@app.get("/users")
def list_users():
    with engine.begin() as conn:
        rows = conn.execute(
            text(
                '''
                SELECT id, full_name, email, created_at
                FROM users
                ORDER BY created_at DESC
                '''
            )
        ).mappings().all()
        return [dict(row) for row in rows]
