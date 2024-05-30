To run the EXAMCE algorithm, run the following command at the command-prompt, in the directory where you have unzipped the archive.

% runEXAMCEtest.bat <data_input_file> <params_file> <num_clusters_sought>

EXAMPLE:
% runEXAMCEtest.bat testdata\u1060_docs.txt testdata\u1060_properties_k100_1.txt 100


*OUTPUT_FILE_FORMAT:*
The results will be written in a file called <data_input_file>.txt_asgns.txt
The result file will have one line for each vector in the input file, containing the cluster index to which the corresponding vector belongs to. The number in the first line will contain the cluster index of the first vector in the file, the second line will contain the cluster index of the 2nd vector and so on.

*DATA FILE FORMAT*
The first line of the data file is a line containing two numbers (separated by space):
<num_vectors> <vector_dimension>

The rest of the lines contain the data for each vector, in the following format:
dim1,val1 dim2,val2 ...

There is no need to include a dim,value pair if value is zero.

EXAMPLE:
The following set of three vectors in 2 dimensions: {[1,2], [0,1], [3,4]} is represented as follows:
3 2
1,1 2,2
2,1
1,3 2,4

*PARAMS FILE FORMAT*:
The <params_file> is a text file whose lines contain key,value pairs. An example is the file testdata\u1060_properties_k100_1.txt
The important parameters that must be set before each run are the following:
sppsolver,<path-to-scip-executable>
numtries,<number-of-K-Means-runs-to-gather-columns>

The scip.exe executable can be found in the root of the directory where you unzip the EXAMCE archive (same dir as this README file). 
If for some reason the scip.exe file doesn't work for you, there are also a number of other executables starting with "scip" that you may want to try.
