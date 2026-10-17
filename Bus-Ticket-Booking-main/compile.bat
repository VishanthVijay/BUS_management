@echo off
del *.class 2>nul
javac -encoding UTF-8 -cp ".;lib/*" Db.java Qr.java Pdf.java Ui.java PayDialog.java LoginFrame.java AdminPages.java BusApp.java
if errorlevel 1 (echo. & echo COMPILE FAILED - see errors above) else (echo. & echo Compiled OK. Now run run.bat)
pause
