@echo off
echo Starting Smart Expense Manager - Java 21 Spring Boot Full Stack Server...
set JAVA_HOME=C:\Users\DELL\.jdks\jbr-21.0.11
cd /d "%~dp0"
call C:\Users\DELL\.gradle\wrapper\dists\gradle-8.6-bin\afr5mpiioh2wthjmwnkmdsd5w\gradle-8.6\bin\gradle.bat bootRun
pause

