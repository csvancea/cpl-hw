#!/bin/bash

##
## Update the *.s-model and *.ref file contents
##
#
#for source_file in ./tests/tema3/*.cl; do
#	echo "Running with `basename $source_file`"
#	java -jar Tema3.jar $source_file > ./tests/tema3/`basename $source_file .cl`.s-model
#	if [ "$source_file" = "./tests/tema3/32-big.cl" ]; then 
#		echo 5 | spim -exception_file trap.handler.nogc -file tests/tema3/`basename $source_file .cl`.s-model > tests/tema3/`basename $source_file .cl`.ref
#	else
#		spim -exception_file trap.handler.nogc -file tests/tema3/`basename $source_file .cl`.s-model > tests/tema3/`basename $source_file .cl`.ref
#	fi
#done

##
## Accept both ./tester.sh and ./tester.sh <test num> (eg: ./tester.sh 05)
##

if [ -z "$1" ]; then
  SOURCES="./tests/tema3/*.cl"
else
  SOURCES="./tests/tema3/$1*.cl"
fi

# CLASSPATH=.:/usr/local/lib/antlr-4.8-complete.jar
CLASSPATH=./out/production/Tema/:/usr/local/lib/antlr-4.9.3-complete.jar

passed=0
for source_file in $SOURCES; do
	echo -e "\n`basename $source_file`"
	java -cp $CLASSPATH cool.compiler.Compiler $source_file > ./tests/tema3/`basename $source_file .cl`.s
	if [ "$source_file" = "./tests/tema3/32-big.cl" ]; then
		echo 5 | spim -exception_file trap.handler.nogc -file tests/tema3/`basename $source_file .cl`.s > tests/tema3/`basename $source_file .cl`.out
	else
		spim -exception_file trap.handler.nogc -file tests/tema3/`basename $source_file .cl`.s > tests/tema3/`basename $source_file .cl`.out
	fi

	diff tests/tema3/`basename $source_file .cl`.ref tests/tema3/`basename $source_file .cl`.out

	if [ $? = 0 ]; then
		echo -e "Test passed!\n"
		passed=$(($passed + 1))
	else	
		echo -e "Test failed!\n"
	fi
done

echo -e "\nTotal: $(( $(($passed * 100)) / 32))"
