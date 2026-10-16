-- Enable foreign keys
PRAGMA foreign_keys = ON;

-- 1. Categories
CREATE TABLE IF NOT EXISTS categories (
    category_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    code TEXT NOT NULL UNIQUE,
    description TEXT
);

-- 2. Members
CREATE TABLE IF NOT EXISTS members (
    member_id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_staff_id TEXT NOT NULL UNIQUE,
    full_name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    phone TEXT,
    membership_type TEXT NOT NULL CHECK(membership_type IN ('STUDENT', 'FACULTY', 'STAFF')),
    max_books_allowed INTEGER NOT NULL DEFAULT 3,
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'SUSPENDED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Books (Catalog metadata)
CREATE TABLE IF NOT EXISTS books (
    book_id INTEGER PRIMARY KEY AUTOINCREMENT,
    isbn TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    author TEXT NOT NULL,
    publisher TEXT,
    publication_year INTEGER,
    category_id INTEGER NOT NULL,
    total_copies INTEGER NOT NULL DEFAULT 0,
    available_copies INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(category_id)
);

-- 4. Book Copies (Physical inventory)
CREATE TABLE IF NOT EXISTS book_copies (
    copy_id INTEGER PRIMARY KEY AUTOINCREMENT,
    book_id INTEGER NOT NULL,
    barcode TEXT NOT NULL UNIQUE,
    status TEXT NOT NULL DEFAULT 'AVAILABLE' CHECK(status IN ('AVAILABLE', 'BORROWED', 'MAINTENANCE', 'LOST')),
    condition_notes TEXT,
    acquired_date DATE DEFAULT CURRENT_DATE,
    FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE
);

-- 5. Loans (Borrow transactions)
CREATE TABLE IF NOT EXISTS loans (
    loan_id INTEGER PRIMARY KEY AUTOINCREMENT,
    copy_id INTEGER NOT NULL,
    member_id INTEGER NOT NULL,
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    due_date DATE NOT NULL,
    returned_at TIMESTAMP,
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'RETURNED', 'OVERDUE')),
    remarks TEXT,
    FOREIGN KEY (copy_id) REFERENCES book_copies(copy_id),
    FOREIGN KEY (member_id) REFERENCES members(member_id)
);

-- 6. Fines
CREATE TABLE IF NOT EXISTS fines (
    fine_id INTEGER PRIMARY KEY AUTOINCREMENT,
    loan_id INTEGER NOT NULL,
    member_id INTEGER NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK(amount >= 0),
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status TEXT NOT NULL DEFAULT 'UNPAID' CHECK(status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID', 'WAIVED')),
    assessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP,
    FOREIGN KEY (loan_id) REFERENCES loans(loan_id),
    FOREIGN KEY (member_id) REFERENCES members(member_id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_members_id ON members(student_staff_id);
CREATE INDEX IF NOT EXISTS idx_books_isbn ON books(isbn);
CREATE INDEX IF NOT EXISTS idx_books_title ON books(title);
CREATE INDEX IF NOT EXISTS idx_copies_barcode ON book_copies(barcode);
CREATE INDEX IF NOT EXISTS idx_loans_status ON loans(status);
