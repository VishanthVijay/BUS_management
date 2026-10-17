#!/bin/sh
javac -encoding UTF-8 -cp ".:lib/*" Db.java Qr.java Pdf.java Ui.java PayDialog.java LoginFrame.java AdminPages.java BusApp.java && java --enable-native-access=ALL-UNNAMED -cp ".:lib/*" BusApp
