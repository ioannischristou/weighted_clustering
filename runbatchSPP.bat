echo "Running SPP(scp3038,500) with numtries=15"
java -Xmx1000m -cp .;.\classes;.\lib\colt.jar;.\lib\concurrent.jar clustering.ClusteringTester2 testdata\u1060_docs.txt testdata\u1060_properties_k40_1.txt dummy1 dummy2 clustering.KMeansSqrGuarantClusterer
REM echo "Running SPP(scp3038,20) with numtries=20"
REM java -Xmx640m -cp .;.\classes;.\lib\colt.jar;.\lib\concurrent.jar clustering.ClusteringTester2 testdata\pcb3038_docs.txt testdata\scp3038_properties_k20_20.txt dummy1 dummy2 clustering.KMeansSqrGuarantClusterer
REM echo "Running SPP(scp3038,20) with numtries=30"
REM java -Xmx640m -cp .;.\classes;.\lib\colt.jar;.\lib\concurrent.jar clustering.ClusteringTester2 testdata\pcb3038_docs.txt testdata\scp3038_properties_k20_30.txt dummy1 dummy2 clustering.KMeansSqrGuarantClusterer
REM echo "Running SPP(scp3038,20) with numtries=40"
REM java -Xmx640m -cp .;.\classes;.\lib\colt.jar;.\lib\concurrent.jar clustering.ClusteringTester2 testdata\pcb3038_docs.txt testdata\scp3038_properties_k20_40.txt dummy1 dummy2 clustering.KMeansSqrGuarantClusterer
REM echo "Running SPP(scp3038,20) with numtries=50"
REM java -Xmx640m -cp .;.\classes;.\lib\colt.jar;.\lib\concurrent.jar clustering.ClusteringTester2 testdata\pcb3038_docs.txt testdata\scp3038_properties_k20_50.txt dummy1 dummy2 clustering.KMeansSqrGuarantClusterer
REM echo "Running SPP(scp3038,20) with numtries=100"
REM java -Xmx640m -cp .;.\classes;.\lib\colt.jar;.\lib\concurrent.jar clustering.ClusteringTester2 testdata\pcb3038_docs.txt testdata\scp3038_properties_k20_100.txt dummy1 dummy2 clustering.KMeansSqrGuarantClusterer
REM echo "Running SPP(scp3038,20) with numtries=150"
REM java -Xmx640m -cp .;.\classes;.\lib\colt.jar;.\lib\concurrent.jar clustering.ClusteringTester2 testdata\pcb3038_docs.txt testdata\scp3038_properties_k20_150.txt dummy1 dummy2 clustering.KMeansSqrGuarantClusterer
