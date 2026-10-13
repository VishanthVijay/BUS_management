import pyodbc

connection = pyodbc.connect(
    "DRIVER={Oracle in OraDB21Home1};"
    "DBQ=localhost:1521/XE;"
    "UID=system;"
    "PWD=password;"
)

cursor = connection.cursor()

cursor.execute("""
    SELECT BUS_ID,
           BUS_NUMBER,
           ROUTE,
           DRIVER_NAME,
           CAPACITY,
           STATUS
    FROM BUS
""")

print("BUS MANAGEMENT SYSTEM")
print("----------------------")

for row in cursor.fetchall():

    print(
        row.BUS_ID,
        "|",
        row.BUS_NUMBER,
        "|",
        row.ROUTE,
        "|",
        row.DRIVER_NAME,
        "|",
        row.CAPACITY,
        "|",
        row.STATUS
    )

cursor.close()
connection.close()