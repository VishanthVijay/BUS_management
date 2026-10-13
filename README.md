# 🚌 Bus Management System

A modern, responsive Bus Management System designed to simplify the management of buses, routes, and passenger bookings. This project combines a clean web interface with Oracle database connectivity examples using Java JDBC and Python ODBC.

## ✨ Features

- **Interactive Dashboard** — Overview of bus operations and key statistics.
- **Bus Management** — Display bus details and add buses to the current page.
- **Passenger Booking** — Booking form with on-screen confirmation.
- **Routes Navigation** — Navigate between different sections of the website.
- **Oracle JDBC Connectivity** — Java program to retrieve bus records from Oracle Database.
- **Python ODBC Connectivity** — Python example for accessing Oracle data through ODBC.
- **Responsive Design** — Clean interface built with HTML, CSS, and JavaScript.

## 🛠️ Technologies Used

| Technology | Purpose |
|---|---|
| HTML5 | Website structure |
| CSS3 | Styling and responsive layout |
| JavaScript | Interactive website functionality |
| Java | Database connectivity example |
| JDBC | Java-to-Oracle connectivity |
| Python | Alternative database connectivity example |
| pyodbc | Python ODBC integration |
| Oracle Database | Relational database |

## 📂 Project Structure

```text
BUS_MANAGEMENT/
├── INDEX.html
├── style.css
├── script.js
├── BusManagementJDBC.java
└── bus_odbc.py
```

- `INDEX.html` — Main website interface.
- `style.css` — Website styling.
- `script.js` — Navigation, bus form, and booking interactions.
- `BusManagementJDBC.java` — Java JDBC example for retrieving bus records.
- `bus_odbc.py` — Python ODBC example for retrieving bus records.

## 🚀 How to Run the Website

### Requirements

- Visual Studio Code
- A modern web browser
- Live Server extension for VS Code (recommended)

### Steps

1. Clone or download this repository.
2. Open the `BUS_MANAGEMENT` folder in VS Code.
3. Install the **Live Server** extension.
4. Open `INDEX.html`.
5. Right-click the file and select **Open with Live Server**.

The website should open in your default browser.

**Note:** The website currently uses sample data. Bus additions and booking confirmations are handled in the browser and are not permanently stored in Oracle.

## 🗄️ Oracle Database Configuration

The database examples expect a `BUS` table containing the following columns:

- `BUS_ID`
- `BUS_NUMBER`
- `ROUTE`
- `DRIVER_NAME`
- `CAPACITY`
- `STATUS`

Example SQL table:

```sql
CREATE TABLE BUS (
    BUS_ID NUMBER PRIMARY KEY,
    BUS_NUMBER VARCHAR2(20) NOT NULL,
    ROUTE VARCHAR2(100) NOT NULL,
    DRIVER_NAME VARCHAR2(100) NOT NULL,
    CAPACITY NUMBER NOT NULL,
    STATUS VARCHAR2(20) DEFAULT 'ACTIVE'
);
```

Example insert:

```sql
INSERT INTO BUS
VALUES (1, 'TN-01-AB-1234', 'Chennai - Vellore',
        'Arun Kumar', 45, 'ACTIVE');

COMMIT;
```

Verify the records:

```sql
SELECT * FROM BUS;
```

Run these commands using SQL*Plus or Oracle SQL Developer in the appropriate database schema.

## ☕ Java JDBC Setup

1. Install the Java Development Kit (JDK).
2. Download a compatible Oracle JDBC driver JAR from Oracle's official website.
3. Configure the connection URL, username, and password in `BusManagementJDBC.java`.
4. Ensure the JDBC driver is available on the Java classpath.
5. Compile and run the program.

Example commands on Windows:

```bat
javac -cp ".;ojdbc11.jar" BusManagementJDBC.java
java -cp ".;ojdbc11.jar" BusManagementJDBC
```

Replace `ojdbc11.jar` with the actual filename of your downloaded driver.

Make sure the Oracle connection URL uses the correct host, port, and service name for your database.

## 🐍 Python ODBC Setup (Optional)

1. Install Python 3.
2. Install the required package:

```bash
python -m pip install pyodbc
```

3. Install and configure the Oracle ODBC driver.
4. Update the driver name and database credentials in `bus_odbc.py`.
5. Execute:

```bash
python bus_odbc.py
```

The Python example is optional and is not required to launch the website.

## 🔐 Security

- Never publish real database passwords or sensitive credentials on GitHub.
- Replace hardcoded credentials with environment variables or a secure configuration method.
- Use a dedicated database user with only the required permissions.
- If a real password has already been published, change it immediately.

## ⚠️ Current Limitations

- Dashboard statistics are demonstration values.
- Adding a bus updates the displayed page only.
- Booking confirmations are not stored permanently.
- The Java and Python examples query the Oracle database independently of the website.
- A backend service is required to connect website forms securely to Oracle and save records.

## 🔮 Future Enhancements

- User login and authentication
- Admin dashboard
- Complete bus CRUD operations
- Persistent passenger bookings
- Route and schedule management
- Search and filtering
- Booking history and reports
- Secure backend integration with Oracle Database

## 📄 License

This project is available for educational and demonstration purposes. Add a license file if you intend to distribute it under specific reuse terms.

---

**Developed as a Bus Management System project using web technologies and Oracle database connectivity examples.**
