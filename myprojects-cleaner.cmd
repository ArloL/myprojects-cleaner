@echo off

IF "%1%"=="selfupdate" (
    call mvn --batch-mode --update-snapshots dependency:copy -Dartifact=de.evosec:myprojects-cleaner:RELEASE:jar -DoutputDirectory=%~dp0 -Dmdep.stripVersion=true
    move /y %~dp0myprojects-cleaner.jar %~dp0myprojects-cleaner
    GOTO :EOF
)

java -jar "%~dp0myprojects-cleaner" %*
