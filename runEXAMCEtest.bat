REM run as:
REM runEXAMCETest.bat <docs_file> <props_file> <K>
REM Example:
REM runEXAMCETest.bat testdata\r11849_docs.txt testdata\r11849_properties_k100_1.txt 100
java -Xmx10g -cp dist\codocup-rte.jar clustering.ClusteringTester2 %1 %2 dummy1 dummy2 clustering.KMeansSqrGuarantClusterer %3

