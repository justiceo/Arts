#!/bin/bash

svnfile=$(<avro.svn)
echo "starting log" > secfile
for line in $svnfile; do
	section=$(echo $line | egrep '<logentry' )
	if [[ ! -z $section ]]
	then
		echo -e "\n**************************************\n" 
	fi

	revision=$( echo $line | awk -F'"' '/revision/{print $2}' )
	if [[ ! -z $revision ]]
	then
		echo "Commit: " $revision
	fi

	sWord=$( echo $line | awk -F'>' '/author/{print $2}' | awk -F'<' '/author/{print $1}')
	if [[ ! -z $sWord ]]
	then
		echo "Author: " $sWord
	fi
	
	date=$( echo $line | awk -F'>' '/date/{print $2}' | awk -F'<' '/date/{print $1}')
	if [[ ! -z $date ]]
	then
		echo -e "Date: " $date "\n" 
	fi
	
	files=$( echo $line | grep 'paths')
        if [[ ! -z $files ]]
        then
		svnLog=$(<svnlog.file)
		for log in $svnLog; do
			start=$( echo $log | egrep '===')
			if [[ ! -z $start ]]
			then
				prvIndex=""	
				echo -e "\n**** starting a new one *******\n"
				posCount=1
				negCount=1
			fi
			posLines=$( echo $log | egrep '^\+' )
			if [[ ! -z $posLines ]]
			then
				(( posCount+=1 ))
			fi
			negLines=$( echo $log | egrep '^\-' )
			if [[ ! -z $negLines ]]
			then
				(( negCount+=1 ))
			fi
			lindex=$( echo $log | awk -F":" '/Index/{print $1}')

			if [[ ! -z $fileName ]]
                        then
                                echo "file name is: " $log
                                $fileName=""
                        fi
			

                        if [[ ! -z $lindex ]]
                        then
				str=$( echo $log | cut -d ":" -f2 )
				(( csum =posCount+negCount ))
				echo $csum " changes; " $posCount " insertions; " $negCount " deletions"
				echo "index is: " $log
				fileName=1
                        fi
			
		done
		
        fi


	mssg=$( echo $line | awk -F' ' '/msg/{print $0}' | cut -d ">" -f2)
        if [[ ! -z $mssg ]]
        then
                echo -e "\tThis is message:  " $line
        fi
	

done
 
